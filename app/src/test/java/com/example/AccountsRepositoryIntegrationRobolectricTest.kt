package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.AccountType
import com.example.data.model.CommitmentKind
import com.example.data.model.CreditReservePolicy
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.InstallmentChargeStatus
import com.example.data.model.InstallmentPlanStatus
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.model.PaymentFrequency
import com.example.data.model.PaymentStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionEntryKind
import com.example.data.model.TransactionType
import com.example.data.model.affectsBudgetTotals
import com.example.data.model.effectiveAmountMinor
import com.example.data.model.withExactAmountMinor
import com.example.data.repository.AccountsRepository
import com.example.data.repository.BudgetRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountsRepositoryIntegrationRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var accountsRepo: AccountsRepository
    private lateinit var budgetRepo: BudgetRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountsRepo = AccountsRepository.fromDatabase(db)
        budgetRepo = BudgetRepository(
            transactionDao = db.transactionDao(),
            budgetDao = db.budgetDao(),
            savingsGoalDao = db.savingsGoalDao(),
            recurringExpenseDao = db.recurringExpenseDao(),
            smartCartDao = db.smartCartDao(),
            paymentCommitmentDao = db.paymentCommitmentDao(),
            beneficiaryDao = db.beneficiaryDao(),
            shoppingTemplateDao = db.shoppingTemplateDao(),
            database = db,
            financialAccountDao = db.financialAccountDao(),
            installmentPlanDao = db.installmentPlanDao(),
            installmentScheduleDao = db.installmentScheduleDao(),
            accountLedgerDao = db.accountLedgerDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun monetaryWritePaths_treatAmountMinorAsPrimaryTruth_andProtectLegacyCopyCallers() = runBlocking {
        val checking = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Banrural Monetaria",
                accountType = AccountType.BANK_CHECKING,
                openingBalanceMinor = 500_000L
            )
        )

        // 1. Inserción desde amountMinor como verdad primaria (incluso si amount venía en 0.0)
        val rawTx = TransactionEntity(
            title = "Supermercado",
            amount = 0.0,
            type = TransactionType.EXPENSE,
            category = "Alimentación & Súper",
            dateMillis = 1_760_000_000_000L,
            accountId = checking.id,
            amountMinor = 199_95L
        )
        val txId = budgetRepo.insertTransaction(rawTx)
        val storedTx = db.accountLedgerDao().getTransactionById(txId)!!
        assertEquals(199_95L, storedTx.effectiveAmountMinor)
        assertEquals(199.95, storedTx.amount, 0.0)

        // 2. Llamador heredado que solo cambia `amount` vía .copy(amount = ...) sin actualizar amountMinor
        val legacyCopiedEdit = storedTx.copy(amount = 249.50)
        budgetRepo.updateTransaction(legacyCopiedEdit)
        val afterLegacyEdit = db.accountLedgerDao().getTransactionById(txId)!!
        assertEquals(249_50L, afterLegacyEdit.effectiveAmountMinor)
        assertEquals(249.50, afterLegacyEdit.amount, 0.0)

        // 3. Llamador moderno que actualiza amountMinor vía withExactAmountMinor
        budgetRepo.updateTransaction(afterLegacyEdit.withExactAmountMinor(310_25L))
        val afterExactEdit = db.accountLedgerDao().getTransactionById(txId)!!
        assertEquals(310_25L, afterExactEdit.effectiveAmountMinor)
        assertEquals(310.25, afterExactEdit.amount, 0.0)

        // 4. Compromiso de pago y actualización heredada .copy(amount = ...) mantienen paridad en compromiso + tx
        val commitmentId = budgetRepo.createPaymentCommitment(
            PaymentCommitmentEntity(
                title = "Luz",
                beneficiaryName = "EEGSA",
                amount = 0.0,
                dueDateMillis = 1_760_100_000_000L,
                amountMinor = 325_40L,
                paymentSourceAccountId = checking.id
            )
        )
        val savedCommitment = db.paymentCommitmentDao().getCommitmentById(commitmentId)!!
        assertEquals(325_40L, savedCommitment.effectiveAmountMinor)
        assertEquals(325.40, savedCommitment.amount, 0.0)

        budgetRepo.updatePaymentCommitment(savedCommitment.copy(amount = 412.85))
        val updatedCommitment = db.paymentCommitmentDao().getCommitmentById(commitmentId)!!
        val updatedLinkedTx = db.accountLedgerDao().getTransactionById(updatedCommitment.linkedTransactionId!!)!!
        assertEquals(412_85L, updatedCommitment.effectiveAmountMinor)
        assertEquals(412.85, updatedCommitment.amount, 0.0)
        assertEquals(412_85L, updatedLinkedTx.effectiveAmountMinor)
        assertEquals(412.85, updatedLinkedTx.amount, 0.0)

        // 5. Importación CSV asignada a cuenta usa redondeo exacto a centavos y deduplica
        val csv = """
            Fecha,Descripción,Monto,Tipo,Categoría
            10/10/2026,Farmacia Cruz Verde,125.125,EXPENSE,Salud & Medicina
        """.trimIndent()
        val import1 = budgetRepo.importFromCsv(csv, targetAccountId = checking.id)
        val import2 = budgetRepo.importFromCsv(csv, targetAccountId = checking.id)
        assertEquals(1, import1.newCount)
        assertEquals(1, import2.duplicateCount)
        val importedTx = db.accountLedgerDao().getTransactionsForAccountList(checking.id)
            .first { it.title == "Farmacia Cruz Verde" }
        assertEquals(125_13L, importedTx.effectiveAmountMinor)
        assertEquals(125.13, importedTx.amount, 0.0)
    }

    @Test
    fun accountsCrudArchivingAndBalanceAdjustments_preserveHistoryAndBudgetTotals() = runBlocking {
        val cash = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Efectivo Casa",
                accountType = AccountType.CASH,
                openingBalanceMinor = 500_00L,
                isDefault = true
            )
        )
        val bank = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Banco Industrial",
                accountType = AccountType.BANK_CHECKING,
                openingBalanceMinor = 2_000_00L,
                isDefault = true // Debe limpiar isDefault de Efectivo Casa
            )
        )

        val reloadedCash = db.financialAccountDao().getAccountById(cash.id)!!
        assertFalse(reloadedCash.isDefault)
        assertTrue(db.financialAccountDao().getAccountById(bank.id)!!.isDefault)

        // Movimiento histórico sin cuenta asignada (`accountId = null`)
        val legacyTxId = budgetRepo.insertTransaction(
            TransactionEntity(
                title = "Compra histórica previa",
                amount = 150.0,
                type = TransactionType.EXPENSE,
                category = "Otros Gastos",
                dateMillis = 1_760_000_000_000L,
                amountMinor = 150_00L,
                accountId = null
            )
        )

        // Gasto real en efectivo antes de archivar
        budgetRepo.insertTransaction(
            TransactionEntity(
                title = "Panadería",
                amount = 50.0,
                type = TransactionType.EXPENSE,
                category = "Alimentación & Súper",
                dateMillis = 1_760_010_000_000L,
                amountMinor = 50_00L,
                accountId = cash.id
            )
        )

        // Archivar Efectivo Casa: conserva movimientos históricos intactos
        accountsRepo.archiveAccount(cash.id)
        val archivedCash = db.financialAccountDao().getAccountById(cash.id)!!
        assertTrue(archivedCash.isArchived)
        assertFalse(archivedCash.isDefault)
        assertEquals(1, db.accountLedgerDao().getTransactionsForAccountList(cash.id).size)

        // Conciliación de saldo en Banco Industrial (2000 -> 2350) sin inflar presupuesto
        val adj = accountsRepo.recordBalanceAdjustment(
            accountId = bank.id,
            targetBalanceOrDebtMinor = 2_350_00L,
            notes = "Arqueo de libreta"
        )
        assertNotNull(adj.transactionId)
        assertEquals(350_00L, adj.deltaMinor)

        val bankSnap = accountsRepo.getAccountSnapshot(bank.id)
        assertEquals(2_350_00L, bankSnap.currentBalanceMinor)

        val adjTx = db.accountLedgerDao().getTransactionById(adj.transactionId!!)!!
        assertEquals(TransactionEntryKind.BALANCE_ADJUSTMENT, adjTx.entryKind)
        assertFalse(adjTx.affectsBudgetTotals)

        // No-op si se vuelve a ajustar al mismo saldo
        val noopAdj = accountsRepo.recordBalanceAdjustment(bank.id, 2_350_00L)
        assertNull(noopAdj.transactionId)

        // Asociar movimiento histórico sin cuenta al banco actualiza portfolio limpiamente
        val beforePortfolio = accountsRepo.getPortfolioSnapshot()
        assertEquals(1, beforePortfolio.summary.unassignedTransactionsCount)
        accountsRepo.assignAccountToTransaction(legacyTxId, bank.id)
        val afterPortfolio = accountsRepo.getPortfolioSnapshot()
        assertEquals(0, afterPortfolio.summary.unassignedTransactionsCount)
        assertEquals(2_200_00L, afterPortfolio.summary.snapshotsByAccountId[bank.id]!!.currentBalanceMinor)
    }

    @Test
    fun atomicTransfers_shiftBalances_preventPartialStates_andExcludeFromBudgetTotals() = runBlocking {
        val checking = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Cuenta Monetaria",
                accountType = AccountType.BANK_CHECKING,
                openingBalanceMinor = 4_000_00L
            )
        )
        val coop = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Cooperativa UPA",
                accountType = AccountType.COOPERATIVE,
                openingBalanceMinor = 1_000_00L
            )
        )
        val archivedSavings = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Ahorro Cerrado",
                accountType = AccountType.BANK_SAVINGS,
                openingBalanceMinor = 100_00L
            )
        )
        accountsRepo.archiveAccount(archivedSavings.id)

        // 1. Transferencia atómica válida con operationKey idempotente
        val res1 = accountsRepo.transferBetweenAccounts(
            fromAccountId = checking.id,
            toAccountId = coop.id,
            amountMinor = 750_00L,
            title = "Aporte mensual cooperativa",
            operationKey = "trf_oct_01"
        )
        assertFalse(res1.wasDeduplicated)

        // 2. Reintento con mismo operationKey no duplica movimientos
        val res2 = accountsRepo.transferBetweenAccounts(
            fromAccountId = checking.id,
            toAccountId = coop.id,
            amountMinor = 750_00L,
            operationKey = "trf_oct_01"
        )
        assertTrue(res2.wasDeduplicated)
        assertEquals(res1.transferGroupId, res2.transferGroupId)

        val portfolio = accountsRepo.getPortfolioSnapshot()
        assertEquals(3_250_00L, portfolio.summary.snapshotsByAccountId[checking.id]!!.currentBalanceMinor)
        assertEquals(1_750_00L, portfolio.summary.snapshotsByAccountId[coop.id]!!.currentBalanceMinor)
        // Activos líquidos totales inalterados (4000 + 1000 = 5000)
        assertEquals(5_000_00L, portfolio.summary.totalLiquidAssetsMinor)

        // Ninguna pata de transferencia afecta totales de presupuesto
        val allTxs = db.transactionDao().getAllTransactionsList()
        assertEquals(2, allTxs.size)
        assertTrue(allTxs.none { it.affectsBudgetTotals })

        // 3. Cuenta archivada rechazada atómicamente (0 movimientos nuevos)
        val failedResult = runCatching {
            accountsRepo.transferBetweenAccounts(
                fromAccountId = checking.id,
                toAccountId = archivedSavings.id,
                amountMinor = 100_00L
            )
        }
        assertTrue(failedResult.isFailure)
        assertEquals(2, db.transactionDao().getAllTransactionsList().size)

        // 4. Eliminar una sola pata vía BudgetRepository elimina el grupo completo sin huérfanos
        budgetRepo.deleteTransactionById(res1.outgoingTransactionId)
        assertEquals(0, db.transactionDao().getAllTransactionsList().size)
    }

    @Test
    fun creditCardLifecycle_purchasesInterestPaymentsAndCycleSettlement_neverDoubleCountExpenses() = runBlocking {
        val checking = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Monetaria BI",
                accountType = AccountType.BANK_CHECKING,
                openingBalanceMinor = 5_000_00L
            )
        )
        val card = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Visa Oro",
                accountType = AccountType.CREDIT_CARD,
                institutionName = "Banco Industrial",
                lastFourDigits = "4321",
                openingBalanceMinor = 0L,
                creditLimitMinor = 10_000_00L,
                cutoffDay = 15,
                paymentDueDay = 5,
                autoCreatePaymentReminder = true
            )
        )

        val tz = TimeZone.getTimeZone("America/Guatemala")
        val purchaseBeforeCutoff = millisAt(2026, Calendar.OCTOBER, 10, 14, tz)
        val feeBeforeCutoff = millisAt(2026, Calendar.OCTOBER, 12, 18, tz)
        val postCutoffRefDate = millisAt(2026, Calendar.OCTOBER, 20, 12, tz)

        // Compra e interés dentro del corte de octubre (cierra 15 oct, vence 5 nov)
        val chargeRes = accountsRepo.recordCreditCardCharge(
            cardAccountId = card.id,
            title = "Supermercado Paiz",
            amountMinor = 800_00L,
            category = "Alimentación & Súper",
            dateMillis = purchaseBeforeCutoff,
            operationKey = "paiz_01"
        )
        assertEquals(9_200_00L, chargeRes.newAvailableCreditMinor)

        accountsRepo.recordCreditCardInterestOrFee(
            cardAccountId = card.id,
            title = "Seguro de protección",
            amountMinor = 50_00L,
            dateMillis = feeBeforeCutoff,
            operationKey = "fee_01"
        )

        // Sincronizar recordatorio del corte cerrado (850.00) separando tarjeta y cuenta fuente
        val synced1 = accountsRepo.syncCreditCardStatementCommitment(
            cardAccountId = card.id,
            paymentSourceAccountId = checking.id,
            referenceDateMillis = postCutoffRefDate,
            timeZone = tz
        )!!
        assertEquals(CommitmentKind.CREDIT_CARD_STATEMENT, synced1.commitmentKind)
        assertEquals(card.id, synced1.creditCardAccountId)
        assertEquals(checking.id, synced1.paymentSourceAccountId)
        assertEquals(850_00L, synced1.effectiveAmountMinor)

        // Re-sincronizar no duplica el recordatorio del mismo ciclo
        val synced2 = accountsRepo.syncCreditCardStatementCommitment(
            cardAccountId = card.id,
            paymentSourceAccountId = checking.id,
            referenceDateMillis = postCutoffRefDate,
            timeZone = tz
        )!!
        assertEquals(synced1.id, synced2.id)
        assertEquals(1, db.paymentCommitmentDao().getAllCommitmentsList().size)

        // Abono parcial de Q350.00 directo desde Monetaria BI
        val partialPay = accountsRepo.payCreditCard(
            sourceAccountId = checking.id,
            cardAccountId = card.id,
            amountMinor = 350_00L,
            dateMillis = postCutoffRefDate,
            operationKey = "abono_parcial_1"
        )
        assertTrue(partialPay.validation.isPartialPayment)
        assertEquals(500_00L, partialPay.validation.resultingRevolvingDebtMinor)

        // Re-sincronizar actualiza el recordatorio restante a Q500.00
        val syncedAfterPartial = accountsRepo.syncCreditCardStatementCommitment(
            cardAccountId = card.id,
            paymentSourceAccountId = checking.id,
            referenceDateMillis = postCutoffRefDate,
            timeZone = tz
        )!!
        assertEquals(500_00L, syncedAfterPartial.effectiveAmountMinor)

        // Liquidar el resto (Q500.00) desde Pagos vía BudgetRepository.markPaymentAsPaid
        val settled = budgetRepo.markPaymentAsPaid(syncedAfterPartial.id)
        assertTrue(settled)
        assertFalse("No debe liquidarse dos veces", budgetRepo.markPaymentAsPaid(syncedAfterPartial.id))

        val finalChecking = accountsRepo.getAccountSnapshot(checking.id)
        val finalCard = accountsRepo.getAccountSnapshot(card.id)
        assertEquals(4_150_00L, finalChecking.currentBalanceMinor) // 5000 - 350 - 500
        assertEquals(0L, finalCard.revolvingDebtMinor)
        assertEquals(10_000_00L, finalCard.availableCreditMinor)

        // En presupuesto: solo los Q850.00 de compra+cargo cuentan como gasto; los Q850 de abonos NO se duplican
        val budgetExpensesMinor = db.transactionDao().getAllTransactionsList()
            .filter { it.affectsBudgetTotals && it.type == TransactionType.EXPENSE }
            .sumOf { it.effectiveAmountMinor }
        assertEquals(850_00L, budgetExpensesMinor)
    }

    @Test
    fun visacuotas_configurableReserveAndIdempotentPosting_preventDuplicatesAndReleaseOnCompletion() = runBlocking {
        val tz = TimeZone.getTimeZone("America/Guatemala")
        val cardFullReserve = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Visa Visacuotas",
                accountType = AccountType.CREDIT_CARD,
                creditLimitMinor = 5_000_00L,
                cutoffDay = 15,
                paymentDueDay = 5,
                installmentReservePolicy = CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL
            )
        )
        val cardBilledOnly = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Cooperativa Extra",
                accountType = AccountType.CREDIT_CARD,
                creditLimitMinor = 5_000_00L,
                cutoffDay = 20,
                paymentDueDay = 5,
                installmentReservePolicy = CreditReservePolicy.RESERVE_BILLED_INSTALLMENTS_ONLY
            )
        )

        val firstChargeDate = millisAt(2026, Calendar.OCTOBER, 5, 10, tz)
        // Plan Visacuotas 0%: Q100.00 en 3 cuotas -> 3333, 3333, 3334
        val planFull = accountsRepo.createInstallmentPlan(
            cardAccountId = cardFullReserve.id,
            title = "Audífonos",
            category = "Otros Gastos",
            principalTotalMinor = 100_00L,
            installmentsCount = 3,
            annualInterestRateBps = 0,
            purchaseDateMillis = firstChargeDate,
            firstChargeDateMillis = firstChargeDate,
            timeZone = tz
        )

        // Al crearse: cupo reservado completo = Q100.00; gasto en presupuesto todavía Q0.00
        val snapBeforePost = accountsRepo.getAccountSnapshot(cardFullReserve.id)
        assertEquals(100_00L, snapBeforePost.reservedInstallmentDebtMinor)
        assertEquals(0L, snapBeforePost.revolvingDebtMinor)
        assertEquals(4_900_00L, snapBeforePost.availableCreditMinor)
        assertEquals(0, db.transactionDao().getAllTransactionsList().size)

        // Contabilizar cuota 1 dos veces -> idempotente, solo 1 transacción en libro mayor
        val post1a = accountsRepo.postInstallmentItem(planFull.plan.id, 1, firstChargeDate)
        val post1b = accountsRepo.postInstallmentItem(planFull.plan.id, 1, firstChargeDate)
        assertFalse(post1a.alreadyPosted)
        assertTrue(post1b.alreadyPosted)
        assertEquals(post1a.transactionId, post1b.transactionId)

        val snapAfterPost1 = accountsRepo.getAccountSnapshot(cardFullReserve.id)
        assertEquals(33_33L, snapAfterPost1.revolvingDebtMinor)
        assertEquals(66_67L, snapAfterPost1.reservedInstallmentDebtMinor)
        assertEquals(100_00L, snapAfterPost1.totalDebtMinor)
        assertEquals(4_900_00L, snapAfterPost1.availableCreditMinor)

        // Contabilizar cuotas vencidas hasta diciembre -> cierra cuotas 2 y 3 y completa el plan
        val dueResults = accountsRepo.postDueInstallments(
            upToMillis = millisAt(2026, Calendar.DECEMBER, 20, 23, tz),
            accountId = cardFullReserve.id
        )
        assertEquals(2, dueResults.size)
        val completedPlan = db.installmentPlanDao().getPlanById(planFull.plan.id)!!
        assertEquals(InstallmentPlanStatus.COMPLETED, completedPlan.status)

        val snapCompleted = accountsRepo.getAccountSnapshot(cardFullReserve.id)
        assertEquals(100_00L, snapCompleted.revolvingDebtMinor)
        assertEquals(0L, snapCompleted.reservedInstallmentDebtMinor)
        assertEquals(3, db.transactionDao().getAllTransactionsList().size)

        // Plan con política RESERVE_BILLED_INSTALLMENTS_ONLY e interés (24% anual) + cancelación anticipada
        val planBilledOnly = accountsRepo.createInstallmentPlan(
            cardAccountId = cardBilledOnly.id,
            title = "Refrigeradora",
            principalTotalMinor = 1_200_00L,
            installmentsCount = 6,
            annualInterestRateBps = 2400,
            purchaseDateMillis = firstChargeDate,
            firstChargeDateMillis = firstChargeDate,
            postFirstInstallmentImmediately = true,
            timeZone = tz
        )
        val billedOnlySnap = accountsRepo.getAccountSnapshot(cardBilledOnly.id)
        assertEquals(0L, billedOnlySnap.reservedInstallmentDebtMinor)
        assertTrue(billedOnlySnap.revolvingDebtMinor > 200_00L) // Capital + interés cuota 1

        val closedEarly = accountsRepo.cancelOrPayOffInstallmentPlan(planBilledOnly.plan.id, paidOffEarly = true)
        assertEquals(InstallmentPlanStatus.COMPLETED, closedEarly.status)
        val itemsAfterEarlyPayoff = db.installmentScheduleDao().getItemsForPlanList(planBilledOnly.plan.id)
        assertEquals(InstallmentChargeStatus.POSTED, itemsAfterEarlyPayoff.first().status)
        assertTrue(itemsAfterEarlyPayoff.drop(1).all { it.status == InstallmentChargeStatus.PAID_OFF_EARLY })
    }

    private fun millisAt(year: Int, monthIndex: Int, day: Int, hour: Int, tz: TimeZone): Long {
        return Calendar.getInstance(tz).apply {
            clear()
            set(year, monthIndex, day, hour, 0, 0)
        }.timeInMillis
    }
}
