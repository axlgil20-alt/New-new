package com.example.ui.util

import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.isGastoReal
import com.example.data.model.isGastoSupuesto
import java.util.Calendar

data class BudgetPeriod(
    val year: Int,
    val monthIndex: Int, // 0..11
    val quincena: Int     // 1 or 2
)

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
    val totalNet: Double
)

object QuincenaFilterHelper {

    /**
     * Determina el ciclo financiado por una fecha. Gastos del 1 al 15 pertenecen a Q1;
     * los del 16 al fin pertenecen a Q2. Un pago el 15 financia Q2, y un pago de fin
     * de mes financia Q1 del mes siguiente. Sin tipo, se asume gasto.
     */
    fun getBudgetPeriod(timeMillis: Long, transactionType: String? = null): BudgetPeriod {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        return if (transactionType == TransactionType.INCOME && d == maxDay) {
            // El pago de fin de mes financia del 1 al 15 del mes siguiente
            val nextMonth = if (m == 11) 0 else m + 1
            val nextYear = if (m == 11) y + 1 else y
            BudgetPeriod(year = nextYear, monthIndex = nextMonth, quincena = 1)
        } else if (transactionType == TransactionType.INCOME && d == 15) {
            // El pago del 15 financia del 16 al último día del mes
            BudgetPeriod(year = y, monthIndex = m, quincena = 2)
        } else if (d in 1..15) {
            BudgetPeriod(year = y, monthIndex = m, quincena = 1)
        } else {
            BudgetPeriod(year = y, monthIndex = m, quincena = 2)
        }
    }

