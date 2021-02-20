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
package magnolify.parquet

import java.nio.{ByteBuffer, ByteOrder}
import java.time.LocalDate
import java.util.UUID
import magnolia._
import magnolify.shared.{Converter => _, _}
import magnolify.shims._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.Job
import org.apache.parquet.hadoop.{
  ParquetInputFormat,
  ParquetOutputFormat,
  ParquetReader,
  ParquetWriter,
  api => hadoop
}
import org.apache.parquet.io.api._
import org.apache.parquet.io.{InputFile, OutputFile, ParquetDecodingException}
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Type.Repetition
import org.apache.parquet.schema.{LogicalTypeAnnotation, MessageType, Type, Types}

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

sealed trait ParquetArray

object ParquetArray {
  implicit case object default extends ParquetArray

  object AvroCompat {
    implicit case object avroCompat extends ParquetArray
  }
}

sealed trait ParquetType[T] extends Serializable { self =>
  import ParquetType._

  def schema: MessageType

  def setupInput(job: Job): Unit = {
    job.setInputFormatClass(classOf[ParquetInputFormat[T]])
    ParquetInputFormat.setReadSupportClass(job, classOf[ReadSupport[T]])
    job.getConfiguration.set(ReadTypeKey, SerializationUtils.toBase64(this))
  }

  def setupOutput(job: Job): Unit = {
    job.setOutputFormatClass(classOf[ParquetOutputFormat[T]])
    ParquetOutputFormat.setWriteSupportClass(job, classOf[WriteSupport[T]])
    job.getConfiguration.set(WriteTypeKey, SerializationUtils.toBase64(this))
  }

  def readSupport: ReadSupport[T] = new ReadSupport[T](this)
  def writeSupport: WriteSupport[T] = new WriteSupport[T](this)

  def readBuilder(file: InputFile): ReadBuilder[T] = new ReadBuilder(file, readSupport)
  def writeBuilder(file: OutputFile): WriteBuilder[T] = new WriteBuilder(file, writeSupport)

  def write(c: RecordConsumer, v: T): Unit = ()
  def newConverter: TypeConverter[T] = null
}

object ParquetType {
  implicit def apply[T](implicit f: ParquetField.Record[T]): ParquetType[T] =
    ParquetType(CaseMapper.identity)

  def apply[T](cm: CaseMapper)(implicit f: ParquetField.Record[T]): ParquetType[T] =
    new ParquetType[T] {
      override def schema: MessageType = Schema.message(f.schema(cm))
      override def write(c: RecordConsumer, v: T): Unit = f.write(c, v)(cm)
      override def newConverter: TypeConverter[T] = f.newConverter
    }

  val ReadTypeKey = "parquet.type.read.type"
  val WriteTypeKey = "parquet.type.write.type"

  class ReadBuilder[T](file: InputFile, val readSupport: ReadSupport[T])
      extends ParquetReader.Builder[T](file) {
    override def getReadSupport: ReadSupport[T] = readSupport
  }

  class WriteBuilder[T](file: OutputFile, val writeSupport: WriteSupport[T])
      extends ParquetWriter.Builder[T, WriteBuilder[T]](file) {
    override def self(): WriteBuilder[T] = this
    override def getWriteSupport(conf: Configuration): WriteSupport[T] = writeSupport
  }

  class ReadSupport[T](private var parquetType: ParquetType[T]) extends hadoop.ReadSupport[T] {
    def this() = this(null)

    override def init(context: hadoop.InitContext): hadoop.ReadSupport.ReadContext = {
      if (parquetType == null) {
        parquetType = SerializationUtils.fromBase64(context.getConfiguration.get(ReadTypeKey))
      }
      val requestedSchema = Schema.message(parquetType.schema)
      context.getFileSchema.checkContains(requestedSchema)
      new hadoop.ReadSupport.ReadContext(requestedSchema, java.util.Collections.emptyMap())
    }

    override def prepareForRead(
      configuration: Configuration,
      keyValueMetaData: java.util.Map[String, String],
      fileSchema: MessageType,
      readContext: hadoop.ReadSupport.ReadContext
    ): RecordMaterializer[T] =
      new RecordMaterializer[T] {
        private val root = parquetType.newConverter
        override def getCurrentRecord: T = root.get
        override def getRootConverter: GroupConverter = root.asGroupConverter()
      }
  }

  class WriteSupport[T](private var parquetType: ParquetType[T]) extends hadoop.WriteSupport[T] {
    def this() = this(null)

    private var recordConsumer: RecordConsumer = null

