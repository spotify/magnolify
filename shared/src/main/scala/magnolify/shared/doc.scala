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

package magnolify.shared

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag

class doc(doc: String) extends StaticAnnotation with Serializable {
  override def toString: String = doc
}

object doc {
  private[magnolify] def getDoc(annotations: Seq[Any], name: String): Option[String] =
    getDocAs[doc](annotations, name)

  // `D` keeps each module's own annotation family apart, e.g. `magnolify.tensorflow.doc`
  private[magnolify] def getDocAs[D](annotations: Seq[Any], name: String)(implicit
    ct: ClassTag[D]
  ): Option[String] = {
    // `Option` guards against a `@doc` whose `toString` returns `null`
    val docs = annotations.collect { case ct(d) => Option(d.toString) }
    require(docs.size <= 1, s"More than one @doc annotation: $name")
    docs.headOption.flatten
  }
}
