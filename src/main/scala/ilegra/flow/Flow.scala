package ilegra.flow

import java.time.Duration

import com.typesafe.scalalogging.StrictLogging
import ilegra.flow.step.{FileSink, ReportFunction, RowSorterFunction}
import ilegra.infrastructure.{Config, FileConfiguration, FileSource, FlinkEnvironment, _}
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time

object Flow extends StrictLogging {
  def execute(config: Config)(
    env: StreamExecutionEnvironment = FlinkEnvironment.environment(config.flinkConfig),
    source: FileSource = FileConfiguration.createSource(config.fileConfig),
    lineSorterFn: RowSorterFunction = RowSorterFunction(),
    reportFn: ReportFunction = ReportFunction(),
    fileSink: FileSink = FileSink(config.fileConfig)
  ) = {
    val sourceDs = env
      .readFile(
        source.textInputFormat,
        source.filePath,
        source.fileProcessingMode,
        source.intervalMill
      )

    val classifiedDs: DataStream[RowData] = sourceDs
      .filter(r => r.row.isDefined)
      .process(lineSorterFn)

    val reportDs: DataStream[Report] = reportPipeline(classifiedDs, reportFn, config.flinkConfig)

    reportDs.addSink(fileSink)

    env.execute("listener-files")
  }

  def reportPipeline(dataStream: DataStream[RowData], reportFn: ReportFunction, config: FlinkConfig): DataStream[Report] = {
    val watermark = WatermarkStrategy
      .forBoundedOutOfOrderness[RowData](Duration.ofSeconds(config.timeInSecondsWindow))
      .withTimestampAssigner(new SerializableTimestampAssigner[RowData] {
        override def extractTimestamp(element: RowData, recordTimestamp: Long): Long = element.rowTimestamp
      })

    dataStream
      .assignTimestampsAndWatermarks(watermark)
      .keyBy(f => f.fileName)
      .window(TumblingEventTimeWindows.of(Time.seconds(config.timeInSecondsWindow)))
      .trigger(CustomTrigger.of(config.maxRowsInWindow))
      .process(reportFn)
  }
}