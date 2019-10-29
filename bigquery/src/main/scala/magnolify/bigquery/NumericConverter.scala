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
package magnolify.bigquery

import java.math.MathContext

private object NumericConverter {
  def toBigDecimal(v: Any): BigDecimal = scale(BigDecimal(v.toString))
  def fromBigDecimal(v: BigDecimal): Any = scale(v).toString()

  private val MaxNumericPrecision = 38
  private val MaxNumericScale = 9

  private def scale(value: BigDecimal): BigDecimal = {
    // NUMERIC's max scale is 9, precision is 38
    val scaled = if (value.scale > MaxNumericScale) {
      value.setScale(MaxNumericScale, scala.math.BigDecimal.RoundingMode.HALF_UP)
    } else {
      value
    }
    require(
      scaled.precision <= MaxNumericPrecision,
      s"max allowed precision is $MaxNumericPrecision"
    )
    BigDecimal(scaled.toString, new MathContext(MaxNumericPrecision))
  }
}
