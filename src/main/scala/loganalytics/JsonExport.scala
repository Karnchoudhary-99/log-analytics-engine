package loganalytics

/** Minimal JSON rendering for events and window rollups (stdlib only). */
object JsonExport {

  def escape(raw: String): String =
    raw.flatMap {
      case '"'  => "\\\""
      case '\\' => "\\\\"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c if c < ' ' => f"\\u${c.toInt}%04x"
      case c => c.toString
    }

  def eventJson(e: LogEvent): String = {
    val source = e.source.map(s => s""""${escape(s)}"""").getOrElse("null")
    val fields = e.fields.map { case (k, v) => s""""${escape(k)}":"${escape(v)}"""" }.mkString(",")
    s"""{"timestamp":"${e.timestamp}","level":"${e.level}","source":$source,"message":"${escape(e.message)}","fields":{$fields}}"""
  }

  def eventsJson(events: Seq[LogEvent]): String =
    "[" + events.map(eventJson).mkString(",") + "]"

  def windowJson(w: WindowSummary): String = {
    val levels = w.byLevel.map { case (l, c) => s""""$l":$c""" }.mkString(",")
    val sources = w.bySource.map { case (s, c) => s""""${escape(s)}":$c""" }.mkString(",")
    f"""{"windowStart":"${w.windowStart}","total":${w.total},"errors":${w.errorCount},"errorRate":${w.errorRate}%.4f,"byLevel":{$levels},"bySource":{$sources}}"""
  }

  def windowsJson(windows: Seq[WindowSummary]): String =
    "[" + windows.map(windowJson).mkString(",") + "]"
}
