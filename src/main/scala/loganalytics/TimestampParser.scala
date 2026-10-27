package loganalytics

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import scala.util.Try

/** Parses the timestamp shapes found in real logs. All results are UTC Instants. */
object TimestampParser {
  private val isoFormatter = DateTimeFormatter.ISO_INSTANT
  private val spaceFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)
  private val localFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  def parse(raw: String): Option[Instant] = {
    val s = raw.trim
    if (s.isEmpty) return None
    // Epoch seconds (10 digits) or millis (13 digits), bare numbers only.
    if (s.matches("\\d{10}")) return Try(Instant.ofEpochSecond(s.toLong)).toOption
    if (s.matches("\\d{13}")) return Try(Instant.ofEpochMilli(s.toLong)).toOption
    Try(Instant.parse(s))
      .orElse(Try(LocalDateTime.parse(s, localFormatter).toInstant(ZoneOffset.UTC)))
      .orElse(Try(spaceFormatter.parse(s, Instant.from(_))))
      .toOption
  }

  /** Parse, throwing a readable error for parser error messages. */
  def parseOrThrow(raw: String): Instant =
    parse(raw).getOrElse(throw new DateTimeParseException(s"unparseable timestamp: $raw", raw, 0))
}
