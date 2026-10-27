package loganalytics

import java.time.Instant

/** One window's rollup: total events plus per-level and per-source counts. */
case class WindowSummary(
  windowStart: Instant,
  total: Int,
  byLevel: Map[LogLevel, Int],
  bySource: Map[String, Int]
) {
  def errorCount: Int = byLevel.filter { case (l, _) => l >= LogLevel.Error }.values.sum
  def errorRate: Double = if (total == 0) 0.0 else errorCount.toDouble / total
}

object Aggregation {
  def countByLevel(events: Seq[LogEvent]): Map[LogLevel, Int] =
    events.groupBy(_.level).view.mapValues(_.size).toMap

  def countBySource(events: Seq[LogEvent]): Map[String, Int] =
    events.flatMap(_.source).groupBy(identity).view.mapValues(_.size).toMap

  def topMessages(events: Seq[LogEvent], n: Int): List[(String, Int)] =
    top(events.map(_.message), n)

  def topSources(events: Seq[LogEvent], n: Int): List[(String, Int)] =
    top(events.flatMap(_.source), n)

  private def top(values: Seq[String], n: Int): List[(String, Int)] =
    values.groupBy(identity).view.mapValues(_.size).toList.sortBy { case (_, c) => -c }.take(n)

  /** Bucket events into tumbling windows, ordered by window start. */
  def byWindow(events: Seq[LogEvent], window: TimeWindow): List[WindowSummary] =
    events.groupBy(e => window.bucketStart(e.timestamp)).toList.sortBy(_._1).map {
      case (start, evs) =>
        WindowSummary(start, evs.size, countByLevel(evs), countBySource(evs))
    }

  def errorRate(events: Seq[LogEvent]): Double =
    if (events.isEmpty) 0.0 else events.count(_.isError).toDouble / events.size

  /** Events per second over the span covered by the events (0 if < 2 events). */
  def eventsPerSecond(events: Seq[LogEvent]): Double = {
    if (events.size < 2) 0.0
    else {
      val span = events.map(_.timestamp).max.getEpochSecond - events.map(_.timestamp).min.getEpochSecond
      if (span <= 0) events.size.toDouble else events.size.toDouble / span
    }
  }
}
