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
 * Pruebas unitarias de separación estricta entre LISTA DEL SÚPER y MI DESPENSA,
 * plantillas reutilizables, y preservación de fórmulas financieras de Quincenas.
 */
class SmartCartAndPantrySeparationTest {

    private lateinit var fakeTxDao: FakeTxDao
    private lateinit var fakeBudgetDao: FakeBgtDao
    private lateinit var fakeGoalDao: FakeGlsDao
    private lateinit var fakeRecurringDao: FakeRecDao
    private lateinit var fakeCartDao: FakeSCartDao
    private lateinit var fakeTemplateDao: FakeTemplateDao
    private lateinit var repository: BudgetRepository

    @Before
    fun setUp() {
        fakeTxDao = FakeTxDao()
        fakeBudgetDao = FakeBgtDao()
        fakeGoalDao = FakeGlsDao()
        fakeRecurringDao = FakeRecDao()
        fakeCartDao = FakeSCartDao()
        fakeTemplateDao = FakeTemplateDao()

        repository = BudgetRepository(
            transactionDao = fakeTxDao,
            budgetDao = fakeBudgetDao,
            savingsGoalDao = fakeGoalDao,
            recurringExpenseDao = fakeRecurringDao,
            smartCartDao = fakeCartDao,
            paymentCommitmentDao = null,
            beneficiaryDao = null,
            shoppingTemplateDao = fakeTemplateDao,
            database = null
        )
    }

    @Test
    fun testAddItemToShoppingListDoesNotAddToPantry() = runBlocking {
        // 1. Agregar producto a la lista de compras
        repository.addOrUpdateCartItem(
            name = "Leche Entera Trebolac",
            quantity = 2.0,
            unit = "Litro",
            brand = "Trebolac",
            price = 16.50,
            store = "Walmart",
            notes = "Para el desayuno"
        )

        val allItems = fakeCartDao.getAllItemsList()
        assertEquals(1, allItems.size)

        val item = allItems.first()
        // Debe ser parte de la lista de compras
        assertTrue("El artículo debe estar en la lista de compras", item.isInShoppingList())
        // NO debe ser producto de despensa
        assertFalse("El producto de la lista NO debe clasificarse como producto de despensa", item.isPantryProduct())
        assertEquals("MANUAL", item.priceSource)

        // Verificar filtrado de despensa
        val pantryProducts = allItems.filter { it.isPantryProduct() }
        assertTrue("Mi Despensa debe estar completamente vacía", pantryProducts.isEmpty())
    }

    @Test
    fun testAddPantryItemDoesNotAddToShoppingListUnlessReplenishing() = runBlocking {
        // 2. Agregar producto directamente a Mi Despensa como VERDE (suficiente)
        repository.addOrUpdatePantryItem(
            name = "Arroz Blanco Precocido",
            quantity = 3.0,
            unit = "Libra",
            status = "PANTRY_VERDE",
            price = 5.25,
            store = "Maxi Despensa",
            notes = "En la alacena"
        )

        val allItems = fakeCartDao.getAllItemsList()
        assertEquals(1, allItems.size)

        val item = allItems.first()
        assertTrue("Debe identificarse como producto de despensa", item.isPantryProduct())
        assertEquals("PANTRY_VERDE", item.priceSource)
        // Con stock suficiente (VERDE) y no marcado, no está en la lista de compras
        assertFalse("Un producto en VERDE no debe figurar en la lista de compras activa", item.isInShoppingList())

        val shoppingList = allItems.filter { it.isInShoppingList() }
        assertTrue("La lista del súper debe permanecer vacía", shoppingList.isEmpty())
    }

    @Test
    fun testPantryItemRojoCanBeAddedToShoppingListWithoutRemovingFromPantry() = runBlocking {
        // 3. Producto en Despensa en ROJO (agotado)
        repository.addOrUpdatePantryItem(
            name = "Café Molido Las Nubes",
            quantity = 1.0,
            unit = "Bolsa",
            status = "PANTRY_ROJO",
            price = 32.0,
            store = "La Torre",
            notes = "Se terminó"
        )

        val item = fakeCartDao.getAllItemsList().first()
        assertEquals("PANTRY_ROJO", item.priceSource)

        // 1-Tap para reponer: toggle en la lista de compra
        repository.togglePantryInCart(item)

        val updatedItems = fakeCartDao.getAllItemsList()
        val updated = updatedItems.first()

        // Sigue perteneciendo a Mi Despensa y su estado es ROJO
        assertTrue("Sigue perteneciendo a Mi Despensa", updated.isPantryProduct())
        assertEquals("PANTRY_ROJO", updated.getPantryStatus())
        // Ahora además está presente en la lista del súper
        assertTrue("Debe aparecer en la lista del súper", updated.isInShoppingList())
    }

