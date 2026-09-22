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

class doc(doc: String) extends StaticAnnotation with Serializable {
  override def toString: String = doc
}

object doc {

  /**
   * Extract doc string from annotations, validating that only the correct @doc type is used.
   *
   * This helper detects when users accidentally use @doc from a different package
   * (e.g., com.spotify.scio.avro.types.doc) instead of magnolify.shared.doc. Such annotations are
   * silently ignored by Magnolify's type derivation, leading to missing documentation in generated
   * schemas.
   *
   * @param annotations
   *   The annotations to inspect (from Magnolia's CaseClass or Param)
   * @param name
   *   A descriptive name for the annotated element (for error messages)
   * @return
   *   Some(docString) if present, otherwise None
   * @throws IllegalArgumentException
   *   if a wrong @doc annotation is found without a correct one
   */
  def extract(annotations: Seq[Any], name: String): Option[String] = {
    val correctDocs = annotations.collect { case d: doc => d.toString }
    require(correctDocs.size <= 1, s"More than one @doc annotation: $name")

    // Check for @doc annotations from unexpected packages (e.g., com.spotify.scio.avro.types.doc)
    // Only fail if there's no correct @doc - if both are present, assume the user knows what they're doing.
    if (correctDocs.isEmpty) {
      val wrongDocs = annotations.filter { a =>
        a.getClass.getSimpleName == classOf[doc].getSimpleName && !a.isInstanceOf[doc]
      }
      require(
        wrongDocs.isEmpty,
        s"Found @doc annotation(s) from unexpected package(s) on $name: " +
          s"${wrongDocs.map(_.getClass.getName).distinct.mkString(", ")}. " +
          s"Use ${classOf[doc].getName} instead."
      )
    }

    correctDocs.headOption
  }
}
