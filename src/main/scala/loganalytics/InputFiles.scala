package loganalytics

import scala.io.Source
import scala.util.Using

/** Multi-file input: several log files read as one stream. */
object InputFiles {

  /** Concatenated contents of all paths, or the first unreadable path. */
  def readAll(paths: Seq[String]): Either[String, String] = {
    val parts = paths.map { p =>
      Using(Source.fromFile(p))(_.mkString) match {
        case scala.util.Success(text) => Right(text)
        case scala.util.Failure(_)    => Left(p)
      }
    }
    parts.find(_.isLeft) match {
      case Some(Left(bad)) => Left(bad)
      case _               => Right(parts.collect { case Right(t) => t }.mkString("\n"))
    }
  }
}
