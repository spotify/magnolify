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

class EnumTypeSuite extends MagnolifySuite {
  test("JavaEnums") {
    val et = ensureSerializable(implicitly[EnumType[JavaEnums.Color]])
    assertEquals(et.name, "Color")
    assertEquals(et.namespace, "magnolify.test.JavaEnums")
    assertEquals(et.values, List("RED", "GREEN", "BLUE"))
    assertEquals(et.from("RED"), JavaEnums.Color.RED)
    assertEquals(et.to(JavaEnums.Color.RED), "RED")
    val ja = et.annotations.collect { case a: JavaAnnotation => a.value() }
    assertEquals(ja, List("Java Annotation"))
  }

  test("ScalaEnums") {
    val et = ensureSerializable(implicitly[EnumType[ScalaEnums.Color.Type]])
    assertEquals(et.name, "Color")
    assertEquals(et.namespace, "magnolify.test.Simple.ScalaEnums")
    assertEquals(et.values, List("Red", "Green", "Blue"))
    assertEquals(et.from("Red"), ScalaEnums.Color.Red)
    assertEquals(et.to(ScalaEnums.Color.Red), "Red")
    val ja = et.annotations.collect { case a: JavaAnnotation => a.value() }
    val sa = et.annotations.collect { case a: ScalaAnnotation => a.value }
    assertEquals(ja, List("Java Annotation"))
    assertEquals(sa, List("Scala Annotation"))
  }

  test("ADT") {
    val et = ensureSerializable(implicitly[EnumType[ADT.Color]])
    assertEquals(et.name, "Color")
    assertEquals(et.namespace, "magnolify.test.ADT")
    assertEquals(et.values, List("Blue", "Green", "Red")) // ADTs are ordered alphabetically
    assertEquals(et.from("Red"), ADT.Red)
    assertEquals(et.to(ADT.Red), "Red")
    // Magnolia does not capture Java annotations
    val as = et.annotations.collect { case a: ScalaAnnotation => a.value }
    assertEquals(as, List("Color", "Red"))
  }

  test("JavaEnums CaseMapper") {
    val et = ensureSerializable(EnumType[JavaEnums.Color](CaseMapper(_.toLowerCase)))
    assertEquals(et.values.toSet, JavaEnums.Color.values().map(_.name().toLowerCase).toSet)
    assertEquals(et.from("red"), JavaEnums.Color.RED)
    assertEquals(et.to(JavaEnums.Color.RED), "red")
  }

  test("ScalaEnums CaseMapper") {
    val et = ensureSerializable(EnumType[ScalaEnums.Color.Type](CaseMapper(_.toLowerCase)))
    assertEquals(et.values.toSet, ScalaEnums.Color.values.map(_.toString.toLowerCase))
    assertEquals(et.from("red"), ScalaEnums.Color.Red)
    assertEquals(et.to(ScalaEnums.Color.Red), "red")
  }

  test("ADT CaseMapper") {
    val et = ensureSerializable(EnumType[ADT.Color](CaseMapper(_.toLowerCase)))
    assertEquals(et.values.toSet, Set("red", "green", "blue"))
    assertEquals(et.from("red"), ADT.Red)
    assertEquals(et.to(ADT.Red), "red")
  }
}
