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
package magnolify.shared

import magnolify.test._

import scala.annotation.StaticAnnotation

class AnnotationTypeSuite extends MagnolifySuite {
  import AnnotationTypeSuite._
  test("Enumeration") {
    assertEquals(AnnotationType[AccountType.Type].annotations, List(Version("1.0")))
  }

  test("Class") {
    assertEquals(AnnotationType[Account].annotations, List(Version("2.0")))
  }
}

object AnnotationTypeSuite {
  case class Version(version: String) extends StaticAnnotation

  @Version("1.0")
  object AccountType extends Enumeration {
    type Type = Value
    val Checking, Saving = Value
  }

  @Version("2.0")
  case class Account(user: String, accountType: AccountType.Type)
}
