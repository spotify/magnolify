/*
 * Copyright 2020 Spotify AB
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

package magnolify

import eu.timepit.refined._
import eu.timepit.refined.api._

import scala.language.implicitConversions

package object refined {
  object guava {
    import com.google.common.hash.{Funnel, PrimitiveSink}
    implicit def refineFunnel[T, P](implicit
      f: Funnel[T],
      v: Validate[T, P]
    ): Funnel[Refined[T, P]] =
      new Funnel[Refined[T, P]] {
        override def funnel(from: Refined[T, P], into: PrimitiveSink): Unit =
          f.funnel(from.value, into)
      }
  }

  object avro {
    import magnolify.avro.AvroField
    implicit def refinedAvro[T: AvroField, P](implicit
      v: Validate[T, P]
    ): AvroField[Refined[T, P]] =
      AvroField.from[T](refineV[P].unsafeFrom(_))(_.value)
  }

  object bigquery {
    import magnolify.bigquery.TableRowField
    implicit def refinedTableRow[T: TableRowField, P](implicit
      v: Validate[T, P]
    ): TableRowField[Refined[T, P]] =
      TableRowField.from[T](refineV[P].unsafeFrom(_))(_.value)
  }

  object bigtable {
    import magnolify.bigtable.BigtableField
    implicit def refinedBigtable[T: BigtableField.Primitive, P](implicit
      v: Validate[T, P]
    ): BigtableField.Primitive[Refined[T, P]] =
      BigtableField.from[T](refineV[P].unsafeFrom(_))(_.value)
  }

  object datastore {
    import magnolify.datastore.EntityField
    implicit def refinedEntity[T: EntityField, P](implicit
      v: Validate[T, P]
    ): EntityField[Refined[T, P]] =
      EntityField.from[T](refineV[P].unsafeFrom(_))(_.value)
  }

  object protobuf {
    import magnolify.protobuf.ProtobufField
    implicit def refinedProtobuf[T: ProtobufField, P](implicit
      v: Validate[T, P]
    ): ProtobufField[Refined[T, P]] =
      ProtobufField.from[T](refineV[P].unsafeFrom(_))(_.value)
  }

  object tensorflow {
    import magnolify.tensorflow.ExampleField
    implicit def refinedExample[T: ExampleField.Primitive, P](implicit
      v: Validate[T, P]
    ): ExampleField.Primitive[Refined[T, P]] =
      ExampleField.from[T](refineV[P].unsafeFrom(_))(_.value)
  }
}
