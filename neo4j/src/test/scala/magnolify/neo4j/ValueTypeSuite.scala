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

package magnolify.neo4j

import cats.Eq
import magnolify.test.MagnolifySuite
import magnolify.test.Simple._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.shared.CaseMapper
import org.scalacheck.{Arbitrary, Prop}

import java.net.URI
import scala.reflect.ClassTag

class ValueTypeSuite extends MagnolifySuite {

  private def test[T: Arbitrary: ClassTag](implicit t: ValueType[T], eq: Eq[T]): Unit = {
    val tpe = ensureSerializable(t)
    property(className[T]) {
      Prop.forAll { t: T =>
        val v = tpe(t)
        val copy = tpe(v)
        eq.eqv(t, copy)
      }
    }
  }

  test[Integers]
  test[Floats]
  test[Required]
  test[Nullable]
  test[Repeated]

  {
    import Collections._
    test[Collections]
    test[MoreCollections]
  }

  {
    import unsafe._
    import Enums._
    import UnsafeEnums._
    test[Enums]
    test[UnsafeEnums]
  }

  {
    import Custom._
    implicit val nvUri: ValueField[URI] = ValueField.from[String](URI.create)(_.toString)
    test[Custom]
  }

  test("AnyVal") {
    implicit val vt: ValueType[HasValueClass] = ValueType[HasValueClass]
    test[HasValueClass]

    val record = vt(HasValueClass(ValueClass("String")))
    assert(record.get("vc").asString() == "String")
  }

  test("LowerCamel mapping") {
    implicit val vt: ValueType[LowerCamel] = ValueType[LowerCamel](CaseMapper(_.toUpperCase))
    test[LowerCamel]

    val fields = LowerCamel.fields.map(_.toUpperCase)

    val record = vt(LowerCamel.default)
    assert(!fields.map(record.get).exists(_.isNull))
    assert(!record.get("INNERFIELD").get("INNERFIRST").isNull)
  }
}
