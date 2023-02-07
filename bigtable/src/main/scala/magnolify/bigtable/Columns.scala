/*
 * Copyright 2023 Spotify AB
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

package magnolify.bigtable

import com.google.bigtable.v2.Column
import com.google.protobuf.ByteString

private object Columns {
  private def find(
    xs: java.util.List[Column],
    columnQualifier: String,
    matchPrefix: Boolean
  ): (Int, Int, Boolean) = {
    val cq = ByteString.copyFromUtf8(columnQualifier)
    val pre = if (matchPrefix) ByteString.copyFromUtf8(s"$columnQualifier.") else ByteString.EMPTY
    var low = 0
    var high = xs.size()
    var idx = -1
    var isNested = false
    while (idx == -1 && low < high) {
      val mid = (high + low) / 2
      val current = xs.get(mid).getQualifier
      if (matchPrefix && current.startsWith(pre)) {
        idx = mid
        isNested = true
      } else {
        val c = ByteStringComparator.INSTANCE.compare(current, cq)
        if (c < 0) {
          low = mid + 1
        } else if (c == 0) {
          idx = mid
          low = mid + 1
        } else {
          high = mid
        }
      }
    }

    if (isNested) {
      low = idx - 1
      while (low >= 0 && xs.get(low).getQualifier.startsWith(pre)) {
        low -= 1
      }
      high = idx + 1
      while (high < xs.size() && xs.get(high).getQualifier.startsWith(pre)) {
        high += 1
      }
      (low + 1, high, isNested)
    } else {
      (idx, idx, isNested)
    }
  }

  def find(xs: java.util.List[Column], columnQualifier: String): Column = {
    val (idx, _, _) = find(xs, columnQualifier, false)
    if (idx == -1) null else xs.get(idx)
  }

  def findNullable(
    xs: java.util.List[Column],
    columnQualifier: String
  ): Option[java.util.List[Column]] = {
    val (low, high, isNested) = find(xs, columnQualifier, true)
    if (isNested) {
      Some(xs.subList(low, high))
    } else {
      if (low == -1) None else Some(java.util.Collections.singletonList(xs.get(low)))
    }
  }
}
