package loganalytics

import java.time.{Duration, Instant}

/** A fixed-size tumbling window: every instant maps to exactly one bucket. */
case class TimeWindow(size: Duration) {
  require(!size.isZero && !size.isNegative, "window size must be positive")

  /** Start of the bucket containing `instant` (epoch-aligned). */
  def bucketStart(instant: Instant): Instant = {
    val secs = size.getSeconds
    val epoch = instant.getEpochSecond
    val start = (epoch / secs) * secs
    Instant.ofEpochSecond(start)
  }

  def contains(windowStart: Instant, instant: Instant): Boolean =
    !instant.isBefore(windowStart) && instant.isBefore(windowStart.plus(size))
}

object TimeWindow {
  /** Parse "15m", "1h", "30s", "1d" window sizes. */
  def parse(raw: String): Option[TimeWindow] = {
    val trimmed = raw.trim.toLowerCase
    val num = trimmed.dropRight(1)
    val unit = trimmed.lastOption
    (num.toLongOption, unit) match {
      case (Some(n), Some('s')) if n > 0 => Some(TimeWindow(Duration.ofSeconds(n)))
      case (Some(n), Some('m')) if n > 0 => Some(TimeWindow(Duration.ofMinutes(n)))
      case (Some(n), Some('h')) if n > 0 => Some(TimeWindow(Duration.ofHours(n)))
      case (Some(n), Some('d')) if n > 0 => Some(TimeWindow(Duration.ofDays(n)))
      case _ => None
    }
  }
}
