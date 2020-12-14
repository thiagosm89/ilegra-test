package ilegra.flow.step

import java.time.ZonedDateTime

import ilegra.Fixtures
import ilegra.flow.Flow
import ilegra.flow.infrastructure.ConfigTest
import ilegra.infrastructure.{Customer, RawLine, Report, RowData, Sales, Seller}
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.junit.runner.RunWith
import org.scalatest.AsyncWordSpec
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Millis, Span}

import scala.compat.java8.FutureConverters._

@RunWith(classOf[JUnitRunner])
class RowSorterFunctionSpec extends AsyncWordSpec with Fixtures {

  "RawLine seller must return a Seller object" in asyncWithMockedFlinkSink[RowData] { sink =>
    sink.cleanup()

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    val lineSorterFn: RowSorterFunction = RowSorterFunction()

    val ds: DataStream[RawLine] = env.fromElements(
      RawLine("teste.txt", 8787238949L, ZonedDateTime.now().toInstant.toEpochMilli, Option(("001", "XXXXX", "VENDEDOR", "98.09")))
    )

    val lineSorterDs = ds.process(lineSorterFn)

    lineSorterDs.addSink(sink)

    env.execute()

    val timeout = Timeout(Span(60000, Millis))

    whenReady(future = sink.future().toScala, timeout = timeout) { evs =>
      evs.get(0).data.isInstanceOf[Seller] should equal(true)
    }
  }

  "RawLine customer must return a Customer object" in asyncWithMockedFlinkSink[RowData] { sink =>
    sink.cleanup()

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    val lineSorterFn: RowSorterFunction = RowSorterFunction()

    val ds: DataStream[RawLine] = env.fromElements(
      RawLine("teste.txt", 8787238949L, ZonedDateTime.now().toInstant.toEpochMilli, Option(("002", "XXXXX", "CLIENTE", "RURAL")))
    )

    val lineSorterDs = ds.process(lineSorterFn)

    lineSorterDs.addSink(sink)

    env.execute()

    val timeout = Timeout(Span(60000, Millis))

    whenReady(future = sink.future().toScala, timeout = timeout) { evs =>
      evs.get(0).data.isInstanceOf[Customer] should equal(true)
    }
  }

  "RawLine sale must return a Sales object" in asyncWithMockedFlinkSink[RowData] { sink =>
    sink.cleanup()

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    val lineSorterFn: RowSorterFunction = RowSorterFunction()

    val ds: DataStream[RawLine] = env.fromElements(
      RawLine("teste.txt", 8787238949L, ZonedDateTime.now().toInstant.toEpochMilli, Option(("003", "10", "[2-93-8.9]", "NOME VENDEDOR")))
    )

    val lineSorterDs = ds.process(lineSorterFn)

    lineSorterDs.addSink(sink)

    env.execute()

    val timeout = Timeout(Span(60000, Millis))

    whenReady(future = sink.future().toScala, timeout = timeout) { evs =>
      evs.get(0).data.isInstanceOf[Sales] should equal(true)
    }
  }

}


