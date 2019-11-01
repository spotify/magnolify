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
package magnolify.avro.test

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import cats._
import cats.instances.all._
import magnolify.avro.{AvroType, GenericRecordType}
import magnolify.avro.GenericRecordType._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import magnolify.test._
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.scalacheck._

import scala.reflect._

object AvroTypeSpec extends MagnolifySpec("AvroRecordType") {
  private val encoder = EncoderFactory.get
  private val decoder = DecoderFactory.get

  private def test[T: Arbitrary: ClassTag](
    implicit tpe: AvroType.Aux2[T, GenericRecord],
    eq: Eq[T]
  ): Unit = {
    ensureSerializable(tpe)
    val converter = GenericRecordType[T]

    property(className[T]) = Prop.forAll { caseClass: T =>
      val avroRepr = converter.to(caseClass)
      val avroCopy = roundtripAvro(avroRepr)
      val copy = converter.from(avroCopy)

      Prop.all(eq.eqv(caseClass, copy))
    }
  }

  // Mimics Beam's ser/de of Avro records
  private def roundtripAvro(gr: GenericRecord): GenericRecord = {
    val datumWriter = new GenericDatumWriter[GenericRecord](gr.getSchema)
    val bytesOut = new ByteArrayOutputStream()
    datumWriter.write(gr, encoder.directBinaryEncoder(bytesOut, null))

    val datumReader = new GenericDatumReader[GenericRecord](gr.getSchema)
    datumReader.read(
      null,
      decoder.directBinaryDecoder(new ByteArrayInputStream(bytesOut.toByteArray), null)
    )
  }

  case class Bytes(b: Array[Byte])
  implicit val eqBytes: Eq[Bytes] = Eq.instance[Bytes] { case (b1, b2) => b1.b.sameElements(b2.b) }

  {
    test[Integers]
    test[Required]
    test[Nullable]
    test[Repeated]
    test[Nested]
    test[Bytes]
  }

  case class CollectionPrimitive(l: List[Int], m: Map[String, Int])
  case class CollectionNestedRecord(l: List[Nested], m: Map[String, Nested])
  case class CollectionNestedList(l: List[Nested], m: Map[String, List[Int]])
  case class CollectionNullable(l: List[Nullable], m: Map[String, Nullable])

  {
    test[CollectionPrimitive]
    test[CollectionNestedRecord]
    test[CollectionNestedList]
    test[CollectionNullable]
  }
}
