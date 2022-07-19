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

package magnolify.datastore.semiauto

import magnolia1._
import magnolify.datastore
import magnolify.datastore.{key => _, _}
import magnolify.shared.CaseMapper
import com.google.datastore.v1._

import scala.annotation.implicitNotFound

object EntityFieldDerivation {

  type Typeclass[T] = EntityField[T]

  def join[T: KeyField](caseClass: CaseClass[Typeclass, T]): EntityField[T] =
    new EntityField.Record[T] {
      private val (keyIndex, key): (Int, datastore.key) = {
        val keys = caseClass.parameters
          .flatMap(p =>
            getKey(p.annotations, s"${caseClass.typeName.full}#${p.label}").map(k => p -> k)
          )

        require(
          keys.size <= 1,
          s"More than one field with @key annotation: ${caseClass.typeName.full}#[${keys.map(_._1.label).mkString(", ")}]"
        )
        keys.headOption match {
          case None =>
            (-1, null)
          case Some((p, k)) =>
            require(
              !p.typeclass.keyField.isInstanceOf[KeyField.NotSupported[_]],
              s"No KeyField[T] instance: ${caseClass.typeName.full}#${p.label}"
            )
            (p.index, k)
        }
      }

      private val excludeFromIndexes: Array[Boolean] = {
        val a = new Array[Boolean](caseClass.parameters.length)
        caseClass.parameters.foreach { p =>
          a(p.index) =
            getExcludeFromIndexes(p.annotations, s"${caseClass.typeName.full}#${p.label}")
        }
        a
      }

      override val keyField: KeyField[T] = implicitly[KeyField[T]]

      override def fromEntity(v: Entity)(cm: CaseMapper): T =
        caseClass.construct { p =>
          val f = v.getPropertiesOrDefault(cm.map(p.label), null)
          if (f == null && p.default.isDefined) {
            p.default.get
          } else {
            p.typeclass.from(f)(cm)
          }
        }

      override def toEntity(v: T)(cm: CaseMapper): Entity.Builder =
        caseClass.parameters.foldLeft(Entity.newBuilder()) { (eb, p) =>
          val value = p.dereference(v)
          val vb = p.typeclass.to(value)(cm)
          if (vb != null) {
            eb.putProperties(
              cm.map(p.label),
              vb.setExcludeFromIndexes(excludeFromIndexes(p.index))
                .build()
            )
          }
          if (p.index == keyIndex) {
            val partitionId = {
              val b = PartitionId.newBuilder()
              if (key.project != null) {
                b.setProjectId(key.project)
              }
              b.setNamespaceId(
                if (key.namespace != null) key.namespace else caseClass.typeName.owner
              )
            }
            val path = {
              val b = Key.PathElement.newBuilder()
              b.setKind(if (key.kind != null) key.kind else caseClass.typeName.short)
              p.typeclass.keyField.setKey(b, value)
            }
            val kb = Key
              .newBuilder()
              .setPartitionId(partitionId)
              .addPath(path)
            eb.setKey(kb)
          }
          eb
        }

      private def getKey(annotations: Seq[Any], name: String): Option[datastore.key] = {
        val keys = annotations.collect { case k: datastore.key => k }
        require(keys.size <= 1, s"More than one @key annotation: $name")
        keys.headOption
      }

      private def getExcludeFromIndexes(annotations: Seq[Any], name: String): Boolean = {
        val excludes = annotations.collect { case e: excludeFromIndexes => e.exclude }
        require(excludes.size <= 1, s"More than one @excludeFromIndexes annotation: $name")
        excludes.headOption.getOrElse(false)
      }
    }

  @implicitNotFound("Cannot derive EntityField for sealed trait")
  private sealed trait Dispatchable[T]
  def split[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): EntityField[T] = ???

  implicit def apply[T]: EntityField[T] = macro Magnolia.gen[T]
}
