package loganalytics

/** Plain-text rendering of a parse report and its aggregates. */
object Report {
  def render(report: ParseReport, window: TimeWindow): String = {
    val events = report.events
    val sb = new StringBuilder
    sb.append(s"Parsed ${report.successCount} events, ${report.errorCount} failures " +
      f"(success rate ${report.successRate * 100}%.1f%%)\n")
    if (events.isEmpty) return sb.toString
    sb.append(f"Error rate: ${Aggregation.errorRate(events) * 100}%.1f%%\n")
    sb.append("By level:\n")
    Aggregation.countByLevel(events).toList.sortBy(_._1.rank).foreach { case (l, c) =>
      sb.append(s"  $l: $c\n")
    }
    sb.append("Top sources:\n")
    Aggregation.topSources(events, 5).foreach { case (s, c) => sb.append(s"  $s: $c\n") }
    sb.append("Top messages:\n")
    Aggregation.topMessages(events, 5).foreach { case (m, c) => sb.append(s"  [$c] $m\n") }
    sb.append(s"Windows (${window.size.toMinutes} min):\n")
    Aggregation.byWindow(events, window).foreach { w =>
      sb.append(s"  ${w.windowStart} total=${w.total} errors=${w.errorCount}\n")
    }
    sb.toString
  }

  /** Full report plus the error-rate trend and burst sections. */
  def renderWithTrend(report: ParseReport, window: TimeWindow): String =
    render(report, window) +
      TrendReport.renderTrend(Trend.series(report.events, window)) +
      TrendReport.renderBursts(BurstDetector.detect(Aggregation.byWindow(report.events, window)))

  def renderEvent(e: LogEvent): String =
    s"${e.timestamp} ${e.level} [${e.source.getOrElse("-")}] ${e.message}" +
      (if (e.fields.isEmpty) "" else " " + e.fields.map { case (k, v) => s"$k=$v" }.mkString(" "))
}
