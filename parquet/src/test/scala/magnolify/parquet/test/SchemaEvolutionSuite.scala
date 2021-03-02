/*
 * Copyright 2021 Spotify AB.
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
package magnolify.parquet.test

import magnolify.parquet._
import magnolify.parquet.unsafe._
import magnolify.shared.UnsafeEnum
import magnolify.shims.JavaConverters._
import magnolify.test._
import org.apache.avro.generic.{GenericRecord, GenericRecordBuilder}
import org.apache.avro.{JsonProperties, Schema}
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.avro.{
  AvroParquetReader,
  AvroParquetWriter,
  AvroReadSupport,
  GenericDataSupplier
}

object SchemaEvolutionSuite {
  private val namespace = "magnolify.parquet"
  private val nullVal = JsonProperties.NULL_VALUE

  private def nullableString: Schema =
    Schema.createUnion(List(Schema.Type.NULL, Schema.Type.STRING).map(Schema.create).asJava)

  val (locationSchema1, locationSchema2): (Schema, Schema) = {
    def country = new Schema.Field("country", Schema.create(Schema.Type.STRING), "", null)
    def state = new Schema.Field("state", Schema.create(Schema.Type.STRING), "", null)

    val v1 = Schema.createRecord("LocationV1", "", namespace, false)
    v1.setFields(List(country, state).asJava)

    val v2 = Schema.createRecord("LocationV2", "", namespace, false)
    val zip = new Schema.Field("zip", nullableString, "", nullVal)
    v2.setFields(List(country, state, zip).asJava)

    (v1, v2)
  }

  /*
  V2 has 4 changes
  - New value for enum field "account_type"
  - New nested nullable string field "location.zip"
  - New top level nullable string field "email"
  - New top level repeated string field "aliases"
   */
  val (userSchema1, userSchema2): (Schema, Schema) = {
    def id = new Schema.Field("id", Schema.create(Schema.Type.LONG), "", null)
    def name = new Schema.Field("name", Schema.create(Schema.Type.STRING), "", null)
    def accountType = new Schema.Field("account_type", Schema.create(Schema.Type.STRING), "", null)

    val v1 = Schema.createRecord("UserV1", "", namespace, false)
    val location1 = new Schema.Field("location", locationSchema1, "", null)
    v1.setFields(List(id, name, accountType, location1).asJava)

    val v2 = Schema.createRecord("UserV2", "", namespace, false)
    val location2 = new Schema.Field("location", locationSchema2, "", null)
    val email = new Schema.Field("email", nullableString, "", nullVal)
    val aliases = new Schema.Field(
      "aliases",
      Schema.createArray(Schema.create(Schema.Type.STRING)),
      "",
      java.util.Collections.emptyList()
    )
    v2.setFields(List(id, name, accountType, location2, email, aliases).asJava)

    (v1, v2)
  }

  def avroLocation1(country: String, state: String): GenericRecord =
    new GenericRecordBuilder(locationSchema1).set("country", country).set("state", state).build()

  def avroLocation2(country: String, state: String, zip: String): GenericRecord =
    new GenericRecordBuilder(locationSchema2)
      .set("country", country)
      .set("state", state)
      .set("zip", zip)
      .build()

  def avroUser1(
    id: Long,
    name: String,
    accountType: String,
    country: String,
    state: String
  ): GenericRecord =
    new GenericRecordBuilder(userSchema1)
      .set("id", id)
      .set("name", name)
      .set("account_type", accountType)
      .set("location", avroLocation1(country, state))
      .build()

  def avroUser2(
    id: Long,
    name: String,
    accountType: String,
    country: String,
    state: String,
    zip: String,
    email: String,
    aliases: Seq[String]
  ): GenericRecord =
    new GenericRecordBuilder(userSchema2)
      .set("id", id)
      .set("name", name)
      .set("account_type", accountType)
      .set("location", avroLocation2(country, state, zip))
      .set("email", email)
      .set("aliases", aliases.asJava)
      .build()

  case class Location1(country: String, state: String)
  case class Location2(country: String, state: String, zip: Option[String])

  object AccountType1 extends Enumeration {
    type Type = Value
    val Checking, Saving = Value
  }

  object AccountType2 extends Enumeration {
    type Type = Value
    val Checking, Saving, Credit = Value
  }

  case class User1(
    id: Long,
    name: String,
    account_type: UnsafeEnum[AccountType1.Type],
    location: Location1
  )
  case class User2(
    id: Long,
    name: String,
    account_type: UnsafeEnum[AccountType2.Type],
    location: Location2,
    email: Option[String],
    aliases: Seq[String]
  )

  val avro1: Seq[GenericRecord] = Seq(
    avroUser1(0, "Alice", "Checking", "US", "NY"),
    avroUser1(1, "Bob", "Saving", "US", "NJ"),
    avroUser1(2, "Carol", "Checking", "US", "CT"),
    avroUser1(3, "Dan", "Saving", "US", "MA")
  )

  val avro2: Seq[GenericRecord] = Seq(
    avroUser2(0, "Alice", "Checking", "US", "NY", "12345", "alice@aol.com", Seq("Ada", "Ana")),
    avroUser2(1, "Bob", "Saving", "US", "NJ", null, null, Nil),
    avroUser2(2, "Carol", "Checking", "US", "CT", null, "carol@aol.com", Nil),
    avroUser2(3, "Dan", "Saving", "US", "MA", "54321", null, Nil),
    avroUser2(4, "Ed", "Credit", "US", "CO", "98765", "ed@aol.com", Nil)
  )

  val avro2as1: Seq[GenericRecord] =
    avro1 :+ avroUser1(4, "Ed", "Credit", "US", "CO")

  val avro1as2: Seq[GenericRecord] = Seq(
    avroUser2(0, "Alice", "Checking", "US", "NY", null, null, null),
    avroUser2(1, "Bob", "Saving", "US", "NJ", null, null, null),
    avroUser2(2, "Carol", "Checking", "US", "CT", null, null, null),
    avroUser2(3, "Dan", "Saving", "US", "MA", null, null, null)
  )

  val scala1: Seq[User1] = Seq(
    User1(0, "Alice", UnsafeEnum(AccountType1.Checking), Location1("US", "NY")),
    User1(1, "Bob", UnsafeEnum(AccountType1.Saving), Location1("US", "NJ")),
    User1(2, "Carol", UnsafeEnum(AccountType1.Checking), Location1("US", "CT")),
    User1(3, "Dan", UnsafeEnum(AccountType1.Saving), Location1("US", "MA"))
  )

  val scala2: Seq[User2] = Seq(
    User2(
      0,
      "Alice",
      UnsafeEnum(AccountType2.Checking),
      Location2("US", "NY", Some("12345")),
      Some("alice@aol.com"),
      Seq("Ada", "Ana")
    ),
    User2(1, "Bob", UnsafeEnum(AccountType2.Saving), Location2("US", "NJ", None), None, Nil),
    User2(
      2,
      "Carol",
      UnsafeEnum(AccountType2.Checking),
      Location2("US", "CT", None),
      Some("carol@aol.com"),
      Nil
    ),
    User2(
      3,
      "Dan",
      UnsafeEnum(AccountType2.Saving),
      Location2("US", "MA", Some("54321")),
      None,
      Nil
    ),
    User2(
      4,
      "Ed",
      UnsafeEnum(AccountType2.Credit),
      Location2("US", "CO", Some("98765")),
      Some("ed@aol.com"),
      Nil
    )
  )

  val scala2as1: Seq[User1] =
    scala1 :+ User1(4, "Ed", UnsafeEnum.Unknown("Credit"), Location1("US", "CO"))

  val scala1as2: Seq[User2] = Seq(
    User2(0, "Alice", UnsafeEnum(AccountType2.Checking), Location2("US", "NY", None), None, Nil),
    User2(1, "Bob", UnsafeEnum(AccountType2.Saving), Location2("US", "NJ", None), None, Nil),
    User2(2, "Carol", UnsafeEnum(AccountType2.Checking), Location2("US", "CT", None), None, Nil),
    User2(3, "Dan", UnsafeEnum(AccountType2.Saving), Location2("US", "MA", None), None, Nil)
  )
}

