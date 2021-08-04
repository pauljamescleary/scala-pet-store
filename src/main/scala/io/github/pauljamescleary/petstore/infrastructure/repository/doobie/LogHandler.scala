package io.github.pauljamescleary.petstore.infrastructure.repository.doobie

import doobie.util.log
import doobie.util.log.{ExecFailure, ProcessingFailure, Success}
import org.slf4j.LoggerFactory

object LogHandler {
  val logger = LoggerFactory.getLogger(getClass)
  // TODO: Replace with Log4cats based approach once available as discussed in https://github.com/pauljamescleary/scala-pet-store/issues/56
  val petStoreLogHandler = log.LogHandler {
    case Success(s, a, e1, e2) =>
      logger.info(s"""Successful Statement Execution:
        |
        |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
        |
        | arguments = [${a.mkString(", ")}]
        |   elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (${(e1 + e2).toMillis} ms total)
      """.stripMargin)

    case ProcessingFailure(s, a, e1, e2, t) =>
      logger.error(s"""Failed Resultset Processing:
        |
        |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
        |
        | arguments = [${a.mkString(", ")}]
        |   elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (failed) (${(e1 + e2).toMillis} ms total)
        |   failure = ${t.getMessage}
      """.stripMargin)

    case ExecFailure(s, a, e1, t) =>
      logger.error(s"""Failed Statement Execution:
        |
        |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
        |
        | arguments = [${a.mkString(", ")}]
        |   elapsed = ${e1.toMillis} ms exec (failed)
        |   failure = ${t.getMessage}
      """.stripMargin)

  }
}
