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

package magnolify.bigtable

import com.google.bigtable.v2.Mutation
import com.google.bigtable.v2.Mutation.SetCell
import com.google.protobuf.ByteString

private[bigtable] object Mutations {

  def newSetCell(columnQualifier: ByteString, value: ByteString): SetCell.Builder =
    SetCell
      .newBuilder()
      .setColumnQualifier(columnQualifier)
      .setValue(value)

  def newSetCellMutation(familyName: String,
                         timestampMicros: Long)
                        (setCell: SetCell.Builder): Mutation =
    Mutation
      .newBuilder()
      .setSetCell(
          setCell
          .setFamilyName(familyName)
          .setTimestampMicros(timestampMicros)
      )
      .build()

}
