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

package magnolify.scalacheck

import magnolify.scalacheck.semiauto.CogenDerivation
import magnolify.test.ADT._
import magnolify.test.JavaEnums
import magnolify.test.Simple._
import org.scalacheck.Cogen

import java.net.URI

object TestCogen {
  // enum
  implicit lazy val coJavaEnum: Cogen[JavaEnums.Color] = Cogen(_.ordinal().toLong)
  implicit lazy val coScalaEnums: Cogen[ScalaEnums.Color.Type] = Cogen(_.id.toLong)

  // ADT
  implicit lazy val coNode: Cogen[Node] = CogenDerivation[Node]
  implicit lazy val coGNode: Cogen[GNode[Int]] = CogenDerivation[GNode[Int]]
  implicit lazy val coShape: Cogen[Shape] = CogenDerivation[Shape]
  implicit lazy val coColor: Cogen[Color] = CogenDerivation[Color]
  implicit lazy val coPerson: Cogen[Person] = CogenDerivation[Person]

  // simple
  implicit lazy val coIntegers: Cogen[Integers] = CogenDerivation[Integers]
  implicit lazy val coFloats: Cogen[Floats] = CogenDerivation[Floats]
  implicit lazy val coNumbers: Cogen[Numbers] = CogenDerivation[Numbers]
  implicit lazy val coRequired: Cogen[Required] = CogenDerivation[Required]
  implicit lazy val coNullable: Cogen[Nullable] = CogenDerivation[Nullable]
  implicit lazy val coRepeated: Cogen[Repeated] = CogenDerivation[Repeated]
  implicit lazy val coNested: Cogen[Nested] = CogenDerivation[Nested]
  implicit lazy val coCollections: Cogen[Collections] = CogenDerivation[Collections]
  // implicit lazy val coMoreCollections: Cogen[MoreCollections] = CogenDerivation[MoreCollections]
  implicit lazy val coEnums: Cogen[Enums] = CogenDerivation[Enums]
  implicit lazy val coUnsafeEnums: Cogen[UnsafeEnums] = CogenDerivation[UnsafeEnums]
  implicit lazy val coCustom: Cogen[Custom] = CogenDerivation[Custom]
  implicit lazy val coLowerCamel: Cogen[LowerCamel] = CogenDerivation[LowerCamel]
  implicit lazy val coLowerCamelInner: Cogen[LowerCamelInner] = CogenDerivation[LowerCamelInner]

  // other
  implicit lazy val coUri: Cogen[URI] = Cogen(_.hashCode().toLong)
}
