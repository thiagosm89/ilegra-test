package ilegra.infrastructure

import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._
import scala.util.Try
import scala.util.control.{Exception => ControlException}

case class FlinkConfig(
                        restartAttempts: Int,
                        delayBetweenAttempts: Int,
                        checkpointInterval: Int,
                        minPauseBetweenCheckpoints: Int,
                        maxConcurrentCheckpoints: Int,
                        filePathCheckpoint: String,
                        checkpointTimeout: Long,
                        maxRowsInWindow: Int,
                        timeInSecondsWindow: Int
                      )

case class FileConfig(filePath: String, filePathOut: String, charsetName: String, delimiter: Char, intervalMilli: Long)

case class ApplicationConfig(name: String)

case class Logger(name: String, level: String, additive: Boolean = true)

case class LoggerConfig(loggers: List[Logger])

case class Config(
                   flinkConfig: FlinkConfig,
                   loggerConfig: LoggerConfig,
                   fileConfig: FileConfig
                 )

trait RetryOps {
  def retry[T](fn: => T, n: Int = 3): Try[T] = {
    ControlException.catching(classOf[Throwable])
      .withTry(fn)
      .recoverWith({ case _ if n > 1 => retry(fn, n - 1) })
  }
}

trait ConfigurationConversions {

  def asConfiguration(config: TypesafeConfig) = Config(
    flinkConfig = asFlinkConfig(config),
    loggerConfig = asLoggerConfig(config),
    fileConfig = asFileConfig(config)
  )

  def asLoggers(config: TypesafeConfig) =
    config.getObjectList("loggers").asScala.map(c =>
      Logger(
        name = c.toConfig.getString("name"),
        level = c.toConfig.getString("level"),
        additive = if (c.toConfig.hasPath("additive")) c.toConfig.getBoolean("additive") else true
      )
    )
      .toList

  def asLoggerConfig(config: TypesafeConfig) = LoggerConfig(
    loggers = asLoggers(config)
  )

  def asFlinkConfig(config: TypesafeConfig) = FlinkConfig(
    restartAttempts = config.getInt("flink.restartAttempts"),
    delayBetweenAttempts = config.getInt("flink.delayBetweenAttempts"),
    checkpointInterval = config.getInt("flink.checkpointInterval"),
    minPauseBetweenCheckpoints = config.getInt("flink.minPauseBetweenCheckpoints"),
    maxConcurrentCheckpoints = config.getInt("flink.maxConcurrentCheckpoints"),
    filePathCheckpoint = config.getString("flink.filePathCheckpoint"),
    checkpointTimeout = config.getLong("flink.checkpointTimeout"),
    maxRowsInWindow = config.getInt("flink.maxRowsInWindow"),
    timeInSecondsWindow = config.getInt("flink.timeInSecondsWindow")
  )

  def asFileConfig(config: TypesafeConfig) = FileConfig(
    filePath = config.getString("fileConfig.filePath"),
    filePathOut = config.getString("fileConfig.filePathOut"),
    delimiter = config.getString("fileConfig.delimiter").charAt(0),
    charsetName = config.getString("fileConfig.charsetName"),
    intervalMilli = config.getLong("fileConfig.intervalMilli")
  )
}

trait ConfigurationOps {
  self: ConfigurationConversions =>

  def load(): TypesafeConfig = loadConf().resolve()

  private def loadConf(): TypesafeConfig = ConfigFactory.load(getApplicationConf)

  private def getApplicationConf = {
    if (Environment.isDocker) "application-docker.conf" else "application.conf"
  }
}

object Configuration extends ConfigurationOps
  with ConfigurationConversions
  with StrictLogging
  with RetryOps {

  def fetchAll(): Config = {
    val application = load()

    val cfg = asConfiguration(application)

    logger.info("fetched and parsed configuration {}", cfg)

    cfg
  }
}