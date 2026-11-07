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
import java.util.Calendar

class PaymentCommitmentIntegrationTest {

    private lateinit var fakeTxDao: FakeTestTransactionDao
    private lateinit var fakeBudgetDao: FakeTestBudgetDao
    private lateinit var fakeGoalDao: FakeTestSavingsGoalDao
    private lateinit var fakeRecurringDao: FakeTestRecurringExpenseDao
    private lateinit var fakeCartDao: FakeTestSmartCartDao
    private lateinit var fakePaymentDao: FakeTestPaymentCommitmentDao
    private lateinit var fakeBeneficiaryDao: FakeTestBeneficiaryDao
    private lateinit var repository: BudgetRepository

    @Before
    fun setUp() {
        fakeTxDao = FakeTestTransactionDao()
        fakeBudgetDao = FakeTestBudgetDao()
        fakeGoalDao = FakeTestSavingsGoalDao()
        fakeRecurringDao = FakeTestRecurringExpenseDao()
        fakeCartDao = FakeTestSmartCartDao()
        fakePaymentDao = FakeTestPaymentCommitmentDao()
        fakeBeneficiaryDao = FakeTestBeneficiaryDao()

        repository = BudgetRepository(
            transactionDao = fakeTxDao,
            budgetDao = fakeBudgetDao,
            savingsGoalDao = fakeGoalDao,
            recurringExpenseDao = fakeRecurringDao,
            smartCartDao = fakeCartDao,
            paymentCommitmentDao = fakePaymentDao,
            beneficiaryDao = fakeBeneficiaryDao,
            database = null
        )
    }

    @Test
    fun testCreatePaymentCommitmentGeneratesTentativeTransaction() = runBlocking {
        val dueMillis = System.currentTimeMillis() + 86400000L * 3 // en 3 días

        val commitment = PaymentCommitmentEntity(
            title = "Recibo EEGSA",
            beneficiaryName = "EEGSA",
            description = "Luz de la casa",
            amount = 350.0,
            dueDateMillis = dueMillis,
            category = "Vivienda & Servicios",
            frequency = PaymentFrequency.MONTHLY,
            isVariableAmount = true,
            reminderDaysBefore = 3,
            notes = "Contador 12345"
        )

        val commitmentId = repository.createPaymentCommitment(commitment)

        // 1. Verificar compromiso creado
        val retrieved = fakePaymentDao.getCommitmentById(commitmentId)
        assertNotNull(retrieved)
        assertEquals("Recibo EEGSA", retrieved!!.title)
        assertEquals(350.0, retrieved.amount, 0.01)
        assertEquals(PaymentStatus.PENDING, retrieved.status)
        assertFalse(retrieved.isPaid)
        assertTrue(retrieved.linkedTransactionId != null && retrieved.linkedTransactionId!! > 0)

        // 2. Verificar transacción tentativa vinculada en Room
        val linkedTx = fakeTxDao.transactions.find { it.id == retrieved.linkedTransactionId }
        assertNotNull(linkedTx)
        assertEquals(350.0, linkedTx!!.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, linkedTx.type)
        assertEquals("Vivienda & Servicios", linkedTx.category)
        assertTrue("La transacción creada debe ser tentativa para no afectar balance real", linkedTx.isTentative)
        assertEquals("PENDING", linkedTx.tentativeDecision)
        assertFalse("La transacción no debe estar verificada antes de pagarse", linkedTx.isVerified)
    }

    @Test
    fun testMarkPaymentAsPaidConfirmsTransactionAndSetsStatusPaid() = runBlocking {
        val dueMillis = System.currentTimeMillis()
        val commitment = PaymentCommitmentEntity(
            title = "Internet Residencial",
            beneficiaryName = "Claro",
            description = "Fibra óptica 100MB",
            amount = 299.0,
            dueDateMillis = dueMillis,
            category = "Telefonía & Internet",
            frequency = PaymentFrequency.ONCE,
            isVariableAmount = false,
            reminderDaysBefore = 1,
            notes = "Pago con tarjeta"
        )

        val commitmentId = repository.createPaymentCommitment(commitment)
        val commitmentBefore = fakePaymentDao.getCommitmentById(commitmentId)!!
        val txId = commitmentBefore.linkedTransactionId!!

        // Marcar como pagado
        val marked = repository.markPaymentAsPaid(commitmentId)
        assertTrue(marked)

        // 1. Verificar compromiso pagado
        val commitmentAfter = fakePaymentDao.getCommitmentById(commitmentId)!!
        assertEquals(PaymentStatus.PAID, commitmentAfter.status)
        assertTrue(commitmentAfter.isPaid)
        assertNotNull("paidDateMillis debe registrarse", commitmentAfter.paidDateMillis)

        // 2. Verificar que la transacción se confirmó
        val linkedTx = fakeTxDao.transactions.find { it.id == txId }!!
        assertFalse("Al pagarse, la transacción ya no es tentativa", linkedTx.isTentative)
        assertEquals("CONFIRMED", linkedTx.tentativeDecision)
        assertTrue("Al pagarse, se marca como verificada", linkedTx.isVerified)
    }

