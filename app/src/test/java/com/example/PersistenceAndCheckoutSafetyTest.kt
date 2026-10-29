package com.example

import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PersistenceAndCheckoutSafetyTest {

    private lateinit var fakeTxDao: FakeTransactionDao
    private lateinit var fakeBudgetDao: FakeBudgetDao
    private lateinit var fakeGoalDao: FakeSavingsGoalDao
    private lateinit var fakeRecurringDao: FakeRecurringExpenseDao
    private lateinit var fakeCartDao: FakeSmartCartDao
    private lateinit var repository: BudgetRepository

    @Before
    fun setUp() {
        fakeTxDao = FakeTransactionDao()
        fakeBudgetDao = FakeBudgetDao()
        fakeGoalDao = FakeSavingsGoalDao()
        fakeRecurringDao = FakeRecurringExpenseDao()
        fakeCartDao = FakeSmartCartDao()

        repository = BudgetRepository(
            transactionDao = fakeTxDao,
            budgetDao = fakeBudgetDao,
            savingsGoalDao = fakeGoalDao,
            recurringExpenseDao = fakeRecurringDao,
            smartCartDao = fakeCartDao,
            database = null
        )
    }

    @Test
    fun testCheckoutCreatesTransactionAndUpdatesPantryAndRemovesOneOffItems() = runBlocking {
        // Preparar artículos: 1 despensa agotada y 1 artículo común de una sola vez
        val pantryItem = SmartCartItemEntity(
            id = 1L,
            name = "Frijoles Ducal",
            quantity = 2.0,
            priceSource = "PANTRY_ROJO+CART",
            isChecked = true
        )
        val oneOffItem = SmartCartItemEntity(
            id = 2L,
            name = "Chocolate Artesanal",
            quantity = 1.0,
            priceSource = "MANUAL",
            isChecked = true
        )

        fakeCartDao.insertItem(pantryItem)
        fakeCartDao.insertItem(oneOffItem)

        val success = repository.confirmPurchaseAndUpdatePantry(
            title = "Súper La Torre Quincenal",
            amount = 175.50,
            storeName = "La Torre",
            purchasedItemNames = listOf("Frijoles Ducal", "Chocolate Artesanal")
        )

        assertTrue("El checkout debe ser exitoso", success)

        // 1. Debe haber insertado exactamente una transacción
        assertEquals(1, fakeTxDao.transactions.size)
        val tx = fakeTxDao.transactions[0]
        assertEquals("Súper La Torre Quincenal", tx.title)
        assertEquals(175.50, tx.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("Alimentación & Súper", tx.category)

        // 2. El producto de despensa debe haber pasado a PANTRY_VERDE y quedar desmarcado
        val updatedPantry = fakeCartDao.items.find { it.id == 1L }
        assertNotNull("El producto de despensa debe conservarse", updatedPantry)
        assertEquals("PANTRY_VERDE", updatedPantry?.priceSource)
        assertFalse(updatedPantry!!.isChecked)

        // 3. El producto de una sola vez debe haber sido eliminado del carrito para no quedar huérfano
        val remainingOneOff = fakeCartDao.items.find { it.id == 2L }
        assertNull("El producto no-despensa debe ser eliminado del carrito tras la compra", remainingOneOff)
    }

    @Test
    fun testCheckoutDeduplicationPreventsDoubleRecordingOnRapidTaps() = runBlocking {
        fakeCartDao.insertItem(
            SmartCartItemEntity(id = 10L, name = "Huevos de Granja", priceSource = "PANTRY_AMARILLO", isChecked = true)
        )

        // Primer tap
        val firstResult = repository.confirmPurchaseAndUpdatePantry(
            title = "Compra Walmart",
            amount = 95.00,
            storeName = "Walmart",
            purchasedItemNames = listOf("Huevos de Granja")
        )
        assertTrue("El primer registro debe tener éxito", firstResult)
        assertEquals(1, fakeTxDao.transactions.size)

        // Segundo tap inmediato (mismo monto, tienda y lista dentro de la ventana de protección)
        val duplicateResult = repository.confirmPurchaseAndUpdatePantry(
            title = "Compra Walmart",
            amount = 95.00,
            storeName = "Walmart",
            purchasedItemNames = listOf("Huevos de Granja")
        )
        assertFalse("El segundo tap rápido debe ser rechazado para evitar duplicar el gasto", duplicateResult)
        assertEquals("No debe haber transacciones duplicadas", 1, fakeTxDao.transactions.size)
    }

    @Test
    fun testCheckoutRejectsZeroAmountWithNoItems() = runBlocking {
        val result = repository.confirmPurchaseAndUpdatePantry(
            title = "",
            amount = 0.0,
            storeName = "General",
            purchasedItemNames = emptyList()
        )
        assertFalse("Checkout con monto 0 y lista vacía debe ser rechazado", result)
        assertEquals(0, fakeTxDao.transactions.size)
    }

    @Test
    fun testClearCheckedCartItemsSafelyCleansOneOffAndPreservesPantry() = runBlocking {
        val pantry1 = SmartCartItemEntity(id = 1L, name = "Leche", priceSource = "PANTRY_VERDE", isChecked = true)
        val oneOff1 = SmartCartItemEntity(id = 2L, name = "Baterías AA", priceSource = "General", isChecked = true)
        val uncheckedOneOff = SmartCartItemEntity(id = 3L, name = "Jabón", priceSource = "General", isChecked = false)

        fakeCartDao.insertItem(pantry1)
        fakeCartDao.insertItem(oneOff1)
        fakeCartDao.insertItem(uncheckedOneOff)

        repository.clearCheckedCartItems()

        // pantry1 debe seguir existiendo pero con isChecked = false
        val p = fakeCartDao.items.find { it.id == 1L }
        assertNotNull(p)
        assertFalse(p!!.isChecked)

        // oneOff1 estaba marcado, debe haber sido eliminado
        assertNull(fakeCartDao.items.find { it.id == 2L })

        // uncheckedOneOff no estaba marcado, debe seguir existiendo
        assertNotNull(fakeCartDao.items.find { it.id == 3L })
    }

    @Test
    fun testClearAllDataClearsAllStores() = runBlocking {
        fakeTxDao.insertTransaction(TransactionEntity(title = "Sueldo", amount = 5000.0, type = TransactionType.INCOME, category = "Sueldo", dateMillis = 1000L))
        fakeBudgetDao.setBudget(BudgetLimitEntity(category = "Alimentación", monthlyLimit = 2000.0))
        fakeGoalDao.insertGoal(SavingsGoalEntity(name = "Emergencia", targetAmount = 5000.0))
        fakeRecurringDao.insert(RecurringExpenseEntity(title = "Internet", amount = 300.0, category = "Servicios", frequency = "Mensual"))
        fakeCartDao.insertItem(SmartCartItemEntity(name = "Pan", priceSource = "PANTRY_VERDE"))

        repository.clearAllData()

        assertEquals(0, fakeTxDao.transactions.size)
        assertEquals(0, fakeBudgetDao.budgets.size)
        assertEquals(0, fakeGoalDao.goals.size)
        assertEquals(0, fakeRecurringDao.expenses.size)
        assertEquals(0, fakeCartDao.items.size)
    }

    // =========================================================================
    // IN-MEMORY FAKES PARA PRUEBAS SIN SOBRECARGA DE DISPOSITIVO
    // =========================================================================

    class FakeTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()
        private var nextId = 1L

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions)
        override suspend fun getAllTransactionsList(): List<TransactionEntity> = transactions.toList()
        override fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.dateMillis in startMillis..endMillis })

        override suspend fun insertTransaction(tx: TransactionEntity): Long {
            val id = if (tx.id == 0L) nextId++ else tx.id
            val copy = tx.copy(id = id)
            transactions.removeAll { it.id == id }
            transactions.add(copy)
            return id
        }

        override suspend fun insertAll(txs: List<TransactionEntity>) {
            for (t in txs) insertTransaction(t)
        }

        override suspend fun updateTransaction(tx: TransactionEntity) {
            val idx = transactions.indexOfFirst { it.id == tx.id }
            if (idx != -1) transactions[idx] = tx
        }

        override suspend fun deleteTransaction(tx: TransactionEntity) {
            transactions.removeAll { it.id == tx.id }
        }

        override suspend fun deleteById(id: Long) {
            transactions.removeAll { it.id == id }
        }

        override suspend fun clearAll() {
            transactions.clear()
        }
    }

    class FakeBudgetDao : BudgetDao {
        val budgets = mutableListOf<BudgetLimitEntity>()

        override fun getAllBudgets(): Flow<List<BudgetLimitEntity>> = flowOf(budgets)
        override suspend fun setBudget(budget: BudgetLimitEntity) {
            budgets.removeAll { it.category == budget.category }
            budgets.add(budget)
        }
        override suspend fun insertAll(budgets: List<BudgetLimitEntity>) {
            for (b in budgets) setBudget(b)
        }
        override suspend fun deleteBudget(category: String) {
            budgets.removeAll { it.category == category }
        }
        override suspend fun clearAll() {
            budgets.clear()
        }
    }

    class FakeSavingsGoalDao : SavingsGoalDao {
        val goals = mutableListOf<SavingsGoalEntity>()
        private var nextId = 1L

        override fun getAllGoals(): Flow<List<SavingsGoalEntity>> = flowOf(goals)
        override suspend fun insertGoal(goal: SavingsGoalEntity): Long {
            val id = if (goal.id == 0L) nextId++ else goal.id
            val copy = goal.copy(id = id)
            goals.removeAll { it.id == id }
            goals.add(copy)
            return id
        }
        override suspend fun insertAll(goals: List<SavingsGoalEntity>) {
            for (g in goals) insertGoal(g)
        }
        override suspend fun updateGoal(goal: SavingsGoalEntity) {
            val idx = goals.indexOfFirst { it.id == goal.id }
            if (idx != -1) goals[idx] = goal
        }
        override suspend fun deleteGoal(goal: SavingsGoalEntity) {
            goals.removeAll { it.id == goal.id }
        }
        override suspend fun deleteById(id: Long) {
            goals.removeAll { it.id == id }
        }
        override suspend fun clearAll() {
            goals.clear()
        }
    }

    class FakeRecurringExpenseDao : RecurringExpenseDao {
        val expenses = mutableListOf<RecurringExpenseEntity>()
        private var nextId = 1L

        override fun getAll(): Flow<List<RecurringExpenseEntity>> = flowOf(expenses)
        override suspend fun insert(expense: RecurringExpenseEntity): Long {
            val id = if (expense.id == 0L) nextId++ else expense.id
            val copy = expense.copy(id = id)
            expenses.removeAll { it.id == id }
            expenses.add(copy)
            return id
        }
        override suspend fun insertAll(expenses: List<RecurringExpenseEntity>) {
            for (e in expenses) insert(e)
        }
        override suspend fun update(expense: RecurringExpenseEntity) {
            val idx = expenses.indexOfFirst { it.id == expense.id }
            if (idx != -1) expenses[idx] = expense
        }
        override suspend fun delete(expense: RecurringExpenseEntity) {
            expenses.removeAll { it.id == expense.id }
        }
        override suspend fun deleteById(id: Long) {
            expenses.removeAll { it.id == id }
        }
        override suspend fun clearAll() {
            expenses.clear()
        }
    }

    class FakeSmartCartDao : SmartCartDao {
        val items = mutableListOf<SmartCartItemEntity>()
        private var nextId = 1L

        override fun getAllItems(): Flow<List<SmartCartItemEntity>> = flowOf(items)
        override suspend fun getAllItemsList(): List<SmartCartItemEntity> = items.toList()

        override suspend fun toggleItemChecked(id: Long) {
            val idx = items.indexOfFirst { it.id == id }
            if (idx != -1) items[idx] = items[idx].copy(isChecked = !items[idx].isChecked)
        }

        override suspend fun updatePantryStatus(id: Long, status: String) {
            val idx = items.indexOfFirst { it.id == id }
            if (idx != -1) items[idx] = items[idx].copy(priceSource = status)
        }

        override suspend fun updatePantryStatusToGreenByName(name: String) {
            val clean = name.trim().lowercase()
            for (i in items.indices) {
                if (items[i].name.trim().lowercase() == clean && items[i].priceSource.startsWith("PANTRY_")) {
                    items[i] = items[i].copy(priceSource = "PANTRY_VERDE")
                }
            }
        }

        override suspend fun markItemPurchasedAsGreen(id: Long) {
            val idx = items.indexOfFirst { it.id == id }
            if (idx != -1) items[idx] = items[idx].copy(priceSource = "PANTRY_VERDE", isChecked = false)
        }

        override suspend fun markItemPurchasedAsGreenByName(name: String) {
            val clean = name.trim().lowercase()
            for (i in items.indices) {
                if (items[i].name.trim().lowercase() == clean) {
                    items[i] = items[i].copy(priceSource = "PANTRY_VERDE", isChecked = false)
                }
            }
        }

        override suspend fun incorporateNeedToBuyIntoCart() {
            for (i in items.indices) {
                val s = items[i].priceSource
                if (s.contains("AMARILLO") || s.contains("ROJO") || s == "REPOSICION" || s == "OCR") {
                    items[i] = items[i].copy(isChecked = true)
                }
            }
        }

        override suspend fun uncheckAll() {
            for (i in items.indices) {
                items[i] = items[i].copy(isChecked = false)
            }
        }

        override suspend fun insertItem(item: SmartCartItemEntity): Long {
            val id = if (item.id == 0L) nextId++ else item.id
            val copy = item.copy(id = id)
            items.removeAll { it.id == id }
            items.add(copy)
            return id
        }

        override suspend fun insertAll(items: List<SmartCartItemEntity>) {
            for (it in items) insertItem(it)
        }

        override suspend fun updateItem(item: SmartCartItemEntity) {
            val idx = items.indexOfFirst { it.id == item.id }
            if (idx != -1) items[idx] = item
        }

        override suspend fun deleteItem(item: SmartCartItemEntity) {
            items.removeAll { it.id == item.id }
        }

        override suspend fun deleteCheckedItems() {
            items.removeAll { it.isChecked && !it.priceSource.startsWith("PANTRY_") }
        }

        override suspend fun clearCartItems() {
            items.removeAll { !it.priceSource.startsWith("PANTRY_") }
        }

        override suspend fun clearPantryItems() {
            items.removeAll { it.priceSource.startsWith("PANTRY_") }
        }

        override suspend fun clearAll() {
            items.clear()
        }
    }
}
