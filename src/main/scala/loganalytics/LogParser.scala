package loganalytics

/** A parser for one log-line shape. Left is a human-readable failure. */
trait LogParser {
  def parse(line: String): Either[String, LogEvent]
}

/** Result of parsing a whole input: the events plus per-line failures. */
case class ParseReport(events: List[LogEvent], errors: List[(Int, String, String)]) {
  def successCount: Int = events.size
  def errorCount: Int = errors.size
  def successRate: Double =
    if (events.size + errors.size == 0) 1.0
    else events.size.toDouble / (events.size + errors.size)
}

object LogParser {
  /** Parse every non-blank line, collecting failures instead of aborting. */
  def parseLines(lines: Seq[String], parser: LogParser): ParseReport = {
    val (events, errors) = lines.zipWithIndex.foldLeft((List.empty[LogEvent], List.empty[(Int, String, String)])) {
      case ((evs, errs), (line, idx)) =>
        if (line.trim.isEmpty) (evs, errs)
        else parser.parse(line) match {
          case Right(ev)  => (evs :+ ev, errs)
          case Left(err)  => (evs, errs :+ ((idx + 1, line, err)))
        }
    }
    ParseReport(events, errors)
  }
}
