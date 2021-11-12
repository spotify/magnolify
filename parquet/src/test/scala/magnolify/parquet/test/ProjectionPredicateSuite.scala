/*
 * Copyright 2021 Spotify AB.
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
import org.apache.parquet.io.InvalidRecordException
import org.apache.parquet.io.api.Binary

import scala.reflect.ClassTag

class ProjectionPredicateSuite extends MagnolifySuite {
  private val records = (1 to 100).toList.map { i =>
    val j = i + 100
    Wide(
      i % 2 == 0,
      i % 3 == 0,
      i,
      j,
      i.toString,
      j.toString,
      if (i % 2 == 0) Some(i) else None,
      (1 to i).toList,
      Instant.ofEpochMilli(i),
      WideInner(s"s$i", if (i % 2 == 0) Some(s"o$i") else None, (1 to i).map("r" + _).toList)
    )
  }

  private val bytes = {
    val pt = ParquetType[Wide]
    val out = new TestOutputFile
    val writer = pt.writeBuilder(out).build()
    records.foreach(writer.write)
    writer.close()
    out.getBytes
  }

  private def testProjection[T: ClassTag](
    fn: Wide => T
  )(implicit rt: ParquetType[T], eq: Eq[List[T]]): Unit =
    test(s"Projection.${className[T]}") {
      val in = new TestInputFile(bytes)
      val reader = rt.readBuilder(in).build()
      var r = reader.read()
      val b = List.newBuilder[T]
      while (r != null) {
        b += r
        r = reader.read()
      }
      reader.close()
      eq.eqv(b.result(), records.map(fn))
    }

  {
    testProjection[ProjectionNested1](t => ProjectionNested1(t.s1, ProjectionInner1(t.inner.s)))
    testProjection[ProjectionNested2](t => ProjectionNested2(t.s1, ProjectionInner2(t.inner.o)))
    testProjection[ProjectionNested3](t => ProjectionNested3(t.s1, ProjectionInner3(t.inner.r)))
    testProjection[ProjectionSubset](t => ProjectionSubset(t.b1, t.i1, t.s1, t.inner))
    testProjection[ProjectionOrdering1](t => ProjectionOrdering1(t.s1, t.i1, t.b1))
    testProjection[ProjectionOrdering2](t => ProjectionOrdering2(t.b2, t.b1, t.i2, t.i1))
    testProjection[ProjectionLogical](t => ProjectionLogical(t.i.toEpochMilli))
    testProjection[ProjectionNestedOptional1](t =>
      ProjectionNestedOptional1(t.s1, Some(ProjectionInnerOptional1(Some(t.inner.s))))
    )
  }

  private def testBadProjection[T: ClassTag](implicit rt: ParquetType[T]): Unit =
    test(s"BadProjection.${className[T]}") {
      val in = new TestInputFile(bytes)
      val reader = rt.readBuilder(in).build()
      intercept[InvalidRecordException](reader.read())
    }

  {
    testBadProjection[ProjectionBadName]
    testBadProjection[ProjectionBadType]
    testBadProjection[ProjectionBadRepetition1]
    testBadProjection[ProjectionBadRepetition2]
    testBadProjection[ProjectionBadRepetition3]
    testBadProjection[ProjectionBadRepetition4]
    testBadProjection[ProjectionBadRepetition5]
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
    val colI1 = FilterApi.intColumn("i1")
    val colI2 = FilterApi.intColumn("i2")

    val pLtEq = FilterApi.ltEq(colI1, jl.Integer.valueOf(10))
    val eLtEq = records.filter(_.i1 <= 10)
    testPredicate[Wide]("ltEq", pLtEq, eLtEq)

    val pGtEq = FilterApi.gtEq(colI1, jl.Integer.valueOf(90))
    val eGtEq = records.filter(_.i1 >= 90)
    testPredicate[Wide]("gtEq", pGtEq, eGtEq)

    val pOr = FilterApi.or(pLtEq, pGtEq)
    val eOr = records.filter(t => t.i1 <= 10 || t.i1 >= 90)
    testPredicate[Wide]("or", pOr, eOr)

    val pAnd = FilterApi.and(
      FilterApi.gtEq(colI1, jl.Integer.valueOf(40)),
      FilterApi.ltEq(colI1, jl.Integer.valueOf(60))
    )
    val eAnd = records.filter(t => t.i1 >= 40 && t.i1 <= 60)
    testPredicate[Wide]("and", pAnd, eAnd)

    val pMulti = FilterApi.or(
      FilterApi.and(
        FilterApi.gtEq(colI1, jl.Integer.valueOf(45)),
        FilterApi.ltEq(colI1, jl.Integer.valueOf(55))
      ),
      FilterApi.or(
        FilterApi.eq(colI2, jl.Integer.valueOf(115)),
        FilterApi.eq(colI2, jl.Integer.valueOf(125))
      )
    )
    val eMulti = records.filter(t => (t.i1 >= 45 && t.i1 <= 55) || (t.i2 == 115 || t.i2 == 125))
    testPredicate[Wide]("multi", pMulti, eMulti)

    val pOpt1 = FilterApi.gtEq(FilterApi.intColumn("o"), jl.Integer.valueOf(10))
    val eOpt1 = records.filter(_.o.exists(_ >= 10))
    testPredicate[Wide]("opt1", pOpt1, eOpt1)

    // Predicate on missing OPTIONAL field
    val pOpt2 = FilterApi.eq(FilterApi.intColumn("o"), jl.Integer.valueOf(15))
    val eOpt2 = records.filter(_.o.contains(15))
    testPredicate[Wide]("opt2", pOpt2, eOpt2)

    val eInner1 = FilterApi.eq(FilterApi.binaryColumn("inner.s"), Binary.fromString("s50"))
    val oInner1 = records.filter(_.inner.s == "s50")
    testPredicate[Wide]("inner1", eInner1, oInner1)

    val eInner2 = FilterApi.eq(FilterApi.binaryColumn("inner.o"), Binary.fromString("o50"))
    val oInner2 = records.filter(_.inner.o.contains("o50"))
    testPredicate[Wide]("inner2", eInner2, oInner2)

    val pSubset1 = pLtEq
    val eSubset1 = eLtEq.map(t => ProjectionSubset(t.b1, t.i1, t.s1, t.inner))
    testPredicate[ProjectionSubset]("subset1", pSubset1, eSubset1)

    // Predicate on field not in projection
    val pSubset2 = pMulti
    val eSubset2 = eMulti.map(t => ProjectionSubset(t.b1, t.i1, t.s1, t.inner))
    testPredicate[ProjectionSubset]("subset2", pSubset2, eSubset2)
  }

  private def testBadPredicate(name: String, predicate: FilterPredicate): Unit =
    test(s"BadPredicate.$name") {
      val pt = ParquetType[Wide]
      val in = new TestInputFile(bytes)
      val reader = pt.readBuilder(in).withFilter(FilterCompat.get(predicate)).build()
      intercept[IllegalArgumentException](reader.read())
    }

  {
    // FIXME: Parquet does not validate non-existent fields
    // val badName = FilterApi.eq(FilterApi.intColumn("i3"), jl.Integer.valueOf(0))
    // testBadPredicate[Wide]("name", badName)

    val badType = FilterApi.eq(FilterApi.intColumn("b1"), jl.Integer.valueOf(0))
    testBadPredicate("type", badType)

    val badRepetition = FilterApi.eq(FilterApi.intColumn("r"), jl.Integer.valueOf(0))
    testBadPredicate("repetition", badRepetition)
  }
}

case class WideInner(s: String, o: Option[String], r: List[String])
case class Wide(
  b1: Boolean,
  b2: Boolean,
  i1: Int,
  i2: Int,
  s1: String,
  s2: String,
  o: Option[Int],
  r: List[Int],
  i: Instant,
  inner: WideInner
)

case class ProjectionInner1(s: String)
case class ProjectionInner2(o: Option[String])
case class ProjectionInner3(r: List[String])
case class ProjectionNested1(s1: String, inner: ProjectionInner1)
case class ProjectionNested2(s1: String, inner: ProjectionInner2)
case class ProjectionNested3(s1: String, inner: ProjectionInner3)

case class ProjectionSubset(b1: Boolean, i1: Int, s1: String, inner: WideInner)
case class ProjectionOrdering1(s1: String, i1: Int, b1: Boolean)
case class ProjectionOrdering2(b2: Boolean, b1: Boolean, i2: Int, i1: Int)
case class ProjectionLogical(i: Long)

case class ProjectionBadName(b1: Boolean, i3: Int)
case class ProjectionBadType(b1: Boolean, i1: Long)
case class ProjectionBadRepetition1(b1: Boolean, i1: List[Int])
case class ProjectionBadRepetition2(b1: Boolean, o: Int)
case class ProjectionBadRepetition3(b1: Boolean, o: List[Int])
case class ProjectionBadRepetition4(b1: Boolean, r: Int)
case class ProjectionBadRepetition5(b1: Boolean, r: Option[Int])

case class ProjectionInnerOptional1(s: Option[String])
case class ProjectionNestedOptional1(s1: String, inner: Option[ProjectionInnerOptional1])
