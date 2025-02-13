/*
 * Copyright 2025 Spotify AB
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

package magnolify.bigquery

import magnolify.bigquery.TableRowField.at

package object decimal {
  object numeric {
    implicit val trfNumeric: TableRowField[BigDecimal] =
      at[BigDecimal]("NUMERIC")(NumericConverter.toBigDecimal)(NumericConverter.fromBigDecimal)

    private object NumericConverter {
      // https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#numeric-type
      private val MaxPrecision = 38
      private val MaxScale = 9

      def toBigDecimal(v: Any): BigDecimal = BigDecimal(v.toString)

      def fromBigDecimal(v: BigDecimal): Any = {
        require(
          v.precision <= MaxPrecision,
          s"Cannot encode BigDecimal $v: precision ${v.precision} > $MaxPrecision"
        )
        require(v.scale <= MaxScale, s"Cannot encode BigDecimal $v: scale ${v.scale} > $MaxScale")
        v.toString()
      }
    }
  }

  object bignumeric {
    implicit val trfBigNumeric: TableRowField[BigDecimal] =
      at[BigDecimal]("BIGNUMERIC")(BigNumericConverter.toBigDecimal)(
        BigNumericConverter.fromBigDecimal
      )

    private object BigNumericConverter {
      // https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#numeric-type
      private val MaxScale = 38

      def toBigDecimal(v: Any): BigDecimal = BigDecimal(v.toString)
      def fromBigDecimal(v: BigDecimal): Any = {
        require(v.scale <= 38, s"Cannot encode BigDecimal $v: scale ${v.scale} > $MaxScale")
        require(
          v.precision <= v.scale + 38,
          s"Cannot encode BigDecimal $v: precision ${v.precision} > (scale[${v.scale}] + 38)"
        )

        v.toString()
      }
    }
  }
}
