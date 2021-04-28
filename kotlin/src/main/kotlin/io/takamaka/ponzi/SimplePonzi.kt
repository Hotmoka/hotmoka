package io.takamaka.ponzi

import io.takamaka.code.lang.*
import java.math.BigInteger
import io.takamaka.code.lang.Takamaka.require

class SimplePonzi : Contract() {
    private val _10 = BigInteger.valueOf(10L)
    private val _11 = BigInteger.valueOf(11L)
    private var currentInvestor: PayableContract? = null
    private var currentInvestment = BigInteger.ZERO

    @Payable @FromContract(PayableContract::class)
    fun invest(amount: BigInteger) {
        val minimum = currentInvestment.multiply(_11).divide(_10)
        require(amount.compareTo(minimum) >= 0, "You must invest at least $minimum")
        if (currentInvestor != null)
            currentInvestor!!.receive(amount)
        currentInvestor = caller() as PayableContract
        currentInvestment = amount
    }

    @View
    fun getCurrentInvestment(): BigInteger {
      return currentInvestment
    }
}