    @Test
    fun testMarkPaymentAsPaidIsIdempotent() = runBlocking {
        val commitment = PaymentCommitmentEntity(
            title = "Cuota Colegio",
            beneficiaryName = "Colegio Los Cedros",
            description = "Colegiatura mes",
            amount = 800.0,
            dueDateMillis = System.currentTimeMillis(),
            category = "Educación",
            frequency = PaymentFrequency.ONCE,
            isVariableAmount = false,
            reminderDaysBefore = 2,
            notes = ""
        )

        val commitmentId = repository.createPaymentCommitment(commitment)
        val initialTxCount = fakeTxDao.transactions.size

        // Primera llamada
        val firstResult = repository.markPaymentAsPaid(commitmentId)
        assertTrue(firstResult)
        val commitmentAfterFirst = fakePaymentDao.getCommitmentById(commitmentId)!!
        assertEquals(PaymentStatus.PAID, commitmentAfterFirst.status)
        assertEquals(initialTxCount, fakeTxDao.transactions.size)

        val paidDateFirst = commitmentAfterFirst.paidDateMillis

        // Segunda llamada redundante (idempotencia)
        val secondResult = repository.markPaymentAsPaid(commitmentId)
        assertFalse("Llamada repetida debe retornar false y ser ignorada", secondResult)
        val commitmentAfterSecond = fakePaymentDao.getCommitmentById(commitmentId)!!
        assertEquals(PaymentStatus.PAID, commitmentAfterSecond.status)
        assertEquals(paidDateFirst, commitmentAfterSecond.paidDateMillis)
        assertEquals("No se deben haber creado transacciones duplicadas", initialTxCount, fakeTxDao.transactions.size)
    }

    @Test
    fun testRecurringPaymentGeneratesNextCommitmentWhenPaid() = runBlocking {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.MARCH, 15, 10, 0, 0)
        val march15 = cal.timeInMillis

        val commitment = PaymentCommitmentEntity(
            title = "Plan Celular Familiar",
            beneficiaryName = "Tigo",
            description = "Plan mensual",
            amount = 250.0,
            dueDateMillis = march15,
            category = "Telefonía & Internet",
            frequency = PaymentFrequency.MONTHLY,
            isVariableAmount = false,
            reminderDaysBefore = 3,
            notes = "Línea principal"
        )

        val commitmentId = repository.createPaymentCommitment(commitment)

        // Marcar como pagado
        repository.markPaymentAsPaid(commitmentId)

        // 1. El compromiso original debe quedar como PAID
        val original = fakePaymentDao.getCommitmentById(commitmentId)!!
        assertEquals(PaymentStatus.PAID, original.status)
        assertTrue(original.isPaid)
        assertEquals(march15, original.dueDateMillis)

        // 2. Debe existir un segundo compromiso para el siguiente mes (abril 2026)
        val allCommitments = fakePaymentDao.commitments
        assertEquals("Deben existir dos compromisos: el pagado y el próximo generado", 2, allCommitments.size)

        val nextCommitment = allCommitments.find { it.id != commitmentId }!!
        assertEquals("Plan Celular Familiar", nextCommitment.title)
        assertEquals("Tigo", nextCommitment.beneficiaryName)
        assertEquals(250.0, nextCommitment.amount, 0.01)
        assertEquals(PaymentStatus.PENDING, nextCommitment.status)
        assertFalse(nextCommitment.isPaid)
        assertEquals(PaymentFrequency.MONTHLY, nextCommitment.frequency)

        // Verificar que la fecha se avanzó a Abril
        val calNext = Calendar.getInstance()
        calNext.timeInMillis = nextCommitment.dueDateMillis
        assertEquals(Calendar.APRIL, calNext.get(Calendar.MONTH))
        assertEquals(15, calNext.get(Calendar.DAY_OF_MONTH))

