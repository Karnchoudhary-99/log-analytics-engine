package loganalytics

import java.time.Instant
import scala.io.Source
import scala.util.Using

/** CLI: parse | aggregate | query over a log file (mixed formats). */
object Main {
  private val usage =
    """Usage:
      |  log-analytics parse <file>
      |  log-analytics aggregate <file> [--window 5m]
      |  log-analytics query <file> [--level ERROR] [--source web]
      |      [--from 2026-10-27T00:00:00Z] [--to 2026-10-28T00:00:00Z]
      |      [--contains text] [--field key=value]
      |""".stripMargin

  def main(args: Array[String]): Unit = {
    if (args.length < 2) { println(usage); sys.exit(2) }
    val (command, file) = (args(0), args(1))
    val opts = parseOpts(args.drop(2))
    val text = Using(Source.fromFile(file))(_.mkString).getOrElse {
      System.err.println(s"cannot read file: $file"); sys.exit(1)
    }
    val report = MultiFormatParser.parseText(text)
    command match {
      case "parse" =>
        println(Report.render(report, TimeWindow.parse(opts.getOrElse("window", "5m")).getOrElse(TimeWindow(java.time.Duration.ofMinutes(5)))))
        report.errors.foreach { case (line, raw, err) => System.err.println(s"line $line: $err [$raw]") }
      case "aggregate" =>
        val window = TimeWindow.parse(opts.getOrElse("window", "5m")).getOrElse {
          System.err.println(s"bad window: ${opts("window")}"); sys.exit(1)
        }
        println(Report.render(report, window))
      case "query" =>
        val query = LogQuery(
          minLevel = opts.get("level").flatMap(LogLevel.parse),
          source = opts.get("source"),
          from = opts.get("from").flatMap(TimestampParser.parse),
          to = opts.get("to").flatMap(TimestampParser.parse),
          messageContains = opts.get("contains"),
          fieldEquals = opts.get("field").map(f => f.split("=", 2) match {
            case Array(k, v) => Map(k -> v); case _ => Map.empty[String, String]
          }).getOrElse(Map.empty)
        )
        val matched = query(report.events)
        println(s"${matched.size} matching events:")
        matched.foreach(e => println(Report.renderEvent(e)))
      case other =>
        System.err.println(s"unknown command: $other"); println(usage); sys.exit(2)
    }
  }

  private def parseOpts(args: Seq[String]): Map[String, String] =
    args.grouped(2).collect { case Seq(k, v) if k.startsWith("--") => k.drop(2) -> v }.toMap
}
