package ilegra.infrastructure

object ApplicationVMOption {

  // [name_file]=application-dev.conf
  // -Dfile.config=[name_file]
  private val FileConfig = "file.config"

  // -Ddevelopment.mode=[true|false]
  private val DevelopmentMode = "development.mode"

  // Return value defined in VM Option -Dfile.config=
  def getFileConfig = Option(System.getProperty(FileConfig)).map(s => s.trim.toLowerCase())

  // Return value defined in VM option -Ddevelopment.mode=
  def isDevelopmentMode = Option(System.getProperty(DevelopmentMode)).map(s => s.trim.toLowerCase()) match {
    case None => false
    case skip => skip.exists(s => s.equals("true"))
  }
}
