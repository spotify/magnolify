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
package magnolify.parquet.test

import magnolify.parquet._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.{Job, TaskAttemptID}
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl
import org.apache.parquet.hadoop.{ParquetInputFormat, ParquetOutputFormat}
import org.apache.parquet.hadoop.metadata.CompressionCodecName

object Test {
  private val fs = FileSystem.getLocal(new Configuration())

  private def makeTemp: Path = {
    val tmp = sys.props("java.io.tmpdir")
    val ts = System.currentTimeMillis()
    val p = new Path(s"$tmp/parquet-type-$ts.parquet")
    fs.deleteOnExit(p)
    p
  }

  def main(args: Array[String]): Unit = {
    val temp = makeTemp
    val xs = (0 until 10).map(i => Record(i.toString, i))
    val job = Job.getInstance()

    val pt = ParquetType[Record]
    pt.setupOutput(job)
    val out = new ParquetOutputFormat[Record]()
    val writer = out.getRecordWriter(job.getConfiguration, temp, CompressionCodecName.GZIP)
    xs.foreach(writer.write(null, _))
    writer.close(null)

    pt.setupInput(job)
    val in = new ParquetInputFormat[Record]()
    val context = new TaskAttemptContextImpl(job.getConfiguration, new TaskAttemptID())
    FileInputFormat.setInputPaths(job, temp)
    val split = in.getSplits(job).get(0)
    val reader = in.createRecordReader(split, context)
    reader.initialize(split, context)
    while (reader.nextKeyValue()) {
      println(reader.getCurrentValue)
    }
    reader.close()
  }

  case class Record(s: String, i: Int)
}
