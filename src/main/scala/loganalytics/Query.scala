package loganalytics

import java.time.Instant

/**
 * A composable log query. Every set field is an AND filter; unset fields
 * do not filter. `minLevel` is inclusive (>=).
 */
case class LogQuery(
  minLevel: Option[LogLevel] = None,
  source: Option[String] = None,
  from: Option[Instant] = None,
  to: Option[Instant] = None,
  messageContains: Option[String] = None,
  fieldEquals: Map[String, String] = Map.empty
) {
  def apply(events: Seq[LogEvent]): List[LogEvent] = events.filter(matches).toList

  def matches(e: LogEvent): Boolean =
    minLevel.forall(e.level >= _) &&
    source.forall(s => e.source.contains(s)) &&
    from.forall(f => !e.timestamp.isBefore(f)) &&
    to.forall(t => e.timestamp.isBefore(t)) &&
    messageContains.forall(sub => e.message.toLowerCase.contains(sub.toLowerCase)) &&
    fieldEquals.forall { case (k, v) => e.field(k).contains(v) }
}

object LogQuery {
  def errorsOnly: LogQuery = LogQuery(minLevel = Some(LogLevel.Error))
}