class SchemaEvolutionSuite extends MagnolifySuite {
  import SchemaEvolutionSuite._

  private def writeAvro(xs: Seq[GenericRecord], schema: Schema): Array[Byte] = {
    val out = new TestOutputFile
    val writer = AvroParquetWriter.builder[GenericRecord](out).withSchema(schema).build()
    xs.foreach(writer.write)
    writer.close()
    out.getBytes
  }

  private def readAvro(bytes: Array[Byte], schema: Schema): Seq[GenericRecord] = {
    val in = new TestInputFile(bytes)
    val conf = new Configuration()
    AvroReadSupport.setAvroDataSupplier(conf, classOf[GenericDataSupplier])
    AvroReadSupport.setAvroReadSchema(conf, schema)
    AvroReadSupport.setRequestedProjection(conf, schema)
    val reader = AvroParquetReader.builder[GenericRecord](in).withConf(conf).build()

    val b = Seq.newBuilder[GenericRecord]
    var r = reader.read()
    while (r != null) {
      b += r
      r = reader.read()
    }
    reader.close()
    b.result()
  }

  private def writeScala[T](xs: Seq[T])(implicit pt: ParquetType[T]): Array[Byte] = {
    val out = new TestOutputFile
    val writer = pt.writeBuilder(out).build()
    xs.foreach(writer.write)
    writer.close()
    out.getBytes
  }

