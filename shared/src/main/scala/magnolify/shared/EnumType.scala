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

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

sealed trait EnumType[T] extends Serializable {
  val name: String
  val namespace: String
  val values: List[String]
  def from(v: String): T
  def to(v: T): String
}

object EnumType {
  // FIXME: support case mapper and annotations
  implicit def javaEnumType[T <: Enum[T]](implicit ct: ClassTag[T]): EnumType[T] =
    new EnumType[T] {
      private val cls: Class[_] = ct.runtimeClass
      private val map: Map[String, T] = cls
        .getMethod("values")
        .invoke(null)
        .asInstanceOf[Array[T]]
        .iterator
        .map(v => v.name() -> v)
        .toMap

      override val name: String = cls.getSimpleName
      override val namespace: String = cls.getCanonicalName.replaceFirst(s".$name$$", "")
      override val values: List[String] = map.values.map(_.name()).toList
      override def from(v: String): T = map(v)
      override def to(v: T): String = v.name()
    }

  implicit def scalaEnumType[T <: Enumeration#Value](implicit tt: TypeTag[T]): EnumType[T] = {
    val ref = tt.tpe.asInstanceOf[TypeRef]
    val n = ref.sym.name.toString // `type $Name = Value`
    val ns = ref.pre.typeSymbol.asClass.fullName // `object Namespace extends Enumeration`
    val enum = runtimeMirror(getClass.getClassLoader)
      .runtimeClass(ref.pre)
      .getField("MODULE$")
      .get(null)
      .asInstanceOf[Enumeration]

    new EnumType[T] {
      override val name: String = n
      override val namespace: String = ns
      override val values: List[String] = enum.values.map(_.toString).toList
      override def from(v: String): T = enum.withName(v).asInstanceOf[T]
      override def to(v: T): String = v.toString
    }
  }
}
