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

import magnolify.parquet.ParquetType.WriteSupport
import magnolify.parquet.{MagnolifyParquetProperties, ParquetType}

import java.util.concurrent.TimeUnit
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import org.apache.hadoop.conf.Configuration
import org.scalacheck._
import org.openjdk.jmh.annotations._

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

@State(Scope.Benchmark)
class ParquetReadState(pt: ParquetType[Nested]) {
  import org.apache.parquet.io._
  import org.apache.parquet.column.impl.ColumnWriteStoreV1
  import org.apache.parquet.column.ParquetProperties
  import org.apache.parquet.hadoop.api.InitContext

  var reader: RecordReader[Nested] = null

  @Setup(Level.Invocation)
  def setup(): Unit = {
    // Write page
    val columnIO = new ColumnIOFactory(true).getColumnIO(pt.schema)
    val memPageStore = new ParquetInMemoryPageStore(1)
    val columns = new ColumnWriteStoreV1(
      pt.schema,
      memPageStore,
      ParquetProperties.builder.withPageSize(800).withDictionaryEncoding(false).build
    )
    val writeSupport = pt.writeSupport
    val recordWriter = columnIO.getRecordWriter(columns)
    writeSupport.prepareForWrite(recordWriter)
    writeSupport.write(MagnolifyBench.nested)
    recordWriter.flush()
    columns.flush()

    // Read and convert page
    val conf = new Configuration()
    val readSupport = pt.readSupport
    reader = columnIO.getRecordReader(
      memPageStore,
      readSupport.prepareForRead(
        conf,
        new java.util.HashMap,
        pt.schema,
        readSupport.init(new InitContext(conf, new java.util.HashMap, pt.schema)))
    )
  }
}

@State(Scope.Benchmark)
class ParquetWriteState(pt: ParquetType[Nested]) {
  import org.apache.parquet.io._
  import org.apache.parquet.column.impl.ColumnWriteStoreV1
  import org.apache.parquet.column.ParquetProperties

  var writer: WriteSupport[Nested] = null

  @Setup(Level.Invocation)
  def setup(): Unit = {
    val columnIO = new ColumnIOFactory(true).getColumnIO(pt.schema)
    val memPageStore = new ParquetInMemoryPageStore(1)
    val columns = new ColumnWriteStoreV1(
      pt.schema,
      memPageStore,
      ParquetProperties.builder.withPageSize(800).withDictionaryEncoding(false).build
    )
    val writeSupport = pt.writeSupport
    val recordWriter = columnIO.getRecordWriter(columns)
    writeSupport.prepareForWrite(recordWriter)
    this.writer = writeSupport
  }
}

object ParquetStates {
  def confWithGroupedArraysProp(propValue: Boolean): Configuration = {
    val conf = new Configuration()
    conf.setBoolean(MagnolifyParquetProperties.WriteGroupedArrays, propValue)
    conf
  }
  class DefaultParquetReadState extends ParquetReadState(ParquetType[Nested](confWithGroupedArraysProp(false)))
  class DefaultParquetWriteState extends ParquetWriteState(ParquetType[Nested](confWithGroupedArraysProp(false)))

  class ParquetAvroCompatReadState extends ParquetReadState(ParquetType[Nested](confWithGroupedArraysProp(true)))
  class ParquetAvroCompatWriteState extends ParquetWriteState(ParquetType[Nested](confWithGroupedArraysProp(true)))
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class ParquetBench {
  import MagnolifyBench._

  @Benchmark def parquetWrite(state: ParquetStates.DefaultParquetWriteState): Unit = state.writer.write(nested)
  @Benchmark def parquetRead(state: ParquetStates.DefaultParquetReadState): Nested = state.reader.read()
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class ParquetAvroCompatBench {
  import MagnolifyBench._

  @Benchmark def parquetWrite(state: ParquetStates.ParquetAvroCompatWriteState): Unit = state.writer.write(nested)
  @Benchmark def parquetRead(state: ParquetStates.ParquetAvroCompatReadState): Nested = state.reader.read()
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
  @Benchmark def exampleFrom: ExampleNested = exampleType.from(example.getFeatures.getFeatureMap.asScala.toMap)
}

// Collections are not supported
case class BigtableNested(b: Boolean, i: Int, s: String, r: Required, o: Option[Required])

// Option[T] and Seq[T] not supported
case class ExampleNested(b: Boolean, i: Int, s: String, r: Required, o: Option[Required])
