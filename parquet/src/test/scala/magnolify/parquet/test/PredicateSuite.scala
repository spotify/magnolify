/*
 * Copyright 2022 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package magnolify.parquet.test

import java.{lang => jl}
import java.time.Instant
import cats._
import magnolify.cats.auto._
import magnolify.parquet._
import magnolify.parquet.logical.millis._
import magnolify.test._
import magnolify.test.Time._
import org.apache.parquet.filter2.compat.FilterCompat
import org.apache.parquet.filter2.predicate.{FilterApi, FilterPredicate}
import org.apache.parquet.io.api.Binary

import scala.reflect.ClassTag

class PredicateSuite extends MagnolifySuite {
  implicit val baEq: cats.Eq[Array[Byte]] = (x: Array[Byte], y: Array[Byte]) => x.diff(y).isEmpty
  implicit val pfDecimal = ParquetField.decimal32(9)
  implicit val projectionT = ParquetType[Projection]
  implicit val projectionSubsetT = ParquetType[ProjectionSmall]

  private val records = (1 to 100).toList.map { i =>
    Projection(
      i % 2 == 0,
      i,
      i.toString,
      (0 to i).toList,
      if (i % 2 == 0) Some(i) else None,
      Instant.ofEpochMilli(i),
      ProjectionInner(i.toString, if (i % 2 == 0) Some(s"o$i") else None),
      i.toByte,
      i.toLong,
      i.toFloat,
      i.toDouble,
      Array.fill(i)(i.toByte),
      BigDecimal(i)
    )
  }
  private val bytes = {
    val pt = ParquetType[Projection]
    val out = new TestOutputFile
    val writer = pt.writeBuilder(out).build()
    records.foreach(writer.write)
    writer.close()
    out.getBytes
  }

  private def testPredicate[T: ClassTag](
    name: String,
    predicate: FilterPredicate,
    expected: List[T]
  )(implicit pt: ParquetType[T], eq: Eq[List[T]]): Unit =
    test(s"Predicate.${className[T]}.$name") {
      val in = new TestInputFile(bytes)
      val reader = pt.readBuilder(in).withFilter(FilterCompat.get(predicate)).build()
      var r = reader.read()
      val b = List.newBuilder[T]
      while (r != null) {
        b += r
        r = reader.read()
      }
      reader.close()
      eq.eqv(b.result(), expected)
    }

  {
    testPredicate[Projection](
      "customStr",
      Predicate.onField[String]("s1")(_.toInt % 5 == 0),
      records.filter(_.s1.toInt % 5 == 0)
    )
    testPredicate[Projection](
      "customBool",
      Predicate.onField[Boolean]("b1")(identity),
      records.filter(_.b1)
    )
    testPredicate[Projection](
      "customInt",
      Predicate.onField[Int]("i1")(_ % 2 == 0),
      records.filter(_.i1 % 2 == 0)
    )
    testPredicate[Projection](
      "customInstant",
      Predicate.onField[Instant]("i2")(_.toEpochMilli % 2 == 0),
      records.filter(_.i2.toEpochMilli % 2 == 0)
    )
    testPredicate[Projection](
      "customNested",
      Predicate.onField[String]("inner.s")(_.toInt % 5 == 0),
      records.filter(_.inner.s.toInt % 5 == 0)
    )
    testPredicate[Projection](
      "customOpt",
      Predicate.onField[Int]("o")(_ % 5 == 0),
      records.filter(_.o.exists(_ % 5 == 0))
    )
    testPredicate[Projection](
      "customLong",
      Predicate.onField[Long]("l2")(_ % 2 == 0),
      records.filter(_.l2 % 2 == 0)
    )
    testPredicate[Projection](
      "customFloat",
      Predicate.onField[Float]("f")(_ >= 45.5f),
      records.filter(_.f >= 50.0f)
    )
    testPredicate[Projection](
      "customDouble",
      Predicate.onField[Double]("d")(_ >= 50.0),
      records.filter(_.d >= 50.0)
    )
    testPredicate[Projection](
      "customByteArray",
      Predicate.onField[Array[Byte]]("ba")(_.length % 2 == 0),
      records.filter(_.ba.length % 2 == 0)
    )
    testPredicate[Projection](
      "customBigDecimal",
      Predicate.onField[BigDecimal]("bd")(_.bigDecimal.intValue() % 2 == 0),
      records.filter(_.bd.bigDecimal.intValue() % 2 == 0)
    )
  }

  {
    val colI1 = FilterApi.intColumn("i1")
    val colI2 = FilterApi.longColumn("i2")

    val pLtEq = FilterApi.ltEq(colI1, jl.Integer.valueOf(10))
    val eLtEq = records.filter(_.i1 <= 10)
    testPredicate[Projection]("ltEq", pLtEq, eLtEq)

    val pGtEq = FilterApi.gtEq(colI1, jl.Integer.valueOf(90))
    val eGtEq = records.filter(_.i1 >= 90)
    testPredicate[Projection]("gtEq", pGtEq, eGtEq)

    val pOr = FilterApi.or(pLtEq, pGtEq)
    val eOr = records.filter(t => t.i1 <= 10 || t.i1 >= 90)
    testPredicate[Projection]("or", pOr, eOr)

    val pAnd = FilterApi.and(
      FilterApi.gtEq(colI1, jl.Integer.valueOf(40)),
      FilterApi.ltEq(colI1, jl.Integer.valueOf(60))
    )
    val eAnd = records.filter(t => t.i1 >= 40 && t.i1 <= 60)
    testPredicate[Projection]("and", pAnd, eAnd)

    val pMulti = FilterApi.or(
      FilterApi.and(
        FilterApi.gtEq(colI1, jl.Integer.valueOf(45)),
        FilterApi.ltEq(colI1, jl.Integer.valueOf(55))
      ),
      FilterApi.or(
        FilterApi.eq(colI2, jl.Long.valueOf(115)),
        FilterApi.eq(colI2, jl.Long.valueOf(125))
      )
    )
    val eMulti = records.filter(t => (t.i1 >= 45 && t.i1 <= 55) || (t.i2 == 115 || t.i2 == 125))
    testPredicate[Projection]("multi", pMulti, eMulti)

    val pOpt1 = FilterApi.gtEq(FilterApi.intColumn("o"), jl.Integer.valueOf(10))
    val eOpt1 = records.filter(_.o.exists(_ >= 10))
    testPredicate[Projection]("opt1", pOpt1, eOpt1)

    // Predicate on missing OPTIONAL field
    val pOpt2 = FilterApi.eq(FilterApi.intColumn("o"), jl.Integer.valueOf(15))
    val eOpt2 = records.filter(_.o.contains(15))
    testPredicate[Projection]("opt2", pOpt2, eOpt2)

    val eInner1 = FilterApi.eq(FilterApi.binaryColumn("inner.s"), Binary.fromString("s50"))
    val oInner1 = records.filter(_.inner.s == "s50")
    testPredicate[Projection]("inner1", eInner1, oInner1)

    val eInner2 = FilterApi.eq(FilterApi.binaryColumn("inner.o"), Binary.fromString("o50"))
    val oInner2 = records.filter(_.inner.o.contains("o50"))
    testPredicate[Projection]("inner2", eInner2, oInner2)

    val pSubset1 = pLtEq
    val eSubset1 = eLtEq.map(t => ProjectionSmall(t.b1, t.i1, t.s1, t.inner))
    testPredicate[ProjectionSmall]("subset1", pSubset1, eSubset1)

    // Predicate on field not in projection
    val pSubset2 = pMulti
    val eSubset2 = eMulti.map(t => ProjectionSmall(t.b1, t.i1, t.s1, t.inner))
    testPredicate[ProjectionSmall]("subset2", pSubset2, eSubset2)
  }

  private def testBadPredicate(name: String, predicate: FilterPredicate): Unit =
    test(s"BadPredicate.$name") {
      val pt = ParquetType[Projection]
      val in = new TestInputFile(bytes)
      val reader = pt.readBuilder(in).withFilter(FilterCompat.get(predicate)).build()
      intercept[IllegalArgumentException](reader.read())
    }

  {
    // FIXME: Parquet does not validate non-existent fields
    // val badName = FilterApi.eq(FilterApi.intColumn("i3"), jl.Integer.valueOf(0))
    // testBadPredicate[Projection]("name", badName)

    val badType = FilterApi.eq(FilterApi.intColumn("b1"), jl.Integer.valueOf(0))
    testBadPredicate("type", badType)

    val badRepetition = FilterApi.eq(FilterApi.intColumn("l"), jl.Integer.valueOf(0))
    testBadPredicate("repetition", badRepetition)

    val badType2 = Predicate.onField[Int]("b1")(_ % 2 == 0)
    testBadPredicate("type2", badType2)
  }
}

case class Projection(
  b1: Boolean,
  i1: Int,
  s1: String,
  l: List[Int],
  o: Option[Int],
  i2: Instant,
  inner: ProjectionInner,
  b2: Byte,
  l2: Long,
  f: Float,
  d: Double,
  ba: Array[Byte],
  bd: BigDecimal
)
case class ProjectionInner(s: String, o: Option[String])
case class ProjectionSmall(b1: Boolean, i1: Int, s1: String, inner: ProjectionInner)
