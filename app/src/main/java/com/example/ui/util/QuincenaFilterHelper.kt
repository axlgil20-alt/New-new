package com.example.ui.util

import com.example.data.model.TransactionEntity
import com.example.data.model.affectsBudgetTotals
import com.example.data.model.effectiveAmountBigDecimal
import com.example.data.model.isGastoReal
import com.example.data.model.isGastoSupuesto
import com.example.data.model.isIngresoReal
import com.example.data.model.isIngresoSupuesto
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class BudgetPeriod(val year: Int, val monthIndex: Int, val quincena: Int)

data class QuincenaFilteredData(
    val q1Transactions: List<TransactionEntity>,
    val q2Transactions: List<TransactionEntity>,
    val monthTransactions: List<TransactionEntity>,
    val q1Income: Double,
    val q1Expense: Double,
    val q1PendingExpense: Double,
    val q1Net: Double,
    val q2Income: Double,
    val q2Expense: Double,
    val q2PendingExpense: Double,
    val q2Net: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val totalPendingExpense: Double,
    val totalNet: Double,
    val q1PendingIncome: Double = 0.0,
    val q2PendingIncome: Double = 0.0,
    val totalPendingIncome: Double = 0.0
)

/** Motor de fechas oficial de Quincenas 2.0. Todas las fechas financieras son locales de Guatemala. */
object QuincenaFilterHelper {
    val FINANCIAL_ZONE: ZoneId = ZoneId.of("America/Guatemala")

    /** El tipo del movimiento nunca modifica el ciclo financiero. */
    fun localDate(timeMillis: Long): LocalDate = Instant.ofEpochMilli(timeMillis).atZone(FINANCIAL_ZONE).toLocalDate()

    fun atStartOfDay(date: LocalDate): Long = date.atStartOfDay(FINANCIAL_ZONE).toInstant().toEpochMilli()

    fun atEndOfDay(date: LocalDate): Long = date.plusDays(1).atStartOfDay(FINANCIAL_ZONE).toInstant().toEpochMilli() - 1L

    fun getBudgetPeriod(timeMillis: Long, transactionType: String? = null): BudgetPeriod {
        val date = localDate(timeMillis)
        val month = YearMonth.from(date)
        return when {
            date.dayOfMonth == month.lengthOfMonth() -> {
                val next = month.plusMonths(1)
                BudgetPeriod(next.year, next.monthValue - 1, 1)
            }
            date.dayOfMonth <= 14 -> BudgetPeriod(date.year, date.monthValue - 1, 1)
            else -> BudgetPeriod(date.year, date.monthValue - 1, 2)
        }
    }

    /** Q1: último día del mes anterior al día 14; Q2: día 15 al penúltimo día. */
    fun getBudgetPeriodRange(year: Int, monthIndex: Int, quincena: Int): Pair<Long, Long> {
        val month = YearMonth.of(year, monthIndex + 1)
        val start = if (quincena == 2) month.atDay(15) else month.minusMonths(1).atEndOfMonth()
        val end = if (quincena == 2) month.atEndOfMonth().minusDays(1) else month.atDay(14)
        return atStartOfDay(start) to atEndOfDay(end)
    }

    fun isDateInBudgetMonth(dateMillis: Long, targetYear: Int, targetMonthIndex: Int, transactionType: String? = null): Boolean {
        val period = getBudgetPeriod(dateMillis, transactionType)
        return period.year == targetYear && period.monthIndex == targetMonthIndex
    }

    fun isDateInQuincena(dateMillis: Long, quincenaNumber: Int, transactionType: String? = null): Boolean =
        quincenaNumber <= 0 || getBudgetPeriod(dateMillis, transactionType).quincena == quincenaNumber

