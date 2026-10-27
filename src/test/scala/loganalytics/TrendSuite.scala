package loganalytics

import java.time.{Duration, Instant}
import org.scalatest.funsuite.AnyFunSuite

class TrendSuite extends AnyFunSuite {
  private def ev(ts: String, level: LogLevel): LogEvent =
    LogEvent(Instant.parse(ts), level, Some("web"), "msg")

  test("trend series buckets error rates per window") {
    val events = List(
      ev("2026-10-27T18:00:10Z", LogLevel.Info),
      ev("2026-10-27T18:00:20Z", LogLevel.Error),
      ev("2026-10-27T18:05:10Z", LogLevel.Error),
      ev("2026-10-27T18:05:20Z", LogLevel.Error)
    )
    val points = Trend.series(events, TimeWindow(Duration.ofMinutes(5)))
    assert(points.map(_.errorRate) == List(0.5, 1.0))
    assert(points.map(_.total) == List(2, 2))
  }

  test("slope sign and direction follow the series") {
    val rising = List(
      TrendPoint(Instant.parse("2026-10-27T18:00:00Z"), 0.0, 2),
      TrendPoint(Instant.parse("2026-10-27T18:05:00Z"), 0.5, 2),
      TrendPoint(Instant.parse("2026-10-27T18:10:00Z"), 1.0, 2)
    )
    assert(Trend.slope(rising) == 0.5)
    assert(Trend.direction(rising) == "rising")
    assert(Trend.direction(rising.reverse) == "falling")
    assert(Trend.direction(List(rising.head)) == "stable")
    assert(Trend.slope(Nil) == 0.0)
  }
}
