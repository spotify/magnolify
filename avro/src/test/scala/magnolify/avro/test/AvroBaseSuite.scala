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
package magnolify.avro.test

import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import magnolify.scalacheck.auto._
import org.scalacheck._
import scala.reflect._
import magnolify.avro._
import cats._
import org.apache.avro.generic.GenericRecord
import magnolify.test.MagnolifySuite

trait AvroBaseSuite extends MagnolifySuite {

  private[magnolify] def test[T: Arbitrary: ClassTag](implicit
    t: AvroType[T],
    eqt: Eq[T],
    eqr: Eq[GenericRecord] = Eq.instance(_ == _)
  ): Unit = {
    val tpe = ensureSerializable(t)
    // FIXME: test schema
    val copier = new Copier(tpe.schema)
    property(className[T]) {
      Prop.forAll { t: T =>
        val r = tpe(t)
        val rCopy = copier(r)
        val copy = tpe(rCopy)
        Prop.all(eqt.eqv(t, copy), eqr.eqv(r, rCopy))
      }
    }
  }

  class Copier(private val schema: Schema) {
    private val encoder = EncoderFactory.get
    private val decoder = DecoderFactory.get

    private val datumWriter = new GenericDatumWriter[GenericRecord](schema)
    private val datumReader = new GenericDatumReader[GenericRecord](schema)

    def apply(r: GenericRecord): GenericRecord = {
      val baos = new ByteArrayOutputStream()
      datumWriter.write(r, encoder.directBinaryEncoder(baos, null))
      datumReader.read(
        null,
        decoder.directBinaryDecoder(new ByteArrayInputStream(baos.toByteArray), null)
      )
    }
  }
}
