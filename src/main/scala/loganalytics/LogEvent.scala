package loganalytics

import java.time.Instant

/**
 * One structured log event.
 *
 * @param timestamp when the event happened (UTC)
 * @param level     severity
 * @param source    emitting component / service, when known
 * @param message   human-readable message, with key=value pairs stripped out
 * @param fields    structured fields (k=v pairs, HTTP status, JSON extras, ...)
 */
case class LogEvent(
  timestamp: Instant,
  level: LogLevel,
  source: Option[String],
  message: String,
  fields: Map[String, String] = Map.empty
) {
  def isError: Boolean = level >= LogLevel.Error
  def field(key: String): Option[String] = fields.get(key)
}
