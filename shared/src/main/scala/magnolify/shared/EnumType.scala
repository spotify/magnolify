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

import scala.reflect.ClassTag

sealed trait EnumType[T] extends Serializable { self =>
  val name: String
  val namespace: String
  val values: List[String]
  val valueSet: Set[String]
  val annotations: List[Any]
  def from(v: String): T
  def to(v: T): String

  def map(cm: CaseMapper): EnumType[T] = {
    val cMap = values.map(v => cm.map(v) -> v).toMap
    EnumType.create(
      name,
      namespace,
      values.map(cm.map),
      annotations,
      v => self.from(cMap(v))
    )
  }
}

object EnumType {
  def apply[T](implicit et: EnumType[T]): EnumType[T] = et
  def apply[T](cm: CaseMapper)(implicit et: EnumType[T]): EnumType[T] = et.map(cm)

  def create[T](
    _name: String,
    _namespace: String,
    _values: List[String],
    _annotations: List[Any],
    f: String => T
  ): EnumType[T] = new EnumType[T] {

    @transient private lazy val fMap =
      _values.map(v => v -> f(v)).toMap

    @transient private lazy val gMap =
      _values.map(v => f(v) -> v).toMap

    override val name: String = _name
    override val namespace: String = _namespace
    override val values: List[String] = _values
    override val valueSet: Set[String] = _values.toSet
    override val annotations: List[Any] = _annotations
    override def from(v: String): T = fMap(v)
    override def to(v: T): String = gMap(v)
  }

  // Java `enum`
  def javaEnumType[T <: Enum[T]](implicit ct: ClassTag[T]): EnumType[T] = {
    val cls = ct.runtimeClass.asInstanceOf[Class[T]]
    val n = ReflectionUtils.name[T]
    val ns = ReflectionUtils.namespace[T]
    val map: Map[String, T] = cls.getEnumConstants.iterator
      .map(v => v.name() -> v)
      .toMap
    EnumType.create(n, ns, map.keys.toList, cls.getAnnotations.toList, map(_))
  }
}
