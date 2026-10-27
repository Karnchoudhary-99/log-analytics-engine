package loganalytics

import java.time.Instant
import scala.io.Source
import scala.util.Using

/** CLI: parse | aggregate | query | alerts over a log file (mixed formats). */
object Main {
  private val usage =
    """Usage:
      |  log-analytics parse <file> [--format text|json|csv]
      |  log-analytics aggregate <file> [--window 5m] [--format text|json|csv]
      |  log-analytics query <file> [--level ERROR] [--source web]
      |      [--from 2026-10-27T00:00:00Z] [--to 2026-10-28T00:00:00Z]
      |      [--contains text] [--field key=value] [--format text|json|csv]
      |  log-analytics alerts <file> [--window 5m]
      |      [--alert name:rate=0.5,count=3,window=5m]
      |""".stripMargin

  def main(args: Array[String]): Unit = {
    if (args.length < 2) { println(usage); sys.exit(2) }
    val (command, file) = (args(0), args(1))
    val opts = parseOpts(args.drop(2))
    val text = Using(Source.fromFile(file))(_.mkString).getOrElse {
      System.err.println(s"cannot read file: $file"); sys.exit(1)
    }
    val report = MultiFormatParser.parseText(text)
    val format = opts.getOrElse("format", "text")
    command match {
      case "parse" =>
        format match {
          case "json" => println(JsonExport.eventsJson(report.events))
          case "csv"  => print(CsvExport.eventsCsv(report.events))
          case _ =>
            println(Report.render(report, TimeWindow.parse(opts.getOrElse("window", "5m")).getOrElse(TimeWindow(java.time.Duration.ofMinutes(5)))))
        }
        report.errors.foreach { case (line, raw, err) => System.err.println(s"line $line: $err [$raw]") }
      case "aggregate" =>
        val window = TimeWindow.parse(opts.getOrElse("window", "5m")).getOrElse {
          System.err.println(s"bad window: ${opts("window")}"); sys.exit(1)
        }
        val windows = Aggregation.byWindow(report.events, window)
        format match {
          case "json" => println(JsonExport.windowsJson(windows))
          case "csv"  => print(CsvExport.windowsCsv(windows))
          case _      => println(Report.render(report, window))
        }
      case "alerts" =>
        val rules = opts.get("alert").map(AlertRule.parse).getOrElse(Some(AlertRule.default)) match {
          case Some(rule) => List(rule)
          case None       => System.err.println(s"bad alert spec: ${opts("alert")}"); sys.exit(1)
        }
        print(AlertReport.render(AlertEvaluator.evaluate(rules, report.events)))
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
        format match {
          case "json" => println(JsonExport.eventsJson(matched))
          case "csv"  => print(CsvExport.eventsCsv(matched))
          case _ =>
            println(s"${matched.size} matching events:")
            matched.foreach(e => println(Report.renderEvent(e)))
        }
      case other =>
        System.err.println(s"unknown command: $other"); println(usage); sys.exit(2)
    }
  }

  private def parseOpts(args: Seq[String]): Map[String, String] =
    args.grouped(2).collect { case Seq(k, v) if k.startsWith("--") => k.drop(2) -> v }.toMap
}
