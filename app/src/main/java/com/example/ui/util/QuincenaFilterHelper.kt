package com.example.ui.util

import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.util.Calendar

data class QuincenaFilteredData(
    val q1Transactions: List<TransactionEntity>,
    val q2Transactions: List<TransactionEntity>,
    val monthTransactions: List<TransactionEntity>,
    val q1Income: Double,
    val q1Expense: Double,
    val q1Net: Double,
    val q2Income: Double,
    val q2Expense: Double,
    val q2Net: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val totalNet: Double
)

object QuincenaFilterHelper {

    /**
     * Filtra rigurosamente las transacciones para la vista de Quincenas:
     * - Mes y año: Exclusivamente transacciones cuya fecha real pertenezca a targetYear y targetMonthIndex (0-indexado).
     * - Q1: Del día 1 al día 15 inclusive.
     * - Q2: Del día 16 hasta el último día del mes (hasta el 31 en meses de 31 días como diciembre u octubre).
     * - Totales: Calculados estrictamente sobre las listas filtradas correspondientes.
     */
    fun filterByQuincena(
        transactions: List<TransactionEntity>,
        targetYear: Int,
        targetMonthIndex: Int
    ): QuincenaFilteredData {
        val cal = Calendar.getInstance()
        val q1 = mutableListOf<TransactionEntity>()
        val q2 = mutableListOf<TransactionEntity>()
        val monthList = mutableListOf<TransactionEntity>()

        for (tx in transactions) {
            cal.timeInMillis = tx.dateMillis
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)

            // Filtrado estricto por año y mes
            if (y == targetYear && m == targetMonthIndex) {
                monthList.add(tx)
                val day = cal.get(Calendar.DAY_OF_MONTH)
                if (day in 1..15) {
                    q1.add(tx)
                } else if (day >= 16) {
                    q2.add(tx)
                }
            }
        }

        val q1Inc = q1.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val q1Exp = q1.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val q2Inc = q2.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val q2Exp = q2.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totInc = monthList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totExp = monthList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        return QuincenaFilteredData(
            q1Transactions = q1,
            q2Transactions = q2,
            monthTransactions = monthList,
            q1Income = q1Inc,
            q1Expense = q1Exp,
            q1Net = q1Inc - q1Exp,
            q2Income = q2Inc,
            q2Expense = q2Exp,
            q2Net = q2Inc - q2Exp,
            totalIncome = totInc,
            totalExpense = totExp,
            totalNet = totInc - totExp
        )
    }

    /**
     * Verifica si dos timestamps corresponden exactamente al mismo día calendario local.
     */
    fun isSameCalendarDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
