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

import magnolia._

import scala.language.experimental.macros
import scala.reflect.ClassTag
import scala.reflect.macros._

sealed trait EnumType[T] extends Serializable { self =>
  val name: String
  val namespace: String
  val values: List[String]
  val annotations: List[Any]
  def from(v: String): T
  def to(v: T): String

  def map(cm: CaseMapper): EnumType[T] = {
    val m1 = values.map(v => cm.map(v) -> self.from(v)).toMap
    val m2 = values.map(v => self.from(v) -> cm.map(v)).toMap
    EnumType.create(name, namespace, values.map(cm.map), annotations, m1(_), m2(_))
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
    f: String => T,
    g: T => String
  ): EnumType[T] = new EnumType[T] {
    override val name: String = _name
    override val namespace: String = _namespace
    override val values: List[String] = _values
    override val annotations: List[Any] = _annotations
    override def from(v: String): T = f(v)
    override def to(v: T): String = g(v)
  }

  //////////////////////////////////////////////////

  // Java `enum`
  implicit def javaEnumType[T <: Enum[T]](implicit ct: ClassTag[T]): EnumType[T] = {
    val cls: Class[_] = ct.runtimeClass
    val n = cls.getSimpleName
    val ns = cls.getCanonicalName.replaceFirst(s".$n$$", "")
    val map: Map[String, T] = cls
      .getMethod("values")
      .invoke(null)
      .asInstanceOf[Array[T]]
      .iterator
      .map(v => v.name() -> v)
      .toMap
    EnumType.create(n, ns, map.keys.toList, cls.getAnnotations.toList, map(_), _.name())
  }

  //////////////////////////////////////////////////

  // Scala `Enumeration`
  implicit def scalaEnumType[T <: Enumeration#Value]: EnumType[T] = macro scalaEnumTypeImpl[T]

  def scalaEnumTypeImpl[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    val ref = wtt.tpe.asInstanceOf[TypeRef]
    val n = ref.sym.name.toString // `type $Name = Value`
    val ns = ref.pre.typeSymbol.asClass.fullName // `object Namespace extends Enumeration`
    val map = q"${ref.pre.termSymbol}.values.map(x => x.toString -> x).toMap"

    // Scala 2.12 & 2.13 macros seem to handle annotations differently
    // Scala annotation works in both but Java annotations only works in 2.13
    val saType = typeOf[scala.annotation.StaticAnnotation]
    val jaType = typeOf[java.lang.annotation.Annotation]
    val trees = ref.pre.typeSymbol.annotations.collect {
      case t if t.tree.tpe <:< saType && !(t.tree.tpe <:< jaType) =>
        // FIXME `t.tree` should work but somehow crashes the compiler
        val q"new $n(..$args)" = t.tree
        q"new $n(..$args)"
    }

    // Get Java annotations via reflection
    val j = q"classOf[${ref.pre.typeSymbol.asClass}].getAnnotations.toList"
    val annotations = q"_root_.scala.List(..$trees) ++ $j"

    q"""
        _root_.magnolify.shared.EnumType.create[$wtt](
          $n, $ns, $map.keys.toList, $annotations, $map.apply(_), _.toString)
     """
  }

  //////////////////////////////////////////////////

  // Scala ADT
  implicit def gen[T](implicit lp: shapeless.LowPriority): Typeclass[T] = macro lowPrioGen[T]

  def lowPrioGen[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = Magnolia.gen[T](c)

  type Typeclass[T] = EnumType[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = {
    require(caseClass.isObject, s"Cannot derive EnumType[T] for case class ${caseClass.typeName}")
    val n = caseClass.typeName.short
    val ns = caseClass.typeName.owner
    EnumType.create(
      n,
      ns,
      List(n),
      caseClass.annotations.toList,
      _ => caseClass.rawConstruct(Nil),
      _ => n
    )
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = {
    val n = sealedTrait.typeName.short
    val ns = sealedTrait.typeName.owner
    val subs = sealedTrait.subtypes.map(_.typeclass)
    val values = subs.flatMap(_.values).toList
    val m = subs.map(s => s.name -> s.from(s.name)).toMap
    val annotations = (sealedTrait.annotations ++ subs.flatMap(_.annotations)).toList
    EnumType.create(
      n,
      ns,
      values,
      annotations,
      m(_),
      t => sealedTrait.dispatch(t)(_.typeclass.name)
    )
  }
}
