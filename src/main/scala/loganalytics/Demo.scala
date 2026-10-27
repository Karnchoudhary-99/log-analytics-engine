package loganalytics

import java.time.Duration

/** Runnable demo: parses a mixed-format sample and prints the full report. */
object Demo {
  val sample: String =
    """2026-10-27T18:00:00Z INFO [web] Server started port=8080
      |2026-10-27T18:00:10Z INFO [web] User login succeeded user=alice
      |127.0.0.1 - alice [27/Oct/2026:18:01:00 +0000] "GET /dashboard HTTP/1.1" 200 2048 "-" "curl/8.0"
      |{"timestamp":"2026-10-27T18:02:00Z","level":"ERROR","service":"db","message":"connection pool exhausted","pool":"main"}
      |2026-10-27T18:03:00Z WARN [db] Slow query duration_ms=4200
      |127.0.0.1 - - [27/Oct/2026:18:04:00 +0000] "GET /missing HTTP/1.1" 404 0 "-" "curl/8.0"
      |2026-10-27T18:05:00Z ERROR [web] Request failed user=alice status=500
      |""".stripMargin

  def main(args: Array[String]): Unit = {
    val report = MultiFormatParser.parseText(sample)
    println(Report.render(report, TimeWindow(Duration.ofMinutes(5))))
    println("Errors only:")
    LogQuery.errorsOnly(report.events).foreach(e => println("  " + Report.renderEvent(e)))
    println("Alerts (default rule: error rate >= 50% in a 5 min window):")
    print(AlertReport.render(AlertEvaluator.evaluateDefault(report.events)))
    println("First event as JSON:")
    report.events.headOption.foreach(e => println("  " + JsonExport.eventJson(e)))
  }
}
