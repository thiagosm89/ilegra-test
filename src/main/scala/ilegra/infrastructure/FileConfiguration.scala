package ilegra.infrastructure

import java.time.ZonedDateTime

import org.apache.flink.api.common.io.{DelimitedInputFormat, FilePathFilter}
import org.apache.flink.api.java.io.TextInputFormat
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.functions.source.{FileProcessingMode, TimestampedFileInputSplit}

case class RawLineInputFormat(p: Path, conf: FileConfig) extends DelimitedInputFormat[RawLine] {
  private val textInputFormat: TextInputFormat = new TextInputFormat(p)
  textInputFormat.setFilesFilter(FilePathFilter.createDefaultFilter)
  textInputFormat.setCharsetName(conf.charsetName)

  override def readRecord(reuse: RawLine, bytes: Array[Byte], offset: Int, numBytes: Int): RawLine = {
    val rowString = textInputFormat.readRecord(null, bytes, offset, numBytes)

    val timestampSplit = this.currentSplit.asInstanceOf[TimestampedFileInputSplit]

    val fileName = timestampSplit.getPath.getName

    val modificationTime = timestampSplit.getModificationTime

    timestampSplit.getLength

    val milli = ZonedDateTime.now().toInstant.toEpochMilli

    if (rowString.isEmpty) {
      RawLine(fileName, modificationTime, milli, None)
    } else {
      //split line by delimiter
      val cols: Array[String] = rowString.split(conf.delimiter)

      RawLine(fileName, modificationTime, milli, Option(Tuple4(cols(0), cols(1), cols(2), cols(3))))
    }
  }
}

case class FileSource(
                       textInputFormat: RawLineInputFormat,
                       filePath: String,
                       fileProcessingMode: FileProcessingMode,
                       charsetName: String = "UTF-8",
                       intervalMill: Long = 3000
                     )

object FileConfiguration {
  def createSource(config: FileConfig): FileSource = {
    val filePath = config.filePath

    val format = RawLineInputFormat(new Path(filePath), config)

    FileSource(
      format,
      filePath,
      FileProcessingMode.PROCESS_CONTINUOUSLY,
      config.charsetName,
      config.intervalMilli
    )
  }
}
