package loganalytics

/**
 * Minimal flat-JSON-object log parser (stdlib only, no JSON dependency).
 * Handles string / number / boolean / null values; nested objects and
 * arrays are rejected, since structured logs are normally flat.
 *
 * Recognised keys: timestamp|time|ts|@timestamp, level|severity,
 * source|logger|service|app, message|msg|log. Everything else lands in fields.
 */
object JsonParser extends LogParser {

  private class Cursor(val s: String) {
    var pos: Int = 0
    def skipWs(): Unit = while (pos < s.length && s.charAt(pos).isWhitespace) pos += 1
    def peek: Char = if (pos < s.length) s.charAt(pos) else 0.toChar
    def expect(c: Char): Boolean = { skipWs(); if (peek == c) { pos += 1; true } else false }
  }

  private def parseString(c: Cursor): Either[String, String] = {
    if (!c.expect('"')) return Left("expected string")
    val sb = new StringBuilder
    var done = false
    while (!done && c.pos < c.s.length) {
      val ch = c.s.charAt(c.pos); c.pos += 1
      ch match {
        case '"' => done = true
        case '\\' if c.pos < c.s.length =>
          val esc = c.s.charAt(c.pos); c.pos += 1
          sb.append(esc match {
            case 'n' => '\n'; case 't' => '\t'; case 'r' => '\r'
            case other => other
          })
        case other => sb.append(other)
      }
    }
    if (done) Right(sb.toString) else Left("unterminated string")
  }

  private def parseValue(c: Cursor): Either[String, String] = {
    c.skipWs()
    if (c.peek == '"') parseString(c)
    else {
      val start = c.pos
      while (c.pos < c.s.length && !",}".contains(c.s.charAt(c.pos))) c.pos += 1
      val raw = c.s.substring(start, c.pos).trim
      if (raw.isEmpty) Left("expected value")
      else if (raw == "null") Right("")
      else Right(raw)
    }
  }

  def parseObject(raw: String): Either[String, Map[String, String]] = {
    val c = new Cursor(raw.trim)
    if (!c.expect('{')) return Left("not a JSON object")
    var fields = Map.empty[String, String]
    var first = true
    while (true) {
      c.skipWs()
      if (c.peek == '}') { c.pos += 1; return Right(fields) }
      if (!first && !c.expect(',')) return Left("expected ',' between fields")
      first = false
      val key = parseString(c) match { case Right(k) => k; case Left(e) => return Left(e) }
      if (!c.expect(':')) return Left(s"expected ':' after key $key")
      val value = parseValue(c) match { case Right(v) => v; case Left(e) => return Left(e) }
      fields += key -> value
    }
    Left("unterminated object")
  }

  def parse(rawLine: String): Either[String, LogEvent] = {
    parseObject(rawLine) match {
      case Left(err) => Left(err)
      case Right(obj) =>
        val tsRaw = List("timestamp", "time", "ts", "@timestamp").flatMap(obj.get).headOption
        val levelRaw = List("level", "severity").flatMap(obj.get).headOption
        val message = List("message", "msg", "log").flatMap(obj.get).headOption.getOrElse("")
        val source = List("source", "logger", "service", "app").flatMap(obj.get).headOption
        val reserved = Set("timestamp", "time", "ts", "@timestamp", "level", "severity",
          "source", "logger", "service", "app", "message", "msg", "log")
        val fields = obj.filter { case (k, _) => !reserved.contains(k) }.filter { case (_, v) => v.nonEmpty }
        (tsRaw.flatMap(TimestampParser.parse), levelRaw.flatMap(LogLevel.parse)) match {
          case (Some(ts), Some(level)) => Right(LogEvent(ts, level, source, message, fields))
          case (None, _) => Left(s"missing or unparseable timestamp in JSON line: $rawLine")
          case (_, None) => Left(s"missing or unknown level in JSON line: $rawLine")
        }
    }
  }
}
