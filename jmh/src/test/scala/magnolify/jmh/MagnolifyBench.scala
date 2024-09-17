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

import magnolify.jmh.MagnolifyBench.nested
import magnolify.parquet.{MagnolifyParquetProperties, ParquetType, TestInputFile, TestOutputFile}

import java.util.concurrent.TimeUnit
import magnolify.scalacheck.auto.*
import magnolify.test.Simple.*
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.{ParquetReader, ParquetWriter}
import org.scalacheck.*
import org.openjdk.jmh.annotations.*

import scala.jdk.CollectionConverters.*

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
class ParquetReadState()(implicit pt: ParquetType[Nested]) {
  var out: TestOutputFile = null
  var reader: ParquetReader[Nested] = null

  @Setup(Level.Invocation)
  def setup(): Unit = {
    out = new TestOutputFile
    val writer = pt.writeBuilder(out).build()
    writer.write(nested)
    writer.close()

    val in = new TestInputFile(out.getBytes)
    reader = pt.readBuilder(in).build()
  }

  @TearDown(Level.Invocation)
  def tearDown(): Unit = {
    reader.close()
  }
}

@State(Scope.Benchmark)
class ParquetWriteState()(implicit pt: ParquetType[Nested]) {
  var writer: ParquetWriter[Nested] = null

  @Setup(Level.Invocation)
  def setup(): Unit = {
    val out = new TestOutputFile
    writer = pt.writeBuilder(out).build()
  }

  @TearDown(Level.Invocation)
  def tearDown(): Unit = {
    writer.close()
  }
}


@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class ParquetBench {
  import MagnolifyBench._
  implicit val pt: ParquetType[Nested] = ParquetType[Nested]

  @Benchmark def parquetWrite(state: ParquetWriteState): Unit = state.writer.write(nested)
  @Benchmark def parquetRead(state: ParquetReadState): Nested = state.reader.read()
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class ParquetAvroCompatBench {
  import MagnolifyBench._
  val conf = new Configuration()
  conf.setBoolean(MagnolifyParquetProperties.WriteGroupedArrays, MagnolifyParquetProperties.WriteGroupedArraysDefault)
  implicit val pt: ParquetType[Nested] = ParquetType[Nested](conf)

  @Benchmark def parquetWrite(state: ParquetWriteState): Unit = state.writer.write(nested)
  @Benchmark def parquetRead(state: ParquetReadState): Nested = state.reader.read()
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
