package loganalytics

import java.time.{Duration, Instant}
import org.scalatest.funsuite.AnyFunSuite

class AlertSuite extends AnyFunSuite {
  private def ev(ts: String, level: LogLevel): LogEvent =
    LogEvent(Instant.parse(ts), level, Some("web"), "msg")

  private val window5 = TimeWindow(Duration.ofMinutes(5))

  test("rule fires on error rate or error count, independently") {
    val summary = WindowSummary(Instant.parse("2026-10-27T18:00:00Z"), 4,
      Map(LogLevel.Info -> 3, LogLevel.Error -> 1), Map("web" -> 4))
    assert(AlertRule("r", window5, minErrorRate = Some(0.25)).triggeredBy(summary))
    assert(!AlertRule("r", window5, minErrorRate = Some(0.26)).triggeredBy(summary))
    assert(AlertRule("r", window5, minErrorCount = Some(1)).triggeredBy(summary))
    assert(!AlertRule("r", window5, minErrorCount = Some(2)).triggeredBy(summary))
  }

  test("rule spec parsing accepts rate/count/window and rejects junk") {
    val rule = AlertRule.parse("sev:rate=0.5,count=3,window=15m")
    assert(rule.isDefined)
    assert(rule.get.name == "sev" && rule.get.minErrorRate == Some(0.5))
    assert(rule.get.minErrorCount == Some(3) && rule.get.window == TimeWindow(Duration.ofMinutes(15)))
    assert(AlertRule.parse("sev:window=5m").isEmpty)
    assert(AlertRule.parse("sev:rate=2.0").isEmpty)
    assert(AlertRule.parse("sev:bogus=1").isEmpty)
    assert(AlertRule.parse("nocolon").isEmpty)
  }

  test("evaluator fires only in the bad window") {
    val events = List(
      ev("2026-10-27T18:00:10Z", LogLevel.Info),
      ev("2026-10-27T18:00:20Z", LogLevel.Info),
      ev("2026-10-27T18:05:10Z", LogLevel.Error),
      ev("2026-10-27T18:05:20Z", LogLevel.Error)
    )
    val alerts = AlertEvaluator.evaluate(List(AlertRule("bad", window5, minErrorRate = Some(0.5))), events)
    assert(alerts.size == 1)
    assert(alerts.head.windowStart == Instant.parse("2026-10-27T18:05:00Z"))
    assert(alerts.head.severity == "critical")
    assert(AlertEvaluator.evaluate(List(AlertRule("bad", window5, minErrorRate = Some(0.5))), events.take(2)).isEmpty)
  }

  test("severity bands and report ordering") {
    val critical = Alert("r", Instant.parse("2026-10-27T18:00:00Z"), Duration.ofMinutes(5), 2, 2, 1.0)
    val warning = Alert("r", Instant.parse("2026-10-27T18:05:00Z"), Duration.ofMinutes(5), 4, 1, 0.25)
    assert(critical.severity == "critical" && warning.severity == "warning")
    val rendered = AlertReport.render(List(warning, critical))
    assert(rendered.indexOf("CRITICAL") < rendered.indexOf("WARNING"))
    assert(AlertReport.render(Nil) == "No alerts fired.\n")
  }
}
