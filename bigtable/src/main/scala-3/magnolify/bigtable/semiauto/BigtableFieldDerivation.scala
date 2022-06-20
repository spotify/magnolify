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

package magnolify.bigtable.semiauto

import com.google.bigtable.v2.Column
import com.google.bigtable.v2.Mutation.SetCell
import magnolia1.*
import magnolify.bigtable.BigtableField
import magnolify.bigtable.BigtableField.Record
import magnolify.shared.{CaseMapper, Value}

import scala.annotation.implicitNotFound
import scala.deriving.Mirror

object BigtableFieldDerivation extends ProductDerivation[BigtableField]:

  def join[T](caseClass: CaseClass[BigtableField, T]): BigtableField[T] =
    new BigtableField.Record[T] {
      private def key(prefix: String, label: String): String =
        if (prefix == null) label else s"$prefix.$label"

      override def get(xs: java.util.List[Column], k: String)(cm: CaseMapper): Value[T] = {
        var fallback = true
        val r = caseClass.construct { p =>
          val cq = key(k, cm.map(p.label))
          val v = p.typeclass.get(xs, cq)(cm)
          if (v.isSome) {
            fallback = false
          }
          v.getOrElse(p.default)
        }
        // result is default if all fields are default
        if (fallback) Value.Default(r) else Value.Some(r)
      }

      override def put(k: String, v: T)(cm: CaseMapper): Seq[SetCell.Builder] =
        caseClass.params.flatMap(p => p.typeclass.put(key(k, cm.map(p.label)), p.deref(v))(cm))
    }

  inline given apply[T](using Mirror.Of[T]): BigtableField.Record[T] =
    derived[T].asInstanceOf[BigtableField.Record[T]]
