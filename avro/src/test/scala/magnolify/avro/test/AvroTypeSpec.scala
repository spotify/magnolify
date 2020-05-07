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
import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDate}

import cats._
import cats.instances.all._
import magnolify.avro._
import magnolify.avro.unsafe._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.shims.JavaConverters._
import magnolify.test.Simple._
import magnolify.test._
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.scalacheck._

import scala.reflect._

object AvroTypeSpec extends MagnolifySpec("AvroType") {
  private def test[T: Arbitrary: ClassTag](implicit
    t: AvroType[T],
    eqt: Eq[T],
    eqr: Eq[GenericRecord] = Eq.instance(_ == _)
  ): Unit = {
    val tpe = ensureSerializable(t)
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
  test[Unsafe]

  {
    import Collections._
    test[Collections]
    test[MoreCollections]
  }

  {
    import Custom._
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
      r.get("m")
        .asInstanceOf[java.util.Map[CharSequence, Any]]
        .asScala
        .toList
        .map(kv => (kv._1.toString, kv._2))
        .sortBy(_._1)
    implicit val eqMapPrimitive: Eq[GenericRecord] = Eq.instance((x, y) => f(x) == f(y))
    test[MapPrimitive]
    test[MapNested]
  }

  {
    val at = ensureSerializable(AvroType[AvroDoc])
    val schema = at.schema
    require(schema.getDoc == "Avro with doc")
    val fields = schema.getFields.asScala
    require(fields.find(_.name() == "s").exists(_.doc() == "string"))
    require(fields.find(_.name() == "i").exists(_.doc() == "integers"))
  }

  {
    val at = ensureSerializable(AvroType[CustomDoc])
    val schema = at.schema
    require(schema.getDoc == """{"doc": "Avro with doc", "path": "/path/to/my/data"}""")
    val fields = schema.getFields.asScala
    require(
      fields
        .find(_.name() == "s")
        .exists(
          _.doc() ==
            """{"doc": "string", "since": "2020-01-01"}"""
        )
    )
    require(
      fields
        .find(_.name() == "i")
        .exists(
          _.doc() ==
            """{"doc": "integers", "since": "2020-02-01"}"""
        )
    )
  }

  require(
    expectException[IllegalArgumentException](AvroType[DoubleRecordDoc]).getMessage ==
      "requirement failed: More than one @doc annotation: magnolify.avro.test.DoubleRecordDoc"
  )
  require(
    expectException[IllegalArgumentException](AvroType[DoubleFieldDoc]).getMessage ==
      "requirement failed: More than one @doc annotation: magnolify.avro.test.DoubleFieldDoc#i"
  )
}

case class Unsafe(b: Byte, c: Char, s: Short)
case class AvroTypes(bs: Array[Byte])
case class MapPrimitive(m: Map[String, Int])
case class MapNested(m: Map[String, Nested])

@doc("Avro with doc")
case class AvroDoc(@doc("string") s: String, @doc("integers") i: Integers)

class datasetDoc(doc: String, path: String) extends doc(s"""{"doc": "$doc", "path": "$path"}""")

class fieldDoc(doc: String, since: LocalDate)
    extends doc(
      s"""{"doc": "$doc", "since": "${since.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))}"}"""
    )

@datasetDoc("Avro with doc", "/path/to/my/data")
case class CustomDoc(
  @fieldDoc("string", LocalDate.of(2020, 1, 1)) s: String,
  @fieldDoc("integers", LocalDate.of(2020, 2, 1)) i: Integers
)

@doc("doc1")
@doc("doc2")
case class DoubleRecordDoc(i: Int)
case class DoubleFieldDoc(@doc("doc1") @doc("doc2") i: Int)

private class Copier(private val schema: Schema) {
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
