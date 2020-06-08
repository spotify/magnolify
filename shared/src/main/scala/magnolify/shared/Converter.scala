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

trait Converter[T, Reader, Writer] extends Serializable {
  val caseMapper: CaseMapper = identity
  def from(v: Reader): T
  def to(v: T): Writer
}

trait CaseMapper extends Serializable {
  def map(name: String): String
}

object CaseMapper {
  val toSnakeCase = new CaseMapper {
    override def map(name: String): String = {
      def snakify(chars: List[Char]): List[Char] =  chars match {
        case '-' :: ls => '_' :: snakify(ls) // kebab-case => kebab_case 
        case '_' :: ls => '_' :: snakify(ls) // snake_case => snake_case
        case c :: ls if(c.toUpper == c) => '_' :: (c.toLower :: snakify(ls)) // camelCase => camel_case 
        case c :: ls => c :: snakify(ls)
        case a => a
      }
      if (name.isEmpty()) name 
      else (name.charAt(0).toLower :: snakify(name.substring(1).toList)).mkString
    } 
  }
}