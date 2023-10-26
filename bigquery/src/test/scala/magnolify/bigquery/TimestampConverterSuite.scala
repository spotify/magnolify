package magnolify.bigquery

import scala.util.control.NonFatal

class TimestampConverterSuite extends munit.ScalaCheckSuite {

  test("TIMESTAMP") {
    Seq(
      // this should be the default format sent by BQ API
      "2023-01-01 10:11:12.123456 UTC",
      // optional digits
      "2023-01-01 10:11:12.123456",
      "2023-01-01 10:11:12.1234",
      "2023-01-01 10:11:12",
      "2023-1-1 0:1:2",
      // time separator
      "2023-01-01T10:11:12.123456 UTC",
      "2023-01-01t10:11:12.123456 UTC",
      // offsets
      "2023-01-01t10:11:12.123456Z",
      "2023-01-01t10:11:12.123456z",
      "2023-01-01t10:11:12.123456+01:02",
      "2023-01-01t10:11:12.123456-01:02",
      // "2023-01-01t10:11:12.123456+1:2", not possible to get +H:M offset pattern
      // "2023-01-01t10:11:12.123456+1", skipped
      // zone
      "2023-01-01t10:11:12.123456 Europe/London",
      "2023-01-01t10:11:12.123456 UTC+01:02",
      "2023-01-01t10:11:12.123456 UTC-01:02",
      // "2023-01-01t10:11:12.123456 UTC+1:2", not possible to get +H:M offset pattern
      // "2023-01-01t10:11:12.123456 UTC+1", skipped
    ).foreach { ts =>
      try {
        TimestampConverter.toInstant(ts)
      } catch {
        case NonFatal(e) => throw new Exception(s"Failed parsing $ts", e)
      }
    }
    // formatter uses ISO
    val javaInstant = TimestampConverter.toInstant("2023-01-01 10:11:12.123456 UTC")
    val actual = TimestampConverter.fromInstant(javaInstant)
    assertEquals(actual, "2023-01-01T10:11:12.123456Z")
  }

  test("DATE") {
    Seq(
      "2023-01-02",
      "2023-01-2",
      "2023-1-2",
    ).foreach { date =>
      try {
        TimestampConverter.toLocalDate(date)
      } catch {
        case NonFatal(e) => throw new Exception(s"Failed parsing $date", e)
      }
    }
    // formatter uses ISO
    val javaLocalDate = TimestampConverter.toLocalDate("2023-01-02")
    val actual = TimestampConverter.fromLocalDate(javaLocalDate)
    assertEquals(actual, "2023-01-02")
  }

  test("TIME") {
    Seq(
      "10:11:12.123456",
      "10:11:12.1234",
      "10:11:12",
      "0:1:2",
      "0:1:2.123456"
    ).foreach { t =>
      try {
        TimestampConverter.toLocalTime(t)
      } catch {
        case NonFatal(e) => throw new Exception(s"Failed parsing $t", e)
      }
    }
    // formatter uses ISO
    val javaLocalTime = TimestampConverter.toLocalTime("16:03:57.029881")
    val actual = TimestampConverter.fromLocalTime(javaLocalTime)
    assertEquals(actual, "16:03:57.029881")
  }

  test("DATETIME") {
    Seq(
      "2023-01-01 10:11:12.123456",
      // optional digits
      "2023-01-01 10:11:12.123456",
      "2023-01-01 10:11:12.1234",
      "2023-01-01 10:11:12",
      "2023-1-1 0:1:2",
      // time separator
      "2023-01-01T10:11:12.123456",
      "2023-01-01t10:11:12.123456",
    ).foreach { datetime =>
        try {
          TimestampConverter.toLocalDateTime(datetime)
        } catch {
          case NonFatal(e) => throw new Exception(s"Failed parsing $datetime", e)
        }
    }
    // formatter uses ISO
    val javaLocalDateTime = TimestampConverter.toLocalDateTime("2023-01-01 10:11:12.123456")
    val actual = TimestampConverter.fromLocalDateTime(javaLocalDateTime)
    assertEquals(actual, "2023-01-01T10:11:12.123456")
  }
}
