package loganalytics

import java.time.Instant

/** A window whose volume dwarfs the rest of the stream. */
case class Burst(windowStart: Instant, total: Int, baselineMean: Double, ratio: Double)

/**
 * Burst detection: a window is a burst when its event count is at least
 * `minRatio` times the mean of all OTHER windows and at least
 * `minTotal` events (so tiny streams never burst). A single-window
 * stream has no baseline and never bursts.
 */
object BurstDetector {

  def detect(
    windows: Seq[WindowSummary],
    minRatio: Double = 3.0,
    minTotal: Int = 5
  ): List[Burst] = {
    if (windows.size < 2) return Nil
    windows.flatMap { w =>
      val others = windows.filterNot(_ == w).map(_.total)
      val baseline = others.sum.toDouble / others.size
      val ratio = if (baseline == 0.0) Double.PositiveInfinity else w.total / baseline
      if (w.total >= minTotal && ratio >= minRatio)
        Some(Burst(w.windowStart, w.total, baseline, ratio))
      else None
    }.toList.sortBy(_.windowStart)
  }
}
