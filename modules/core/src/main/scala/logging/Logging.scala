package logging

import com.typesafe.scalalogging.Logger

trait Logging {

  protected lazy val loggerName: String = getClass.getName

  protected val log: Logger = Logger(loggerName)

}
