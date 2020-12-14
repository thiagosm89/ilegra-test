package ilegra

import java.io.{File, PrintWriter}
import java.time.Instant

import ilegra.infrastructure.Customer
import org.scalacheck.{Arbitrary, Gen, ScalacheckShapeless}
import com.fortysevendeg.scalacheck.datetime.jdk8.ArbitraryJdk8._
import org.scalacheck.rng.Seed

import scala.collection.immutable

object GenerateFileMock extends App with ScalacheckShapeless {

  private val customersQuantityToGenerate = 50 //Define quantos clientes serão gerados de forma aleatória
  private val salesQuantityToGenerate = 100 //Define quantas vendas serão registradas
  private val filename = "arquivo_teste.txt" //Define o nome do arquivo a ser gerado

  private val areaGen: Gen[String] = getAreaGen()
  private val itemGen: Gen[(Long, Double)] = getItemGen()

  val pw = new PrintWriter(new File(filename))

  for(salesman: (String, String, Double) <- salesmanList()) {
    pw.write("001ç%sç%sç%s\n".format(salesman._2, salesman._1, salesman._3)) //Geramos os vendedores no arquivo
  }

  for(customer <- getCustomersList(customersQuantityToGenerate)) {
    val area = applyGen[String](areaGen)
    pw.write("002ç%sç%sç%s\n".format(customer._2, customer._1, area)) //Geramos os vendedores no arquivo
  }

  for (saleId <- 0 to salesQuantityToGenerate) {
    val salesmanName = getSalesmanNameRandom()
    pw.write(generateSaleRow(salesmanName, saleId)) //Geramos as linhas de vendas
  }
  pw.close

  def generateSaleRow(salesmanName: String, saleId: Long): String = {
    val item = applyGen[(Long, Double)](itemGen)
    val itemId = item._1
    val itemPrice = item._2
    val itemQuantity = Gen.chooseNum(1,100).apply(Gen.Parameters.default, Seed(Instant.now().toEpochMilli)).get

    val itemStr = "[%s-%s-%s]".format(itemId, itemQuantity, itemPrice)

    "003ç%sç%sç%s\n".format(saleId, itemStr, salesmanName)
  }

  def getSalesmanNameRandom() = {
    val salesman = applyGen[(String, String, Double)](Gen.oneOf(salesmanList()))
    salesman._1
  }

  def getItemGen() = {
    Gen.oneOf(
      (1L, 6.90),
      (2L, 8.90),
      (3L, 5.00),
      (4L, 8.91),
      (5L, 50.00),
      (7L, 19.89),
      (8L, 3.40),
      (9L, 5.67),
      (10L, 8.63),
      (11L, 4.50)
    )
  }

  def getAreaGen() = {
    Gen.oneOf(
      "RURAL",
      "URBANA",
          "AREA 1",
      "AREA 2",
      "AREA 3",
      "AREA 4",
      "AREA 5",
      "AREA 6",
      "AREA 7",
      "AREA 8",
      "AREA 9",
      "AREA 10"
    )
  }

  // ._1 = nome do cliente em maiúsculo
  // ._2 = cpf do cliente
  def getCustomersList(number: Int) = {
    val generate = new Generate()

    val customerNamesSeq: immutable.Seq[String] = Gen.listOfN(number, Gen.alphaUpperStr.suchThat(s => !s.isEmpty))
      .apply(Gen.Parameters.default, Seed(Instant.now().toEpochMilli)).get

    customerNamesSeq
      .map(name => (name.toUpperCase(), generate.cpf(false)))
      .toList
  }

  def salesmanList() = {
    val generate = new Generate()
    Seq(
      ("VENDEDOR 01", generate.cpf(false), 8000.05),
      ("VENDEDOR 02", generate.cpf(false), 7654.78),
      ("VENDEDOR 03", generate.cpf(false), 5243.00),
      ("VENDEDOR 04", generate.cpf(false), 1625.98),
      ("VENDEDOR 05", generate.cpf(false), 8178.67)
    )
  }

  def applyGen[T](gen: Gen[T]) = {
    gen.apply(Gen.Parameters.default, Seed(Instant.now().toEpochMilli)).get
  }
}
