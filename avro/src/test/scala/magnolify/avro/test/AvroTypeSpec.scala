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
import java.net.URI
import java.time.Duration

import cats._
import cats.instances.all._
import magnolify.avro._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import magnolify.test._
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.scalacheck._

import scala.collection.JavaConverters._
import scala.reflect._

object AvroTypeSpec extends MagnolifySpec("AvroRecordType") {
  private def test[T: Arbitrary: ClassTag](implicit tpe: AvroType[T], eqt: Eq[T],
                                           eqr: Eq[GenericRecord] = Eq.instance(_ == _)): Unit = {
    ensureSerializable(tpe)
    // FIXME: test schema
    val copier = new Copier(tpe.schema)
    property(className[T]) = Prop.forAll { t: T =>
      val r = tpe(t)
      val rCopy = copier(r)
      val copy = tpe(rCopy)
      Prop.all(eqt.eqv(t, copy), eqr.eqv(r, rCopy))
    }
  }

  test[Integers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]

  {
    implicit val eqArray: Eq[Array[Int]] = Eq.by(_.toList)
    test[Collections]
  }

  {
    import Custom._
    implicit val eqUri: Eq[URI] = Eq.by(_.toString)
    implicit val eqDuration: Eq[Duration] = Eq.by(_.toMillis)
    implicit val trfUri: AvroField[URI] = AvroField.from[String](URI.create)(_.toString)
    implicit val trfDuration: AvroField[Duration] =
      AvroField.from[Long](Duration.ofMillis)(_.toMillis)
    test[Custom]
  }

  {
    implicit val eqByteArray: Eq[Array[Byte]] = Eq.by(_.toList)
    test[AvroTypes]
  }

  {
    def f[T](r: GenericRecord): List[(String, Any)] =
      r.get("m").asInstanceOf[java.util.Map[CharSequence, Any]].asScala
        .toList
        .map(kv => (kv._1.toString, kv._2))
        .sortBy(_._1)
    implicit val eqMapPrimitive: Eq[GenericRecord] = Eq.instance((x, y) => f(x) == f(y))
    test[MapPrimitive]
    test[MapNested]
  }
}

case class AvroTypes(bs: Array[Byte])
case class MapPrimitive(m: Map[String, Int])
case class MapNested(m: Map[String, Nested])

private class Copier(private val schema: Schema) {
  private val encoder = EncoderFactory.get
  private val decoder = DecoderFactory.get

  private val datumWriter = new GenericDatumWriter[GenericRecord](schema)
  private val datumReader = new GenericDatumReader[GenericRecord](schema)

  def apply(r: GenericRecord): GenericRecord = {
    val baos = new ByteArrayOutputStream()
    datumWriter.write(r, encoder.directBinaryEncoder(baos, null))
    datumReader.read(null,
      decoder.directBinaryDecoder(new ByteArrayInputStream(baos.toByteArray), null))
  }
}