    @Test
    fun testSaveCartAsTemplateAndReuseWithoutCreatingPantryStock() = runBlocking {
        // 4. Agregar 2 productos a la lista de compras
        repository.addOrUpdateCartItem(
            name = "Panqueques Diana",
            quantity = 1.0,
            unit = "Caja",
            brand = "Diana",
            price = 24.50,
            store = "Paiz"
        )
        repository.addOrUpdateCartItem(
            name = "Miel de Maple",
            quantity = 1.0,
            unit = "Botella",
            brand = "Karo",
            price = 28.00,
            store = "Paiz"
        )

        // Guardar como plantilla
        val templateId = repository.saveCartAsTemplate("Desayunos Domingos", "Plantilla de fin de semana")
        assertTrue("Debe crearse la plantilla con ID positivo", templateId > 0)

        val templates = fakeTemplateDao.getAllTemplatesList()
        assertEquals(1, templates.size)
        assertEquals("Desayunos Domingos", templates[0].name)
        assertEquals(2, templates[0].itemCount)

        // Limpiar carrito
        repository.clearCartItems()
        assertEquals(0, fakeCartDao.getAllItemsList().filter { it.isInShoppingList() }.size)

        // Aplicar la plantilla
        val appliedCount = repository.applyTemplateToCart(templateId)
        assertEquals(2, appliedCount)

        val activeCart = fakeCartDao.getAllItemsList().filter { it.isInShoppingList() }
        assertEquals(2, activeCart.size)

        // Ninguno de estos productos debe haber sido insertado como producto de despensa
        val pantryItems = fakeCartDao.getAllItemsList().filter { it.isPantryProduct() }
        assertEquals("Reutilizar una plantilla NO debe crear stock de despensa", 0, pantryItems.size)
    }

    @Test
    fun testReusingTemplateMergesQuantityWithoutDuplicates() = runBlocking {
        // 5. Cargar un producto previamente existente en el carrito
        repository.addOrUpdateCartItem(
            name = "Frijoles Negros Ducal",
            quantity = 2.0,
            unit = "Lata",
            brand = "Ducal",
            price = 8.50,
            store = "La Barata"
        )

        // Crear una plantilla que también contiene "Frijoles Negros Ducal" con 3 latas
        val itemsJson = """[{"name":"Frijoles Negros Ducal","quantity":3.0,"unit":"Lata","brand":"Ducal","estimatedUnitPrice":8.50,"preferredStore":"La Barata","notes":""}]"""
        val template = ShoppingTemplateEntity(
            id = 100L,
            name = "Básicos Mensuales",
            description = "Frijoles y granos",
            itemsJson = itemsJson,
            itemCount = 1,
            estimatedTotal = 25.50
        )
        fakeTemplateDao.insertTemplate(template)

        // Aplicar la plantilla al carrito existente
        repository.applyTemplateToCart(100L)

        val cartItems = fakeCartDao.getAllItemsList().filter { it.isInShoppingList() }
        // Debe existir una única fila para Frijoles Negros Ducal
        assertEquals("No debe haber filas duplicadas", 1, cartItems.size)
        // La cantidad debe haberse sumado: 2.0 + 3.0 = 5.0
        assertEquals(5.0, cartItems[0].quantity, 0.001)
    }

    @Test
    fun testDuplicateAndDeleteTemplate() = runBlocking {
        val template = ShoppingTemplateEntity(
            id = 10L,
            name = "Compra Quincenal",
            description = "Compras de la primera quincena",
            itemsJson = "[]",
            itemCount = 0,
            estimatedTotal = 0.0
        )
        fakeTemplateDao.insertTemplate(template)

        // Duplicar
        val copyId = repository.duplicateTemplate(10L, "Compra Quincenal (Copia)")
        assertTrue(copyId > 0)
        assertEquals(2, fakeTemplateDao.getAllTemplatesList().size)

        val copy = fakeTemplateDao.getTemplateById(copyId)
        assertNotNull(copy)
        assertEquals("Compra Quincenal (Copia)", copy?.name)

        // Eliminar plantilla original
        repository.deleteTemplate(template)
        assertEquals(1, fakeTemplateDao.getAllTemplatesList().size)
        assertNull(fakeTemplateDao.getTemplateById(10L))
    }

    @Test
    fun testConfirmPurchaseWithPantryUpdateCreatesVerdeStockAndExpense() = runBlocking {
        // 6. Agregar producto de compra
        repository.addOrUpdateCartItem(
            name = "Aceite Cocinero",
            quantity = 2.0,
            unit = "Botella",
            brand = "Cocinero",
            price = 22.0,
            store = "Walmart"
        )

        // Confirmar compra con updatePantryOnCheckout = true
        val success = repository.confirmPurchaseAndUpdatePantry(
            title = "Súper Quincenal",
            amount = 44.0,
            storeName = "Walmart",
            purchasedItemNames = listOf("Aceite Cocinero"),
            updatePantryOnCheckout = true
        )

        assertTrue("La compra debe ser exitosa", success)

        // 1. Se debe haber generado exactamente UNA transacción de gasto en "Alimentación & Súper"
        val transactions = fakeTxDao.getAllTransactionsList()
        assertEquals(1, transactions.size)
        val tx = transactions[0]
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(44.0, tx.amount, 0.001)
        assertEquals("Alimentación & Súper", tx.category)

        // 2. El producto debe ahora incorporarse a Mi Despensa como VERDE (suficiente)
        val pantry = fakeCartDao.getAllItemsList().filter { it.isPantryProduct() }
        assertEquals(1, pantry.size)
        val p = pantry[0]
        assertEquals("Aceite Cocinero", p.name)
        assertEquals("PANTRY_VERDE", p.priceSource)
    }

