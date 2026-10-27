package loganalytics

/** Plain-text rendering of fired alerts, most severe first. */
object AlertReport {

  def render(alerts: Seq[Alert]): String = {
    if (alerts.isEmpty) return "No alerts fired.\n"
    val sb = new StringBuilder
    sb.append(s"${alerts.size} alert(s):\n")
    val rank = Map("critical" -> 0, "warning" -> 1, "info" -> 2)
    alerts.toList.sortBy(a => (rank.getOrElse(a.severity, 9), a.windowStart)).foreach { a =>
      sb.append(f"  [${a.severity.toUpperCase}] ${a.ruleName} @ ${a.windowStart} " +
        f"(window ${a.windowSize.toMinutes} min, ${a.errorCount}/${a.total} errors, " +
        f"rate ${a.errorRate * 100}%.1f%%)\n")
    }
    sb.toString
  }
}
