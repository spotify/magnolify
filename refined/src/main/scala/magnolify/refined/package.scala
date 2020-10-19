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
package magnolify

import eu.timepit.refined._
import eu.timepit.refined.api._
import magnolify.avro.AvroField
import magnolify.bigquery.TableRowField
import magnolify.bigtable.BigtableField
import magnolify.datastore.EntityField
import magnolify.protobuf.ProtobufField
import magnolify.tensorflow.ExampleField

import scala.language.implicitConversions

package object refined {
  object avro {
    implicit def refinedAvro[T: AvroField, P](implicit v: Validate[T, P]): AvroField[Refined[T, P]] =
      AvroField.from[T](unsafeRefine[T, P])(_.value)
  }

  object bigquery {
    implicit def refinedTableRow[T: TableRowField, P](implicit v: Validate[T, P]): TableRowField[Refined[T, P]] =
      TableRowField.from[T](unsafeRefine[T, P])(_.value)
  }

  object bigtable {
    implicit def refinedBigtable[T: BigtableField.Primitive, P](implicit v: Validate[T, P])
      : BigtableField.Primitive[Refined[T, P]] =
      BigtableField.from[T](unsafeRefine[T, P])(_.value)
  }

  object datastore {
    implicit def refinedEntity[T: EntityField, P](implicit v: Validate[T, P]): EntityField[Refined[T, P]] =
      EntityField.from[T](unsafeRefine[T, P])(_.value)
  }

  object protobuf {
    implicit def refinedProtobuf[T: ProtobufField, P](implicit v: Validate[T, P]): ProtobufField[Refined[T, P]] =
      ProtobufField.from[T](unsafeRefine[T, P])(_.value)
  }

  object tensorflow {
    implicit def refinedExample[T: ExampleField.Primitive, P](implicit v: Validate[T, P])
      : ExampleField.Primitive[Refined[T, P]] =
      ExampleField.from[T](unsafeRefine[T, P])(_.value)
  }

  def unsafeRefine[T, P](t: T)(implicit v: Validate[T, P]): Refined[T, P] =
    refineV[P].unsafeFrom(t)
}
