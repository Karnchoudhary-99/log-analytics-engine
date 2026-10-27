package loganalytics

import scala.util.matching.Regex

/** Extracts `key=value` pairs (quoted values allowed) from a message tail. */
object FieldExtractor {
  private val pair: Regex = """(\w+)=("[^"]*"|\S+)""".r

  /** Returns (message with pairs removed, extracted fields). */
  def extract(message: String): (String, Map[String, String]) = {
    var fields = Map.empty[String, String]
    val cleaned = pair.replaceAllIn(message, m => {
      val value = m.group(2)
      fields += m.group(1) -> value.stripPrefix("\"").stripSuffix("\"")
      ""
    })
    (cleaned.trim.replaceAll("\\s+", " "), fields)
  }
}

/**
 * Plain-text lines: `<timestamp> <LEVEL> [source] message key=value ...`
 *
 * Timestamp may be ISO-8601, `yyyy-MM-dd HH:mm:ss`, or epoch seconds/millis.
 * Example: `2026-10-27T18:00:00Z INFO [web] User login succeeded user=alice`
 */
object PlainParser extends LogParser {
  private val line: Regex =
    """^((?:\d{4}-\d{2}-\d{2}T\S+)|(?:\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})|(?:\d{10,13}))\s+([A-Za-z]+)\s+(?:\[([^\]]+)\]\s+)?(.*)$""".r

  def parse(rawLine: String): Either[String, LogEvent] = rawLine.trim match {
    case line(ts, levelRaw, source, rest) =>
      (TimestampParser.parse(ts), LogLevel.parse(levelRaw)) match {
        case (Some(ts), Some(level)) =>
          val (message, fields) = FieldExtractor.extract(rest)
          Right(LogEvent(ts, level, Option(source), message, fields))
        case (None, _) => Left(s"unparseable timestamp in line: $rawLine")
        case (_, None) => Left(s"unknown log level '$levelRaw' in line: $rawLine")
      }
    case _ => Left(s"line does not match plain format: $rawLine")
  }
}
