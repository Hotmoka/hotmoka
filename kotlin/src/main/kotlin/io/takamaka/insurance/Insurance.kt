package io.takamaka.insurance

import io.takamaka.code.lang.*
import io.takamaka.code.util.StorageTreeSet
import io.takamaka.code.lang.Takamaka.require
import io.takamaka.code.util.StorageSet
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId

class Insurance @Payable @FromContract constructor(amount: BigInteger, private val oracle: Contract) : Contract() {
    private val insuredDays: StorageSet<InsuredDay> = StorageTreeSet()
    val MIN: Long = 1_000
    val MAX: Long = 1_000_000_000

    private class InsuredDay(val payer: PayableContract, val amount: Long, date: LocalDate) : Storage() {
        private val day: Int = date.dayOfMonth
        private val month: Int = date.monthValue
        private val year: Int = date.year

        private fun getSeason(date: LocalDate): Season {
            if (date.isAfter(LocalDate.of(date.year, Month.DECEMBER, 21)) &&
                date.isBefore(LocalDate.of(date.year, Month.MARCH, 21))
            )
                return Season.WINTER
            if (date.isAfter(LocalDate.of(date.year, Month.MARCH, 20)) &&
                date.isBefore(LocalDate.of(date.year, Month.JUNE, 21))
            )
                return Season.WINTER
            if (date.isAfter(LocalDate.of(date.year, Month.JUNE, 20)) &&
                date.isBefore(LocalDate.of(date.year, Month.SEPTEMBER, 23))
            )
                return Season.WINTER
            return Season.FALL
        }

        fun isToday(): Boolean {
            return LocalDate.of(year, month, day).equals(today())
        }

        fun isTodayOrBefore(): Boolean {
            return !LocalDate.of(year, month, day).isAfter(today())
        }

        private fun today(): LocalDate {
            val now = Instant.ofEpochMilli(Takamaka.now())
            return LocalDate.ofInstant(now, ZoneId.of("Europe/Rome"))
        }

        fun indemnization(): Long {
            return when (getSeason(LocalDate.of(year, month, day))) {
                Season.WINTER -> amount * 18 / 10 // 180%
                Season.SPRING -> amount * 30 / 10 // 300%
                Season.SUMMER -> amount * 50 / 10 // 500%
                else -> amount * 28 / 10 // 280%
            }
        }
    }

    @Payable @FromContract(PayableContract::class)
    fun buy(amount: Long, day: Int, month: Int, year: Int, duration: Int) {
        require(duration >= 1, "You must insure at least one day")
        require(duration <= 7, "You cannot insure more than a week")
        require(amount >= MIN * duration) { "We insure a single day for at least $MIN units of coin" }
        require(amount <= MAX * duration) { "We insure a single day for up to $MAX units of coin" }

        // If the date is wrong, this generates an exception
        val start = LocalDate.of(year, month, day)
        val payer = caller() as PayableContract
        for (offset in 0..duration)
            insuredDays.add(InsuredDay(payer, amount / duration, start.plusDays(offset.toLong())))
    }

    @FromContract
    fun itRains() {
        require(caller() == oracle, "Only the oracle can call this method")

        // Pay who insured today
        for (day in insuredDays)
            if (day.isToday())
                day.payer.receive(day.indemnization())

        // Clean-up the set of insured days
        for (day in insuredDays)
            if (day.isTodayOrBefore())
                insuredDays.remove(day)
    }

}