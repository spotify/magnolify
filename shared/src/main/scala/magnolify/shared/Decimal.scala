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
package magnolify.shared

object Decimal {
  def fromBytes(ba: Array[Byte], precision: Int, scale: Int): BigDecimal = {
    val bd = BigDecimal(BigInt(ba), scale)
    require(
      bd.precision <= precision,
      s"Cannot decode BigDecimal $bd: precision ${bd.precision} > $precision"
    )
    bd
  }

  def toBytes(bd: BigDecimal, precision: Int, scale: Int): Array[Byte] = {
    val scaled = bd.setScale(scale)
    require(
      scaled.precision <= precision,
      s"Cannot encode BigDecimal $bd: precision ${scaled.precision} > $precision" +
        (if (bd.scale == scaled.scale) {
           ""
         } else {
           s" after set scale from ${bd.scale} to ${scaled.scale}"
         })
    )
    scaled.bigDecimal.unscaledValue().toByteArray
  }
}
