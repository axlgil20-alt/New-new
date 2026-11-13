package com.example.data.service

import com.example.data.model.FinancialAccountEntity
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionEntryKind
import com.example.data.model.TransactionType
import com.example.data.model.effectiveAmountMinor
import com.example.data.model.isGastoSupuesto
import java.math.BigDecimal
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class CreditCardCycleWindow(
    val lastClosedCutoffMillis: Long,
    val currentCycleStartMillis: Long,
    val nextCutoffMillis: Long,
    val paymentDueDateMillis: Long,
    val lastStatementCycleKey: String,
    val currentStatementCycleKey: String
)

data class CreditCardStatementSummary(
    val accountId: Long,
    val cycleWindow: CreditCardCycleWindow,
    /** Saldo facturado pendiente del último corte cerrado (descontando abonos ya realizados después del corte). */
    val closedStatementRemainingDueMinor: Long,
    /** Consumos/cuotas cargados en el ciclo abierto actual (se pagarán en el siguiente corte). */
    val currentCycleNewChargesMinor: Long,
    /** Total de abonos/pagos aplicados después del último corte cerrado. */
    val paymentsAfterLastCutoffMinor: Long,
    /** Verdadero si aún queda saldo exigible del corte cerrado anterior. */
    val hasStatementBalanceDue: Boolean
) {
    val closedStatementRemainingDue: BigDecimal get() = MoneyMinorUnits.toBigDecimal(closedStatementRemainingDueMinor)
    val currentCycleNewCharges: BigDecimal get() = MoneyMinorUnits.toBigDecimal(currentCycleNewChargesMinor)
    val paymentsAfterLastCutoff: BigDecimal get() = MoneyMinorUnits.toBigDecimal(paymentsAfterLastCutoffMinor)
}

/**
 * Motor de ciclos de corte y fechas límite de pago para tarjetas de crédito.
 * Maneja meses de 28, 29, 30 y 31 días sin desbordar hacia el mes siguiente.
 */
object CreditCardCycleCalculator {

    fun resolveDayEndMillis(
        year: Int,
        monthIndex: Int,
        targetDay: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, targetDay.coerceIn(1, maxDay))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun resolveDayNoonMillis(
        year: Int,
        monthIndex: Int,
        targetDay: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, targetDay.coerceIn(1, maxDay))
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Calcula la ventana del ciclo de facturación vigente respecto a [referenceDateMillis].
     *
     * Regla bancaria estándar:
     * - Si el día actual es `<= cutoffDay` (ajustado al mes), el corte en curso cierra este mes
     *   y el último corte cerrado ocurrió el mes anterior.
     * - Si el día actual es `> cutoffDay`, el corte en curso cierra el mes siguiente y el último
     *   corte cerrado ocurrió este mes.
     * - Para la fecha límite de pago del último corte cerrado: si `paymentDueDay > cutoffDay`
     *   vence en el mismo mes del corte; si `paymentDueDay <= cutoffDay` vence en el mes siguiente.
     */
    fun getCycleWindow(
        cutoffDay: Int,
        paymentDueDay: Int,
        referenceDateMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): CreditCardCycleWindow {
        val safeCutoff = cutoffDay.coerceIn(1, 31)
        val safeDue = paymentDueDay.coerceIn(1, 31)

        val refCal = Calendar.getInstance(timeZone).apply { timeInMillis = referenceDateMillis }
        val refYear = refCal.get(Calendar.YEAR)
        val refMonth = refCal.get(Calendar.MONTH)
        val refDay = refCal.get(Calendar.DAY_OF_MONTH)
        val refMonthCutoffDay = safeCutoff.coerceAtMost(refCal.getActualMaximum(Calendar.DAY_OF_MONTH))

        val (lastYear, lastMonth) = if (refDay <= refMonthCutoffDay) {
            previousYearMonth(refYear, refMonth)
        } else {
            Pair(refYear, refMonth)
        }
        val (nextYear, nextMonth) = nextYearMonth(lastYear, lastMonth)

        val lastClosedCutoffMillis = resolveDayEndMillis(lastYear, lastMonth, safeCutoff, timeZone)
        val currentCycleStartMillis = lastClosedCutoffMillis + 1L
        val nextCutoffMillis = resolveDayEndMillis(nextYear, nextMonth, safeCutoff, timeZone)

        val (dueYear, dueMonth) = if (safeDue > safeCutoff) {
            Pair(lastYear, lastMonth)
        } else {
            nextYearMonth(lastYear, lastMonth)
        }
        val paymentDueDateMillis = resolveDayNoonMillis(dueYear, dueMonth, safeDue, timeZone)

        return CreditCardCycleWindow(
            lastClosedCutoffMillis = lastClosedCutoffMillis,
            currentCycleStartMillis = currentCycleStartMillis,
            nextCutoffMillis = nextCutoffMillis,
            paymentDueDateMillis = paymentDueDateMillis,
            lastStatementCycleKey = formatCycleKey(lastYear, lastMonth),
            currentStatementCycleKey = formatCycleKey(nextYear, nextMonth)
        )
    }

