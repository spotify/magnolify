/*
 * Copyright 2019 Spotify AB.
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
package magnolify.avro.test


import cats._
import cats.instances.all._
import magnolify.avro.AvroType._
import magnolify.avro.AvroType
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import magnolify.test._
import org.apache.avro.generic.GenericRecord
import org.scalacheck._

import scala.reflect._

object AvroRecordTypeSpec extends MagnolifySpec("AvroRecordTypeSpec") {
  private def test[T: Arbitrary: ClassTag](implicit tpe: AvroType.Aux2[T, GenericRecord], eq: Eq[T]): Unit = {
    ensureSerializable(tpe)
    property(className[T]) = Prop.forAll { t: T =>
      val r = tpe.to(t)

      val copy = tpe.from(r)
      Prop.all(eq.eqv(t, copy))
    }
  }

  test[Integers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]

  case class CollectionPrimitive(l: List[Int])
  case class CollectionNestedRecord(l: List[Nested])
  case class CollectionNullable(l: List[Nullable])

  {
    test[CollectionPrimitive]
    test[CollectionNestedRecord]
    test[CollectionNullable]
  }
}
