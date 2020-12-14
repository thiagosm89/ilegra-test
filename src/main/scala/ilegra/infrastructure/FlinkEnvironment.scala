package ilegra.infrastructure

import java.time.Duration

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, Watermark, WatermarkStrategy}
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

object FlinkEnvironment {

  def environment(flinkConfig: FlinkConfig): StreamExecutionEnvironment = {

    val env: StreamExecutionEnvironment = if (ApplicationVMOption.isDevelopmentMode)
      StreamExecutionEnvironment.createLocalEnvironmentWithWebUI()
    else
      StreamExecutionEnvironment.getExecutionEnvironment

    if(ApplicationVMOption.isDevelopmentMode) {
      env.setStateBackend(new FsStateBackend(flinkConfig.filePathCheckpoint))
    }

    val restartStrategy = RestartStrategies.fixedDelayRestart(
      flinkConfig.restartAttempts,
      flinkConfig.delayBetweenAttempts
    )

    env.getConfig.setAutoWatermarkInterval(3000L)
    env.getConfig.setRestartStrategy(restartStrategy)
    env.enableCheckpointing(flinkConfig.checkpointInterval)
    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(flinkConfig.maxConcurrentCheckpoints)
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(flinkConfig.minPauseBetweenCheckpoints)
    env.getCheckpointConfig.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)
    env.getCheckpointConfig.setCheckpointTimeout(flinkConfig.checkpointTimeout)

    env
  }
}
