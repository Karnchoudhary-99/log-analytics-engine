package loganalytics

/** Plain-text rendering of a trend series and detected bursts. */
object TrendReport {

  def renderTrend(points: Seq[TrendPoint]): String = {
    if (points.isEmpty) return "No windows to trend.\n"
    val sb = new StringBuilder
    sb.append(f"Error-rate trend: ${Trend.direction(points)} (slope ${Trend.slope(points)}%.3f per window)\n")
    points.foreach { p =>
      sb.append(f"  ${p.windowStart} total=${p.total} rate=${p.errorRate * 100}%.1f%%\n")
    }
    sb.toString
  }

  def renderBursts(bursts: Seq[Burst]): String = {
    if (bursts.isEmpty) return "No bursts detected.\n"
    val sb = new StringBuilder(s"${bursts.size} burst(s):\n")
    bursts.foreach { b =>
      sb.append(f"  ${b.windowStart} total=${b.total} baseline=${b.baselineMean}%.1f ratio=${b.ratio}%.1fx\n")
    }
    sb.toString
  }
}
