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
package magnolify.codegen

import com.google.common.base.CaseFormat

object Names {
  private def detectCase(name: String): CaseFormat = {
    var allLower = true
    var allUpper = true
    var hasHyphen = false
    var hasUnderscore = false
    name.foreach { c =>
      if (c.isLower) allUpper = false
      if (c.isUpper) allLower = false
      if (c == '-') hasHyphen = true
      if (c == '_') hasUnderscore = true
    }
    (hasHyphen, hasUnderscore) match {
      case (false, false) =>
        if (name.head.isUpper) CaseFormat.UPPER_CAMEL else CaseFormat.LOWER_CAMEL
      case (true, false) if allLower => CaseFormat.LOWER_HYPHEN
      case (false, true) if allLower => CaseFormat.LOWER_UNDERSCORE
      case (false, true) if allUpper => CaseFormat.UPPER_UNDERSCORE
      case _                         => throw new IllegalArgumentException(s"Unsupported case format: $name")
    }
  }

  def toUpperCamel(name: String): String =
    detectCase(name).to(CaseFormat.UPPER_CAMEL, name)
}
