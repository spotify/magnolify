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

package magnolify.scalacheck

import org.scalacheck._

sealed trait Fallback[+T] extends Serializable {
  def get: Gen[T]
}

object Fallback {

  private object Fail extends Fallback[Nothing] {
    override val get: Gen[Nothing] = Gen.fail
  }

  def apply[T](g: Gen[T]): Fallback[T] = new Fallback[T] {
    override def get: Gen[T] = g
  }

  def apply[T](v: T): Fallback[T] = Fallback[T](Gen.const(v))
  def apply[T](implicit arb: Arbitrary[T]): Fallback[T] = Fallback[T](arb.arbitrary)

  implicit def defaultFallback[T]: Fallback[T] = Fail
}
