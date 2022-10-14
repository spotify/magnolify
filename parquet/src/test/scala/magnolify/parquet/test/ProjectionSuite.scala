/*
 * Copyright 2022 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package magnolify.parquet.test

import java.time.Instant

import cats._
import magnolify.cats.auto._
import magnolify.parquet._
import magnolify.parquet.logical.millis._
import magnolify.test._
import org.apache.parquet.io.InvalidRecordException

import scala.reflect.ClassTag

class ProjectionSuite extends MagnolifySuite {
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
      Instant.ofEpochMilli(i.toLong),
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
    testProjection[ProjectionReorderedNested1](t =>
      ProjectionReorderedNested1(ProjectionReorderedInner1(t.inner.r, t.inner.s), t.s1)
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

case class ProjectionReorderedInner1(r: List[String], s: String)
case class ProjectionReorderedNested1(inner: ProjectionReorderedInner1, s1: String)
