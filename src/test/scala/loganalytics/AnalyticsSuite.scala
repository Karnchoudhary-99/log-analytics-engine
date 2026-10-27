package loganalytics

import java.time.{Duration, Instant}
import org.scalatest.funsuite.AnyFunSuite

class AnalyticsSuite extends AnyFunSuite {
  private def ev(ts: String, level: LogLevel, source: String, msg: String): LogEvent =
    LogEvent(Instant.parse(ts), level, Some(source), msg)

  private val events = List(
    ev("2026-10-27T18:00:10Z", LogLevel.Info, "web", "request ok"),
    ev("2026-10-27T18:00:50Z", LogLevel.Error, "web", "request failed"),
    ev("2026-10-27T18:05:00Z", LogLevel.Error, "db", "request failed"),
    ev("2026-10-27T18:06:00Z", LogLevel.Warn, "db", "slow query")
  )

  test("time window buckets are epoch-aligned") {
    val w = TimeWindow(Duration.ofMinutes(5))
    assert(w.bucketStart(Instant.parse("2026-10-27T18:03:20Z")) == Instant.parse("2026-10-27T18:00:00Z"))
    assert(TimeWindow.parse("15m") == Some(TimeWindow(Duration.ofMinutes(15))))
    assert(TimeWindow.parse("1h") == Some(TimeWindow(Duration.ofHours(1))))
    assert(TimeWindow.parse("0m").isEmpty && TimeWindow.parse("nope").isEmpty)
  }

  test("aggregation counts by level and source, and windows roll up") {
    assert(Aggregation.countByLevel(events) == Map(LogLevel.Info -> 1, LogLevel.Error -> 2, LogLevel.Warn -> 1))
    assert(Aggregation.countBySource(events) == Map("web" -> 2, "db" -> 2))
    val windows = Aggregation.byWindow(events, TimeWindow(Duration.ofMinutes(5)))
    assert(windows.map(_.windowStart.toString) == List("2026-10-27T18:00:00Z", "2026-10-27T18:05:00Z"))
    assert(windows.head.total == 2 && windows.head.errorRate == 0.5)
    assert(Aggregation.errorRate(events) == 0.5)
  }

  test("top messages rank by frequency") {
    assert(Aggregation.topMessages(events, 1) == List(("request failed", 2)))
  }

  test("query filters compose: level, source, time range, message, field") {
    assert(LogQuery(minLevel = Some(LogLevel.Error)).apply(events).size == 2)
    assert(LogQuery(source = Some("db")).apply(events).size == 2)
    assert(LogQuery(from = Some(Instant.parse("2026-10-27T18:05:00Z"))).apply(events).size == 2)
    assert(LogQuery(messageContains = Some("slow")).apply(events).size == 1)
    val withField = ev("2026-10-27T18:00:00Z", LogLevel.Info, "web", "hi").copy(fields = Map("user" -> "alice"))
    assert(LogQuery(fieldEquals = Map("user" -> "alice")).apply(List(withField)).size == 1)
    assert(LogQuery(fieldEquals = Map("user" -> "bob")).apply(List(withField)).isEmpty)
    assert(LogQuery.errorsOnly.apply(events).size == 2)
  }
}