    override def init(configuration: Configuration): hadoop.WriteSupport.WriteContext = {
      if (parquetType == null) {
        parquetType = SerializationUtils.fromBase64(configuration.get(WriteTypeKey))
      }
      val schema = Schema.message(parquetType.schema)
      new hadoop.WriteSupport.WriteContext(schema, java.util.Collections.emptyMap())
    }

    override def prepareForWrite(recordConsumer: RecordConsumer): Unit =
      this.recordConsumer = recordConsumer

    override def write(record: T): Unit = {
      recordConsumer.startMessage()
      parquetType.write(recordConsumer, record)
      recordConsumer.endMessage()
    }
  }
}

//////////////////////////////////////////////////

sealed trait ParquetField[T] extends Serializable {
  def schema(cm: CaseMapper): Type
  protected val isGroup: Boolean = false
  protected def isEmpty(v: T): Boolean
  def write(c: RecordConsumer, v: T)(cm: CaseMapper): Unit
  def newConverter: TypeConverter[T]

  protected def writeGroup(c: RecordConsumer, v: T)(cm: CaseMapper): Unit = {
    if (isGroup) {
      c.startGroup()
    }
    write(c, v)(cm)
    if (isGroup) {
      c.endGroup()
    }
  }
}

object ParquetField {
  type Typeclass[T] = ParquetField[T]

  sealed trait Record[T] extends ParquetField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Record[T] = new Record[T] {
    override def schema(cm: CaseMapper): Type =
      caseClass.parameters
        .foldLeft(Types.requiredGroup()) { (g, p) =>
          g.addField(Schema.rename(p.typeclass.schema(cm), cm.map(p.label)))
        }
        .named(caseClass.typeName.full)

    override protected val isGroup: Boolean = true
    override protected def isEmpty(v: T): Boolean = false

    override def write(c: RecordConsumer, v: T)(cm: CaseMapper): Unit = {
      caseClass.parameters.foreach { p =>
        val x = p.dereference(v)
        if (!p.typeclass.isEmpty(x)) {
          c.startField(cm.map(p.label), p.index)
          p.typeclass.writeGroup(c, x)(cm)
          c.endField(cm.map(p.label), p.index)
        }
      }
    }

    override def newConverter: TypeConverter[T] =
      new GroupConverter with TypeConverter.Buffered[T] {
        private val fieldConverters = caseClass.parameters.map(_.typeclass.newConverter)
        override def isPrimitive: Boolean = false
        override def getConverter(fieldIndex: Int): Converter = fieldConverters(fieldIndex)
        override def start(): Unit = ()
        override def end(): Unit = {
          if (!isRepeated) {
            buffer.clear()
          }
          buffer += caseClass.construct { p =>
            try {
              fieldConverters(p.index).get
            } catch {
              case e: IllegalArgumentException =>
                val field = s"${caseClass.typeName.full}#${p.label}"
                throw new ParquetDecodingException(s"Failed to decode $field: ${e.getMessage}", e)
            }
          }
        }
      }
  }

