package magnolify.datastore

import java.time.{Duration, Instant}

import com.google.datastore.v1.Value
import com.google.protobuf.Timestamp

object TimestampConverter {
  private val millisPerSecond = Duration.ofSeconds(1).toMillis
  def toInstant(v: Value): Instant = {
    val t = v.getTimestampValue
    Instant.ofEpochMilli(t.getSeconds * millisPerSecond + t.getNanos / 1000000)
  }
  def fromInstant(i: Instant): Value.Builder = {
    val t = Timestamp
      .newBuilder()
      .setSeconds(i.toEpochMilli / millisPerSecond)
      .setNanos((i.toEpochMilli % 1000).toInt * 1000000)
    Value.newBuilder().setTimestampValue(t)
  }
}
