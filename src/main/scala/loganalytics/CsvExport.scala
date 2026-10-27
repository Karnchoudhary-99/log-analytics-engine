package loganalytics

/** CSV rendering for events and window rollups. */
object CsvExport {

  def escapeCell(raw: String): String =
    if (raw.contains(",") || raw.contains("\"") || raw.contains("\n"))
      "\"" + raw.replace("\"", "\"\"") + "\""
    else raw

  def eventsCsv(events: Seq[LogEvent]): String = {
    val sb = new StringBuilder("timestamp,level,source,message\n")
    events.foreach { e =>
      sb.append(s"${e.timestamp},${e.level},${escapeCell(e.source.getOrElse(""))},${escapeCell(e.message)}\n")
    }
    sb.toString
  }

  def windowsCsv(windows: Seq[WindowSummary]): String = {
    val sb = new StringBuilder("windowStart,total,errors,errorRate\n")
    windows.foreach { w =>
      sb.append(f"${w.windowStart},${w.total},${w.errorCount},${w.errorRate}%.4f\n")
    }
    sb.toString
  }
}