    /** Clasifica en qué estado de cuenta (`YYYY-MM`) cae una fecha de consumo dada. */
    fun classifyCycleKey(
        txDateMillis: Long,
        cutoffDay: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val safeCutoff = cutoffDay.coerceIn(1, 31)
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = txDateMillis }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val monthCutoff = safeCutoff.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return if (d <= monthCutoff) {
            formatCycleKey(y, m)
        } else {
            val (ny, nm) = nextYearMonth(y, m)
            formatCycleKey(ny, nm)
        }
    }

    /**
     * Resume el saldo exigible del último corte cerrado vs. los nuevos consumos del ciclo abierto.
     *
     * Fórmula exacta en `BigDecimal`:
     * - `balanceAtLastCutoff = openingBalance + cargos(date <= lastCutoff) - abonos(date <= lastCutoff)`
     * - `paymentsAfterLastCutoff = abonos(lastCutoff < date <= refDate)`
     * - `closedStatementRemainingDue = max(0, balanceAtLastCutoff - paymentsAfterLastCutoff)`
     */
    fun calculateStatementSummary(
        card: FinancialAccountEntity,
        transactions: List<TransactionEntity>,
        referenceDateMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): CreditCardStatementSummary {
        val cutoff = (card.cutoffDay ?: 15).coerceIn(1, 31)
        val due = (card.paymentDueDay ?: 5).coerceIn(1, 31)
        val window = getCycleWindow(cutoff, due, referenceDateMillis, timeZone)

        val validCardTxs = transactions.asSequence()
            .filter { tx ->
                tx.accountId == card.id &&
                    !tx.isGastoSupuesto &&
                    tx.tentativeDecision != "DISCARDED" &&
                    !tx.isSuspectedDuplicate
            }
            .toList()

        var balanceAtCutoffBd = MoneyMinorUnits.toBigDecimal(card.openingBalanceMinor)
        var paymentsAfterCutoffBd = BigDecimal.ZERO.setScale(MoneyMinorUnits.CURRENCY_SCALE, MoneyMinorUnits.ROUNDING_MODE)
        var currentCycleChargesBd = BigDecimal.ZERO.setScale(MoneyMinorUnits.CURRENCY_SCALE, MoneyMinorUnits.ROUNDING_MODE)

        for (tx in validCardTxs) {
            val amtBd = MoneyMinorUnits.toBigDecimal(tx.effectiveAmountMinor)
            val isPaymentOrCredit = tx.entryKind == TransactionEntryKind.CARD_PAYMENT ||
                tx.entryKind == TransactionEntryKind.TRANSFER_IN ||
                tx.type == TransactionType.INCOME

            if (tx.dateMillis <= window.lastClosedCutoffMillis) {
                balanceAtCutoffBd = if (isPaymentOrCredit) {
                    balanceAtCutoffBd.subtract(amtBd)
                } else {
                    balanceAtCutoffBd.add(amtBd)
                }
            } else if (tx.dateMillis <= referenceDateMillis) {
                if (isPaymentOrCredit) {
                    paymentsAfterCutoffBd = paymentsAfterCutoffBd.add(amtBd)
                } else if (tx.dateMillis <= window.nextCutoffMillis) {
                    currentCycleChargesBd = currentCycleChargesBd.add(amtBd)
                }
            } else if (!isPaymentOrCredit && tx.dateMillis <= window.nextCutoffMillis) {
                currentCycleChargesBd = currentCycleChargesBd.add(amtBd)
            }
        }

        val balanceAtCutoffPositiveBd = balanceAtCutoffBd.max(BigDecimal.ZERO)
        val remainingDueBd = balanceAtCutoffPositiveBd.subtract(paymentsAfterCutoffBd).max(BigDecimal.ZERO)

        val remainingDueMinor = MoneyMinorUnits.fromBigDecimal(remainingDueBd)
        val currentCycleChargesMinor = MoneyMinorUnits.fromBigDecimal(currentCycleChargesBd)
        val paymentsAfterCutoffMinor = MoneyMinorUnits.fromBigDecimal(paymentsAfterCutoffBd)

        return CreditCardStatementSummary(
            accountId = card.id,
            cycleWindow = window,
            closedStatementRemainingDueMinor = remainingDueMinor,
            currentCycleNewChargesMinor = currentCycleChargesMinor,
            paymentsAfterLastCutoffMinor = paymentsAfterCutoffMinor,
            hasStatementBalanceDue = remainingDueMinor > 0L
        )
    }

    private fun previousYearMonth(year: Int, monthIndex: Int): Pair<Int, Int> =
        if (monthIndex == 0) Pair(year - 1, 11) else Pair(year, monthIndex - 1)

    private fun nextYearMonth(year: Int, monthIndex: Int): Pair<Int, Int> =
        if (monthIndex == 11) Pair(year + 1, 0) else Pair(year, monthIndex + 1)

    private fun formatCycleKey(year: Int, monthIndex: Int): String =
        String.format(Locale.US, "%04d-%02d", year, monthIndex + 1)
}
