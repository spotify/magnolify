/*
 * Copyright 2022 Spotify AB
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

package magnolify.parquet

import magnolify.shared.{Converter => _, _}
import org.apache.avro.{Schema => AvroSchema}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.{InputFormat, Job, OutputFormat}
import org.apache.parquet.avro.AvroSchemaConverter
import org.apache.parquet.hadoop.{
  api => hadoop,
  ParquetInputFormat,
  ParquetOutputFormat,
  ParquetReader,
  ParquetWriter
}
import org.apache.parquet.io.api._
import org.apache.parquet.io.{InputFile, OutputFile}
import org.apache.parquet.schema.MessageType
import org.slf4j.LoggerFactory

import scala.annotation.nowarn

sealed trait ParquetArray

/**
 * Add `import magnolify.parquet.ParquetArray.AvroCompat._` to generate generate Avro-compatible
 * array schemas. This import is DEPRECATED. Instead, pass the following option to your Parquet
 * Configuration:
 *
 * magnolify.parquet.write-grouped-arrays: true
 */
object ParquetArray {
  implicit case object default extends ParquetArray

  @deprecated(
    message =
      "AvroCompat import is deprecated; set Parquet Configuration option `magnolify.parquet.write-grouped-arrays: true` instead",
    since = "0.8.0"
  )
  object AvroCompat {
    implicit case object avroCompat extends ParquetArray
  }
}

sealed trait ParquetType[T] extends Serializable {
  import ParquetType._

  def schema: MessageType
  def avroSchema: AvroSchema

  def setupInput(job: Job): Unit = setupInput(job.getConfiguration)
  def setupInput(conf: Configuration): Unit = {
    conf.setClass(
      "mapreduce.job.inputformat.class",
      classOf[ParquetInputFormat[T]],
      classOf[InputFormat[_, T]]
    )
    conf.set(ParquetInputFormat.READ_SUPPORT_CLASS, classOf[ReadSupport[T]].getName)
    conf.set(ReadTypeKey, SerializationUtils.toBase64(this))
  }

  def setupOutput(job: Job): Unit = setupOutput(job.getConfiguration)
  def setupOutput(conf: Configuration): Unit = {
    conf.setClass(
      "mapreduce.job.outputformat.class",
      classOf[ParquetOutputFormat[T]],
      classOf[OutputFormat[_, T]]
    )
    conf.set(ParquetOutputFormat.WRITE_SUPPORT_CLASS, classOf[WriteSupport[T]].getName)
    conf.set(WriteTypeKey, SerializationUtils.toBase64(this))
  }

  def readSupport: ReadSupport[T] = new ReadSupport[T](this)
  def writeSupport: WriteSupport[T] = new WriteSupport[T](this)

  def readBuilder(file: InputFile): ReadBuilder[T] = new ReadBuilder(file, readSupport)
  def writeBuilder(file: OutputFile): WriteBuilder[T] = new WriteBuilder(file, writeSupport)

  private[parquet] def properties: MagnolifyParquetProperties

  private[parquet] def write(c: RecordConsumer, v: T): Unit = ()
  private[parquet] def newConverter(): TypeConverter[T] = null
}

object ParquetType {
  private val logger = LoggerFactory.getLogger(this.getClass)

  implicit def apply[T](implicit f: ParquetField[T], pa: ParquetArray): ParquetType[T] =
    ParquetType(CaseMapper.identity, MagnolifyParquetProperties.Default)

  def apply[T](
    cm: CaseMapper
  )(implicit f: ParquetField[T], pa: ParquetArray): ParquetType[T] =
    ParquetType[T](cm, MagnolifyParquetProperties.Default)(f, pa)

  def apply[T](
    conf: Configuration
  )(implicit f: ParquetField[T], pa: ParquetArray): ParquetType[T] =
    ParquetType[T](
      CaseMapper.identity, {
        val writeArrayEncodingOpt = ArrayEncoding.from(conf)
        val writeMetadataOpt = Option(
          conf.get(MagnolifyParquetProperties.WriteAvroSchemaToMetadata)
        ).map(_.toBoolean)

        new MagnolifyParquetProperties {
          override def writeArrayEncoding: ArrayEncoding =
            writeArrayEncodingOpt.getOrElse(super.writeArrayEncoding)
          override def writeAvroSchemaToMetadata: Boolean =
            writeMetadataOpt.getOrElse(super.writeAvroSchemaToMetadata)
        }
      }
    )(f, pa)

  def apply[T](
    properties: MagnolifyParquetProperties
  )(implicit f: ParquetField[T], pa: ParquetArray): ParquetType[T] =
    ParquetType[T](CaseMapper.identity, properties)(f, pa)