        // El nuevo compromiso debe tener su propia transacción tentativa independiente
        assertNotNull(nextCommitment.linkedTransactionId)
        assertNotEquals(original.linkedTransactionId, nextCommitment.linkedTransactionId)
        val nextTx = fakeTxDao.transactions.find { it.id == nextCommitment.linkedTransactionId }!!
        assertTrue(nextTx.isTentative)
        assertEquals("PENDING", nextTx.tentativeDecision)
    }

    @Test
    fun testDeleteUnpaidCommitmentRemovesTentativeTransaction() = runBlocking {
        val commitment = PaymentCommitmentEntity(
            title = "Gasto Proyectado Eventual",
            beneficiaryName = "Servicio Técnico",
            description = "Reparación estufa",
            amount = 450.0,
            dueDateMillis = System.currentTimeMillis() + 86400000L * 5,
            category = "Vivienda & Servicios",
            frequency = PaymentFrequency.ONCE,
            isVariableAmount = false,
            reminderDaysBefore = 1,
            notes = ""
        )

        val commitmentId = repository.createPaymentCommitment(commitment)
        val retrieved = fakePaymentDao.getCommitmentById(commitmentId)!!
        val linkedTxId = retrieved.linkedTransactionId!!
        assertNotNull(fakeTxDao.transactions.find { it.id == linkedTxId })

        // Eliminar compromiso pendiente
        repository.deletePaymentCommitment(retrieved)

        // Debe removerse tanto el compromiso como su transacción tentativa asociada
        assertNull(fakePaymentDao.getCommitmentById(commitmentId))
        assertNull(fakeTxDao.transactions.find { it.id == linkedTxId })
    }

    @Test
    fun testDeletePaidCommitmentPreservesConfirmedTransactionInFinancialHistory() = runBlocking {
        val commitment = PaymentCommitmentEntity(
            title = "Renta de Casa",
            beneficiaryName = "Propietario",
            description = "Renta mensual",
            amount = 2500.0,
            dueDateMillis = System.currentTimeMillis(),
            category = "Vivienda & Servicios",
            frequency = PaymentFrequency.ONCE,
            isVariableAmount = false,
            reminderDaysBefore = 3,
            notes = ""
        )

        val commitmentId = repository.createPaymentCommitment(commitment)
        repository.markPaymentAsPaid(commitmentId)
        val paidCommitment = fakePaymentDao.getCommitmentById(commitmentId)!!
        val linkedTxId = paidCommitment.linkedTransactionId!!

        // Eliminar el compromiso pagado de la agenda de pagos
        repository.deletePaymentCommitment(paidCommitment)

        // El compromiso se elimina de la lista de pagos
        assertNull(fakePaymentDao.getCommitmentById(commitmentId))
        // PERO la transacción confirmada permanece intacta en el registro contable y balance familiar
        val preservedTx = fakeTxDao.transactions.find { it.id == linkedTxId }
        assertNotNull("La transacción real confirmada no debe borrarse al eliminar el registro de pago", preservedTx)
        assertFalse(preservedTx!!.isTentative)
        assertEquals("CONFIRMED", preservedTx.tentativeDecision)
    }

    @Test
    fun testBeneficiaryDefaultSeeding() = runBlocking {
        // Asegurar proveedores predeterminados
        repository.ensureDefaultBeneficiaries()

        val all = fakeBeneficiaryDao.beneficiaries
        assertTrue("Deben precargarse proveedores guatemaltecos comunes", all.isNotEmpty())
        assertNotNull(all.find { it.name == "EEGSA" })
        assertNotNull(all.find { it.name == "Tigo" })
        assertNotNull(all.find { it.name == "Claro" })

        // Insertar un beneficiario personalizado
        val customId = fakeBeneficiaryDao.insertBeneficiary(
            BeneficiaryEntity(
                name = "Colegio Los Pinos",
                category = "Educación",
                defaultDescription = "Colegio de Mateo",
                phoneOrContact = "502 2345-6789",
                notes = "Pago antes del día 5"
            )
        )

        val retrieved = fakeBeneficiaryDao.beneficiaries.find { it.id == customId }
        assertNotNull(retrieved)
        assertEquals("Colegio Los Pinos", retrieved!!.name)
    }

    // =========================================================================
    // IN-MEMORY FAKES PARA PRUEBAS
    // =========================================================================

    class FakeTestPaymentCommitmentDao : PaymentCommitmentDao {
        val commitments = mutableListOf<PaymentCommitmentEntity>()
        private var nextId = 1L

        override fun getAllCommitments(): Flow<List<PaymentCommitmentEntity>> = flowOf(commitments)
        override suspend fun getAllCommitmentsList(): List<PaymentCommitmentEntity> = commitments.toList()
        override suspend fun getCommitmentById(id: Long): PaymentCommitmentEntity? = commitments.find { it.id == id }
        override suspend fun getCommitmentByTransactionId(txId: Long): PaymentCommitmentEntity? =
            commitments.find { it.linkedTransactionId == txId }

        override suspend fun insertCommitment(commitment: PaymentCommitmentEntity): Long {
            val id = if (commitment.id == 0L) nextId++ else commitment.id
            val copy = commitment.copy(id = id)
            commitments.removeAll { it.id == id }
            commitments.add(copy)
            return id
        }

        override suspend fun insertAllCommitments(commitments: List<PaymentCommitmentEntity>) {
            for (c in commitments) insertCommitment(c)
        }

        override suspend fun updateCommitment(commitment: PaymentCommitmentEntity) {
            val idx = commitments.indexOfFirst { it.id == commitment.id }
            if (idx != -1) commitments[idx] = commitment
        }

        override suspend fun deleteCommitment(commitment: PaymentCommitmentEntity) {
            commitments.removeAll { it.id == commitment.id }
        }

        override suspend fun deleteCommitmentById(id: Long) {
            commitments.removeAll { it.id == id }
        }

        override suspend fun clearAll() {
            commitments.clear()
        }
    }

    class FakeTestBeneficiaryDao : BeneficiaryDao {
        val beneficiaries = mutableListOf<BeneficiaryEntity>()
        private var nextId = 1L

        override fun getAllBeneficiaries(): Flow<List<BeneficiaryEntity>> = flowOf(beneficiaries)
        override suspend fun getAllBeneficiariesList(): List<BeneficiaryEntity> = beneficiaries.toList()

        override suspend fun insertBeneficiary(beneficiary: BeneficiaryEntity): Long {
            val id = if (beneficiary.id == 0L) nextId++ else beneficiary.id
            val copy = beneficiary.copy(id = id)
            beneficiaries.removeAll { it.id == id }
            beneficiaries.add(copy)
            return id
        }

        override suspend fun insertAllBeneficiaries(beneficiaries: List<BeneficiaryEntity>) {
            for (b in beneficiaries) insertBeneficiary(b)
        }

        override suspend fun updateBeneficiary(beneficiary: BeneficiaryEntity) {
            val idx = beneficiaries.indexOfFirst { it.id == beneficiary.id }
            if (idx != -1) beneficiaries[idx] = beneficiary
        }

        override suspend fun deleteBeneficiary(beneficiary: BeneficiaryEntity) {
            beneficiaries.removeAll { it.id == beneficiary.id }
        }

        override suspend fun deleteBeneficiaryById(id: Long) {
            beneficiaries.removeAll { it.id == id }
        }

        override suspend fun clearAll() {
            beneficiaries.clear()
        }
    }

    class FakeTestTransactionDao : TransactionDao {
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

        override suspend fun deleteByIds(ids: List<Long>) {
            transactions.removeAll { it.id in ids }
        }

        override suspend fun clearAll() {
            transactions.clear()
        }
    }

    class FakeTestBudgetDao : BudgetDao {
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
        override suspend fun clearAll() { budgets.clear() }
    }

    class FakeTestSavingsGoalDao : SavingsGoalDao {
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
        override suspend fun deleteGoal(goal: SavingsGoalEntity) { goals.removeAll { it.id == goal.id } }
        override suspend fun deleteById(id: Long) { goals.removeAll { it.id == id } }
        override suspend fun clearAll() { goals.clear() }
    }

    class FakeTestRecurringExpenseDao : RecurringExpenseDao {
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
        override suspend fun delete(expense: RecurringExpenseEntity) { expenses.removeAll { it.id == expense.id } }
        override suspend fun deleteById(id: Long) { expenses.removeAll { it.id == id } }
        override suspend fun clearAll() { expenses.clear() }
    }

    class FakeTestSmartCartDao : SmartCartDao {
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
        override suspend fun updatePantryStatusToGreenByName(name: String) {}
        override suspend fun markItemPurchasedAsGreen(id: Long) {}
        override suspend fun markItemPurchasedAsGreenByName(name: String) {}
        override suspend fun incorporateNeedToBuyIntoCart() {}
        override suspend fun uncheckAll() {}
        override suspend fun insertItem(item: SmartCartItemEntity): Long {
            val id = if (item.id == 0L) nextId++ else item.id
            val copy = item.copy(id = id)
            items.removeAll { it.id == id }
            items.add(copy)
            return id
        }
        override suspend fun insertAll(items: List<SmartCartItemEntity>) {}
        override suspend fun updateItem(item: SmartCartItemEntity) {}
        override suspend fun deleteItem(item: SmartCartItemEntity) {}
        override suspend fun deleteCheckedItems() {}
        override suspend fun clearCartItems() {}
        override suspend fun clearPantryItems() {}
        override suspend fun clearAll() { items.clear() }
    }
}
