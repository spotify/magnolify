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

package magnolify.avro

import magnolify.test.MagnolifySuite

/**
 * Tests for detecting wrong @doc annotations from unexpected packages.
 *
 * When users accidentally use @doc from a different package (e.g., com.spotify.scio.avro.types.doc)
 * instead of magnolify.shared.doc, the doc strings are silently ignored. This test verifies
 * that Magnolify now detects and reports this issue.
 */
class WrongDocAnnotationSuite extends MagnolifySuite {

  test("WrongDocOnlyOnField - should fail when field has wrong @doc without correct @doc") {
    val ex = intercept[IllegalArgumentException] {
      AvroType[WrongDocOnlyOnField]
    }
    assert(ex.getMessage.contains("unexpected package"))
    assert(ex.getMessage.contains("WrongDocOnlyOnField#userId"))
  }

  test("WrongDocOnlyOnRecord - should fail when record has wrong @doc without correct @doc") {
    val ex = intercept[IllegalArgumentException] {
      AvroType[WrongDocOnlyOnRecord]
    }
    assert(ex.getMessage.contains("unexpected package"))
    assert(ex.getMessage.contains("WrongDocOnlyOnRecord"))
  }

  test("BothDocsOnField - should succeed when field has both wrong and correct @doc") {
    // Having both annotations means the user knows what they're doing
    val at = AvroType[BothDocsOnField]
    // The correct @doc should be used
    val field = at.schema.getField("userId")
    assertEquals(field.doc(), "correct doc from magnolify")
  }

  test("BothDocsOnRecord - should succeed when record has both wrong and correct @doc") {
    val at = AvroType[BothDocsOnRecord]
    assertEquals(at.schema.getDoc, "correct record doc from magnolify")
  }

  test("CorrectDocOnly - should succeed with only magnolify @doc") {
    val at = AvroType[CorrectDocOnly]
    val field = at.schema.getField("userId")
    assertEquals(field.doc(), "correct doc")
  }
}

// Test case: field has only the wrong @doc
case class WrongDocOnlyOnField(
    @magnolify.avro.wrongpkg.doc("wrong doc") userId: String
)

// Test case: record has only the wrong @doc
@magnolify.avro.wrongpkg.doc("wrong record doc")
case class WrongDocOnlyOnRecord(userId: String)

// Test case: field has both wrong and correct @doc - should succeed
case class BothDocsOnField(
    @magnolify.avro.wrongpkg.doc("wrong doc") @doc("correct doc from magnolify") userId: String
)

// Test case: record has both wrong and correct @doc - should succeed
@magnolify.avro.wrongpkg.doc("wrong record doc")
@doc("correct record doc from magnolify")
case class BothDocsOnRecord(userId: String)

// Test case: only correct @doc - should succeed
case class CorrectDocOnly(
    @doc("correct doc") userId: String
)