  @nowarn("cat=deprecation")
  def apply[T](
    cm: CaseMapper,
    props: MagnolifyParquetProperties
  )(implicit f: ParquetField[T], pa: ParquetArray): ParquetType[T] = f match {
    case r: ParquetField.Record[_] =>
      new ParquetType[T] {
        // Maintain backwards compat with old AvroCompat import by overriding arrayEncoding property to use
        // 2-level encoding if import is detected.
        private val propertiesWithAvroImportCompat = (pa, props.writeArrayEncoding) match {
          case (ParquetArray.default, _)                                          => props
          case (ParquetArray.AvroCompat.avroCompat, ArrayEncoding.ThreeLevelList) =>
            throw new IllegalStateException(
              "AvroCompat is imported, which sets a 2-level list encoding, but MagnolifyParquetProperties#arrayEncoding is set to 3-level list encoding. Remove either the AvroCompat import or the arrayEncoding override."
            )
          case (ParquetArray.AvroCompat.avroCompat, _) =>
            new MagnolifyParquetProperties {
              override def writeArrayEncoding: ArrayEncoding =
                ArrayEncoding.ThreeLevelArray
              override def writeAvroSchemaToMetadata: Boolean = props.writeAvroSchemaToMetadata
            }
        }

        @transient override def schema: MessageType =
          Schema.message(r.schema(cm, propertiesWithAvroImportCompat))

        @transient override def avroSchema: AvroSchema = {
          val s = new AvroSchemaConverter().convert(schema)
          // add doc to avro schema
          val fieldDocs = f.fieldDocs(cm)
          SchemaUtil.deepCopy(s, f.typeDoc, fieldDocs.get)
        }

        override private[parquet] def properties: MagnolifyParquetProperties =
          propertiesWithAvroImportCompat
        override def write(c: RecordConsumer, v: T): Unit =
          r.write(c, v)(cm, propertiesWithAvroImportCompat)
        override private[parquet] def newConverter(): TypeConverter[T] =
          r.newConverter(propertiesWithAvroImportCompat)
      }
    case _ =>
      throw new IllegalArgumentException(s"ParquetType can only be created from Record. Got $f")
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

  // From AvroReadSupport
  private val AVRO_SCHEMA_METADATA_KEY = "parquet.avro.schema"

  class ReadSupport[T](private var parquetType: ParquetType[T]) extends hadoop.ReadSupport[T] {
    def this() = this(null)

    override def init(context: hadoop.InitContext): hadoop.ReadSupport.ReadContext = {
      if (parquetType == null) {
        // Use deprecated getConfiguration
        // Recommended getParquetConfiguration is only available for parquet 1.14+
        val readKeyType = context.getConfiguration.get(ReadTypeKey): @nowarn("cat=deprecation")
        parquetType = SerializationUtils.fromBase64[ParquetType[T]](readKeyType)
      }

      val writeSchema = context.getFileSchema
      val readSchema = Schema.message(parquetType.schema)

      Schema.checkCompatibility(writeSchema, readSchema)
      new hadoop.ReadSupport.ReadContext(readSchema)
    }

    override def prepareForRead(
      configuration: Configuration,
      keyValueMetaData: java.util.Map[String, String],
      fileSchema: MessageType,
      readContext: hadoop.ReadSupport.ReadContext
    ): RecordMaterializer[T] =
      new RecordMaterializer[T] {
        private val root = parquetType.newConverter()
        override def getCurrentRecord: T = root.get
        override def getRootConverter: GroupConverter = root.asGroupConverter()
      }
  }

  class WriteSupport[T](private var parquetType: ParquetType[T]) extends hadoop.WriteSupport[T] {
    def this() = this(null)

    override def getName: String = "magnolify"

    private var recordConsumer: RecordConsumer = null

    override def init(configuration: Configuration): hadoop.WriteSupport.WriteContext = {
      if (parquetType == null) {
        parquetType = SerializationUtils.fromBase64[ParquetType[T]](configuration.get(WriteTypeKey))
      }

      val schema = Schema.message(parquetType.schema)
      val metadata = new java.util.HashMap[String, String]()

      if (parquetType.properties.writeAvroSchemaToMetadata) {
        try {
          metadata.put(
            AVRO_SCHEMA_METADATA_KEY,
            parquetType.avroSchema.toString()
          )
        } catch {
          // parquet-avro has greater schema restrictions than magnolify-parquet, e.g., parquet-avro does not
          // support Maps with non-Binary key types
          case e: IllegalArgumentException =>
            logger.warn(
              s"Writer schema `$schema` contains a type not supported by Avro schemas; will not write " +
                s"key $AVRO_SCHEMA_METADATA_KEY to file metadata",
              e
            )
        }
      }

      new hadoop.WriteSupport.WriteContext(schema, metadata)
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
