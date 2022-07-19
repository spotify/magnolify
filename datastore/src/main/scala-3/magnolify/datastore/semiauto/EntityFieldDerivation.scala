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

import magnolia1.*
import magnolify.datastore
import magnolify.datastore.{key => _, *}
import magnolify.shared.CaseMapper
import com.google.datastore.v1.*

import scala.deriving.Mirror

object EntityFieldDerivation extends ProductDerivation[EntityField]:

  def join[T](caseClass: CaseClass[EntityField, T]): EntityField[T] = new EntityField.Record[T]:
    private val (keyIndex, key): (Int, datastore.key) = {
      val keys = caseClass.params
        .flatMap(p =>
          getKey(p.annotations, s"${caseClass.typeInfo.full}#${p.label}").map(k => p -> k)
        )
      require(
        keys.size <= 1,
        s"More than one field with @key annotation: ${caseClass.typeInfo.full}#[${keys.map(_._1.label).mkString(", ")}]"
      )
      keys.headOption match {
        case None =>
          (-1, null)
        case Some((p, k)) =>
          require(
            !p.typeclass.keyField.isInstanceOf[KeyField.NotSupported[_]],
            s"No KeyField[T] instance: ${caseClass.typeInfo.full}#${p.label}"
          )
          (p.index, k)
      }
    }

    // TODO not possible to pass KeyField implicit
    override val keyField: KeyField[T] = KeyField.notSupported

    private val excludeFromIndexes: Array[Boolean] = {
      val a = new Array[Boolean](caseClass.params.length)
      caseClass.params.foreach { p =>
        a(p.index) = getExcludeFromIndexes(p.annotations, s"${caseClass.typeInfo.full}#${p.label}")
      }
      a
    }

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
      caseClass.params.foldLeft(Entity.newBuilder()) { (eb, p) =>
        val value = p.deref(v)
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
            b.setNamespaceId(if (key.namespace != null) key.namespace else caseClass.typeInfo.owner)
          }
          val path = {
            val b = Key.PathElement.newBuilder()
            b.setKind(if (key.kind != null) key.kind else caseClass.typeInfo.short)
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
  end join

  // ProductDerivation can be specialized to an EntityField.Record
  inline def apply[T](using mirror: Mirror.Of[T]): EntityField.Record[T] =
    derivedMirror[T].asInstanceOf[EntityField.Record[T]]
