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

package magnolify.shared

import magnolify.test.ADT
import magnolify.test.JavaEnums
import magnolify.test.Simple.ScalaEnums
object TestEnumType {

  implicit val etJava: EnumType[JavaEnums.Color] = EnumType.javaEnumType[JavaEnums.Color]
  implicit val etScala: EnumType[ScalaEnums.Color.Type] =
    EnumType.scalaEnumType[ScalaEnums.Color.Type]
  implicit val etAdt: EnumType[ADT.Color] = EnumType.adtEnumType[ADT.Color]

}
