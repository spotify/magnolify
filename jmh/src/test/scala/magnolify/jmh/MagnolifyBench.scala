/*
 * Copyright 2019 Spotify AB.
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
package magnolify.jmh

import java.util.concurrent.TimeUnit

import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import org.scalacheck._
import org.openjdk.jmh.annotations._

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
  import cats._
  import cats.instances.all._
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
  @Benchmark def monoidEmpty: Integers = mon.empty
  @Benchmark def groupCombine: Integers = grp.combine(integers, integers)
  @Benchmark def groupCombineN: Integers = grp.combineN(xs.head, 100)
  @Benchmark def groupCombineAllOption: Option[Integers] = grp.combineAllOption(xs)
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
  @Benchmark def avroSchmea: Schema = avroType.schema
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class TableRowBench {
  import com.google.api.services.bigquery.model.{TableRow, TableSchema}
  import magnolify.bigquery._
  import MagnolifyBench._
  private implicit val trfInt: TableRowField[Int] = TableRowField.from[Long](_.toInt)(_.toLong)
  private val tableRowType = TableRowType[Nested]
  private val tableRow = tableRowType.to(nested)
  @Benchmark def tableRowTo: TableRow = tableRowType.to(nested)
  @Benchmark def tableRowFrom: Nested = tableRowType.from(tableRow)
  @Benchmark def tableRowSchema: TableSchema = tableRowType.schema
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class EntityBench {
  import com.google.datastore.v1.Entity
  import magnolify.datastore._
  import MagnolifyBench._
  private implicit val efInt: EntityField[Int] = EntityField.from[Long](_.toInt)(_.toLong)
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
  import magnolify.test.Proto2._
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
  import com.google.protobuf.ByteString
  import magnolify.tensorflow._
  import org.tensorflow.example.Example
  import MagnolifyBench._
  private implicit val efInt: ExampleField.Primitive[Int] =
    ExampleField.from[Long](_.toInt)(_.toLong)
  private implicit val efBoolean: ExampleField.Primitive[Boolean] =
    ExampleField.from[Long](_ == 1)(x => if (x) 1 else 0)
  private implicit val efString: ExampleField.Primitive[String] =
    ExampleField.from[ByteString](_.toStringUtf8)(ByteString.copyFromUtf8)
  private val exampleType = ExampleType[ExampleNested]
  private val exampleNested = implicitly[Arbitrary[ExampleNested]].arbitrary(prms, seed).get
  private val example = exampleType.to(exampleNested).build()
  @Benchmark def exampleTo: Example.Builder = exampleType.to(exampleNested)
  @Benchmark def exampleFrom: ExampleNested = exampleType.from(example)
}

// Option[T] and Seq[T] not supported
case class ExampleNested(b: Boolean, i: Int, s: String, r: Required, o: Option[Required])
