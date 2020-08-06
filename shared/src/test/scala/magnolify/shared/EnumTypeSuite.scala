/*
 * Copyright 2020 Spotify AB.
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
import magnolify.test.Simple._

class EnumTypeSuite extends munit.FunSuite {
  test("JavaEnums") {
    val et = implicitly[EnumType[JavaEnums.Color]]
    assertEquals(et.name, "Color")
    assertEquals(et.namespace, "magnolify.test.JavaEnums")
    assertEquals(et.from("RED"), JavaEnums.Color.RED)
    assertEquals(et.to(JavaEnums.Color.RED), "RED")
  }

  test("ScalaEnums") {
    val et = implicitly[EnumType[ScalaEnums.Color.Type]]
    assertEquals(et.name, "Type")
    assertEquals(et.namespace, "magnolify.test.Simple.ScalaEnums.Color")
    assertEquals(et.from("Red"), ScalaEnums.Color.Red)
    assertEquals(et.to(ScalaEnums.Color.Red), "Red")
  }
}
