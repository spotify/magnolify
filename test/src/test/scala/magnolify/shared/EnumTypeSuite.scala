/*
 * Copyright 2020 Spotify AB
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

import magnolify.test.*
import magnolify.test.Simple.*

import scala.util.{Properties, Try}

class EnumTypeSuite extends MagnolifySuite {
  test("JavaEnums") {
    val et = ensureSerializable(EnumType[JavaEnums.Color])
    assertEquals(et.name, "Color")
    assertEquals(et.namespace, "magnolify.test.JavaEnums")
    assertEquals(et.values, List("RED", "GREEN", "BLUE"))
    assertEquals(et.from("RED"), JavaEnums.Color.RED)
    assertEquals(et.to(JavaEnums.Color.RED), "RED")
    val ja = et.annotations.collect { case a: JavaAnnotation => a.value() }
    assertEquals(ja, List("Java Annotation"))
  }

  test("ScalaEnums") {
    val et = ensureSerializable(EnumType[ScalaEnums.Color.Type])
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
    val etPrimaryColor = ensureSerializable(EnumType[ADT.PrimaryColor])
    assertEquals(etPrimaryColor.name, "PrimaryColor")
    assertEquals(etPrimaryColor.namespace, "magnolify.test.ADT")
    assertEquals(
      etPrimaryColor.values,
      List("Blue", "Green", "Red")
    ) // ADTs are ordered alphabetically
    assertEquals(etPrimaryColor.from("Red"), ADT.Red)
    assertEquals(etPrimaryColor.to(ADT.Red), "Red")
    // Magnolia does not capture Java annotations
    val annPrimaryColor = etPrimaryColor.annotations.collect { case a: ScalaAnnotation => a.value }
    assertEquals(annPrimaryColor, List("Color", "PrimaryColor"))

    val etColor = ensureSerializable(EnumType[ADT.Color])
    assertEquals(etColor.name, "Color")
    assertEquals(etColor.namespace, "magnolify.test.ADT")
    assertEquals(
      etColor.values,
      List("Blue", "Cyan", "Green", "Magenta", "Red", "Yellow")
    ) // ADTs are ordered alphabetically
    assertEquals(etColor.from("Magenta"), ADT.Magenta)
    assertEquals(etColor.to(ADT.Magenta), "Magenta")
    // Magnolia does not capture Java annotations
    val as = etColor.annotations.collect { case a: ScalaAnnotation => a.value }
    assertEquals(as, List("Color"))
  }

  test("ADT No Default Constructor") {
    val et = ensureSerializable(EnumType[ADT.Person])
    assertEquals(et.name, "Person")
    assertEquals(et.namespace, "magnolify.test.ADT")
    assertEquals(et.values, List("Aldrin", "Neil")) // ADTs are ordered alphabetically
    assertEquals(et.from("Aldrin"), ADT.Aldrin)
    assertEquals(et.to(ADT.Neil), "Neil")
  }

  test("ADT should not generate for invalid types") {
    // explicit
    {
      val error = compileErrors("EnumType.gen[Option[ADT.Color]]")
      val scala2Error =
        """|error:
           |magnolia: could not find EnumType.Typeclass for type magnolify.test.ADT.Color
           |    in parameter 'value' of product type Some[magnolify.test.ADT.Color]
           |    in coproduct type Option[magnolify.test.ADT.Color]
           |
           |EnumType.gen[Option[ADT.Color]]
           |            ^
           |""".stripMargin
      val scala3Error =
        """|error: Cannot derive EnumType for non singleton sum type
           |      val error = compileErrors("EnumType.gen[Option[ADT.Color]]")
           |                              ^
           |""".stripMargin
      if (Properties.versionNumberString.startsWith("2.12")) {
        assertNoDiff(error, scala2Error)
      } else {
        // scala 3 uses 2.13
        Try(assertNoDiff(error, scala2Error))
          .orElse(Try(assertNoDiff(error, scala3Error)))
          .get
      }
    }

    // implicit
    {
      val error = compileErrors("EnumType[Option[ADT.Color]]")
      val scala2Error =
        """|error: could not find implicit value for parameter et: magnolify.shared.EnumType[Option[magnolify.test.ADT.Color]]
           |EnumType[Option[ADT.Color]]
           |        ^
           |""".stripMargin

      val scala3Error =
        """|error: Cannot derive EnumType for non singleton sum type
           |      val error = compileErrors("EnumType[Option[ADT.Color]]")
           |                              ^
           |""".stripMargin

      if (Properties.versionNumberString.startsWith("2.12")) {
        assertNoDiff(error, scala2Error)
      } else {
        // scala 3 uses 2.13
        Try(assertNoDiff(error, scala2Error))
          .orElse(Try(assertNoDiff(error, scala3Error)))
          .get
      }
    }
  }

  test("JavaEnums CaseMapper") {
    val et = ensureSerializable(EnumType[JavaEnums.Color](CaseMapper(_.toLowerCase)))
    assertEquals(et.values, JavaEnums.Color.values().map(_.name().toLowerCase).toList)
    assertEquals(et.from("red"), JavaEnums.Color.RED)
    assertEquals(et.to(JavaEnums.Color.RED), "red")
  }

  test("ScalaEnums CaseMapper") {
    val et = ensureSerializable(EnumType[ScalaEnums.Color.Type](CaseMapper(_.toLowerCase)))
    assertEquals(et.values, List("red", "green", "blue"))
    assertEquals(et.from("red"), ScalaEnums.Color.Red)
    assertEquals(et.to(ScalaEnums.Color.Red), "red")
  }

  test("ADT CaseMapper") {
    val et = ensureSerializable(EnumType[ADT.PrimaryColor](CaseMapper(_.toLowerCase)))
    assertEquals(et.values, List("blue", "green", "red")) // ADTs are ordered alphabetically
    assertEquals(et.from("red"), ADT.Red)
    assertEquals(et.to(ADT.Red), "red")
  }
}
