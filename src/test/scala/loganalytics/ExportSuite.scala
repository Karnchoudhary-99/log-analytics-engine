package loganalytics

import java.time.Instant
import org.scalatest.funsuite.AnyFunSuite

class ExportSuite extends AnyFunSuite {
  private val event = LogEvent(Instant.parse("2026-10-27T18:00:00Z"), LogLevel.Error,
    Some("web"), "say \"hi\", now", Map("user" -> "alice"))

  test("JSON export escapes quotes and keeps fields") {
    val json = JsonExport.eventJson(event)
    assert(json.contains("\\\"hi\\\""))
    assert(json.contains("\"level\":\"ERROR\""))
    assert(json.contains("\"user\":\"alice\""))
    assert(JsonExport.eventsJson(List(event)).startsWith("[") && JsonExport.eventsJson(Nil) == "[]")
  }

  test("JSON window export carries totals and error rate") {
    val w = WindowSummary(Instant.parse("2026-10-27T18:00:00Z"), 4,
      Map(LogLevel.Error -> 1, LogLevel.Info -> 3), Map("web" -> 4))
    val json = JsonExport.windowJson(w)
    assert(json.contains("\"total\":4") && json.contains("\"errors\":1"))
    assert(json.contains("\"errorRate\":0.2500"))
  }

  test("CSV export quotes cells containing commas or quotes") {
    val csv = CsvExport.eventsCsv(List(event))
    assert(csv.startsWith("timestamp,level,source,message\n"))
    assert(csv.contains("\"say \"\"hi\"\", now\""))
    val windows = CsvExport.windowsCsv(List(
      WindowSummary(Instant.parse("2026-10-27T18:00:00Z"), 4,
        Map(LogLevel.Error -> 1), Map("web" -> 4))))
    assert(windows.startsWith("windowStart,total,errors,errorRate\n"))
    assert(windows.contains(",4,1,0.2500\n"))
  }
}
