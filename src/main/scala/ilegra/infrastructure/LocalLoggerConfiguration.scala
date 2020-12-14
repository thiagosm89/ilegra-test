package ilegra.infrastructure

import com.typesafe.scalalogging.StrictLogging
import org.apache.log4j.{Level, LogManager}

object LocalLoggerConfiguration extends StrictLogging {

  val dummyLogger = LogManager.getLogger("dummy")

  val consoleAppender  = dummyLogger.getAppender("console")

  def configure(config: Config) = configureLoggers(config.loggerConfig.loggers)

  def configureLoggers(loggers: List[Logger]) = loggers.foreach { l =>
    val level = Level.toLevel(l.level, Level.INFO)

    Option(LogManager.getLogger(l.name)).foreach { sl =>

      // make sure we do not spend time setting logger configurations
      // and log a configuration message if logger is already configured
      val alreadyConfigured = sl.getLevel == level &&
        sl.getAdditivity == l.additive &&
        sl.getAppender(consoleAppender.getName) != null

      if (!alreadyConfigured) {
        sl.setLevel(level)
        sl.setAdditivity(l.additive)
        sl.addAppender(consoleAppender)

        logger.info("logger [{}] configured with level [{}] and additivity [{}]", sl.getName, l.level, l.additive)
      }
    }
  }
}
