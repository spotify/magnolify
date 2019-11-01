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
import magnolify.avro.AvroType
import magnolify.avro.AvroType._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import magnolify.test._
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.scalacheck._

import scala.reflect._

object AvroRecordTypeSpec extends MagnolifySpec("AvroRecordType") {
  private val encoder = EncoderFactory.get
  private val decoder = DecoderFactory.get

  private def test[T: Arbitrary: ClassTag](
    implicit tpe: AvroType.Aux2[T, GenericRecord],
    eq: Eq[T]
  ): Unit = {
    ensureSerializable(tpe)

    property(className[T]) = Prop.forAll { caseClass: T =>
      val avroRepr = tpe.to(caseClass)
      val avroCopy = roundtripAvro(avroRepr)
      val copy = tpe.from(avroCopy)

      Prop.all(
        tpe.schema.equals(avroRepr.getSchema),
        avroRepr.equals(avroCopy),
        eq.eqv(caseClass, copy)
      )
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

  test[Integers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]

  case class CollectionPrimitive(l: List[Int])
  case class CollectionNestedRecord(l: List[Nested])
  case class CollectionNullable(l: List[Nullable])

  {
    test[CollectionPrimitive]
    test[CollectionNestedRecord]
    test[CollectionNullable]
  }
}
