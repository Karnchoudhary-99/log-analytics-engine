package loganalytics

import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.util.Try
import scala.util.matching.Regex

/**
 * Apache combined log format:
 * `127.0.0.1 - alice [27/Oct/2026:18:00:00 +0000] "GET /index HTTP/1.1" 200 1234 "-" "curl/8.0"`
 *
 * Level is derived from the HTTP status: 5xx -> ERROR, 4xx -> WARN, else INFO.
 */
object ApacheParser extends LogParser {
  private val line: Regex =
    """^(\S+) \S+ (\S+) \[([^\]]+)\] "([^"]*)" (\d{3}) (\S+) "([^"]*)" "([^"]*)"$""".r
  private val dateFormat =
    DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH)

  def parse(rawLine: String): Either[String, LogEvent] = rawLine.trim match {
    case line(ip, user, date, request, status, bytes, referer, agent) =>
      Try(dateFormat.parse(date, java.time.ZonedDateTime.from(_)).toInstant).toOption match {
        case None => Left(s"unparseable Apache date in line: $rawLine")
        case Some(ts) =>
          val code = status.toInt
          val level = if (code >= 500) LogLevel.Error else if (code >= 400) LogLevel.Warn else LogLevel.Info
          val fields = Map(
            "client_ip" -> ip,
            "user" -> user,
            "status" -> status,
            "bytes" -> bytes,
            "referer" -> referer,
            "user_agent" -> agent
          ).filter { case (_, v) => v != "-" }
          Right(LogEvent(ts, level, Some("http"), request, fields))
      }
    case _ => Left(s"line does not match Apache combined format: $rawLine")
  }
}
