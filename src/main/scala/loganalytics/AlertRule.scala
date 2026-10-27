package loganalytics

/**
 * A threshold rule evaluated over tumbling windows: a window fires the
 * rule when its error rate reaches `minErrorRate` OR its error count
 * reaches `minErrorCount` (whichever thresholds are set).
 */
case class AlertRule(
  name: String,
  window: TimeWindow,
  minErrorRate: Option[Double] = None,
  minErrorCount: Option[Int] = None
) {
  require(name.nonEmpty, "alert rule name must not be empty")
  require(
    minErrorRate.isEmpty || (minErrorRate.get >= 0.0 && minErrorRate.get <= 1.0),
    "minErrorRate must be between 0 and 1"
  )
  require(minErrorCount.isEmpty || minErrorCount.get >= 0, "minErrorCount must not be negative")
  require(minErrorRate.isDefined || minErrorCount.isDefined, "at least one threshold is required")

  def triggeredBy(summary: WindowSummary): Boolean =
    minErrorRate.exists(summary.errorRate >= _) || minErrorCount.exists(summary.errorCount >= _)
}

object AlertRule {

  /** Default rule used by the CLI when no `--alert` spec is given. */
  val default: AlertRule =
    AlertRule("high-error-rate", TimeWindow.parse("5m").get, minErrorRate = Some(0.5))

  /**
   * Parse a CLI spec of the form `name:rate=0.5,count=3,window=5m`
   * (rate and count are optional, but at least one must be present).
   */
  def parse(raw: String): Option[AlertRule] = {
    val parts = raw.split(":", 2)
    if (parts.length != 2 || parts(0).trim.isEmpty) return None
    val name = parts(0).trim
    var window = TimeWindow.parse("5m").get
    var rate: Option[Double] = None
    var count: Option[Int] = None
    for (kv <- parts(1).split(",")) {
      val kvParts = kv.split("=", 2)
      if (kvParts.length != 2) return None
      kvParts(0).trim match {
        case "rate"   => rate = kvParts(1).trim.toDoubleOption.filter(r => r >= 0.0 && r <= 1.0)
        case "count"  => count = kvParts(1).trim.toIntOption.filter(_ >= 0)
        case "window" => TimeWindow.parse(kvParts(1).trim) match {
          case Some(w) => window = w
          case None    => return None
        }
        case _ => return None
      }
      if ((kvParts(0).trim == "rate" && rate.isEmpty) || (kvParts(0).trim == "count" && count.isEmpty)) return None
    }
    if (rate.isEmpty && count.isEmpty) None
    else Some(AlertRule(name, window, rate, count))
  }
}
