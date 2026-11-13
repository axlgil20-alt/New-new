package com.example.ui.util

import com.example.data.model.TransactionEntity
import com.example.data.model.effectiveAmountBigDecimal
import com.example.data.model.effectiveAmountMinor
import com.example.data.model.isGastoReal
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Cálculo local de límites mensuales; no escribe ni genera transacciones. */
object BudgetLimitCalculator {
    const val QUETZAL_SYMBOL = "Q "
    private val hundred = BigDecimal("100")
    private val nearLimitPercent = BigDecimal("80")

    data class Usage(
        val spent: BigDecimal,
        val limit: BigDecimal,
        val remaining: BigDecimal,
        val exceededBy: BigDecimal,
        val percentUsed: BigDecimal,
        val progress: Float,
        val isOverLimit: Boolean
    )

    fun monthlySpend(
        transactions: List<TransactionEntity>,
        category: String,
        year: Int,
        monthIndex: Int
    ): BigDecimal {
        val monthStart = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        val monthEndExclusive = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
        }.timeInMillis

        return transactions.asSequence()
            .filter { tx ->
                tx.category.trim().equals(category.trim(), ignoreCase = true) &&
                    tx.isGastoReal &&
                    !tx.isSuspectedDuplicate &&
                    tx.dateMillis >= monthStart && tx.dateMillis < monthEndExclusive &&
                    tx.effectiveAmountMinor >= 0L
            }
            .fold(BigDecimal.ZERO) { total, tx ->
                total.add(tx.effectiveAmountBigDecimal)
            }
            .setScale(2, RoundingMode.HALF_UP)
    }

    fun usage(spentAmount: BigDecimal, limitAmount: Double): Usage {
        val spent = spentAmount.setScale(2, RoundingMode.HALF_UP)
        val limit = if (limitAmount.isFinite() && limitAmount > 0.0) {
            BigDecimal.valueOf(limitAmount).setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2)
        }
        if (limit.signum() <= 0) {
            return Usage(spent, limit, BigDecimal.ZERO.setScale(2), spent.max(BigDecimal.ZERO), BigDecimal.ZERO, 0f, spent.signum() > 0)
        }

        val difference = limit.subtract(spent).setScale(2, RoundingMode.HALF_UP)
        val percent = spent.multiply(hundred).divide(limit, 2, RoundingMode.HALF_UP)
        val rawProgress = spent.divide(limit, 6, RoundingMode.HALF_UP).toFloat()
        return Usage(
            spent = spent,
            limit = limit,
            remaining = difference.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
            exceededBy = difference.negate().max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
            percentUsed = percent,
            progress = rawProgress.coerceIn(0f, 1f),
            isOverLimit = difference.signum() < 0
        )
    }

    /** El umbral acordado es 80%; no depende del porcentaje redondeado que se muestra en pantalla. */
    fun isNearLimit(usage: Usage): Boolean =
        !usage.isOverLimit && usage.limit.signum() > 0 &&
            usage.spent.multiply(hundred) >= usage.limit.multiply(nearLimitPercent)

    fun monthLabel(year: Int, monthIndex: Int): String {
        val calendar = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMMM yyyy", Locale("es", "GT"))
            .format(Date(calendar.timeInMillis))
            .replaceFirstChar { it.titlecase(Locale("es", "GT")) }
    }
}
