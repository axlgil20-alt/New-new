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

/**
 * Pruebas unitarias para la Fase 3:
 * Integración funcional de Mi despensa + Smart Cart.
 *
 * Verifica:
 * 1. Un producto que necesita reposición aparece una sola vez.
 * 2. Repetir la generación de la lista no crea duplicados.
 * 3. Un producto con existencias suficientes (PANTRY_VERDE) no se agrega innecesariamente.
 * 4. Se conservan cantidad, unidad y precio de los productos.
 * 5. Los productos añadidos manualmente no se duplican al ejecutar reposición.
 * 6. La integración no crea transacciones en Room antes de confirmar la compra.
 */
class PantrySmartCartIntegrationTest {

    private lateinit var fakeTxDao: FakeTxDao
    private lateinit var fakeBudgetDao: FakeBgtDao
    private lateinit var fakeGoalDao: FakeGlsDao
    private lateinit var fakeRecurringDao: FakeRecDao
    private lateinit var fakeCartDao: FakeSCartDao
    private lateinit var repository: BudgetRepository

    @Before
    fun setUp() {
        fakeTxDao = FakeTxDao()
        fakeBudgetDao = FakeBgtDao()
        fakeGoalDao = FakeGlsDao()
        fakeRecurringDao = FakeRecDao()
        fakeCartDao = FakeSCartDao()

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
    fun testReplenishmentItemAppearsOnlyOnce() = runBlocking {
        // Despensa con 1 producto en amarillo y 1 en rojo
        fakeCartDao.insertItem(
            SmartCartItemEntity(id = 1L, name = "Huevos de Granja", priceSource = "PANTRY_AMARILLO", quantity = 1.0)
        )
        fakeCartDao.insertItem(
            SmartCartItemEntity(id = 2L, name = "Frijoles Negros", priceSource = "PANTRY_ROJO", quantity = 2.0)
        )

        val added = repository.incorporateNeedToBuyIntoCart()
        assertEquals(2, added)

        val cartList = fakeCartDao.items.filter { it.isInShoppingList() }
        assertEquals(2, cartList.size)

        // Cada producto aparece exactamente una vez
        assertEquals(1, cartList.count { it.name == "Huevos de Granja" })
        assertEquals(1, cartList.count { it.name == "Frijoles Negros" })
        assertEquals(2, fakeCartDao.items.size)
    }

    @Test
    fun testRepeatingReplenishmentDoesNotCreateDuplicates() = runBlocking {
        fakeCartDao.insertItem(
            SmartCartItemEntity(id = 1L, name = "Aceite Mazola", priceSource = "PANTRY_ROJO", quantity = 1.0)
        )

        // Primera ejecución
        val firstRun = repository.incorporateNeedToBuyIntoCart()
        assertEquals(1, firstRun)
        assertEquals(1, fakeCartDao.items.filter { it.isInShoppingList() }.size)

        // Segunda y tercera ejecución repetida
        val secondRun = repository.incorporateNeedToBuyIntoCart()
        val thirdRun = repository.incorporateNeedToBuyIntoCart()

        assertEquals(0, secondRun)
        assertEquals(0, thirdRun)

        // La cantidad total en base de datos y en lista sigue siendo 1 sin duplicados
        assertEquals(1, fakeCartDao.items.size)
        assertEquals(1, fakeCartDao.items.filter { it.isInShoppingList() }.size)
    }

    @Test
    fun testSufficientStockProductNotAdded() = runBlocking {
        // Producto con stock verde (suficiente)
        fakeCartDao.insertItem(
            SmartCartItemEntity(id = 10L, name = "Azúcar Caña Real", priceSource = "PANTRY_VERDE", quantity = 2.0)
        )

        val added = repository.incorporateNeedToBuyIntoCart()
        assertEquals(0, added)

        // No debe estar en la lista de compras
        val cartList = fakeCartDao.items.filter { it.isInShoppingList() }
        assertTrue("El producto con stock suficiente no debe agregarse al carrito", cartList.isEmpty())
        assertFalse(fakeCartDao.items[0].isInShoppingList())
    }

    @Test
    fun testQuantityUnitAndPricePreserved() = runBlocking {
        fakeCartDao.insertItem(
            SmartCartItemEntity(
                id = 5L,
                name = "Leche Trebolac",
                quantity = 3.5,
                unit = "Litro",
                estimatedUnitPrice = 18.75,
                storeName = "La Torre",
                priceSource = "PANTRY_ROJO"
            )
        )

        val added = repository.incorporateNeedToBuyIntoCart()
        assertEquals(1, added)

        val inCart = fakeCartDao.items.find { it.id == 5L }
        assertNotNull(inCart)
        assertEquals(3.5, inCart!!.quantity, 0.001)
        assertEquals("Litro", inCart.unit)
        assertEquals(18.75, inCart.estimatedUnitPrice, 0.001)
        assertEquals("La Torre", inCart.storeName)
        assertTrue(inCart.isInShoppingList())
        assertEquals("PANTRY_ROJO+CART", inCart.priceSource)
    }

    @Test
    fun testManuallyAddedProductNotDuplicatedWhenReplenishing() = runBlocking {
        // 1. Producto en despensa permanente (por terminarse)
        fakeCartDao.insertItem(
            SmartCartItemEntity(
                id = 1L,
                name = "Frijoles Ducal",
                brand = "Ducal",
                quantity = 1.0,
                unit = "Bolsa",
                estimatedUnitPrice = 10.0,
                priceSource = "PANTRY_AMARILLO"
            )
        )

        // 2. Usuario ya agregó manualmente "Frijoles Ducal" con precio actualizado y más cantidad
        fakeCartDao.insertItem(
            SmartCartItemEntity(
                id = 2L,
                name = "Frijoles Ducal",
                brand = "Ducal",
                quantity = 3.0,
                unit = "Bolsa",
                estimatedUnitPrice = 12.50,
                priceSource = "MANUAL",
                storeName = "Paiz",
                isChecked = true
            )
        )

        assertEquals(2, fakeCartDao.items.size)

        // 3. Generar reposición
        val count = repository.incorporateNeedToBuyIntoCart()
        assertEquals(1, count)

        // Debe haber exactamente 1 producto en la base de datos (se fusionaron para evitar duplicados)
        assertEquals(1, fakeCartDao.items.size)

        val merged = fakeCartDao.items[0]
        assertEquals("Frijoles Ducal", merged.name)
        assertEquals("PANTRY_AMARILLO+CART", merged.priceSource)
        // Se preserva la cantidad mayor especificada por el usuario
        assertEquals(3.0, merged.quantity, 0.001)
        // Se preserva el precio manual ingresado
        assertEquals(12.50, merged.estimatedUnitPrice, 0.001)
        assertEquals("Paiz", merged.storeName)
        assertTrue(merged.isChecked)
    }

    @Test
    fun testIntegrationCreatesNoTransactionsBeforeCheckout() = runBlocking {
        fakeCartDao.insertItem(SmartCartItemEntity(id = 1L, name = "Pan Bimbo", priceSource = "PANTRY_ROJO"))
        fakeCartDao.insertItem(SmartCartItemEntity(id = 2L, name = "Café León", priceSource = "PANTRY_AMARILLO"))

        // Generar lista de reposición
        repository.incorporateNeedToBuyIntoCart()

        // Cambiar estados de despensa
        repository.updatePantryStatus(1L, "PANTRY_VERDE")

        // Marcar y desmarcar
        repository.toggleCartItemChecked(2L)

        // Ninguna de estas operaciones previas al checkout debe haber creado transacciones de gasto
        assertEquals("No debe haber transacciones antes de confirmar la compra", 0, fakeTxDao.transactions.size)
    }

    // =========================================================================
    // IMPLEMENTACIONES FAKE DE ACCESO A DATOS PARA PRUEBAS RÁPIDAS EN JVM
    // =========================================================================

    class FakeTxDao : TransactionDao {
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
        override suspend fun insertAll(txs: List<TransactionEntity>) { transactions.addAll(txs) }
        override suspend fun updateTransaction(tx: TransactionEntity) {}
        override suspend fun deleteTransaction(tx: TransactionEntity) { transactions.remove(tx) }
        override suspend fun deleteById(id: Long) { transactions.removeAll { it.id == id } }
        override suspend fun deleteByIds(ids: List<Long>) { transactions.removeAll { it.id in ids } }
        override suspend fun clearAll() { transactions.clear() }
    }

    class FakeBgtDao : BudgetDao {
        override fun getAllBudgets(): Flow<List<BudgetLimitEntity>> = flowOf(emptyList())
        override suspend fun setBudget(budget: BudgetLimitEntity) {}
        override suspend fun insertAll(budgets: List<BudgetLimitEntity>) {}
        override suspend fun deleteBudget(category: String) {}
        override suspend fun clearAll() {}
    }

    class FakeGlsDao : SavingsGoalDao {
        override fun getAllGoals(): Flow<List<SavingsGoalEntity>> = flowOf(emptyList())
        override suspend fun insertGoal(goal: SavingsGoalEntity): Long = 1L
        override suspend fun insertAll(goals: List<SavingsGoalEntity>) {}
        override suspend fun updateGoal(goal: SavingsGoalEntity) {}
        override suspend fun deleteGoal(goal: SavingsGoalEntity) {}
        override suspend fun deleteById(id: Long) {}
        override suspend fun clearAll() {}
    }

    class FakeRecDao : RecurringExpenseDao {
        override fun getAll(): Flow<List<RecurringExpenseEntity>> = flowOf(emptyList())
        override suspend fun insert(expense: RecurringExpenseEntity): Long = 1L
        override suspend fun insertAll(expenses: List<RecurringExpenseEntity>) {}
        override suspend fun update(expense: RecurringExpenseEntity) {}
        override suspend fun delete(expense: RecurringExpenseEntity) {}
        override suspend fun deleteById(id: Long) {}
        override suspend fun clearAll() {}
    }

    class FakeSCartDao : SmartCartDao {
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
                if (items[i].name.trim().lowercase() == clean) {
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
                if (s.contains("AMARILLO") || s.contains("ROJO") || s == "REPOSICION") {
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
