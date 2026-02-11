/*
 * Copyright 2021 Spotify AB
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

package magnolify.parquet

import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import munit._
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.{Job, TaskAttemptID}
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.apache.parquet.hadoop.{ParquetInputFormat, ParquetOutputFormat}
import org.scalacheck.{Arbitrary, Gen}

class HadoopSuite extends FunSuite {
  test("Hadoop") {
    assume(
      System.getProperty("java.version").stripPrefix("1.").takeWhile(_.isDigit).toInt < 22,
      "Skip on Java 22+ until Hadoop 3.4.3 is released"
    )

    val job = Job.getInstance()
    val fs = FileSystem.getLocal(job.getConfiguration)
    val ts = System.currentTimeMillis()
    val path = new Path(sys.props("java.io.tmpdir"), s"parquet-type-$ts.parquet")
    fs.deleteOnExit(path)

    val xs = Gen.listOfN(100, Arbitrary.arbitrary[Nested]).sample.toList.flatten
    val pt = ParquetType[Nested]

    pt.setupOutput(job)
    val writer = new ParquetOutputFormat[Nested]()
      .getRecordWriter(job.getConfiguration, path, CompressionCodecName.UNCOMPRESSED)

    xs.foreach(writer.write(null, _))
    writer.close(null)

    pt.setupInput(job)
    val context = new TaskAttemptContextImpl(job.getConfiguration, new TaskAttemptID())
    FileInputFormat.setInputPaths(job, path)
    val in = new ParquetInputFormat[Nested]()
    val splits = in.getSplits(job)
    require(splits.size() == 1)
    val reader = in.createRecordReader(splits.get(0), context)
    reader.initialize(splits.get(0), context)

    val b = List.newBuilder[Nested]
    while (reader.nextKeyValue()) {
      b += reader.getCurrentValue
    }
    reader.close()
    val actual = b.result()

    assertEquals(actual, xs)
    fs.delete(path, false)
  }
}
