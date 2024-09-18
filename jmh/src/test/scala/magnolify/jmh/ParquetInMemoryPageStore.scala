/*
 * Copyright 2024 Spotify AB
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

package magnolify.jmh

import org.apache.parquet.bytes.{ByteBufferReleaser, BytesInput, HeapByteBufferAllocator}
import org.apache.parquet.column.{ColumnDescriptor, Encoding}
import org.apache.parquet.column.page._
import org.apache.parquet.column.statistics._

import scala.collection.mutable

/**
 * An in-memory Parquet page store modeled after parquet-java's MemPageStore, used to benchmark
 * ParquetType conversion between Parquet Groups and Scala case classes
 */
class ParquetInMemoryPageStore(rowCount: Long) extends PageReadStore with PageWriteStore {
  lazy val writers = new mutable.HashMap[ColumnDescriptor, ParquetInMemoryWriter]()
  lazy val readers = new mutable.HashMap[ColumnDescriptor, ParquetInMemoryReader]()

  override def getPageReader(path: ColumnDescriptor): PageReader =
    readers.getOrElseUpdate(
      path, {
        val writer = writers(path)
        new ParquetInMemoryReader(writer.numValues, writer.pages.toList, writer.dictionaryPage)
      }
    )

  override def getPageWriter(path: ColumnDescriptor): PageWriter =
    writers.getOrElseUpdate(path, new ParquetInMemoryWriter())

  override def getRowCount: Long = rowCount
}

class ParquetInMemoryReader(valueCount: Long, pages: List[DataPage], dictionaryPage: DictionaryPage)
    extends PageReader {
  lazy val pagesIt = pages.iterator

  override def readDictionaryPage(): DictionaryPage = dictionaryPage
  override def getTotalValueCount: Long = valueCount
  override def readPage(): DataPage = pagesIt.next()
}

class ParquetInMemoryWriter extends PageWriter {
  var numRows = 0
  var numValues: Long = 0
  var memSize: Long = 0
  val pages = new mutable.ListBuffer[DataPage]()
  var dictionaryPage: DictionaryPage = null

  override def writePage(
    bytesInput: BytesInput,
    valueCount: Int,
    statistics: Statistics[_],
    rlEncoding: Encoding,
    dlEncoding: Encoding,
    valuesEncoding: Encoding
  ): Unit =
    writePage(bytesInput, valueCount, 1, statistics, rlEncoding, dlEncoding, valuesEncoding)

  override def writePage(
    bytesInput: BytesInput,
    valueCount: Int,
    rowCount: Int,
    statistics: Statistics[_],
    sizeStatistics: SizeStatistics,
    rlEncoding: Encoding,
    dlEncoding: Encoding,
    valuesEncoding: Encoding
  ): Unit =
    writePage(bytesInput, valueCount, rowCount, statistics, rlEncoding, dlEncoding, valuesEncoding)

  override def writePage(
    bytesInput: BytesInput,
    valueCount: Int,
    rowCount: Int,
    statistics: Statistics[_],
    rlEncoding: Encoding,
    dlEncoding: Encoding,
    valuesEncoding: Encoding
  ): Unit = {
    pages.addOne(
      new DataPageV1(
        bytesInput.copy(new ByteBufferReleaser(new HeapByteBufferAllocator)),
        valueCount,
        bytesInput.size().toInt,
        statistics,
        rlEncoding,
        dlEncoding,
        valuesEncoding
      )
    )
    memSize += bytesInput.size()
    numRows += rowCount
    numValues += valueCount
  }

  override def writePageV2(
    rowCount: Int,
    nullCount: Int,
    valueCount: Int,
    repetitionLevels: BytesInput,
    definitionLevels: BytesInput,
    dataEncoding: Encoding,
    data: BytesInput,
    statistics: Statistics[_]
  ): Unit = ???

  override def getMemSize: Long = memSize

  override def allocatedSize(): Long = memSize

  override def writeDictionaryPage(dictionaryPage: DictionaryPage): Unit =
    this.dictionaryPage = dictionaryPage

  override def memUsageString(prefix: String): String = s"$prefix $memSize bytes"
}
