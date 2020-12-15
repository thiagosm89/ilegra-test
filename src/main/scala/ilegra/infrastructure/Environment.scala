package ilegra.infrastructure

object Environment {
  // -Ddevelopment.mode=[true|false]
  private val DevelopmentMode = "development.mode"

  private val Environment = "ENVIRONMENT"

  def isDocker = Option(System.getenv(Environment)).map(s => s.toLowerCase.equals("docker")).getOrElse(false)

  // Return value defined in VM option -Ddevelopment.mode=
  def isDevelopmentMode = Option(System.getProperty(DevelopmentMode)).map(s => s.trim.toLowerCase()) match {
    case None => false
    case skip => skip.exists(s => s.equals("true"))
  }
}
