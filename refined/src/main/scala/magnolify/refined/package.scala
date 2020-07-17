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

package object refined extends RefinedAvro with RefinedBigQuery

trait RefinedAvro {
  import eu.timepit.refined.api.{RefType, Validate}
  import magnolify.avro._

  implicit def refAvroT[F[_, _], T, P](implicit
    af: AvroField[T],
    rt: RefType[F],
    v: Validate[T, P]
  ): AvroField[F[T, P]] =
    AvroField.from[T] { t =>
      val res = v.validate(t)
      if (res.isPassed)
        rt.unsafeWrap[T, P](t)
      else
        throw new RuntimeException(
          s"Refined predicate Failed: ${v.showExpr(t)}"
        )
    }(rt.unwrap)
}

trait RefinedBigQuery {
  import eu.timepit.refined.api.{RefType, Validate}
  import magnolify.bigquery.TableRowField

  implicit def refBqT[F[_, _], T, P](implicit
    btr: TableRowField[T],
    rt: RefType[F],
    v: Validate[T, P]
  ): TableRowField[F[T, P]] =
    TableRowField.from[T] { t: T =>
      val res = v.validate(t)
      if (res.isPassed)
        rt.unsafeWrap[T, P](t)
      else
        throw new RuntimeException(s"Refined predication failed: ${v.showExpr(t)}")
    }(rt.unwrap)

}
