package ilegra.flow.infrastructure

import ilegra.infrastructure.FlinkConfig

object ConfigTest {
  val flinkConfig = FlinkConfig(10, 1000, 2000, 500, 1, "file:///tmp/flink/validation/checkpoints", 1000, 10000, 10)
}
