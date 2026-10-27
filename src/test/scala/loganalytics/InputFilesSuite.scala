package loganalytics

import java.io.{File, PrintWriter}
import org.scalatest.funsuite.AnyFunSuite

class InputFilesSuite extends AnyFunSuite {
  private def tmpFile(contents: String): File = {
    val f = File.createTempFile("la-input", ".log")
    f.deleteOnExit()
    val w = new PrintWriter(f)
    try w.write(contents) finally w.close()
    f
  }

  test("readAll concatenates files and reports the bad path") {
    val a = tmpFile("line one\n")
    val b = tmpFile("line two\n")
    val combined = InputFiles.readAll(List(a.getAbsolutePath, b.getAbsolutePath))
    assert(combined.isRight)
    assert(combined.toOption.get.contains("line one") && combined.toOption.get.contains("line two"))
    assert(InputFiles.readAll(List("/nonexistent/la-missing.log")) == Left("/nonexistent/la-missing.log"))
  }
}
