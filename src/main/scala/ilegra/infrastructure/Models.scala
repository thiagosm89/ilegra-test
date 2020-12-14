package ilegra.infrastructure

case class RawLine(fileName: String,
                   modifiedTime: Long,
                   rowTimestamp: Long,
                   row: Option[Row])

case class RowData(fileName: String, modifiedTime: Long, rowTimestamp: Long, data: Data)

sealed trait Data {
  def id(): String
}

object Seller {
  lazy val Id: String = "001"
}

case class Seller(suid: String, name: String, salary: Double) extends Data {
  override def id(): String = Seller.Id
}

object Customer {
  lazy val Id: String = "002"
}

case class Customer(suid: String, name: String, businessArea: String) extends Data {
  override def id(): String = Customer.Id
}

object Sales {
  lazy val Id: String = "003"
}

case class Sales(saleId: Long, items: Seq[Item], salesmanName: String) extends Data {
  override def id(): String = Sales.Id
}

case class Item(id: Long, quantity: Integer, price: Double)

case class Report(fileName: String,
                  countCustomers: Long,
                  countSellers: Long,
                  mostExpansiveSaleId: Option[Long],
                  worstSellerName: Option[String]) {
  def format(): String = {
    """Quantidade de clientes no arquivo de entrada: %s
       Quantidade de vendedores no arquivo de entrada: %s
       Id da venda mais cara: %s
       O pior vendedor: %s""".stripMargin
      .format(countCustomers, countSellers, mostExpansiveSaleId.getOrElse(0L), worstSellerName.getOrElse(""))
  }
}
