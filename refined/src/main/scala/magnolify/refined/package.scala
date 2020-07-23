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

import eu.timepit.refined.api.{RefType, Validate}

package object refined extends RefinedAvro with RefinedBigQuery with RefinedBigTable

trait RefinedBase extends Serializable {
  def wrap[F[_, _], T, P](t: T)(implicit rt: RefType[F], v: Validate[T, P]): F[T, P] = {
    val res = v.validate(t)
    if (res.isPassed)
      rt.unsafeWrap[T, P](t)
    else
      throw new RuntimeException(
        s"Refined predicate Failed: ${v.showExpr(t)}"
      )
  }
}

trait RefinedAvro extends RefinedBase {
  import magnolify.avro._
  import eu.timepit.refined.api.{RefType, Validate}

  implicit def refAvroT[F[_, _], T, P](implicit
    af: AvroField[T],
    rt: RefType[F],
    v: Validate[T, P]
  ): AvroField[F[T, P]] =
    AvroField.from[T](wrap[F, T, P])(rt.unwrap)
}

trait RefinedBigQuery extends RefinedBase {
  import magnolify.bigquery.TableRowField

  implicit def refBqT[F[_, _], T, P](implicit
    btr: TableRowField[T],
    rt: RefType[F],
    v: Validate[T, P]
  ): TableRowField[F[T, P]] =
    TableRowField.from[T](wrap[F, T, P])(rt.unwrap)

}

trait RefinedBigTable extends RefinedBase {
  import magnolify.bigtable.BigtableField
  import magnolify.bigtable.BigtableField.Primitive

  implicit def refBT[F[_, _], T, P](implicit
    btc: Primitive[T],
    rt: RefType[F],
    v: Validate[T, P]
  ): BigtableField[F[T, P]] =
    BigtableField.from[T](wrap[F, T, P])(rt.unwrap)

}
