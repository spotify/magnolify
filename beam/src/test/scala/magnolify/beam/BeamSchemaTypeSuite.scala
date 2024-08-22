/*
 * Copyright 2024 Spotify AB
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

package magnolify.beam

import cats.*
import magnolify.cats.auto.*
import magnolify.cats.TestEq.*
import magnolify.scalacheck.auto.*
import magnolify.scalacheck.TestArbitrary.*
import magnolify.test.MagnolifySuite
import magnolify.test.Simple.*
import org.joda.time as joda
import org.scalacheck.{Arbitrary, Gen, Prop}

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import java.util.UUID
import scala.reflect.ClassTag

class BeamSchemaTypeSuite extends MagnolifySuite {
  private def test[T: Arbitrary: ClassTag](implicit
    bst: BeamSchemaType[T],
    eq: Eq[T]
  ): Unit = {
    // Ensure serializable even after evaluation of `schema`
    bst.schema: Unit
    ensureSerializable(bst)

    property(className[T]) {
      Prop.forAll { (t: T) =>
        val converted = bst.apply(t)
        val roundtripped = bst.apply(converted)
        Prop.all(eq.eqv(t, roundtripped))
      }
    }
  }

  test[Integers]
  test[Floats]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]
  test[Collections]
  test[MoreCollections]

  test[Maps]
  test[Logical]

  {
    import magnolify.beam.unsafe._
    import magnolify.shared.TestEnumType._
    test[Enums]
    test[UnsafeEnums]
  }

  implicit val arbBigDecimal: Arbitrary[BigDecimal] =
    Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(BigDecimal(_)))
  test[Decimal]

  {
    import magnolify.beam.logical.millis._
    test[Time]
    test[Joda]
  }

//  {
//    //   FIXME need special Eq instances that are lossy
//    import magnolify.beam.logical.micros._
//    test[Time]
//    test[Joda]
//  }
//
//  {
////   FIXME need special Eq instances that are lossy
//    import magnolify.beam.logical.nanos._
//    test[Time]
//    test[Joda]
//  }

//  {
//    implicit val bst: BeamSchemaType[LowerCamel] =
//      BeamSchemaType[LowerCamel](CaseMapper(_.toUpperCase))
//    test[LowerCamel]
//
//    test("LowerCamel mapping") {
//      val schema = bst.schema
//      // FIXME
//    }
//  }

}

case class Decimal(bd: BigDecimal, bdo: Option[BigDecimal])
case class Logical(
  u: UUID,
  uo: Option[UUID],
  ul: List[UUID],
  ulo: List[Option[UUID]]
)

case class Time(
  i: Instant,
  d: LocalDate,
  dt: LocalDateTime,
  t: LocalTime
)
case class Joda(
  i: joda.Instant,
  dt: joda.DateTime,
  lt: joda.LocalTime,
  d: joda.Duration
)
case class Maps(
  ms: Map[String, String],
  mi: Map[Int, Int],
  mso: Map[Option[String], Option[String]],
  ml: Map[UUID, UUID],
  mlo: Map[Option[UUID], Option[UUID]]
)
