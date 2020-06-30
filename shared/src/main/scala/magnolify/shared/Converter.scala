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
package magnolify.shared

import java.util.UUID

trait Converter[T, Reader, Writer] extends Serializable {
  protected val caseMapper: CaseMapper = CaseMapper.identity // TODO: remove the default
  def from(v: Reader): T
  def to(v: T): Writer
}

sealed trait CaseMapper extends Serializable {
  val uuid: UUID
  def map(label: String): String
}

object CaseMapper {
  def apply(f: String => String): CaseMapper = new CaseMapper {
    override val uuid: UUID = UUID.randomUUID()
    override def map(label: String): String = f(label)
  }

  val identity: CaseMapper = new CaseMapper {
    override val uuid: UUID = UUID.nameUUIDFromBytes(Array.emptyByteArray)
    override def map(label: String): String = label
  }
}
