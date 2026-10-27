package loganalytics

import java.time.Instant
import org.scalatest.funsuite.AnyFunSuite

class ParserSuite extends AnyFunSuite {
  test("plain line parses timestamp, level, source, message and k=v fields") {
    val ev = PlainParser.parse("2026-10-27T18:00:00Z INFO [web] User login succeeded user=alice ip=10.0.0.1")
    assert(ev.isRight)
    val e = ev.toOption.get
    assert(e.timestamp == Instant.parse("2026-10-27T18:00:00Z"))
    assert(e.level == LogLevel.Info)
    assert(e.source == Some("web"))
    assert(e.message == "User login succeeded")
    assert(e.field("user") == Some("alice"))
    assert(e.field("ip") == Some("10.0.0.1"))
  }

  test("plain parser accepts space-separated and epoch timestamps") {
    assert(PlainParser.parse("2026-10-27 18:00:00 WARN [db] Slow query").isRight)
    assert(PlainParser.parse("1793126400 ERROR Disk full").isRight)
  }

  test("plain parser rejects unknown levels and bad timestamps") {
    assert(PlainParser.parse("2026-10-27T18:00:00Z NOPE something").isLeft)
    assert(PlainParser.parse("not-a-date INFO something").isLeft)
  }

  test("level aliases parse (WARNING, ERR)") {
    assert(LogLevel.parse("WARNING") == Some(LogLevel.Warn))
    assert(LogLevel.parse("ERR") == Some(LogLevel.Error))
    assert(LogLevel.parse("bogus").isEmpty)
  }

  test("Apache line parses status, maps 5xx to ERROR and 4xx to WARN") {
    val ok = ApacheParser.parse("""127.0.0.1 - alice [27/Oct/2026:18:00:00 +0000] "GET /index HTTP/1.1" 200 1234 "-" "curl/8.0"""")
    assert(ok.isRight)
    assert(ok.toOption.get.level == LogLevel.Info)
    assert(ok.toOption.get.field("status") == Some("200"))
    val err = ApacheParser.parse("""127.0.0.1 - - [27/Oct/2026:18:01:00 +0000] "GET /x HTTP/1.1" 500 0 "-" "curl/8.0"""")
    assert(err.toOption.get.level == LogLevel.Error)
    val warn = ApacheParser.parse("""127.0.0.1 - - [27/Oct/2026:18:02:00 +0000] "GET /x HTTP/1.1" 404 0 "-" "curl/8.0"""")
    assert(warn.toOption.get.level == LogLevel.Warn)
  }

  test("JSON line parses known keys and keeps extras as fields") {
    val ev = JsonParser.parse("""{"timestamp":"2026-10-27T18:00:00Z","level":"ERROR","service":"api","message":"boom","request_id":"abc"}""")
    assert(ev.isRight)
    val e = ev.toOption.get
    assert(e.level == LogLevel.Error && e.source == Some("api") && e.message == "boom")
    assert(e.field("request_id") == Some("abc"))
  }

  test("JSON parser rejects lines missing timestamp or level") {
    assert(JsonParser.parse("""{"level":"INFO","message":"hi"}""").isLeft)
    assert(JsonParser.parse("""{"timestamp":"2026-10-27T18:00:00Z","message":"hi"}""").isLeft)
  }

  test("multi-format parser handles a mixed file and reports failures") {
    val text =
      """2026-10-27T18:00:00Z INFO [web] started
        |127.0.0.1 - - [27/Oct/2026:18:01:00 +0000] "GET / HTTP/1.1" 200 10 "-" "curl"
        |{"timestamp":"2026-10-27T18:02:00Z","level":"ERROR","message":"boom"}
        |this line parses as nothing
        |""".stripMargin
    val report = MultiFormatParser.parseText(text)
    assert(report.successCount == 3)
    assert(report.errorCount == 1)
    assert(report.errors.head._1 == 4)
  }
}
