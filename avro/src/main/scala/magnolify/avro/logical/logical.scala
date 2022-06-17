package magnolify.avro

package object logical {

  object time {
    object micros extends AvroTimeMicrosImplicits
    object millis extends AvroTimeMillisImplicits
  }

  object bigquery extends AvroBigQueryImplicits {
    def registerLogicalTypes(): Unit = AvroBigQuery.registerLogicalTypes()
  }

}
