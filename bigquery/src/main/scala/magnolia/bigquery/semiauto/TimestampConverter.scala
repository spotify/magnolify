package magnolia.bigquery.semiauto

import org.joda.time._
import org.joda.time.format._

private object TimestampConverter {
  // FIXME: verify that these match BigQuery specification
  // TIMESTAMP
  // YYYY-[M]M-[D]D[ [H]H:[M]M:[S]S[.DDDDDD]][time zone]
  private val timestampPrinter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS ZZZ")
  private val timestampParser = new DateTimeFormatterBuilder()
    .append(DateTimeFormat.forPattern("yyyy-MM-dd"))
    .appendOptional(new DateTimeFormatterBuilder()
      .append(DateTimeFormat.forPattern(" HH:mm:ss").getParser)
      .appendOptional(DateTimeFormat.forPattern(".SSSSSS").getParser)
      .toParser)
    .appendOptional(new DateTimeFormatterBuilder()
      .append(null, Array(" ZZZ", "ZZ").map(p => DateTimeFormat.forPattern(p).getParser))
      .toParser)
    .toFormatter
    .withZoneUTC()

  // DATE
  // YYYY-[M]M-[D]D
  private val datePrinter = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC()
  private val dateParser = datePrinter

  // TIME
  // [H]H:[M]M:[S]S[.DDDDDD]
  private val timePrinter = DateTimeFormat.forPattern("HH:mm:ss.SSSSSS").withZoneUTC()
  private val timeParser = new DateTimeFormatterBuilder()
    .append(DateTimeFormat.forPattern("HH:mm:ss").getParser)
    .appendOptional(DateTimeFormat.forPattern(".SSSSSS").getParser)
    .toFormatter
    .withZoneUTC()

  // DATETIME
  // YYYY-[M]M-[D]D[ [H]H:[M]M:[S]S[.DDDDDD]]
  private val datetimePrinter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
  private val datetimeParser = new DateTimeFormatterBuilder()
    .append(DateTimeFormat.forPattern("yyyy-MM-dd"))
    .appendOptional(new DateTimeFormatterBuilder()
      .append(DateTimeFormat.forPattern(" HH:mm:ss").getParser)
      .appendOptional(DateTimeFormat.forPattern(".SSSSSS").getParser)
      .toParser)
    .toFormatter
    .withZoneUTC()

  def toInstant(v: Any): Instant = timestampParser.parseDateTime(v.toString).toInstant
  def fromInstant(i: Instant): Any = timestampPrinter.print(i)

  def toLocalDate(v: Any): LocalDate = dateParser.parseLocalDate(v.toString)
  def fromLocalDate(d: LocalDate): Any = datePrinter.print(d)

  def toLocalTime(v: Any): LocalTime = timeParser.parseLocalTime(v.toString)
  def fromLocalTime(t: LocalTime): Any = timePrinter.print(t)

  def toLocalDateTime(v: Any): LocalDateTime = datetimeParser.parseLocalDateTime(v.toString)
  def fromLocalDateTime(dt: LocalDateTime): Any = datetimePrinter.print(dt)
}
