package ilegra.flow.step

import com.typesafe.scalalogging.StrictLogging
import ilegra.infrastructure.{Customer, FlinkConfig, Report, RowData, Sales, Seller}
import org.apache.flink.api.common.state.{MapState, MapStateDescriptor, StateTtlConfig, ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.time.Time
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.collection.JavaConverters._

object ReportFunction {
  val customerSuidDescriptor = new MapStateDescriptor[String, Char]("customers-suid", createTypeInformation[String], createTypeInformation[Char])

  val sellersNameDescriptor = new MapStateDescriptor[String, Double]("sellers-name", createTypeInformation[String], createTypeInformation[Double])

  val modificationTimeDescriptor = new ValueStateDescriptor[Long]("modification-time", createTypeInformation[Long])

  val mostExpensiveSaleDescriptor = new ValueStateDescriptor[(Double, Long)]("most-expensive-sale", createTypeInformation[(Double, Long)])

  val worstSellerDescriptor = new ValueStateDescriptor[String]("worst-seller", createTypeInformation[String])
}

case class ReportFunction() extends ProcessWindowFunction[RowData, Report, String, TimeWindow] with StrictLogging {

  private val ttlStates = StateTtlConfig
    .newBuilder(Time.minutes(20))
    .setUpdateType(StateTtlConfig.UpdateType.OnReadAndWrite)
    .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
    .build()

  private type SaleBySeller = (String, Double)

  private var countCustomerState: MapState[String, Char] = _
  ReportFunction.customerSuidDescriptor.enableTimeToLive(ttlStates)

  private var sellerState: MapState[String, Double] = _
  ReportFunction.sellersNameDescriptor.enableTimeToLive(ttlStates)

  private var longerModificationTimeState: ValueState[Long] = _
  ReportFunction.modificationTimeDescriptor.enableTimeToLive(ttlStates)

  private var mostExpensiveSaleState: ValueState[(Double, Long)] = _
  ReportFunction.mostExpensiveSaleDescriptor.enableTimeToLive(ttlStates)

  private var worstSellerState: ValueState[String] = _
  ReportFunction.worstSellerDescriptor.enableTimeToLive(ttlStates)

  private val worstSellerReduce = (s1: SaleBySeller, s2: SaleBySeller) => if (s1._2 < s2._2) s1 else s2


  override def open(parameters: Configuration): Unit = {
    countCustomerState = getRuntimeContext.getMapState(ReportFunction.customerSuidDescriptor)

    sellerState = getRuntimeContext.getMapState(ReportFunction.sellersNameDescriptor)

    longerModificationTimeState = getRuntimeContext.getState(ReportFunction.modificationTimeDescriptor)

    mostExpensiveSaleState = getRuntimeContext.getState(ReportFunction.mostExpensiveSaleDescriptor)

    worstSellerState = getRuntimeContext.getState(ReportFunction.worstSellerDescriptor)
  }

  override def process(key: String, context: Context, elements: Iterable[RowData], out: Collector[Report]): Unit = {

    logger.debug("elements size: {}")

    val report = elements
      .toStream
      .map(element => validatedRow(element))
      .filter(t => t._1)
      .map(e => e._2)
      .map(element => generateReport(element))
      .reduce((_, r2) => r2)

    out.collect(report)
  }

  private def validatedRow(element: RowData): (Boolean, RowData) = {
    val longerModificationTime = longerModificationTimeState.value()

    val timeRow = element.modifiedTime

    val lastModificationIsLess = longerModificationTime < timeRow

    if (timeRow == longerModificationTime ^ lastModificationIsLess) {
      rearrangeStates(element, lastModificationIsLess)
      (true, element)
    } else {
      (false, element)
    }
  }

  private def rearrangeStates(element: RowData, shouldRearrange: Boolean): Unit = {
    if (shouldRearrange) {
      if (longerModificationTimeState.value() > 0)
        logger.info("state reorganization occurred because the {} file was changed.", element.fileName)
      else
        logger.info("state reorganization occurred because the first line of the {} file was read..", element.fileName)

      clearBusinessStates()
      longerModificationTimeState.update(element.modifiedTime)
    }
  }

  // Limpamos as
  private def clearBusinessStates(): Unit = {
    countCustomerState.clear()
    sellerState.clear()
    mostExpensiveSaleState.clear()
    worstSellerState.clear()
  }

  private def generateReport(element: RowData) = {

    val salesRep = salesReport(element)

    Report(
      element.fileName,
      countCustomers(element),
      countSellers(element),
      salesRep._1,
      salesRep._2
    )

  }

  def countCustomers(file: RowData): Int = {
    logger.debug("count customers. {}", file)

    file.data match {
      case Customer(suid, _, _) =>
        if (!countCustomerState.contains(suid))
          countCustomerState.put(suid, 'S')
      case _ =>
    }

    countCustomerState.keys().asScala.size
  }

  def countSellers(file: RowData): Int = {
    logger.debug("count sellers. {}", file)

    file.data match {
      case Seller(_, name, _) =>
        if (!sellerState.contains(name))
          sellerState.put(name, 0.00)

      case _ =>
    }

    sellerState.keys().asScala.size
  }

  def salesReport(rowData: RowData): (Option[Long], Option[String]) = {
    logger.debug("worst seller. {}", rowData)

    rowData.data match {
      case Sales(saleId, items, salesmanName) =>
        val totalPrice = items
          .map(item => item.quantity * item.price)
          .sum

        mostExpensiveSaleReport(totalPrice, saleId)
        worstSellerReport(salesmanName, totalPrice)
      case _ =>
    }

    if (mostExpensiveSaleState.value() != null)
      (Option(mostExpensiveSaleState.value()._2), Option(worstSellerState.value()))
    else
      (None, None)
  }

  def mostExpensiveSaleReport(totalPrice: Double, saleId: Long): Unit = {
    val mostExpensiveSale = mostExpensiveSaleState.value()
    if (mostExpensiveSale == null || totalPrice > mostExpensiveSale._1) {
      mostExpensiveSaleState.update((totalPrice, saleId))
    }
  }

  def worstSellerReport(name: String, totalPrice: Double): Unit = {
    val value = if (sellerState.contains(name))
      sellerState.get(name) + totalPrice
    else
      totalPrice

    sellerState.put(name, value)

    val tuples: Iterable[SaleBySeller] = sellerState.keys().asScala
      .zip(sellerState.values().asScala)

    val worstSellerName = tuples.reduce(worstSellerReduce)._1

    worstSellerState.update(worstSellerName)
  }
}
