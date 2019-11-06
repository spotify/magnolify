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
import magnolify.avro2._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import magnolify.test._
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.scalacheck._

import scala.reflect._

object AvroTypeSpec extends MagnolifySpec("AvroRecordType") {
  private def test[T: Arbitrary: ClassTag](implicit tpe: AvroType[T], eq: Eq[T]): Unit = {
    ensureSerializable(tpe)
    // FIXME: test schema
    val copier = new Copier(tpe.schema)
    property(className[T]) = Prop.forAll { t: T =>
      val r = tpe(t)
      val rCopy = copier(r)
      val copy = tpe(rCopy)
      eq.eqv(t, copy)
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
//  }
//
//  case class CollectionPrimitive(l: List[Int], m: Map[String, Int])
//  case class CollectionNestedRecord(l: List[Nested], m: Map[String, Nested])
//  case class CollectionNestedList(l: List[Nested], m: Map[String, List[Int]])
//  case class CollectionNullable(l: List[Nullable], m: Map[String, Nullable])
//
//  {
//    test[CollectionPrimitive]
//    test[CollectionNestedRecord]
//    test[CollectionNestedList]
//    test[CollectionNullable]
//  }
}

case class AvroTypes(bs: Array[Byte])

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
