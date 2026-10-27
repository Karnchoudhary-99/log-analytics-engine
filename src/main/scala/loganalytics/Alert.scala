package loganalytics

import java.time.{Duration, Instant}

/** One fired alert: a rule that a single window tripped. */
case class Alert(
  ruleName: String,
  windowStart: Instant,
  windowSize: Duration,
  total: Int,
  errorCount: Int,
  errorRate: Double
) {
  /** Plain-language severity band, for triage ordering. */
  def severity: String =
    if (errorRate >= 0.5 || errorCount >= 20) "critical"
    else if (errorRate >= 0.25 || errorCount >= 5) "warning"
    else "info"
}
