/*
 * Copyright 2019 Spotify AB
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

import java.util.concurrent.TimeUnit
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import org.scalacheck._
import org.openjdk.jmh.annotations._

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

object MagnolifyBench {
  val seed: rng.Seed = rng.Seed(0)
  val prms: Gen.Parameters = Gen.Parameters.default
  val nested: Nested = implicitly[Arbitrary[Nested]].arbitrary(prms, seed).get
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class ScalaCheckBench {
  import MagnolifyBench._
  private val arb = implicitly[Arbitrary[Nested]]
  private val co = implicitly[Cogen[Nested]]
  @Benchmark def arbitrary: Option[Nested] = arb.arbitrary(prms, seed)
  @Benchmark def cogen: rng.Seed = co.perturb(rng.Seed(0), nested)
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class CatsBench {
  import magnolify.cats.semiauto._
  import MagnolifyBench._
  private val integers = implicitly[Arbitrary[Integers]].arbitrary(prms, seed).get
  private val xs = Array.fill(100)(integers)
  private val sg = SemigroupDerivation[Integers]
  private val mon = MonoidDerivation[Integers]
  private val grp = GroupDerivation[Integers]
  private val h = HashDerivation[Nested]
  private val e = EqDerivation[Nested]
  @Benchmark def semigroupCombine: Integers = sg.combine(integers, integers)
  @Benchmark def semigroupCombineN: Integers = sg.combineN(xs.head, 100)
  @Benchmark def semigroupCombineAllOption: Option[Integers] = sg.combineAllOption(xs)
  @Benchmark def monoidCombine: Integers = mon.combine(integers, integers)
  @Benchmark def monoidCombineN: Integers = mon.combineN(xs.head, 100)
  @Benchmark def monoidCombineAllOption: Option[Integers] = mon.combineAllOption(xs)
  @Benchmark def monoidCombineAll: Integers = mon.combineAll(xs)
  @Benchmark def monoidEmpty: Integers = mon.empty
  @Benchmark def groupCombine: Integers = grp.combine(integers, integers)
  @Benchmark def groupCombineN: Integers = grp.combineN(xs.head, 100)
  @Benchmark def groupCombineAllOption: Option[Integers] = grp.combineAllOption(xs)
  @Benchmark def groupCombineAll: Integers = grp.combineAll(xs)
  @Benchmark def groupEmpty: Integers = grp.empty
  @Benchmark def groupInverse: Integers = grp.inverse(integers)
  @Benchmark def groupRemove: Integers = grp.remove(integers, integers)
  @Benchmark def hash: Int = h.hash(nested)
  @Benchmark def eq: Boolean = e.eqv(nested, nested)
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class AvroBench {
  import magnolify.avro._
  import org.apache.avro.Schema
  import org.apache.avro.generic.GenericRecord
  import MagnolifyBench._
  private val avroType = AvroType[Nested]
  private val genericRecord = avroType.to(nested)
  @Benchmark def avroTo: GenericRecord = avroType.to(nested)
  @Benchmark def avroFrom: Nested = avroType.from(genericRecord)
  @Benchmark def avroSchema: Schema = AvroType[Nested].schema
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class TableRowBench {
  import com.google.api.services.bigquery.model.{TableRow, TableSchema}
  import magnolify.bigquery._
  import magnolify.bigquery.unsafe._
  import MagnolifyBench._
  private val tableRowType = TableRowType[Nested]
  private val tableRow = tableRowType.to(nested)
  @Benchmark def tableRowTo: TableRow = tableRowType.to(nested)
  @Benchmark def tableRowFrom: Nested = tableRowType.from(tableRow)
  @Benchmark def tableRowSchema: TableSchema = TableRowType[Nested].schema
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class BigtableBench {
  import com.google.bigtable.v2.Mutation
  import com.google.protobuf.ByteString
  import magnolify.bigtable._
  import MagnolifyBench._
  private val bigtableType = BigtableType[BigtableNested]
  private val bigtableNested = implicitly[Arbitrary[BigtableNested]].arbitrary(prms, seed).get
  private val mutations = bigtableType(bigtableNested, "cf")
  private val row = BigtableType.mutationsToRow(ByteString.EMPTY, mutations)
  @Benchmark def bigtableTo: Seq[Mutation] = bigtableType(bigtableNested, "cf")
  @Benchmark def bigtableFrom: BigtableNested = bigtableType(row, "cf")
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class EntityBench {
  import com.google.datastore.v1.Entity
  import magnolify.datastore._
  import magnolify.datastore.unsafe._
  import MagnolifyBench._
  private val entityType = EntityType[Nested]
  private val entity = entityType.to(nested).build()
  @Benchmark def entityTo: Entity.Builder = entityType.to(nested)
  @Benchmark def entityFrom: Nested = entityType.from(entity)
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class ProtobufBench {
  import magnolify.protobuf._
  import magnolify.protobuf.Proto2._
  import MagnolifyBench._
  private val protoType = ProtobufType[Nested, NestedP2]
  private val protoNested = protoType.to(nested)
  @Benchmark def protoTo: NestedP2 = protoType.to(nested)
  @Benchmark def protoFrom: Nested = protoType.from(protoNested)
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class ExampleBench {
  import magnolify.tensorflow._
  import magnolify.tensorflow.unsafe._
  import org.tensorflow.proto.example.Example
  import MagnolifyBench._
  private val exampleType = ExampleType[ExampleNested]
  private val exampleNested = implicitly[Arbitrary[ExampleNested]].arbitrary(prms, seed).get
  private val example = exampleType.to(exampleNested).build()
  @Benchmark def exampleTo: Example.Builder = exampleType.to(exampleNested)
  @Benchmark def exampleFrom: ExampleNested =
    exampleType.from(example.getFeatures.getFeatureMap.asScala.toMap)
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class ParquetBench {
  import MagnolifyBench._
  import ParquetStates._
  import magnolify.avro._
  import org.apache.avro.generic.GenericRecord

  private val genericRecord = AvroType[Nested].to(nested)

  @Benchmark def parquetWriteMagnolify(state: ParquetCaseClassWriteState): Unit =
    state.writer.write(nested)
  @Benchmark def parquetWriteAvro(state: ParquetAvroWriteState): Unit =
    state.writer.write(genericRecord)

  @Benchmark def parquetReadMagnolify(state: ParquetCaseClassReadState): Nested =
    state.reader.read()
  @Benchmark def parquetReadAvro(state: ParquetAvroReadState): GenericRecord = state.reader.read()
}

object ParquetStates {
  import MagnolifyBench._
  import magnolify.avro._
  import magnolify.parquet._
  import magnolify.parquet.ParquetArray.AvroCompat._
  import org.apache.avro.generic.{GenericData, GenericRecord}
  import org.apache.hadoop.conf.Configuration
  import org.apache.parquet.conf.PlainParquetConfiguration
  import org.apache.parquet.avro.{AvroReadSupport, AvroWriteSupport}
  import org.apache.parquet.column.ParquetProperties
  import org.apache.parquet.hadoop.api.{ReadSupport, WriteSupport}
  import org.apache.parquet.schema.MessageType
  import org.apache.parquet.io._
  import org.apache.parquet.io.api.{Binary, RecordConsumer}
  import org.apache.parquet.column.impl.ColumnWriteStoreV1

  @State(Scope.Benchmark)
  class ReadState[T](
    schema: MessageType,
    writeSupport: WriteSupport[T],
    readSupport: ReadSupport[T],
    record: T
  ) {
    import org.apache.parquet.hadoop.api.InitContext

    var reader: RecordReader[T] = null

    @Setup(Level.Trial)
    def setup(): Unit = {
      // Write page
      val columnIO = new ColumnIOFactory(true).getColumnIO(schema)
      val pageStore = new ParquetInMemoryPageStore(1)
      val columnWriteStore = new ColumnWriteStoreV1(
        schema,
        pageStore,
        ParquetProperties.builder.withPageSize(800).withDictionaryEncoding(false).build
      )
      val recordConsumer = columnIO.getRecordWriter(columnWriteStore)
      writeSupport.init(new PlainParquetConfiguration())
      writeSupport.prepareForWrite(recordConsumer)
      writeSupport.write(record)
      recordConsumer.flush()
      columnWriteStore.flush()

      // Set up reader
      val conf = new Configuration()
      reader = columnIO.getRecordReader(
        pageStore,
        readSupport.prepareForRead(
          conf,
          new java.util.HashMap,
          schema,
          readSupport.init(new InitContext(conf, new java.util.HashMap, schema))
        )
      ): @nowarn("cat=deprecation")
    }
  }

  @State(Scope.Benchmark)
  class WriteState[T](writeSupport: WriteSupport[T]) {
    val writer = writeSupport

    @Setup(Level.Trial)
    def setup(): Unit = {
      writeSupport.init(new PlainParquetConfiguration())
      // Use a no-op RecordConsumer; we want to measure only the record -> group conversion, and not pollute the
      // benchmark with background tasks like flushing pages/blocks or validating records
      writeSupport.prepareForWrite(new RecordConsumer {
        override def startMessage(): Unit = {}
        override def endMessage(): Unit = {}
        override def startField(field: String, index: Int): Unit = {}
        override def endField(field: String, index: Int): Unit = {}
        override def startGroup(): Unit = {}
        override def endGroup(): Unit = {}
        override def addInteger(value: Int): Unit = {}
        override def addLong(value: Long): Unit = {}
        override def addBoolean(value: Boolean): Unit = {}
        override def addBinary(value: Binary): Unit = {}
        override def addFloat(value: Float): Unit = {}
        override def addDouble(value: Double): Unit = {}
      })
    }
  }

  // R/W support for Group <-> Case Class Conversion (magnolify-parquet)
  private val parquetType = ParquetType[Nested]
  class ParquetCaseClassReadState
      extends ParquetStates.ReadState[Nested](
        parquetType.schema,
        parquetType.writeSupport,
        parquetType.readSupport,
        nested
      )
  class ParquetCaseClassWriteState
      extends ParquetStates.WriteState[Nested](parquetType.writeSupport)

  // R/W support for Group <-> Avro Conversion (parquet-avro)
  private val avroType = AvroType[Nested]
  class ParquetAvroReadState
      extends ParquetStates.ReadState[GenericRecord](
        parquetType.schema,
        new AvroWriteSupport[GenericRecord](
          parquetType.schema,
          parquetType.avroSchema,
          GenericData.get()
        ),
        new AvroReadSupport[GenericRecord](GenericData.get()),
        avroType.to(nested)
      )
  class ParquetAvroWriteState
      extends ParquetStates.WriteState[GenericRecord](
        new AvroWriteSupport[GenericRecord](
          parquetType.schema,
          parquetType.avroSchema,
          GenericData.get()
        )
      )
}

// Collections are not supported
case class BigtableNested(b: Boolean, i: Int, s: String, r: Required, o: Option[Required])

// Option[T] and Seq[T] not supported
case class ExampleNested(b: Boolean, i: Int, s: String, r: Required, o: Option[Required])
