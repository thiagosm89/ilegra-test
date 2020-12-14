/*
package ilegra

import ilegra.infrastructure.Report
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

case class ApplicationJobTest() extends FlatSpec with Matchers with BeforeAndAfter {
  val flinkCluster = new MiniClusterWithClientResource(new MiniClusterResourceConfiguration.Builder()
    .setNumberSlotsPerTaskManager(1)
    .setNumberTaskManagers(2)
    .build)

  before {
    flinkCluster.before()
  }

  after {
    flinkCluster.after()
  }
}

class CollectSink extends SinkFunction[Report] {
  override def invoke(value: Int): Unit = {
    synchronized {
      print("Collecting" + value)
      CollectSink.values.add(value)
    }
  }
}

object CollectSink {
  val values: util.List[Int] = new util.ArrayList()
}

*/