    fun filterByQuincena(transactions: List<TransactionEntity>, targetYear: Int, targetMonthIndex: Int): QuincenaFilteredData {
        val q1 = mutableListOf<TransactionEntity>()
        val q2 = mutableListOf<TransactionEntity>()
        val monthList = mutableListOf<TransactionEntity>()
        transactions.filter { it.affectsBudgetTotals }.forEach { tx ->
            val period = getBudgetPeriod(tx.dateMillis, tx.type)
            if (period.year == targetYear && period.monthIndex == targetMonthIndex) {
                monthList += tx
                if (period.quincena == 1) q1 += tx else q2 += tx
            }
        }
        fun income(list: List<TransactionEntity>) = list.filter { it.isIngresoReal }.fold(BigDecimal.ZERO) { a, tx -> a.add(tx.effectiveAmountBigDecimal) }
        fun pendingIncome(list: List<TransactionEntity>) = list.filter { it.isIngresoSupuesto }.fold(BigDecimal.ZERO) { a, tx -> a.add(tx.effectiveAmountBigDecimal) }
        fun expense(list: List<TransactionEntity>) = list.filter { it.isGastoReal }.fold(BigDecimal.ZERO) { a, tx -> a.add(tx.effectiveAmountBigDecimal) }
        fun pending(list: List<TransactionEntity>) = list.filter { it.isGastoSupuesto && it.tentativeDecision != "DISCARDED" }.fold(BigDecimal.ZERO) { a, tx -> a.add(tx.effectiveAmountBigDecimal) }
        val q1i = income(q1); val q1e = expense(q1); val q1p = pending(q1); val q1pi = pendingIncome(q1)
        val q2i = income(q2); val q2e = expense(q2); val q2p = pending(q2); val q2pi = pendingIncome(q2)
        val ti = income(monthList); val te = expense(monthList); val tp = pending(monthList); val tpi = pendingIncome(monthList)
        return QuincenaFilteredData(q1, q2, monthList, q1i.toDouble(), q1e.toDouble(), q1p.toDouble(), q1i.subtract(q1e).toDouble(), q2i.toDouble(), q2e.toDouble(), q2p.toDouble(), q2i.subtract(q2e).toDouble(), ti.toDouble(), te.toDouble(), tp.toDouble(), ti.subtract(te).toDouble(), q1pi.toDouble(), q2pi.toDouble(), tpi.toDouble())
    }

    fun isSameCalendarDay(time1: Long, time2: Long): Boolean = localDate(time1) == localDate(time2)

    fun getDayRange(timeMillis: Long): Pair<Long, Long> {
        val day = localDate(timeMillis)
        return atStartOfDay(day) to atEndOfDay(day)
    }

    fun getWeekRange(timeMillis: Long): Pair<Long, Long> {
        val date = localDate(timeMillis)
        val start = date.minusDays((date.dayOfWeek.value - 1).toLong())
        return atStartOfDay(start) to atEndOfDay(start.plusDays(6))
    }

    fun getYearRange(year: Int): Pair<Long, Long> = atStartOfDay(LocalDate.of(year, 1, 1)) to atEndOfDay(LocalDate.of(year, 12, 31))

    fun findDuplicateTransactionIds(transactions: List<TransactionEntity>): Set<Long> {
        val ids = mutableSetOf<Long>()
        transactions.forEachIndexed { i, a ->
            if (a.isSuspectedDuplicate) ids += a.id
            transactions.drop(i + 1).forEach { b ->
                if (a.transferGroupId != null && a.transferGroupId == b.transferGroupId) return@forEach
                if (a.accountId != null && b.accountId != null && a.accountId != b.accountId) return@forEach
                if (a.type.equals(b.type, true) && a.entryKind.equals(b.entryKind, true) && a.effectiveAmountMinor == b.effectiveAmountMinor && isSameCalendarDay(a.dateMillis, b.dateMillis) && a.title.trim().equals(b.title.trim(), true)) ids += a.id.also { ids += b.id }
            }
        }
        return ids
    }

    fun calculateQuincenaRollover(transactions: List<TransactionEntity>, targetYear: Int, targetMonthIndex: Int, targetQuincena: Int): Double {
        if (transactions.isEmpty()) return 0.0
        fun key(year: Int, month: Int, q: Int) = year * 24 + month * 2 + q - 1
        val target = key(targetYear, targetMonthIndex, if (targetQuincena in 1..2) targetQuincena else 1)
        val inc = mutableMapOf<Int, BigDecimal>(); val exp = mutableMapOf<Int, BigDecimal>()
        transactions.filter { it.affectsBudgetTotals }.forEach { tx ->
            val p = getBudgetPeriod(tx.dateMillis, tx.type); val k = key(p.year, p.monthIndex, p.quincena)
            if (tx.isIngresoReal) inc[k] = (inc[k] ?: BigDecimal.ZERO).add(tx.effectiveAmountBigDecimal)
            else if (tx.isGastoReal) exp[k] = (exp[k] ?: BigDecimal.ZERO).add(tx.effectiveAmountBigDecimal)
        }
        val first = (inc.keys + exp.keys).filter { it < target }.minOrNull() ?: return 0.0
        var rollover = BigDecimal.ZERO
        for (k in first until target) {
            val closing = (inc[k] ?: BigDecimal.ZERO).add(rollover).subtract(exp[k] ?: BigDecimal.ZERO)
            rollover = if (closing.signum() > 0) closing else BigDecimal.ZERO
        }
        return rollover.toDouble()
    }
}
