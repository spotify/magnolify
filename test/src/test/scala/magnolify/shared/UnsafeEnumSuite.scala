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
import magnolify.test.Simple._

class UnsafeEnumSuite extends MagnolifySuite {
  test("JavaEnums") {
    assertEquals(UnsafeEnum(JavaEnums.Color.RED), UnsafeEnum.Known(JavaEnums.Color.RED))

    assertEquals(UnsafeEnum.from[JavaEnums.Color]("RED"), UnsafeEnum.Known(JavaEnums.Color.RED))
    assertEquals(UnsafeEnum.from[JavaEnums.Color]("PURPLE"), UnsafeEnum.Unknown("PURPLE"))

    assertEquals(UnsafeEnum.to(UnsafeEnum.Known(JavaEnums.Color.RED)), "RED")
    assertEquals(UnsafeEnum.to(UnsafeEnum.Unknown("PURPLE")), "PURPLE")
  }

  test("ScalaEnums") {
    assertEquals(UnsafeEnum(ScalaEnums.Color.Red), UnsafeEnum.Known(ScalaEnums.Color.Red))

    assertEquals(
      UnsafeEnum.from[ScalaEnums.Color.Type]("Red"),
      UnsafeEnum.Known(ScalaEnums.Color.Red)
    )
    assertEquals(UnsafeEnum.from[ScalaEnums.Color.Type]("Purple"), UnsafeEnum.Unknown("Purple"))

    assertEquals(UnsafeEnum.to(UnsafeEnum.Known(ScalaEnums.Color.Red)), "Red")
    assertEquals(UnsafeEnum.to(UnsafeEnum.Unknown("Purple")), "Purple")
  }

  test("ADT") {
    assertEquals(UnsafeEnum(ADT.Red), UnsafeEnum.Known(ADT.Red))

    assertEquals(UnsafeEnum.from[ADT.Color]("Red"), UnsafeEnum.Known(ADT.Red))
    assertEquals(UnsafeEnum.from[ADT.Color]("Purple"), UnsafeEnum.Unknown("Purple"))

    assertEquals(UnsafeEnum.to(UnsafeEnum.Known(ADT.Red)), "Red")
    assertEquals(UnsafeEnum.to(UnsafeEnum.Unknown("Purple")), "Purple")
  }
}
