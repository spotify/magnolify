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

import magnolify.scalacheck.semiauto.*
import magnolify.shared.UnsafeEnum
import magnolify.test.ADT.*
import magnolify.test.JavaEnums
import magnolify.test.Simple.*
import org.scalacheck.Cogen

import java.net.URI

object TestCogen {
  // enum
  implicit lazy val coJavaEnum: Cogen[JavaEnums.Color] = Cogen(_.ordinal().toLong)
  implicit lazy val coScalaEnums: Cogen[ScalaEnums.Color.Type] = Cogen(_.id.toLong)
  implicit def coUnsafeEnum[T: Cogen]: Cogen[UnsafeEnum[T]] = Cogen.gen[UnsafeEnum[T]]

  // ADT
  implicit lazy val coNode: Cogen[Node] = Cogen.gen[Node]
  implicit lazy val coGNode: Cogen[GNode[Int]] = Cogen.gen[GNode[Int]]
  implicit lazy val coShape: Cogen[Shape] = Cogen.gen[Shape]
  implicit lazy val coColor: Cogen[Color] = Cogen.gen[Color]
  implicit lazy val coPerson: Cogen[Person] = Cogen.gen[Person]

  // simple
  implicit lazy val coIntegers: Cogen[Integers] = Cogen.gen[Integers]
  implicit lazy val coFloats: Cogen[Floats] = Cogen.gen[Floats]
  implicit lazy val coNumbers: Cogen[Numbers] = Cogen.gen[Numbers]
  implicit lazy val coRequired: Cogen[Required] = Cogen.gen[Required]
  implicit lazy val coNullable: Cogen[Nullable] = Cogen.gen[Nullable]
  implicit lazy val coRepeated: Cogen[Repeated] = Cogen.gen[Repeated]
  implicit lazy val coNested: Cogen[Nested] = Cogen.gen[Nested]
  implicit lazy val coCollections: Cogen[Collections] = Cogen.gen[Collections]
  // implicit lazy val coMoreCollections: Cogen[MoreCollections] = Cogen.gen[MoreCollections]
  implicit lazy val coEnums: Cogen[Enums] = Cogen.gen[Enums]
  implicit lazy val coUnsafeEnums: Cogen[UnsafeEnums] = Cogen.gen[UnsafeEnums]
  implicit lazy val coCustom: Cogen[Custom] = Cogen.gen[Custom]
  implicit lazy val coLowerCamel: Cogen[LowerCamel] = Cogen.gen[LowerCamel]
  implicit lazy val coLowerCamelInner: Cogen[LowerCamelInner] = Cogen.gen[LowerCamelInner]

  // other
  implicit lazy val coUri: Cogen[URI] = Cogen(_.hashCode().toLong)
}
