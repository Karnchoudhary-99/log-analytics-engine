package loganalytics

/** Log severity levels, ordered from least to most severe. */
sealed abstract class LogLevel(val name: String, val rank: Int) extends Ordered[LogLevel] {
  override def compare(that: LogLevel): Int = rank.compare(that.rank)
  override def toString: String = name
}

object LogLevel {
  case object Trace extends LogLevel("TRACE", 0)
  case object Debug extends LogLevel("DEBUG", 1)
  case object Info  extends LogLevel("INFO", 2)
  case object Warn  extends LogLevel("WARN", 3)
  case object Error extends LogLevel("ERROR", 4)
  case object Fatal extends LogLevel("FATAL", 5)

  val all: List[LogLevel] = List(Trace, Debug, Info, Warn, Error, Fatal)

  /** Parse a level name, accepting common aliases (WARNING -> WARN, ERR -> ERROR). */
  def parse(raw: String): Option[LogLevel] = raw.trim.toUpperCase match {
    case "TRACE" | "TRC"             => Some(Trace)
    case "DEBUG" | "DBG"             => Some(Debug)
    case "INFO" | "INFORMATION"      => Some(Info)
    case "WARN" | "WARNING"          => Some(Warn)
    case "ERROR" | "ERR"             => Some(Error)
    case "FATAL" | "CRITICAL" | "CRIT" => Some(Fatal)
    case _                           => None
  }

  def atLeast(level: LogLevel): LogLevel => Boolean = (l: LogLevel) => l >= level
}
