package ilegra

import com.typesafe.scalalogging.StrictLogging
import ilegra.flow.Flow
import ilegra.infrastructure.{Config, Configuration, LocalLoggerConfiguration}

object ApplicationJob extends App with StrictLogging {
  val config: Config = Configuration.fetchAll()

  LocalLoggerConfiguration.configure(config)

  Flow.execute(config)()
}
