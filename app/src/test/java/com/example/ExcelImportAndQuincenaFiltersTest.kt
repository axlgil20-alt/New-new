package com.example

import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.BudgetRepository
import com.example.ui.util.QuincenaFilterHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Pruebas obligatorias para:
 * A. Importación Excel sin duplicados
 * B. Filtros de Quincenas (Q1: 1-15, Q2: 16-fin de mes, límites de mes/año, totales)
 */
class ExcelImportAndQuincenaFiltersTest {

    private lateinit var fakeTxDao: FakeTransactionDao
    private lateinit var repository: BudgetRepository

    @Before
    fun setUp() {
        fakeTxDao = FakeTransactionDao()
        repository = BudgetRepository(
            transactionDao = fakeTxDao,
            budgetDao = FakeBudgetDao(),
            savingsGoalDao = FakeSavingsGoalDao(),
            recurringExpenseDao = FakeRecurringExpenseDao(),
            smartCartDao = FakeSmartCartDao(),
            database = null
        )
    }

    // =========================================================================
    // PRUEBAS DE IMPORTACIÓN EXCEL SIN DUPLICADOS
    // =========================================================================

    @Test
    fun testImportTwiceDoesNotDuplicateOperations() = runBlocking {
        val csvData = """
            Fecha,Tipo,Concepto,Categoría,Descripción,Monto Gasto,Monto Ingreso
            10/10/2026,Gasto,Súper La Torre,Alimentación,Despensa quincenal,450.00,0.00
            15/10/2026,Ingreso,Sueldo Quincenal,Sueldo,Primera quincena,0.00,5000.00
            18/10/2026,Gasto,Gasolina Shell,Transporte,Tanque lleno,250.00,0.00
        """.trimIndent()

        // Primera importación: Debe insertar los 3 registros
        val summary1 = repository.importFromCsv(csvData)
        assertEquals(3, summary1.newCount)
        assertEquals(0, summary1.duplicateCount)
        assertEquals(3, fakeTxDao.transactions.size)

        // Segunda importación con el mismo archivo: Debe omitir los 3 duplicados
        val summary2 = repository.importFromCsv(csvData)
        assertEquals(0, summary2.newCount)
        assertEquals(3, summary2.duplicateCount)
        // La base de datos no debe crecer
        assertEquals("La base de datos debe mantener exactamente 3 transacciones", 3, fakeTxDao.transactions.size)
    }

    @Test
    fun testTwoLegitimateDistinctTransactionsWithSameDateAndAmountAreBothPreserved() = runBlocking {
        // Dos compras legítimas el mismo día por el mismo monto en la misma gasolinera
        val csvData = """
            Fecha,Tipo,Concepto,Categoría,Descripción,Monto Gasto,Monto Ingreso
            15/10/2026,Gasto,Gasolina Shell,Transporte,Carga matutina,200.00,0.00
            15/10/2026,Gasto,Gasolina Shell,Transporte,Carga nocturna viaje,200.00,0.00
        """.trimIndent()

        // Primera importación: Ambas operaciones legítimas deben registrarse
        val summary1 = repository.importFromCsv(csvData)
        assertEquals(2, summary1.newCount)
        assertEquals(0, summary1.duplicateCount)
        assertEquals(2, fakeTxDao.transactions.size)

        // Si se reimporta el mismo archivo, ambas son reconocidas como ya existentes y se omiten
        val summary2 = repository.importFromCsv(csvData)
        assertEquals(0, summary2.newCount)
        assertEquals(2, summary2.duplicateCount)
        assertEquals(2, fakeTxDao.transactions.size)
    }