  private def readScala[T](bytes: Array[Byte])(implicit pt: ParquetType[T]): Seq[T] = {
    val in = new TestInputFile(bytes)
    val reader = pt.readBuilder(in).build()

    val b = Seq.newBuilder[T]
    var r = reader.read()
    while (r != null) {
      b += r
      r = reader.read()
    }
    reader.close()
    b.result()
  }

  private val avroBytes1 = writeAvro(avro1, userSchema1)
  private val avroBytes2 = writeAvro(avro2, userSchema2)
  private val scalaBytes1 = writeScala[User1](scala1)
  private val scalaBytes2 = writeScala[User2](scala2)

  private val scalaCompatBytes1 = {
    import magnolify.parquet.ParquetArray.AvroCompat._
    writeScala[User1](scala1)
  }

  private val scalaCompatBytes2 = {
    import magnolify.parquet.ParquetArray.AvroCompat._
    writeScala[User2](scala2)
  }

  //////////////////////////////////////////////////

  test("Avro V1 => Avro V1") {
    assertEquals(readAvro(avroBytes1, userSchema1), avro1)
  }

  test("Avro V2 => Avro V2") {
    assertEquals(readAvro(avroBytes2, userSchema2), avro2)
  }

  test("Avro V1 => Avro V2") {
    assertEquals(readAvro(avroBytes1, userSchema2), avro1as2)
  }

  test("Avro V2 => Avro V1") {
    assertEquals(readAvro(avroBytes2, userSchema1), avro2as1)
  }

  //////////////////////////////////////////////////

  test("Scala V1 => Scala V1") {
    assertEquals(readScala[User1](scalaBytes1), scala1)
  }

  test("Scala V2 => Scala V2") {
    assertEquals(readScala[User2](scalaBytes2), scala2)
  }

  test("Scala V1 => Scala V2") {
    assertEquals(readScala[User2](scalaBytes1), scala1as2)
  }

  test("Scala V2 => Scala V1") {
    assertEquals(readScala[User1](scalaBytes2), scala2as1)
  }

  //////////////////////////////////////////////////

  test("Scala Compat V1 => Scala Compat V1") {
    import magnolify.parquet.ParquetArray.AvroCompat._
    assertEquals(readScala[User1](scalaCompatBytes1), scala1)
  }

  test("Scala Compat V2 => Scala Compat V2") {
    import magnolify.parquet.ParquetArray.AvroCompat._
    assertEquals(readScala[User2](scalaCompatBytes2), scala2)
  }

  test("Scala Compat V1 => Scala Compat V2") {
    import magnolify.parquet.ParquetArray.AvroCompat._
    assertEquals(readScala[User2](scalaCompatBytes1), scala1as2)
  }

  test("Scala Compat V2 => Scala Compat V1") {
    import magnolify.parquet.ParquetArray.AvroCompat._
    assertEquals(readScala[User1](scalaCompatBytes2), scala2as1)
  }

  //////////////////////////////////////////////////

  test("Avro V1 => Scala V1") {
    import magnolify.parquet.ParquetArray.AvroCompat._
    assertEquals(readScala[User1](avroBytes1), scala1)
  }

  test("Avro V2 => Scala V2") {
    import magnolify.parquet.ParquetArray.AvroCompat._
    assertEquals(readScala[User2](avroBytes2), scala2)
  }

  test("Avro V1 => Scala V2") {
    import magnolify.parquet.ParquetArray.AvroCompat._
    assertEquals(readScala[User2](avroBytes1), scala1as2)
  }

  test("Avro V2 => Scala V1") {
    import magnolify.parquet.ParquetArray.AvroCompat._
    assertEquals(readScala[User1](avroBytes2), scala2as1)
  }

  //////////////////////////////////////////////////

  test("Scala V1 => Avro V1") {
    import magnolify.parquet.ParquetArray.AvroCompat._
    assertEquals(readAvro(scalaCompatBytes1, userSchema1), avro1)
  }

  test("Scala V2 => Avro V2") {
    import magnolify.parquet.ParquetArray.AvroCompat._
    assertEquals(readAvro(scalaCompatBytes2, userSchema2), avro2)
  }

  test("Scala V1 => Avro V2") {
    import magnolify.parquet.ParquetArray.AvroCompat._
    assertEquals(readAvro(scalaCompatBytes1, userSchema2), avro1as2)
  }

  test("Scala V2 => Avro V1") {
    import magnolify.parquet.ParquetArray.AvroCompat._
    assertEquals(readAvro(scalaCompatBytes2, userSchema1), avro2as1)
  }
}