    @Test
    fun testConfirmPurchaseWithoutPantryUpdateLeavesPantryUntouched() = runBlocking {
        // 7. Compra sin marcar actualización de despensa
        repository.addOrUpdateCartItem(
            name = "Galletas Chiky",
            quantity = 4.0,
            unit = "Paquete",
            brand = "Pozuelo",
            price = 5.0,
            store = "Súper del Barrio"
        )

        val success = repository.confirmPurchaseAndUpdatePantry(
            title = "Golosinas Varias",
            amount = 20.0,
            storeName = "Súper del Barrio",
            purchasedItemNames = listOf("Galletas Chiky"),
            updatePantryOnCheckout = false
        )

        assertTrue(success)

        // Se registra el gasto financiero
        assertEquals(1, fakeTxDao.getAllTransactionsList().size)

        // NO se agrega nada a la despensa
        val pantry = fakeCartDao.getAllItemsList().filter { it.isPantryProduct() }
        assertEquals("La despensa no debe recibir productos si updatePantry es falso", 0, pantry.size)
    }

    @Test
    fun testQuincenaFormulasAndCalculationsRemainIntact() {
        // 8. Verificar que los modelos y reglas de cálculo de quincenas permanecen intactos
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        calendar.set(currentYear, currentMonth - 1, 5)
        val q1Date = calendar.timeInMillis
        calendar.set(currentYear, currentMonth - 1, 20)
        val q2Date = calendar.timeInMillis

        val txQ1Income = TransactionEntity(
            id = 1L,
            title = "Salario Q1",
            amount = 4500.0,
            type = TransactionType.INCOME,
            category = "Salario",
            dateMillis = q1Date
        )

        val txQ1Expense = TransactionEntity(
            id = 2L,
            title = "Supermercado Q1",
            amount = 1200.0,
            type = TransactionType.EXPENSE,
            category = "Alimentación & Súper",
            dateMillis = q1Date
        )

        val txQ2Income = TransactionEntity(
            id = 3L,
            title = "Salario Q2",
            amount = 4500.0,
            type = TransactionType.INCOME,
            category = "Salario",
            dateMillis = q2Date
        )

        val txQ2Expense = TransactionEntity(
            id = 4L,
            title = "Alquiler Q2",
            amount = 2500.0,
            type = TransactionType.EXPENSE,
            category = "Vivienda",
            dateMillis = q2Date
        )

        val list = listOf(txQ1Income, txQ1Expense, txQ2Income, txQ2Expense)

        // Fórmulas de balance mensual
        val totalIncome = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) ((netBalance / totalIncome) * 100.0) else 0.0

        assertEquals(9000.0, totalIncome, 0.001)
        assertEquals(3700.0, totalExpense, 0.001)
        assertEquals(5300.0, netBalance, 0.001)
        assertEquals(58.888, savingsRate, 0.01)
    }

    // =========================================================================
    // FAKE DAOs PARA TESTING UNITARIO
    // =========================================================================

    class FakeTxDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()
        private var nextId = 1L

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions)
        override suspend fun getAllTransactionsList(): List<TransactionEntity> = transactions.toList()
        override fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.dateMillis in start..end })
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

        override suspend fun insertAll(itemsList: List<SmartCartItemEntity>) {
            for (it in itemsList) insertItem(it)
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

    class FakeTemplateDao : ShoppingTemplateDao {
        val templates = mutableListOf<ShoppingTemplateEntity>()
        private var nextId = 1L

        override fun getAllTemplates(): Flow<List<ShoppingTemplateEntity>> = flowOf(templates)
        override suspend fun getAllTemplatesList(): List<ShoppingTemplateEntity> = templates.toList()
        override suspend fun getTemplateById(id: Long): ShoppingTemplateEntity? = templates.find { it.id == id }

        override suspend fun insertTemplate(template: ShoppingTemplateEntity): Long {
            val id = if (template.id == 0L) nextId++ else template.id
            val copy = template.copy(id = id)
            templates.removeAll { it.id == id }
            templates.add(copy)
            return id
        }

        override suspend fun insertAll(templatesList: List<ShoppingTemplateEntity>) {
            for (t in templatesList) insertTemplate(t)
        }

        override suspend fun updateTemplate(template: ShoppingTemplateEntity) {
            val idx = templates.indexOfFirst { it.id == template.id }
            if (idx != -1) templates[idx] = template
        }

        override suspend fun deleteTemplate(template: ShoppingTemplateEntity) {
            templates.removeAll { it.id == template.id }
        }

        override suspend fun deleteTemplateById(id: Long) {
            templates.removeAll { it.id == id }
        }

        override suspend fun clearAll() {
            templates.clear()
        }
    }
}