    /**
     * Obtiene el rango de timestamps exactos (inicio 00:00:00.000 a fin 23:59:59.999) para el periodo presupuestario:
     * Rango inclusivo de fechas que puede contener el ciclo, incluyendo la fecha del pago:
     * Q1 incluye el pago de fin de mes anterior y gastos del 1 al 15;
     * Q2 incluye el pago del 15 y gastos del 16 al fin de mes.
     */
    fun getBudgetPeriodRange(year: Int, monthIndex: Int, quincena: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()

        val startMillis: Long = if (quincena == 2) {
            cal.clear()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthIndex)
            cal.set(Calendar.DAY_OF_MONTH, 15)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } else {
            cal.clear()
            val prevMonth = if (monthIndex == 0) 11 else monthIndex - 1
            val prevYear = if (monthIndex == 0) year - 1 else year
            cal.set(Calendar.YEAR, prevYear)
            cal.set(Calendar.MONTH, prevMonth)
            val prevMaxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, prevMaxDay)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }

        val endMillis: Long = if (quincena == 1) {
            cal.clear()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthIndex)
            cal.set(Calendar.DAY_OF_MONTH, 15)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            cal.timeInMillis
        } else {
            cal.clear()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthIndex)
            val curMaxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, curMaxDay)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            cal.timeInMillis
        }

        return Pair(startMillis, endMillis)
    }

    /**
     * Verifica si una transacción pertenece al mes presupuestario indicado.
     */
    fun isDateInBudgetMonth(dateMillis: Long, targetYear: Int, targetMonthIndex: Int, transactionType: String? = null): Boolean {
        val bp = getBudgetPeriod(dateMillis, transactionType)
        return bp.year == targetYear && bp.monthIndex == targetMonthIndex
    }

    /**
     * Verifica si la fecha dada corresponde a la quincena indicada:
     * - 1: Gasto del 1 al 15; pago de fin de mes anterior.
     * - 2: Gasto del 16 al fin; pago del día 15.
     * - <= 0: Todas.
     */
    fun isDateInQuincena(dateMillis: Long, quincenaNumber: Int, transactionType: String? = null): Boolean {
        if (quincenaNumber <= 0) return true
        val bp = getBudgetPeriod(dateMillis, transactionType)
        return bp.quincena == quincenaNumber
    }

    /**
     * Filtra rigurosamente las transacciones para la vista de Quincenas:
     * - Mes y año: Exclusivamente transacciones cuyo ciclo presupuestario pertenezca a targetYear y targetMonthIndex.
     * - Q1: Pago de fin del mes anterior y gastos del día 1 al 15.
     * - Q2: Pago del día 15 y gastos del día 16 al último día del mes.
     * - Totales calculados estrictamente sobre las listas filtradas.
     */
    fun filterByQuincena(
        transactions: List<TransactionEntity>,
        targetYear: Int,
        targetMonthIndex: Int
    ): QuincenaFilteredData {
        val q1 = mutableListOf<TransactionEntity>()
        val q2 = mutableListOf<TransactionEntity>()
        val monthList = mutableListOf<TransactionEntity>()

        for (tx in transactions) {
            val bp = getBudgetPeriod(tx.dateMillis, tx.type)
            if (bp.year == targetYear && bp.monthIndex == targetMonthIndex) {
                monthList.add(tx)
                if (bp.quincena == 1) {
                    q1.add(tx)
                } else if (bp.quincena == 2) {
                    q2.add(tx)
                }
            }
        }

        val q1Inc = q1.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val q1Exp = q1.filter { it.isGastoReal }.sumOf { it.amount }
        val q1Pending = q1.filter { it.isGastoSupuesto && it.tentativeDecision != "DISCARDED" }.sumOf { it.amount }
        val q2Inc = q2.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val q2Exp = q2.filter { it.isGastoReal }.sumOf { it.amount }
        val q2Pending = q2.filter { it.isGastoSupuesto && it.tentativeDecision != "DISCARDED" }.sumOf { it.amount }
        val totInc = monthList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totExp = monthList.filter { it.isGastoReal }.sumOf { it.amount }
        val totPending = monthList.filter { it.isGastoSupuesto && it.tentativeDecision != "DISCARDED" }.sumOf { it.amount }

        return QuincenaFilteredData(
            q1Transactions = q1,
            q2Transactions = q2,
            monthTransactions = monthList,
            q1Income = q1Inc,
            q1Expense = q1Exp,
            q1PendingExpense = q1Pending,
            q1Net = q1Inc - q1Exp,
            q2Income = q2Inc,
            q2Expense = q2Exp,
            q2PendingExpense = q2Pending,
            q2Net = q2Inc - q2Exp,
            totalIncome = totInc,
            totalExpense = totExp,
            totalPendingExpense = totPending,
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

    /**
     * Obtiene los límites de inicio y fin en milisegundos para el día calendario dado.
     */
    fun getDayRange(timeMillis: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    /**
     * Obtiene los límites de inicio y fin para la semana (lunes a domingo) de la fecha dada.
     */
    fun getWeekRange(timeMillis: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = timeMillis
        }
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.DAY_OF_WEEK, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    /**
     * Obtiene los límites de inicio y fin para el año dado.
     */
    fun getYearRange(year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = cal.timeInMillis

        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    /**
     * Identifica los IDs de transacciones que son sospechosas de duplicado en la lista:
     * - Aquellas con isSuspectedDuplicate == true
     * - O transacciones que comparten mismo tipo, monto, mismo día calendario y mismo concepto/comercio
     */
    fun findDuplicateTransactionIds(transactions: List<TransactionEntity>): Set<Long> {
        val duplicateIds = mutableSetOf<Long>()
        val n = transactions.size

        for (i in 0 until n) {
            val tx1 = transactions[i]
            if (tx1.isSuspectedDuplicate) {
                duplicateIds.add(tx1.id)
            }
            for (j in i + 1 until n) {
                val tx2 = transactions[j]
                val sameType = tx1.type.equals(tx2.type, ignoreCase = true)
                val sameAmount = kotlin.math.abs(tx1.amount - tx2.amount) < 0.005
                val sameDay = isSameCalendarDay(tx1.dateMillis, tx2.dateMillis)
                val sameTitle = tx1.title.trim().equals(tx2.title.trim(), ignoreCase = true)

                if (sameType && sameAmount && sameDay && sameTitle) {
                    duplicateIds.add(tx1.id)
                    duplicateIds.add(tx2.id)
                }
            }
        }
        return duplicateIds
    }

    /**
     * Calcula dinámicamente el salario roll-over acumulado de periodos quincenales anteriores.
     * Si la quincena anterior cerró con superávit (Ingresos + Rollover previo - Gastos > 0),
     * ese dinero sobrante se traslada a la quincena actual como "Salario Roll-Over".
     */
    fun calculateQuincenaRollover(
        transactions: List<TransactionEntity>,
        targetYear: Int,
        targetMonthIndex: Int,
        targetQuincena: Int
    ): Double {
        if (transactions.isEmpty()) return 0.0

        // Clave única entera para cada quincena: año * 24 + mes * 2 + (quincena - 1)
        fun periodKey(year: Int, month: Int, quincena: Int): Int {
            return year * 24 + month * 2 + (quincena - 1)
        }

        val targetQ = if (targetQuincena in 1..2) targetQuincena else 1
        val targetKey = periodKey(targetYear, targetMonthIndex, targetQ)

        // Agrupar ingresos y gastos por periodKey según su BudgetPeriod
        val periodIncomeMap = mutableMapOf<Int, Double>()
        val periodExpenseMap = mutableMapOf<Int, Double>()

        for (tx in transactions) {
            val bp = getBudgetPeriod(tx.dateMillis, tx.type)
            val key = periodKey(bp.year, bp.monthIndex, bp.quincena)

            if (tx.type == TransactionType.INCOME) {
                periodIncomeMap[key] = (periodIncomeMap[key] ?: 0.0) + tx.amount
            } else if (tx.isGastoReal) {
                periodExpenseMap[key] = (periodExpenseMap[key] ?: 0.0) + tx.amount
            }
        }

        // Obtener todos los periodos previos ordenados cronológicamente
        val allKeys = (periodIncomeMap.keys + periodExpenseMap.keys).distinct().filter { it < targetKey }.sorted()
        if (allKeys.isEmpty()) return 0.0

        var currentRollover = 0.0
        val minKey = allKeys.first()

        // Iterar periodo a periodo de forma secuencial encadenada
        for (k in minKey until targetKey) {
            val inc = periodIncomeMap[k] ?: 0.0
            val exp = periodExpenseMap[k] ?: 0.0
            val net = (inc + currentRollover) - exp
            currentRollover = net.coerceAtLeast(0.0)
        }

        return currentRollover
    }
}
