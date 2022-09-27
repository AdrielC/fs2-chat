package exd.skynet.price

import squants.market._
import squants.time._

object PricerTest extends App {

  val exchangeRates = List(
    USD / CAD(1.05),
    USD / MXN(12.50),
    USD / JPY(100)
  )

  implicit val moneyContext = defaultMoneyContext withExchangeRates exchangeRates

  val energyPrice = USD(1.20) / Seconds(1)

  val someMoney = Money(350) // 350 in the default Cur

  val usdMoney: Money = someMoney in USD

  val usdBigDecimal: BigDecimal = someMoney to USD

  val yenCost: Money = (energyPrice * Seconds(5)) in JPY

  val northAmericanSales: Money = (CAD(275) + USD(350) + MXN(290)) in USD

  println(northAmericanSales.amount)
}
