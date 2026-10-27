package loganalytics

/** Tries each known format in turn, so one file can mix plain, Apache and JSON lines. */
object MultiFormatParser extends LogParser {
  def parse(line: String): Either[String, LogEvent] = {
    val trimmed = line.trim
    if (trimmed.startsWith("{")) JsonParser.parse(trimmed)
    else {
      ApacheParser.parse(trimmed) match {
        case right @ Right(_) => right
        case Left(_) => PlainParser.parse(trimmed)
      }
    }
  }

  def parseText(text: String): ParseReport =
    LogParser.parseLines(text.split("\n").toSeq, this)
}
