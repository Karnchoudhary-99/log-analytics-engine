package loganalytics

/** Evaluates alert rules window-by-window over a parsed event stream. */
object AlertEvaluator {

  /** All alerts fired, ordered by window start then rule name. */
  def evaluate(rules: Seq[AlertRule], events: Seq[LogEvent]): List[Alert] =
    rules.flatMap { rule =>
      Aggregation.byWindow(events, rule.window)
        .filter(rule.triggeredBy)
        .map(s => Alert(rule.name, s.windowStart, rule.window.size, s.total, s.errorCount, s.errorRate))
    }.toList.sortBy(a => (a.windowStart, a.ruleName))

  /** Convenience: the default rule only. */
  def evaluateDefault(events: Seq[LogEvent]): List[Alert] =
    evaluate(List(AlertRule.default), events)
}
