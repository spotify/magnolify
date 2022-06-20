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

package magnolify.avro.semiauto

import magnolia1.*
import magnolify.avro.{doc, AvroField, AvroType}
import magnolify.shared.CaseMapper
import org.apache.avro.generic.*
import org.apache.avro.{JsonProperties, LogicalType, LogicalTypes, Schema}

import scala.collection.concurrent
import scala.jdk.CollectionConverters.*
import scala.deriving.Mirror
import java.util.UUID

object AvroFieldDerivation extends ProductDerivation[AvroField]:

  def join[T](caseClass: CaseClass[AvroField, T]): AvroField[T] = new AvroField.Record[T]:
    override protected def buildSchema(cm: CaseMapper): Schema = Schema
      .createRecord(
        caseClass.typeInfo.short,
        AvroField.getDoc(caseClass.annotations, caseClass.typeInfo.full),
        caseClass.typeInfo.owner,
        false,
        caseClass.params
          .map { p =>
            new Schema.Field(
              cm.map(p.label),
              p.typeclass.schema(cm),
              AvroField.getDoc(p.annotations, s"${caseClass.typeInfo.full}#${p.label}"),
              p.default
                .map(d => p.typeclass.makeDefault(d)(cm))
                .getOrElse(p.typeclass.fallbackDefault)
            )
          }
          .toList
          .asJava
      )

    // `JacksonUtils.toJson` expects `Map[String, AnyRef]` for `RECORD` defaults
    override def makeDefault(d: T)(cm: CaseMapper): java.util.Map[String, Any] =
      caseClass.params
        .map { p =>
          val name = cm.map(p.label)
          val value = p.typeclass.makeDefault(p.deref(d))(cm)
          name -> value
        }
        .toMap
        .asJava

    override def from(v: GenericRecord)(cm: CaseMapper): T =
      caseClass.construct { p =>
        p.typeclass.fromAny(v.get(p.index))(cm)
      }

    override def to(v: T)(cm: CaseMapper): GenericRecord =
      caseClass.params
        .foldLeft(new GenericData.Record(schema(cm))) { (r, p) =>
          r.put(p.index, p.typeclass.to(p.deref(v))(cm))
          r
        }

  // ProductDerivation can be specialized to an AvroField.Record
  inline given apply[T](using mirror: Mirror.Of[T]): AvroField.Record[T] =
    derived[T].asInstanceOf[AvroField.Record[T]]
