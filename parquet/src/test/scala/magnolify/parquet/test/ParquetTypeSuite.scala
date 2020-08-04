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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URI
import java.time._
import java.util.UUID

import cats._
import magnolify.cats.auto._
import magnolify.parquet._
import magnolify.parquet.unsafe._
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import magnolify.test.Time.Java8._
import magnolify.test._
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl
import org.apache.hadoop.mapreduce.{Job, TaskAttemptID}
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.apache.parquet.hadoop.{ParquetInputFormat, ParquetOutputFormat}
import org.apache.parquet.io._
import org.scalacheck._

import scala.reflect.ClassTag

class ParquetTypeSuite extends MagnolifySuite {
  private def test[T: Arbitrary: ClassTag](implicit
    t: ParquetType[T],
    eq: Eq[T]
  ): Unit = {
    val tpe = ensureSerializable(t)
    property(className[T]) {
      Prop.forAll { t: T =>
        val out = new TestOutputFile
        val writer = tpe.writeBuilder(out).build()
        writer.write(t)
        writer.close()

        val in = new TestInputFile(out.getBytes)
        val reader = tpe.readBuilder(in).build()
        val copy = reader.read()
        val next = reader.read()
        Prop.all(eq.eqv(t, copy), next == null)
      }
    }
  }

  test[Integers]
  test[Floats]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]
  test[Unsafe]

  {
    import Collections._
    test[Collections]
    test[MoreCollections]
  }

  {
    import Enums._
    test[Enums]
  }

  {
    import Custom._
    implicit val pfUri: ParquetField[URI] = ParquetField.from[String](URI.create)(_.toString)
    implicit val afDuration: ParquetField[Duration] =
      ParquetField.from[Long](Duration.ofMillis)(_.toMillis)
    test[Custom]
  }

  {
    implicit val eqByteArray: Eq[Array[Byte]] = Eq.by(_.toList)
    test[ParquetTypes]
  }

  // Precision = number of digits, so 5 means -99999 to 99999
  private def decimal(precision: Int): Arbitrary[BigDecimal] = {
    val nines = math.pow(10, precision).toLong - 1
    Arbitrary(Gen.choose(-nines, nines).map(BigDecimal(_)))
  }

  {
    implicit val arbBigDecimal: Arbitrary[BigDecimal] = decimal(9)
    implicit val pfBigDecimal: ParquetField[BigDecimal] = ParquetField.decimal32(9, 0)
    test[Decimal32]
  }

  {
    implicit val arbBigDecimal: Arbitrary[BigDecimal] = decimal(18)
    implicit val pfBigDecimal: ParquetField[BigDecimal] = ParquetField.decimal64(18, 0)
    test[Decimal64]
  }

  {
    implicit val arbBigDecimal: Arbitrary[BigDecimal] = decimal(18)
    // math.floor(math.log10(math.pow(2, 8*8-1) - 1)) = 18 digits
    implicit val pfBigDecimal: ParquetField[BigDecimal] = ParquetField.decimalFixed(8, 18, 0)
    test[DecimalFixed]
  }

  {
    implicit val arbBigDecimal: Arbitrary[BigDecimal] = decimal(20)
    implicit val pfBigDecimal: ParquetField[BigDecimal] = ParquetField.decimalBinary(20, 0)
    test[DecimalBinary]
  }

  test[Logical]

  {
    import magnolify.parquet.logical.millis._
    test[TimeMillis]
  }

  {
    import magnolify.parquet.logical.micros._
    test[TimeMicros]
  }

  {
    import magnolify.parquet.logical.nanos._
    test[TimeNanos]
  }

  test("Hadoop") {
    val job = Job.getInstance()
    val fs = FileSystem.getLocal(job.getConfiguration)
    val ts = System.currentTimeMillis()
    val path = new Path(sys.props("java.io.tmpdir"), s"parquet-type-$ts.parquet")
    fs.deleteOnExit(path)

    val xs = Gen.listOfN(100, Arbitrary.arbitrary[Nested]).sample.toList.flatten
    val pt = ParquetType[Nested]

    pt.setupOutput(job)
    val writer = new ParquetOutputFormat[Nested]()
      .getRecordWriter(job.getConfiguration, path, CompressionCodecName.GZIP)
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
    val actual = b.result()
    assertEquals(actual, xs)
    fs.delete(path, false)
  }
}

case class Unsafe(c: Char)
case class ParquetTypes(b: Byte, s: Short, ba: Array[Byte])
case class Decimal32(bd: BigDecimal)
case class Decimal64(bd: BigDecimal)
case class DecimalFixed(bd: BigDecimal)
case class DecimalBinary(bd: BigDecimal)
case class Logical(u: UUID, d: LocalDate)
case class TimeMillis(i: Instant, dt: LocalDateTime, ot: OffsetTime, t: LocalTime)
case class TimeMicros(i: Instant, dt: LocalDateTime, ot: OffsetTime, t: LocalTime)
case class TimeNanos(i: Instant, dt: LocalDateTime, ot: OffsetTime, t: LocalTime)

class TestInputFile(ba: Array[Byte]) extends InputFile {
  private val bais = new ByteArrayInputStream(ba)
  override def getLength: Long = ba.length

  override def newStream(): SeekableInputStream = new DelegatingSeekableInputStream(bais) {
    override def getPos: Long = ba.length - bais.available()
    override def seek(newPos: Long): Unit = {
      bais.reset()
      bais.skip(newPos)
    }
  }
}

class TestOutputFile extends OutputFile {
  private val baos = new ByteArrayOutputStream()
  def getBytes: Array[Byte] = baos.toByteArray

  override def create(blockSizeHint: Long): PositionOutputStream = new PositionOutputStream {
    private var pos = 0L
    override def getPos: Long = pos
    override def write(b: Int): Unit = {
      baos.write(b)
      pos += 1
    }
  }

  override def createOrOverwrite(blockSizeHint: Long): PositionOutputStream = ???
  override def supportsBlockSize(): Boolean = false
  override def defaultBlockSize(): Long = 0
}