  @implicitNotFound("Cannot derive ParquetType for sealed trait")
  private sealed trait Dispatchable[T]
  def dispatch[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def apply[T]: Record[T] = macro Magnolia.gen[T]

  //////////////////////////////////////////////////

  def from[T]: FromWord[T] = new FromWord[T]

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit pf: Primitive[T]): Primitive[U] =
      new Primitive[U] {
        override def schema(cm: CaseMapper): Type = pf.schema(cm)
        override def write(c: RecordConsumer, v: U)(cm: CaseMapper): Unit = pf.write(c, g(v))(cm)
        override def newConverter: TypeConverter[U] =
          pf.newConverter.asInstanceOf[TypeConverter.Primitive[T]].map(f)
      }
  }

  //////////////////////////////////////////////////

  sealed trait Primitive[T] extends ParquetField[T] {
    override protected def isEmpty(v: T): Boolean = false
  }

  def primitive[T](
    f: RecordConsumer => T => Unit,
    g: => TypeConverter[T],
    ptn: PrimitiveTypeName,
    lta: => LogicalTypeAnnotation = null
  ): Primitive[T] =
    new Primitive[T] {
      override def schema(cm: CaseMapper): Type = Schema.primitive(ptn, lta)
      override def write(c: RecordConsumer, v: T)(cm: CaseMapper): Unit = f(c)(v)
      override def newConverter: TypeConverter[T] = g
    }

  implicit val pfBoolean =
    primitive[Boolean](_.addBoolean, TypeConverter.newBoolean, PrimitiveTypeName.BOOLEAN)
  implicit val pfByte =
    primitive[Byte](
      c => v => c.addInteger(v),
      TypeConverter.newInt.map(_.toByte),
      PrimitiveTypeName.INT32,
      LogicalTypeAnnotation.intType(8, true)
    )
  implicit val pfShort =
    primitive[Short](
      c => v => c.addInteger(v),
      TypeConverter.newInt.map(_.toShort),
      PrimitiveTypeName.INT32,
      LogicalTypeAnnotation.intType(16, true)
    )
  implicit val pfInt =
    primitive[Int](
      _.addInteger,
      TypeConverter.newInt,
      PrimitiveTypeName.INT32,
      LogicalTypeAnnotation.intType(32, true)
    )
  implicit val pfLong =
    primitive[Long](
      _.addLong,
      TypeConverter.newLong,
      PrimitiveTypeName.INT64,
      LogicalTypeAnnotation.intType(64, true)
    )
  implicit val pfFloat =
    primitive[Float](_.addFloat, TypeConverter.newFloat, PrimitiveTypeName.FLOAT)
  implicit val pfDouble =
    primitive[Double](_.addDouble, TypeConverter.newDouble, PrimitiveTypeName.DOUBLE)

  implicit val pfByteArray =
    primitive[Array[Byte]](
      c => v => c.addBinary(Binary.fromConstantByteArray(v)),
      TypeConverter.newByteArray,
      PrimitiveTypeName.BINARY
    )
  implicit val pfString =
    primitive[String](
      c => v => c.addBinary(Binary.fromString(v)),
      TypeConverter.newString,
      PrimitiveTypeName.BINARY,
      LogicalTypeAnnotation.stringType()
    )

  implicit def pfOption[T](implicit t: ParquetField[T]): ParquetField[Option[T]] =
    new ParquetField[Option[T]] {
      override def schema(cm: CaseMapper): Type =
        Schema.setRepetition(t.schema(cm), Repetition.OPTIONAL)
      override protected def isEmpty(v: Option[T]): Boolean = v.isEmpty

      override def write(c: RecordConsumer, v: Option[T])(cm: CaseMapper): Unit =
        v.foreach(t.writeGroup(c, _)(cm))

      override def newConverter: TypeConverter[Option[T]] = {
        val buffered = t.newConverter.asInstanceOf[TypeConverter.Buffered[T]]
        new TypeConverter.Delegate[T, Option[T]](buffered) {
          override def get: Option[T] = {
            require(inner.buffer.size <= 1, "Optional field size > 1: " + inner.buffer.size)
            val v = inner.buffer.headOption
            inner.buffer.clear()
            v
          }
        }
      }
    }

  implicit def ptIterable[T, C[T]](implicit
    t: ParquetField[T],
    ti: C[T] => Iterable[T],
    fc: FactoryCompat[T, C[T]],
    pa: ParquetArray
  ): ParquetField[C[T]] = {
    val isAvro = pa match {
      case ParquetArray.default               => false
      case ParquetArray.AvroCompat.avroCompat => true
    }

    new ParquetField[C[T]] {
      private val avroArrayField = "array"

      override def schema(cm: CaseMapper): Type = {
        val repeatedSchema = Schema.setRepetition(t.schema(cm), Repetition.REPEATED)
        if (isAvro) {
          Types
            .requiredGroup()
            .addField(Schema.rename(repeatedSchema, avroArrayField))
            .as(LogicalTypeAnnotation.listType())
            .named(t.schema(cm).getName)
        } else {
          repeatedSchema
        }
      }

      override protected val isGroup: Boolean = isAvro
      override protected def isEmpty(v: C[T]): Boolean = v.isEmpty

      override def write(c: RecordConsumer, v: C[T])(cm: CaseMapper): Unit =
        if (isAvro) {
          c.startField(avroArrayField, 0)
          v.foreach(t.writeGroup(c, _)(cm))
          c.endField(avroArrayField, 0)
        } else {
          v.foreach(t.writeGroup(c, _)(cm))
        }

      override def newConverter: TypeConverter[C[T]] = {
        val buffered = t.newConverter.asInstanceOf[TypeConverter.Buffered[T]]
        buffered.isRepeated = true
        val arrayConverter = new TypeConverter.Delegate[T, C[T]](buffered) {
          override def get: C[T] = {
            val v = fc.build(inner.buffer)
            inner.buffer.clear()
            v
          }
        }
        if (isAvro) {
          new GroupConverter with TypeConverter.Buffered[C[T]] {
            override def getConverter(fieldIndex: Int): Converter = {
              require(fieldIndex == 0, "Avro array field index != 0")
              arrayConverter
            }
            override def start(): Unit = ()
            override def end(): Unit = buffer += arrayConverter.get
          }
        } else {
          arrayConverter
        }
      }
    }
  }

  //////////////////////////////////////////////////

  def logicalType[T](lta: => LogicalTypeAnnotation): LogicalTypeWord[T] =
    new LogicalTypeWord[T](lta)

  class LogicalTypeWord[T](lta: => LogicalTypeAnnotation) extends Serializable {
    def apply[U](f: T => U)(g: U => T)(implicit pf: Primitive[T]): Primitive[U] = new Primitive[U] {
      override def schema(cm: CaseMapper): Type = Schema.setLogicalType(pf.schema(cm), lta)
      override def write(c: RecordConsumer, v: U)(cm: CaseMapper): Unit = pf.write(c, g(v))(cm)
      override def newConverter: TypeConverter[U] =
        pf.newConverter.asInstanceOf[TypeConverter.Primitive[T]].map(f)
    }
  }

  // https://github.com/apache/parquet-format/blob/master/LogicalTypes.md
  // Precision and scale are not encoded in the `BigDecimal` type and must be specified
  def decimal32(precision: Int, scale: Int = 0): Primitive[BigDecimal] = {
    require(1 <= precision && precision <= 9, s"Precision for INT32 $precision not within [1, 9]")
    require(0 <= scale && scale < precision, s"Scale $scale not within [0, $precision)")
    logicalType[Int](LogicalTypeAnnotation.decimalType(scale, precision))(x =>
      BigDecimal(BigInt(x), scale)
    )(_.underlying().unscaledValue().intValue())
  }

  def decimal64(precision: Int, scale: Int = 0): Primitive[BigDecimal] = {
    require(1 <= precision && precision <= 18, s"Precision for INT64 $precision not within [1, 18]")
    require(0 <= scale && scale < precision, s"Scale $scale not within [0, $precision)")
    logicalType[Long](LogicalTypeAnnotation.decimalType(scale, precision))(x =>
      BigDecimal(BigInt(x), scale)
    )(_.underlying().unscaledValue().longValue())
  }

  def decimalFixed(length: Int, precision: Int, scale: Int = 0): Primitive[BigDecimal] = {
    val capacity = math.floor(math.log10(math.pow(2, 8 * length - 1) - 1)).toInt
    require(
      1 <= precision && precision <= capacity,
      s"Precision for FIXED($length) not within [1, $capacity]"
    )

    new Primitive[BigDecimal] {
      override def schema(cm: CaseMapper): Type =
        Schema.primitive(
          PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY,
          LogicalTypeAnnotation.decimalType(scale, precision),
          length
        )

      override def write(c: RecordConsumer, v: BigDecimal)(cm: CaseMapper): Unit =
        c.addBinary(Binary.fromConstantByteArray(Decimal.toFixed(v, precision, scale, length)))

      override def newConverter: TypeConverter[BigDecimal] = TypeConverter.newByteArray.map { ba =>
        Decimal.fromBytes(ba, precision, scale)
      }
    }
  }

  def decimalBinary(precision: Int, scale: Int = 0): Primitive[BigDecimal] = {
    require(1 <= precision, s"Precision $precision <= 0")
    require(0 <= scale && scale < precision, s"Scale $scale not within [0, $precision)")
    logicalType[Array[Byte]](LogicalTypeAnnotation.decimalType(scale, precision))(
      Decimal.fromBytes(_, precision, scale)
    )(Decimal.toBytes(_, precision, scale))
  }

  implicit def pfEnum[T](implicit et: EnumType[T]): Primitive[T] =
    logicalType[String](LogicalTypeAnnotation.enumType())(et.from)(et.to)

  implicit val ptUuid: Primitive[UUID] = new Primitive[UUID] {
    override def schema(cm: CaseMapper): Type =
      Schema.primitive(PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY, length = 16)

    override def write(c: RecordConsumer, v: UUID)(cm: CaseMapper): Unit =
      c.addBinary(
        Binary.fromConstantByteArray(
          ByteBuffer
            .allocate(16)
            .order(ByteOrder.BIG_ENDIAN)
            .putLong(v.getMostSignificantBits)
            .putLong(v.getLeastSignificantBits)
            .array()
        )
      )

    override def newConverter: TypeConverter[UUID] = TypeConverter.newByteArray.map { ba =>
      val bb = ByteBuffer.wrap(ba)
      val h = bb.getLong
      val l = bb.getLong
      new UUID(h, l)
    }
  }

  implicit val ptDate: Primitive[LocalDate] =
    logicalType[Int](LogicalTypeAnnotation.dateType())(LocalDate.ofEpochDay(_))(_.toEpochDay.toInt)
}