    @Test
    fun testPartialNewTransactionsInReimport() = runBlocking {
        val initialCsv = """
            Fecha,Tipo,Concepto,Categoría,Descripción,Monto Gasto,Monto Ingreso
            15/10/2026,Gasto,Farmacia Galeno,Salud,Medicinas,150.00,0.00
        """.trimIndent()

        repository.importFromCsv(initialCsv)
        assertEquals(1, fakeTxDao.transactions.size)

        // Archivo actualizado que contiene la fila anterior + una fila nueva
        val updatedCsv = """
            Fecha,Tipo,Concepto,Categoría,Descripción,Monto Gasto,Monto Ingreso
            15/10/2026,Gasto,Farmacia Galeno,Salud,Medicinas,150.00,0.00
            16/10/2026,Gasto,Panadería San Martín,Alimentación,Desayuno,85.00,0.00
        """.trimIndent()

        val summary = repository.importFromCsv(updatedCsv)
        assertEquals("Debe importar exactamente 1 nueva fila", 1, summary.newCount)
        assertEquals("Debe omitir 1 fila ya existente", 1, summary.duplicateCount)
        assertEquals("Total en BD debe ser 2", 2, fakeTxDao.transactions.size)
    }

    // =========================================================================
    // PRUEBAS DE FILTROS REALES DE QUINCENAS (MES, AÑO, Q1 Y Q2)
    // =========================================================================

