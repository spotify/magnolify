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
package magnolify.cats.semiauto

import cats.Group
import magnolia._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

object GroupDerivation {
  type Typeclass[T] = Group[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = new Group[T] {
    override def inverse(a: T): T = caseClass.construct { p =>
      p.typeclass.inverse(p.dereference(a))
    }

    override val empty: T = caseClass.construct(_.typeclass.empty)

    override def combine(x: T, y: T): T = caseClass.construct { p =>
      p.typeclass.combine(p.dereference(x), p.dereference(y))
    }

    override def remove(a: T, b: T): T = caseClass.construct { p =>
      p.typeclass.remove(p.dereference(a), p.dereference(b))
    }
  }

  @implicitNotFound("Cannot derive Group for sealed trait")
  private sealed trait Dispatchable[T]
  def dispatch[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def apply[T]: Typeclass[T] = macro Magnolia.gen[T]
}
