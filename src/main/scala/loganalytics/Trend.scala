package loganalytics

import java.time.Instant

/** One window's error-rate point in a time series. */
case class TrendPoint(windowStart: Instant, errorRate: Double, total: Int)

/** Error-rate trend over tumbling windows, via least-squares slope. */
object Trend {

  /** Per-window error-rate series, in window order. */
  def series(events: Seq[LogEvent], window: TimeWindow): List[TrendPoint] =
    Aggregation.byWindow(events, window).map(w => TrendPoint(w.windowStart, w.errorRate, w.total))

  /**
   * Least-squares slope of error rate per window step (rate points per
   * window). Positive means errors are rising over time.
   */
  def slope(points: Seq[TrendPoint]): Double = {
    if (points.size < 2) return 0.0
    val n = points.size.toDouble
    val xs = points.indices.map(_.toDouble)
    val ys = points.map(_.errorRate)
    val meanX = xs.sum / n
    val meanY = ys.sum / n
    val num = xs.zip(ys).map { case (x, y) => (x - meanX) * (y - meanY) }.sum
    val den = xs.map(x => (x - meanX) * (x - meanX)).sum
    if (den == 0.0) 0.0 else num / den
  }

  /** Plain-language direction from the slope (dead band +/- 0.05 per step). */
  def direction(points: Seq[TrendPoint]): String = {
    val s = slope(points)
    if (s > 0.05) "rising" else if (s < -0.05) "falling" else "stable"
  }
}
