package ilegra

import java.util

import ilegra.infrastructure.Report
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.test.util.MiniClusterWithClientResource
import org.scalacheck.Arbitrary
import org.scalatest.{Assertion, BeforeAndAfter, FlatSpec, Matchers, Suite}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Fixtures extends Matchers with BeforeAndAfter  {
  this: Suite =>

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

  def anyRandomOf[T: Arbitrary](): T = Utils.anyRandomOf[T]()

  def anyRandomOf[T: Arbitrary](number: Int): Stream[T] = Utils.anyRandomOf[T](number)

  def mockedFlinkSink[T]() = new CollectSink[T]()

  def asyncWithMockedFlinkSink[T](fn: CollectSink[T] => Future[Assertion]): Future[Assertion] = {
    val sink = mockedFlinkSink[T]()

    val appliedSink = fn(sink)

    appliedSink.onComplete(_ => sink.cleanup())

    appliedSink
  }

}