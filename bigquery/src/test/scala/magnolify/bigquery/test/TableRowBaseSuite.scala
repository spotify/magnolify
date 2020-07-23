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
package magnolify.bigquery.test

import cats.Eq
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.google.api.services.bigquery.model.TableRow
import magnolify.bigquery.TableRowType
import magnolify.test.MagnolifySuite
import org.scalacheck.{Arbitrary, Prop}

import scala.reflect.ClassTag

trait TableRowBaseSuite extends MagnolifySuite {
  private val mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
  private[magnolify] def test[T: Arbitrary: ClassTag](implicit
    t: TableRowType[T],
    eq: Eq[T]
  ): Unit = {
    val tpe = ensureSerializable(t)
    // FIXME: test schema
    tpe.schema
    property(className[T]) {
      Prop.forAll { t: T =>
        val r = tpe(t)
        val copy1 = tpe(r)
        val copy2 = tpe(mapper.readValue(mapper.writeValueAsString(r), classOf[TableRow]))
        Prop.all(eq.eqv(t, copy1), eq.eqv(t, copy2))
      }
    }
  }
}
