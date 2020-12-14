package ilegra.flow.step

import com.typesafe.scalalogging.StrictLogging
import ilegra.infrastructure.{Customer, Item, RawLine, RowData, Sales, Seller}
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

case class RowSorterFunction() extends ProcessFunction[RawLine, RowData] with StrictLogging {

  override def processElement(rawLine: RawLine,
                              ctx: ProcessFunction[RawLine, RowData]#Context,
                              out: Collector[RowData]): Unit = {

    logger.debug("row sorter called. {}", rawLine)

    val value = rawLine.row.get

    val id = value._1

    val data = id match {

      case Seller.Id => Seller(suid = value._2, name = value._3, salary = value._4.toDouble)

      case Customer.Id => Customer(suid = value._2, name = value._3, businessArea = value._4)

      case Sales.Id => {
        val items = value._3.replace("[", "").replace("]", "").split(",")

        val preparedItems = items
          .map(item => item.split("-"))
          .map(item => Item(id = item(0).toLong, quantity = item(1).toInt, price = item(2).toDouble))

        Sales(saleId = value._2.toLong, items = preparedItems, salesmanName = value._4)
      }
    }

    logger.debug("line outputTag. {}", data)

    out.collect(
      RowData(rawLine.fileName, rawLine.modifiedTime, rawLine.rowTimestamp, data)
    )
  }
}
