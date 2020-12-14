package ilegra.flow.step

import ilegra.infrastructure.{FileConfig, Report, RowData}
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import java.io.{File, FileWriter}

case class FileSink(conf: FileConfig) extends RichSinkFunction[Report] {
  override def invoke(value: Report, context: SinkFunction.Context[_]): Unit = {
    val fw = new FileWriter(conf.filePathOut + File.separator + value.fileName, false)
    fw.write(value.format())
    fw.close()
  }
}
