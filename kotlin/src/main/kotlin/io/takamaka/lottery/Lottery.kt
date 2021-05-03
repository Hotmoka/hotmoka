package io.takamaka.lottery

import io.takamaka.code.lang.*
import io.takamaka.code.lang.Takamaka.now
import io.takamaka.code.util.StorageTreeArray
import io.takamaka.code.lang.Takamaka.require
import java.math.BigInteger


class Lottery: Contract {
    private val N: Int
    private val creator: PayableContract
    private val holders: StorageTreeArray<PayableContract>
    private var numberOfTicketsSold = 0

    @FromContract constructor (N: Int) {
        require(N >= 2, "N must be >= 2!")
        this.N = N
        creator = caller() as PayableContract
        holders = StorageTreeArray(N)
    }

    @Payable @FromContract(PayableContract::class)
    fun buy(money: Int) {
        require(numberOfTicketsSold < N, "This lottery is sold out.")
        require(money >= 100_000 + 100_000 * numberOfTicketsSold / (N - 1), "Money isn't enough.")
        holders.set(numberOfTicketsSold, caller() as PayableContract)
        numberOfTicketsSold++

        if (numberOfTicketsSold == N) {
            val winner = holders.get((now() % N).toInt())
            winner.receive(balance().multiply(BigInteger.valueOf(90)).divide(BigInteger.valueOf(100)))
            creator.receive(balance())
        }
    }

}