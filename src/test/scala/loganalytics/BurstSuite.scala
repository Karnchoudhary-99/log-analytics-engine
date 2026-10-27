package loganalytics

import java.time.Instant
import org.scalatest.funsuite.AnyFunSuite

class BurstSuite extends AnyFunSuite {
  private def window(ts: String, total: Int): WindowSummary =
    WindowSummary(Instant.parse(ts), total, Map(LogLevel.Info -> total), Map("web" -> total))

  test("a window several times the baseline is a burst") {
    val windows = List(window("2026-10-27T18:00:00Z", 2), window("2026-10-27T18:05:00Z", 2),
      window("2026-10-27T18:10:00Z", 30))
    val bursts = BurstDetector.detect(windows)
    assert(bursts.size == 1)
    assert(bursts.head.windowStart == Instant.parse("2026-10-27T18:10:00Z"))
    assert(bursts.head.total == 30 && bursts.head.ratio == 15.0)
  }

  test("no burst for flat traffic, tiny windows, or a single window") {
    val flat = List(window("2026-10-27T18:00:00Z", 4), window("2026-10-27T18:05:00Z", 5))
    assert(BurstDetector.detect(flat).isEmpty)
    val tiny = List(window("2026-10-27T18:00:00Z", 1), window("2026-10-27T18:05:00Z", 4))
    assert(BurstDetector.detect(tiny).isEmpty)
    assert(BurstDetector.detect(List(window("2026-10-27T18:00:00Z", 100))).isEmpty)
  }

  test("burst report renders counts and ratios") {
    val rendered = TrendReport.renderBursts(BurstDetector.detect(List(
      window("2026-10-27T18:00:00Z", 2), window("2026-10-27T18:05:00Z", 30))))
    assert(rendered.contains("1 burst(s)") && rendered.contains("ratio=15.0x"))
    assert(TrendReport.renderBursts(Nil) == "No bursts detected.\n")
  }
}