    private fun createMillis(year: Int, monthIndex: Int, day: Int, hour: Int = 12): Long {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthIndex)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun testOctoberDoesNotShowSeptemberOrNovemberMovements() {
        val txs = listOf(
            TransactionEntity(id = 1, title = "Septiembre Gasto", amount = 100.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 8, 28)), // Septiembre (mes 8)
            TransactionEntity(id = 2, title = "Octubre Gasto 1", amount = 150.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 5)),   // Octubre (mes 9)
            TransactionEntity(id = 3, title = "Octubre Gasto 2", amount = 200.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 20)),  // Octubre (mes 9)
            TransactionEntity(id = 4, title = "Noviembre Gasto", amount = 300.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 10, 3))   // Noviembre (mes 10)
        )

        // Filtrar exclusivamente para Octubre 2026 (mes 9)
        val filtered = QuincenaFilterHelper.filterByQuincena(
            transactions = txs,
            targetYear = 2026,
            targetMonthIndex = 9
        )

        assertEquals("Octubre debe contener exactamente 2 transacciones", 2, filtered.monthTransactions.size)
        assertTrue(filtered.monthTransactions.all { it.title.startsWith("Octubre") })
        assertFalse("No debe contener transacciones de septiembre", filtered.monthTransactions.any { it.title.contains("Septiembre") })
        assertFalse("No debe contener transacciones de noviembre", filtered.monthTransactions.any { it.title.contains("Noviembre") })
    }

    @Test
    fun testQ1DoesNotIncludeQ2AndQ2DoesNotIncludeQ1() {
        val txs = listOf(
            TransactionEntity(id = 1, title = "Q1 Día 1", amount = 50.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 1)),
            TransactionEntity(id = 2, title = "Q1 Día 15 Límite", amount = 100.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 15, 23)),
            TransactionEntity(id = 3, title = "Q2 Día 16 Inicio", amount = 200.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 16, 0)),
            TransactionEntity(id = 4, title = "Q2 Día 30", amount = 300.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 30))
        )

        val filtered = QuincenaFilterHelper.filterByQuincena(
            transactions = txs,
            targetYear = 2026,
            targetMonthIndex = 9
        )

        // Q1 debe tener exclusivamente días 1 al 15
        assertEquals(2, filtered.q1Transactions.size)
        assertTrue(filtered.q1Transactions.any { it.id == 1L })
        assertTrue(filtered.q1Transactions.any { it.id == 2L })
        assertFalse(filtered.q1Transactions.any { it.id == 3L || it.id == 4L })

        // Q2 debe tener exclusivamente días 16 al fin de mes
        assertEquals(2, filtered.q2Transactions.size)
        assertTrue(filtered.q2Transactions.any { it.id == 3L })
        assertTrue(filtered.q2Transactions.any { it.id == 4L })
        assertFalse(filtered.q2Transactions.any { it.id == 1L || it.id == 2L })
    }

    @Test
    fun testDecemberWorksCorrectlyUpToDay31() {
        // Diciembre (mes 11) tiene 31 días
        val txs = listOf(
            TransactionEntity(id = 1, title = "Día 1 Dic", amount = 100.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 11, 1)),
            TransactionEntity(id = 2, title = "Aguinaldo 15 Dic", amount = 4000.0, type = TransactionType.INCOME, category = "Sueldo", dateMillis = createMillis(2026, 11, 15)),
            TransactionEntity(id = 3, title = "Cena 24 Dic", amount = 600.0, type = TransactionType.EXPENSE, category = "Comida", dateMillis = createMillis(2026, 11, 24)),
            TransactionEntity(id = 4, title = "Año Nuevo 31 Dic", amount = 500.0, type = TransactionType.EXPENSE, category = "Fiesta", dateMillis = createMillis(2026, 11, 31, 23))
        )

        val filtered = QuincenaFilterHelper.filterByQuincena(
            transactions = txs,
            targetYear = 2026,
            targetMonthIndex = 11
        )

        assertEquals("Diciembre debe incluir las 4 transacciones", 4, filtered.monthTransactions.size)

        // Q1: Día 1 y 15
        assertEquals(2, filtered.q1Transactions.size)

        // Q2: Día 24 y Día 31
        assertEquals(2, filtered.q2Transactions.size)
        assertTrue("Q2 debe incluir la transacción del 31 de diciembre", filtered.q2Transactions.any { it.id == 4L })
    }

    @Test
    fun testTotalsMatchFilteredTransactionsStrictly() {
        val txs = listOf(
            // Q1
            TransactionEntity(id = 1, title = "Sueldo Q1", amount = 6000.0, type = TransactionType.INCOME, category = "Sueldo", dateMillis = createMillis(2026, 9, 15)),
            TransactionEntity(id = 2, title = "Renta Q1", amount = 2000.0, type = TransactionType.EXPENSE, category = "Vivienda", dateMillis = createMillis(2026, 9, 5)),
            // Q2
            TransactionEntity(id = 3, title = "Bono Q2", amount = 1500.0, type = TransactionType.INCOME, category = "Extra", dateMillis = createMillis(2026, 9, 25)),
            TransactionEntity(id = 4, title = "Súper Q2", amount = 1000.0, type = TransactionType.EXPENSE, category = "Alimentación", dateMillis = createMillis(2026, 9, 20)),
            // Mes diferente (no debe sumar a los totales)
            TransactionEntity(id = 5, title = "Gasto Septiembre", amount = 500.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 8, 15))
        )

        val filtered = QuincenaFilterHelper.filterByQuincena(
            transactions = txs,
            targetYear = 2026,
            targetMonthIndex = 9
        )

        // Totales Q1
        assertEquals(6000.0, filtered.q1Income, 0.001)
        assertEquals(2000.0, filtered.q1Expense, 0.001)
        assertEquals(4000.0, filtered.q1Net, 0.001)

        // Totales Q2
        assertEquals(1500.0, filtered.q2Income, 0.001)
        assertEquals(1000.0, filtered.q2Expense, 0.001)
        assertEquals(500.0, filtered.q2Net, 0.001)

        // Totales del mes completo
        assertEquals(7500.0, filtered.totalIncome, 0.001)
        assertEquals(3000.0, filtered.totalExpense, 0.001)
        assertEquals(4500.0, filtered.totalNet, 0.001)
    }

    @Test
    fun testDays15And16AssignedStrictlyToQ1AndQ2() {
        val txs = listOf(
            TransactionEntity(id = 1, title = "Día 15 Inicio", amount = 100.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 15, 0)),
            TransactionEntity(id = 2, title = "Día 15 Fin", amount = 150.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 15, 23)),
            TransactionEntity(id = 3, title = "Día 16 Inicio", amount = 200.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 16, 0)),
            TransactionEntity(id = 4, title = "Día 16 Fin", amount = 250.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 16, 23))
        )

        val filtered = QuincenaFilterHelper.filterByQuincena(
            transactions = txs,
            targetYear = 2026,
            targetMonthIndex = 9 // Octubre
        )

        assertEquals("Q1 debe contener exactamente los 2 registros del día 15", 2, filtered.q1Transactions.size)
        assertTrue(filtered.q1Transactions.all { it.title.startsWith("Día 15") })

        assertEquals("Q2 debe contener exactamente los 2 registros del día 16", 2, filtered.q2Transactions.size)
        assertTrue(filtered.q2Transactions.all { it.title.startsWith("Día 16") })
    }

    @Test
    fun testFebruaryEndLeapAndNonLeapYearAndDecember31() {
        // Febrero no bisiesto (2025): día 28
        val txFeb28 = TransactionEntity(id = 1, title = "Feb 28 No Bisiesto", amount = 100.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2025, 1, 28))
        val resFeb28 = QuincenaFilterHelper.filterByQuincena(listOf(txFeb28), 2025, 1)
        assertEquals(1, resFeb28.q2Transactions.size)
        assertEquals(0, resFeb28.q1Transactions.size)

        // Febrero bisiesto (2024): día 29
        val txFeb29 = TransactionEntity(id = 2, title = "Feb 29 Bisiesto", amount = 150.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2024, 1, 29))
        val resFeb29 = QuincenaFilterHelper.filterByQuincena(listOf(txFeb29), 2024, 1)
        assertEquals(1, resFeb29.q2Transactions.size)
        assertEquals(0, resFeb29.q1Transactions.size)

        // Diciembre 31: fin de año
        val txDec31 = TransactionEntity(id = 3, title = "Dic 31 Fin de Año", amount = 500.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 11, 31, 23))
        val resDec31 = QuincenaFilterHelper.filterByQuincena(listOf(txDec31), 2026, 11)
        assertEquals(1, resDec31.q2Transactions.size)
        assertEquals(0, resDec31.q1Transactions.size)
    }

    @Test
    fun testExternalIdPreservedAndUsedForDeduplication() = runBlocking {
        val csv = """
            ID,Fecha,Tipo,Concepto,Categoría,Monto Gasto,Monto Ingreso
            TX-1001,10/10/2026,Gasto,Suscripción,Servicios,99.00,0.00
            TX-1002,12/10/2026,Gasto,Cine,Ocio,120.00,0.00
        """.trimIndent()

        val summary1 = repository.importFromCsv(csv)
        assertEquals(2, summary1.newCount)
        assertEquals(0, summary1.duplicateCount)
        assertEquals("TX-1001", fakeTxDao.transactions[0].externalId)
        assertEquals("TX-1002", fakeTxDao.transactions[1].externalId)

        // Re-importar: detecta los mismos externalIds
        val summary2 = repository.importFromCsv(csv)
        assertEquals(0, summary2.newCount)
        assertEquals(2, summary2.duplicateCount)
        assertEquals(2, fakeTxDao.transactions.size)
    }

    @Test
    fun testImportFailureDoesNotLeavePartialState() = runBlocking {
        val failingDao = object : FakeTransactionDao() {
            var shouldFail = false
            override suspend fun insertAll(txs: List<TransactionEntity>) {
                if (shouldFail) throw RuntimeException("Error simulado de base de datos durante la importación")
                super.insertAll(txs)
            }
        }
        val safeRepo = BudgetRepository(
            transactionDao = failingDao,
            budgetDao = FakeBudgetDao(),
            savingsGoalDao = FakeSavingsGoalDao(),
            recurringExpenseDao = FakeRecurringExpenseDao(),
            smartCartDao = FakeSmartCartDao(),
            database = null
        )

        val csv = """
            Fecha,Tipo,Concepto,Categoría,Monto Gasto,Monto Ingreso
            10/10/2026,Gasto,Compra 1,Varios,100.00,0.00
            11/10/2026,Gasto,Compra 2,Varios,200.00,0.00
        """.trimIndent()

        failingDao.shouldFail = true
        try {
            safeRepo.importFromCsv(csv)
            fail("Debió lanzar una excepción")
        } catch (_: Exception) {
            // Esperado
        }

        assertEquals("No deben quedar transacciones parciales tras el fallo", 0, failingDao.transactions.size)
    }

    // =========================================================================
    // FAKES
    // =========================================================================

    open class FakeTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()
        private var nextId = 1L

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions)
        override suspend fun getAllTransactionsList(): List<TransactionEntity> = transactions.toList()
        override fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> = flowOf(transactions)

        override suspend fun insertTransaction(tx: TransactionEntity): Long {
            val id = if (tx.id == 0L) nextId++ else tx.id
            transactions.add(tx.copy(id = id))
            return id
        }
        open override suspend fun insertAll(txs: List<TransactionEntity>) {
            for (t in txs) insertTransaction(t)
        }
        override suspend fun updateTransaction(tx: TransactionEntity) {}
        override suspend fun deleteTransaction(tx: TransactionEntity) {}
        override suspend fun deleteById(id: Long) {}
        override suspend fun clearAll() { transactions.clear() }
    }

    class FakeBudgetDao : BudgetDao {
        override fun getAllBudgets(): Flow<List<BudgetLimitEntity>> = flowOf(emptyList())
        override suspend fun setBudget(budget: BudgetLimitEntity) {}
        override suspend fun insertAll(budgets: List<BudgetLimitEntity>) {}
        override suspend fun deleteBudget(category: String) {}
        override suspend fun clearAll() {}
    }

    class FakeSavingsGoalDao : SavingsGoalDao {
        override fun getAllGoals(): Flow<List<SavingsGoalEntity>> = flowOf(emptyList())
        override suspend fun insertGoal(goal: SavingsGoalEntity): Long = 1L
        override suspend fun insertAll(goals: List<SavingsGoalEntity>) {}
        override suspend fun updateGoal(goal: SavingsGoalEntity) {}
        override suspend fun deleteGoal(goal: SavingsGoalEntity) {}
        override suspend fun deleteById(id: Long) {}
        override suspend fun clearAll() {}
    }

    class FakeRecurringExpenseDao : RecurringExpenseDao {
        override fun getAll(): Flow<List<RecurringExpenseEntity>> = flowOf(emptyList())
        override suspend fun insert(expense: RecurringExpenseEntity): Long = 1L
        override suspend fun insertAll(expenses: List<RecurringExpenseEntity>) {}
        override suspend fun update(expense: RecurringExpenseEntity) {}
        override suspend fun delete(expense: RecurringExpenseEntity) {}
        override suspend fun deleteById(id: Long) {}
        override suspend fun clearAll() {}
    }

    class FakeSmartCartDao : SmartCartDao {
        override fun getAllItems(): Flow<List<SmartCartItemEntity>> = flowOf(emptyList())
        override suspend fun getAllItemsList(): List<SmartCartItemEntity> = emptyList()
        override suspend fun toggleItemChecked(id: Long) {}
        override suspend fun updatePantryStatus(id: Long, status: String) {}
        override suspend fun updatePantryStatusToGreenByName(name: String) {}
        override suspend fun markItemPurchasedAsGreen(id: Long) {}
        override suspend fun markItemPurchasedAsGreenByName(name: String) {}
        override suspend fun incorporateNeedToBuyIntoCart() {}
        override suspend fun uncheckAll() {}
        override suspend fun insertItem(item: SmartCartItemEntity): Long = 1L
        override suspend fun insertAll(items: List<SmartCartItemEntity>) {}
        override suspend fun updateItem(item: SmartCartItemEntity) {}
        override suspend fun deleteItem(item: SmartCartItemEntity) {}
        override suspend fun deleteCheckedItems() {}
        override suspend fun clearCartItems() {}
        override suspend fun clearPantryItems() {}
        override suspend fun clearAll() {}
    }
}
