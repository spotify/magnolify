/*
 * Copyright 2026 Spotify AB
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

import org.scalacheck.{Prop, Properties}

class DocSpec extends Properties("doc") {
  private class versionedDoc(doc: String, version: Int) extends doc(s"$doc, version: $version")
  private class nullDoc extends doc(null)

  private def getDoc(annotations: Seq[Any], name: String = "test"): Option[String] =
    doc.getDoc(annotations, name)

  private def rejects(annotations: Seq[Any], name: String): Prop = Prop {
    try {
      doc.getDoc(annotations, name)
      false
    } catch {
      case e: IllegalArgumentException =>
        e.getMessage == s"requirement failed: More than one @doc annotation: $name"
    }
  }

  property("no annotation") = getDoc(Seq.empty) == None
  property("single annotation") = getDoc(Seq(new doc("x"))) == Some("x")
  property("empty string is kept") = getDoc(Seq(new doc(""))) == Some("")
  property("null toString is dropped") = getDoc(Seq(new doc(null))) == None
  property("subclass") = getDoc(Seq(new versionedDoc("x", 1))) == Some("x, version: 1")
  property("null toString subclass is dropped") = getDoc(Seq(new nullDoc)) == None
  property("duplicate") = rejects(Seq(new doc("a"), new doc("b")), "duplicate")
  property("duplicate with null") = rejects(Seq(new doc(null), new doc("b")), "duplicate")
}
