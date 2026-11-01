package com.example

import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.util.BudgetLimitCalculator
import java.math.BigDecimal
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetLimitCalculatorTest {
    @Test
    fun monthlySpendOnlyCountsRealExpensesOfSelectedMonthAndCategory() {
        val year = 2026
        val monthIndex = Calendar.FEBRUARY
        val transactions = listOf(
            transaction("Compra", 10.10, monthMillis(year, monthIndex, 5), category = "  COMIDA "),
            transaction("Pendiente", 99.00, monthMillis(year, monthIndex, 6), tentative = true, decision = "PENDING"),
            transaction("Descartado", 50.00, monthMillis(year, monthIndex, 7), tentative = true, decision = "DISCARDED"),
            transaction("Ingreso", 20.00, monthMillis(year, monthIndex, 8), type = TransactionType.INCOME),
            transaction("Otra categoría", 31.00, monthMillis(year, monthIndex, 9), category = "Transporte"),
            transaction("Duplicado sospechoso", 30.00, monthMillis(year, monthIndex, 10), suspectedDuplicate = true),
            transaction("Otro mes", 40.00, monthMillis(year, Calendar.JANUARY, 31))
        )

        val spent = BudgetLimitCalculator.monthlySpend(transactions, "Comida", year, monthIndex)

        assertEquals(BigDecimal("10.10"), spent)
    }

    @Test
    fun centValuesAndRemainingAmountAreExact() {
        val result = BudgetLimitCalculator.usage(BigDecimal("0.10").add(BigDecimal("0.20")), 0.50)

        assertEquals(BigDecimal("0.30"), result.spent)
        assertEquals(BigDecimal("0.20"), result.remaining)
        assertEquals(BigDecimal("60.00"), result.percentUsed)
        assertFalse(result.isOverLimit)
        assertFalse(BudgetLimitCalculator.isNearLimit(result))
    }

    @Test
    fun overLimitShowsUncappedPercentageAndCappedProgress() {
        val result = BudgetLimitCalculator.usage(BigDecimal("2.25"), 2.00)

        assertEquals(BigDecimal("0.25"), result.exceededBy)
        assertEquals(BigDecimal("112.50"), result.percentUsed)
        assertEquals(1f, result.progress)
        assertTrue(result.isOverLimit)
    }

    @Test
    fun nearLimitStartsAtExactlyEightyPercent() {
        val atThreshold = BudgetLimitCalculator.usage(BigDecimal("8.00"), 10.00)
        val belowThreshold = BudgetLimitCalculator.usage(BigDecimal("7.99"), 10.00)

        assertTrue(BudgetLimitCalculator.isNearLimit(atThreshold))
        assertFalse(BudgetLimitCalculator.isNearLimit(belowThreshold))
    }

    private fun transaction(
        title: String,
        amount: Double,
        dateMillis: Long,
        category: String = "Comida",
        type: String = TransactionType.EXPENSE,
        tentative: Boolean = false,
        decision: String = "",
        suspectedDuplicate: Boolean = false
    ) = TransactionEntity(
        title = title,
        amount = amount,
        type = type,
        category = category,
        dateMillis = dateMillis,
        isTentative = tentative,
        tentativeDecision = decision,
        isSuspectedDuplicate = suspectedDuplicate
    )

    private fun monthMillis(year: Int, monthIndex: Int, dayOfMonth: Int): Long = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthIndex)
        set(Calendar.DAY_OF_MONTH, dayOfMonth)
        set(Calendar.HOUR_OF_DAY, 12)
    }.timeInMillis
}
