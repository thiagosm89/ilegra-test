package ilegra.flow.step

package io.sicredi.accounting.validation

import java.time.ZonedDateTime
import java.util

import ilegra.infrastructure.{Customer, Item, Report, RowData, Sales, Seller}
import ilegra.{CollectSink, Fixtures}
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.windowing.time.Time
import org.junit.runner.RunWith
import org.scalatest.AsyncWordSpec
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Millis, Span}

import scala.compat.java8.FutureConverters._

@RunWith(classOf[JUnitRunner])
class ReportFunctionSpec extends AsyncWordSpec with Fixtures {

  "seller test" should {
    "when we have different salespeople, then both should be counted" in asyncWithMockedFlinkSink[Report] { sink: CollectSink[Report] =>
      val datas: Seq[RowData] = Seq(
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Seller("XXX", "NAME", 10.00)),
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Seller("YYY", "NAME 01", 20.00))
      )

      pipeline(datas, sink)

      val timeout = Timeout(Span(60000, Millis))

      whenReady(future = sink.future().toScala, timeout = timeout) { evs: util.List[Report] =>
        val report = evs.get(0)
        report.countSellers should equal(2)
        report.countCustomers should equal(0)
        report.worstSellerName should equal(None)
        report.mostExpansiveSaleId should equal(None)
      }
    }

    "when the same seller is repeated, then it should be counted only once" in asyncWithMockedFlinkSink[Report] { sink: CollectSink[Report] =>
      val datas: Seq[RowData] = Seq(
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Seller("XXX", "NAME", 10.00)),
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Seller("XXX", "NAME", 10.00))
      )

      pipeline(datas, sink)

      val timeout = Timeout(Span(60000, Millis))

      whenReady(future = sink.future().toScala, timeout = timeout) { evs: util.List[Report] =>
        val report = evs.get(0)
        report.countSellers should equal(1)
        report.countCustomers should equal(0)
        report.worstSellerName should equal(None)
        report.mostExpansiveSaleId should equal(None)
      }
    }
  }

  "customer test" should {
    "when different customers are found, then we count each one" in asyncWithMockedFlinkSink[Report] { sink: CollectSink[Report] =>
      val datas: Seq[RowData] = Seq(
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Customer("XXX", "NAME", "RURAL")),
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Customer("YYY", "NAME 01", "URBANA"))
      )

      pipeline(datas, sink)

      val timeout = Timeout(Span(60000, Millis))

      whenReady(future = sink.future().toScala, timeout = timeout) { evs: util.List[Report] =>
        val report = evs.get(0)
        report.countSellers should equal(0)
        report.countCustomers should equal(2)
        report.worstSellerName should equal(None)
        report.mostExpansiveSaleId should equal(None)
      }
    }

    "when a client repeats itself on the file, we should only call it once" in asyncWithMockedFlinkSink[Report] { sink: CollectSink[Report] =>
      val datas: Seq[RowData] = Seq(
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Customer("XXX", "NAME", "RURAL")),
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Customer("XXX", "NAME", "RURAL"))
      )

      pipeline(datas, sink)

      val timeout = Timeout(Span(60000, Millis))

      whenReady(future = sink.future().toScala, timeout = timeout) { evs: util.List[Report] =>
        val report = evs.get(0)
        report.countSellers should equal(0)
        report.countCustomers should equal(1)
        report.worstSellerName should equal(None)
        report.mostExpansiveSaleId should equal(None)
      }
    }
  }

  "sale test" should {
    "when the worst seller did not make any sales, then returns his name and the value of the best sale" in asyncWithMockedFlinkSink[Report] { sink: CollectSink[Report] =>
      val datas: Seq[RowData] = Seq(
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Seller("XXX", "VENDEDOR 01", 100.00)),
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Seller("YYY", "VENDEDOR 02", 99.00)),
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Sales(10, Seq(Item(1, 10, 10.90)), "VENDEDOR 02"))
      )

      pipeline(datas, sink)

      val timeout = Timeout(Span(60000, Millis))

      whenReady(future = sink.future().toScala, timeout = timeout) { evs: util.List[Report] =>
        val report = evs.get(0)
        report.countSellers should equal(2)
        report.countCustomers should equal(0)
        report.worstSellerName.get should equal("VENDEDOR 01")
        report.mostExpansiveSaleId.get should equal(10)
      }
    }

    "when all salespeople have made a sale, it returns the value of the best sale and the name of the worst seller" in asyncWithMockedFlinkSink[Report] { sink: CollectSink[Report] =>
      val datas: Seq[RowData] = Seq(
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Seller("XXX", "VENDEDOR 01", 100.00)),
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Seller("YYY", "VENDEDOR 02", 99.00)),
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Sales(10, Seq(Item(1, 10, 10.90)), "VENDEDOR 02")),
        RowData("teste.txt", 87498234987L, ZonedDateTime.now().toInstant.toEpochMilli, Sales(11, Seq(Item(1, 10, 50.09)), "VENDEDOR 01"))
      )

      pipeline(datas, sink)

      val timeout = Timeout(Span(60000, Millis))

      whenReady(future = sink.future().toScala, timeout = timeout) { evs: util.List[Report] =>
        val report = evs.get(0)
        report.countSellers should equal(2)
        report.countCustomers should equal(0)
        report.worstSellerName.get should equal("VENDEDOR 02")
        report.mostExpansiveSaleId.get should equal(11)
      }
    }
  }

  def pipeline(datas: Seq[RowData], sink: CollectSink[Report]): Unit = {
    sink.cleanup()

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    val reportFn: ReportFunction = ReportFunction()

    val ds: DataStream[RowData] = env.fromCollection(datas)

    val dsResponse = ds
      .assignAscendingTimestamps(e => System.currentTimeMillis())
      .keyBy(k => k.fileName)
      .timeWindow(Time.seconds(10))
      .process(reportFn)

    dsResponse.addSink(sink)
    env.execute()
  }

}


