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
import com.example.ui.components.cardPaymentCommitmentsForAccount
import com.example.ui.components.findCardPaymentCommitment
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

        // Reutilizar la clave con otro monto no puede aparentar que el nuevo payload se ejecutó.
        val conflictingRetry = runCatching {
            accountsRepo.transferBetweenAccounts(
                fromAccountId = checking.id,
                toAccountId = coop.id,
                amountMinor = 751_00L,
                operationKey = "trf_oct_01"
            )
        }
        assertTrue(conflictingRetry.isFailure)
        assertEquals(2, db.transactionDao().getAllTransactionsList().size)

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

        val conflictingPaymentRetry = runCatching {
            accountsRepo.payCreditCard(
                sourceAccountId = checking.id,
                cardAccountId = card.id,
                amountMinor = 351_00L,
                dateMillis = postCutoffRefDate,
                operationKey = "abono_parcial_1"
            )
        }
        assertTrue(conflictingPaymentRetry.isFailure)
        assertEquals(2, db.accountLedgerDao().getTransactionsByTransferGroup(partialPay.transferGroupId).size)

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

    @Test
    fun markPaymentAsPaid_cardStatementWithoutSourceAccount_reducesCardDebtWithoutInflatingBudgetExpenses() = runBlocking {
        val card = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Visa Directa",
                accountType = AccountType.CREDIT_CARD,
                creditLimitMinor = 4_000_00L,
                cutoffDay = 15,
                paymentDueDay = 30
            )
        )

        // Cargo real en tarjeta por Q500.00
        budgetRepo.insertTransaction(
            TransactionEntity(
                title = "Supermercado Visa",
                amount = 500.00,
                type = TransactionType.EXPENSE,
                category = "Alimentación & Súper",
                dateMillis = System.currentTimeMillis(),
                accountId = card.id,
                amountMinor = 500_00L,
                entryKind = TransactionEntryKind.STANDARD
            )
        )
        assertEquals(500_00L, accountsRepo.getAccountSnapshot(card.id).revolvingDebtMinor)

        // Compromiso de estado de cuenta SIN paymentSourceAccountId
        val stmtCommitmentId = db.paymentCommitmentDao().insertCommitment(
            PaymentCommitmentEntity(
                title = "Estado de cuenta Visa Directa",
                beneficiaryName = "Banco Emisor",
                amount = 500.00,
                amountMinor = 500_00L,
                dueDateMillis = System.currentTimeMillis(),
                category = "Finanzas y Tarjetas",
                commitmentKind = CommitmentKind.CREDIT_CARD_STATEMENT,
                creditCardAccountId = card.id,
                paymentSourceAccountId = null
            )
        )

        val marked = budgetRepo.markPaymentAsPaid(stmtCommitmentId)
        assertTrue(marked)

        val snapshotAfter = accountsRepo.getAccountSnapshot(card.id)
        assertEquals(0L, snapshotAfter.revolvingDebtMinor)

        val allTxs = db.transactionDao().getAllTransactionsList()
        val cardPaymentTx = allTxs.firstOrNull { it.entryKind == TransactionEntryKind.CARD_PAYMENT && it.accountId == card.id }
        assertNotNull(cardPaymentTx)
        assertFalse(cardPaymentTx!!.affectsBudgetTotals)
        // Solo debe contar el gasto original de Q500.00 en presupuesto (no duplicado)
        val totalBudgetExpenseMinor = allTxs.filter { it.affectsBudgetTotals && it.type == TransactionType.EXPENSE }
            .sumOf { it.amountMinor }
        assertEquals(500_00L, totalBudgetExpenseMinor)
    }

    @Test
    fun pendingCardStatementDoesNotReduceSourceBalance_untilPaymentIsConfirmed() = runBlocking {
        val checking = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Monetaria de pago",
                accountType = AccountType.BANK_CHECKING,
                openingBalanceMinor = 1_000_00L
            )
        )
        val card = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Visa de pago",
                accountType = AccountType.CREDIT_CARD,
                openingBalanceMinor = 800_00L,
                creditLimitMinor = 5_000_00L
            )
        )
        val commitmentId = budgetRepo.createPaymentCommitment(
            PaymentCommitmentEntity(
                title = "Estado de cuenta Visa",
                beneficiaryName = "Banco emisor",
                amount = 250.00,
                amountMinor = 250_00L,
                dueDateMillis = System.currentTimeMillis(),
                category = "Pago de Tarjeta",
                creditCardAccountId = card.id,
                paymentSourceAccountId = checking.id,
                commitmentKind = CommitmentKind.CREDIT_CARD_STATEMENT
            )
        )

        assertEquals(1_000_00L, accountsRepo.getAccountSnapshot(checking.id).currentBalanceMinor)
        assertEquals(800_00L, accountsRepo.getAccountSnapshot(card.id).revolvingDebtMinor)

        assertTrue(budgetRepo.markPaymentAsPaid(commitmentId))
        assertEquals(750_00L, accountsRepo.getAccountSnapshot(checking.id).currentBalanceMinor)
        assertEquals(550_00L, accountsRepo.getAccountSnapshot(card.id).revolvingDebtMinor)
    }

    @Test
    fun cardPaymentCommitmentSelection_onlyReturnsPendingStatementForSelectedCard() {
        val matching = PaymentCommitmentEntity(
            id = 1L,
            title = "Corte Visa",
            beneficiaryName = "Banco",
            amount = 200.0,
            dueDateMillis = 1L,
            commitmentKind = CommitmentKind.CREDIT_CARD_STATEMENT,
            creditCardAccountId = 10L
        )
        val otherCard = matching.copy(id = 2L, creditCardAccountId = 20L)
        val cancelled = matching.copy(id = 3L, status = PaymentStatus.CANCELLED)
        val paid = matching.copy(id = 4L, isPaid = true)
        val ordinaryBill = matching.copy(id = 5L, commitmentKind = CommitmentKind.BILL)
        val secondMatching = matching.copy(id = 6L, title = "Corte anterior")
        val candidates = listOf(otherCard, cancelled, paid, ordinaryBill, matching, secondMatching)

        assertEquals(
            matching.id,
            findCardPaymentCommitment(candidates, cardAccountId = 10L)?.id
        )
        assertEquals(
            listOf(matching.id, secondMatching.id),
            cardPaymentCommitmentsForAccount(candidates, cardAccountId = 10L).map { it.id }
        )
        assertNull(findCardPaymentCommitment(listOf(matching), cardAccountId = null))
    }

    @Test
    fun partialCardPayment_reducesLinkedStatementCommitmentUntilFinalPayment() = runBlocking {
        val checking = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Cuenta para abonos parciales",
                accountType = AccountType.BANK_CHECKING,
                openingBalanceMinor = 2_000_00L
            )
        )
        val card = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Visa con saldo pendiente",
                accountType = AccountType.CREDIT_CARD,
                openingBalanceMinor = 850_00L,
                creditLimitMinor = 5_000_00L
            )
        )
        val commitmentId = budgetRepo.createPaymentCommitment(
            PaymentCommitmentEntity(
                title = "Estado de cuenta Visa",
                beneficiaryName = "Banco emisor",
                amount = 850.00,
                amountMinor = 850_00L,
                dueDateMillis = System.currentTimeMillis(),
                category = "Pago de Tarjeta",
                creditCardAccountId = card.id,
                paymentSourceAccountId = checking.id,
                commitmentKind = CommitmentKind.CREDIT_CARD_STATEMENT
            )
        )
        val initialCommitment = db.paymentCommitmentDao().getCommitmentById(commitmentId)!!
        val firstPayment = accountsRepo.payCreditCard(
            sourceAccountId = checking.id,
            cardAccountId = card.id,
            amountMinor = 350_00L,
            linkedCommitmentId = commitmentId
        )

        assertNull(firstPayment.settledCommitmentId)
        val remainingCommitment = db.paymentCommitmentDao().getCommitmentById(commitmentId)!!
        assertFalse(remainingCommitment.isPaid)
        assertEquals(PaymentStatus.PENDING, remainingCommitment.status)
        assertEquals(500_00L, remainingCommitment.effectiveAmountMinor)
        assertEquals(checking.id, remainingCommitment.paymentSourceAccountId)
        val remainingProjection = db.accountLedgerDao().getTransactionById(initialCommitment.linkedTransactionId!!)!!
        assertTrue(remainingProjection.isTentative)
        assertEquals(500_00L, remainingProjection.effectiveAmountMinor)
        assertEquals(TransactionEntryKind.TRANSFER_OUT, remainingProjection.entryKind)
        assertEquals(card.id, remainingProjection.counterpartAccountId)
        assertEquals(1_650_00L, accountsRepo.getAccountSnapshot(checking.id).currentBalanceMinor)
        assertEquals(500_00L, accountsRepo.getAccountSnapshot(card.id).revolvingDebtMinor)

        val finalPayment = accountsRepo.payCreditCard(
            sourceAccountId = checking.id,
            cardAccountId = card.id,
            amountMinor = 500_00L,
            linkedCommitmentId = commitmentId
        )
        assertEquals(commitmentId, finalPayment.settledCommitmentId)
        val settledCommitment = db.paymentCommitmentDao().getCommitmentById(commitmentId)!!
        assertTrue(settledCommitment.isPaid)
        assertEquals(PaymentStatus.PAID, settledCommitment.status)
        assertEquals(finalPayment.cardPaymentTransactionId, settledCommitment.linkedTransactionId)
        assertEquals(1_150_00L, accountsRepo.getAccountSnapshot(checking.id).currentBalanceMinor)
        assertEquals(0L, accountsRepo.getAccountSnapshot(card.id).revolvingDebtMinor)
        assertEquals(
            0L,
            db.transactionDao().getAllTransactionsList()
                .filter { it.affectsBudgetTotals && it.type == TransactionType.EXPENSE }
                .sumOf { it.effectiveAmountMinor }
        )
    }

    @Test
    fun duplicateCardPayment_doesNotReportDifferentCommitmentAsSettled() = runBlocking {
        val checking = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Cuenta para reintento",
                accountType = AccountType.BANK_CHECKING,
                openingBalanceMinor = 1_000_00L
            )
        )
        val card = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Tarjeta para reintento",
                accountType = AccountType.CREDIT_CARD,
                openingBalanceMinor = 600_00L,
                creditLimitMinor = 3_000_00L
            )
        )
        fun statement(title: String, amountMinor: Long) = PaymentCommitmentEntity(
            title = title,
            beneficiaryName = "Banco emisor",
            amount = MoneyMinorUnits.toDisplayDouble(amountMinor),
            amountMinor = amountMinor,
            dueDateMillis = System.currentTimeMillis(),
            commitmentKind = CommitmentKind.CREDIT_CARD_STATEMENT,
            creditCardAccountId = card.id,
            paymentSourceAccountId = checking.id
        )
        val originalId = budgetRepo.createPaymentCommitment(statement("Corte actual", 200_00L))
        val unrelatedId = budgetRepo.createPaymentCommitment(statement("Otro compromiso", 100_00L))
        val firstPayment = accountsRepo.payCreditCard(
            sourceAccountId = checking.id,
            cardAccountId = card.id,
            amountMinor = 200_00L,
            linkedCommitmentId = originalId,
            operationKey = "card_retry_consistency"
        )
        assertEquals(originalId, firstPayment.settledCommitmentId)
        val transactionCountAfterFirstPayment = db.transactionDao().getAllTransactionsList().size

        val wrongCommitmentReplay = accountsRepo.payCreditCard(
            sourceAccountId = checking.id,
            cardAccountId = card.id,
            amountMinor = 200_00L,
            linkedCommitmentId = unrelatedId,
            operationKey = "card_retry_consistency"
        )
        assertTrue(wrongCommitmentReplay.wasDeduplicated)
        assertNull(wrongCommitmentReplay.settledCommitmentId)
        assertFalse(db.paymentCommitmentDao().getCommitmentById(unrelatedId)!!.isPaid)
        assertEquals(transactionCountAfterFirstPayment, db.transactionDao().getAllTransactionsList().size)
        assertEquals(800_00L, accountsRepo.getAccountSnapshot(checking.id).currentBalanceMinor)
        assertEquals(400_00L, accountsRepo.getAccountSnapshot(card.id).revolvingDebtMinor)

        val correctCommitmentReplay = accountsRepo.payCreditCard(
            sourceAccountId = checking.id,
            cardAccountId = card.id,
            amountMinor = 200_00L,
            linkedCommitmentId = originalId,
            operationKey = "card_retry_consistency"
        )
        assertEquals(originalId, correctCommitmentReplay.settledCommitmentId)
        assertTrue(correctCommitmentReplay.wasDeduplicated)
    }

    @Test
    fun payingDifferentCard_cannotSettleAnotherCardsStatement() = runBlocking {
        val checking = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Cuenta para prueba cruzada",
                accountType = AccountType.BANK_CHECKING,
                openingBalanceMinor = 2_000_00L
            )
        )
        val statementCard = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Visa vinculada",
                accountType = AccountType.CREDIT_CARD,
                openingBalanceMinor = 500_00L,
                creditLimitMinor = 3_000_00L
            )
        )
        val selectedCard = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Mastercard seleccionada",
                accountType = AccountType.CREDIT_CARD,
                openingBalanceMinor = 300_00L,
                creditLimitMinor = 3_000_00L
            )
        )
        val commitmentId = budgetRepo.createPaymentCommitment(
            PaymentCommitmentEntity(
                title = "Estado de cuenta Visa",
                beneficiaryName = "Banco emisor",
                amount = 200.00,
                amountMinor = 200_00L,
                dueDateMillis = System.currentTimeMillis(),
                commitmentKind = CommitmentKind.CREDIT_CARD_STATEMENT,
                creditCardAccountId = statementCard.id,
                paymentSourceAccountId = checking.id
            )
        )
        val txCountBefore = db.transactionDao().getAllTransactionsList().size

        val rejected = runCatching {
            accountsRepo.payCreditCard(
                sourceAccountId = checking.id,
                cardAccountId = selectedCard.id,
                amountMinor = 100_00L,
                linkedCommitmentId = commitmentId
            )
        }

        assertTrue(rejected.isFailure)
        assertEquals(txCountBefore, db.transactionDao().getAllTransactionsList().size)
        assertEquals(2_000_00L, accountsRepo.getAccountSnapshot(checking.id).currentBalanceMinor)
        assertEquals(300_00L, accountsRepo.getAccountSnapshot(selectedCard.id).revolvingDebtMinor)
        assertEquals(500_00L, accountsRepo.getAccountSnapshot(statementCard.id).revolvingDebtMinor)
        val unchangedCommitment = db.paymentCommitmentDao().getCommitmentById(commitmentId)!!
        assertFalse(unchangedCommitment.isPaid)
        assertEquals(200_00L, unchangedCommitment.effectiveAmountMinor)
    }

    @Test
    fun editingPendingCommitment_updatesEntryKindAndTreatsClearedSourceAsNull() = runBlocking {
        val checking = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Cuenta original",
                accountType = AccountType.BANK_CHECKING,
                openingBalanceMinor = 2_000_00L
            )
        )
        val card = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Tarjeta destino",
                accountType = AccountType.CREDIT_CARD,
                openingBalanceMinor = 500_00L,
                creditLimitMinor = 3_000_00L
            )
        )
        val commitmentId = budgetRepo.createPaymentCommitment(
            PaymentCommitmentEntity(
                title = "Pago programado",
                beneficiaryName = "Comercio",
                amount = 100.00,
                amountMinor = 100_00L,
                dueDateMillis = System.currentTimeMillis(),
                category = "Vivienda & Servicios",
                paymentSourceAccountId = checking.id
            )
        )
        val commitment = db.paymentCommitmentDao().getCommitmentById(commitmentId)!!

        budgetRepo.updatePaymentCommitment(
            commitment.copy(
                commitmentKind = CommitmentKind.CREDIT_CARD_STATEMENT,
                creditCardAccountId = card.id,
                paymentSourceAccountId = null
            )
        )

        val pendingTx = db.transactionDao().getAllTransactionsList()
            .single { it.id == commitment.linkedTransactionId }
        assertEquals(TransactionEntryKind.TRANSFER_OUT, pendingTx.entryKind)
        assertNull(pendingTx.accountId)
        assertEquals(card.id, pendingTx.counterpartAccountId)
        assertEquals(2_000_00L, accountsRepo.getAccountSnapshot(checking.id).currentBalanceMinor)

        assertTrue(budgetRepo.markPaymentAsPaid(commitmentId))
        assertEquals(2_000_00L, accountsRepo.getAccountSnapshot(checking.id).currentBalanceMinor)
        assertEquals(400_00L, accountsRepo.getAccountSnapshot(card.id).revolvingDebtMinor)
        val cardPayment = db.transactionDao().getAllTransactionsList()
            .single { it.accountId == card.id && it.entryKind == TransactionEntryKind.CARD_PAYMENT }
        assertEquals(100_00L, cardPayment.amountMinor)
    }

    @Test
    fun updateTransaction_onLinkedTransferLeg_synchronizesAmountAndDateAcrossBothLegs() = runBlocking {
        val bank = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Cuenta Banco",
                accountType = AccountType.BANK_CHECKING,
                openingBalanceMinor = 2_000_00L
            )
        )
        val cash = accountsRepo.createAccount(
            FinancialAccountEntity(
                name = "Efectivo",
                accountType = AccountType.CASH,
                openingBalanceMinor = 200_00L
            )
        )

        val initialDate = 1_770_000_000_000L
        val transferRes = accountsRepo.transferBetweenAccounts(
            fromAccountId = bank.id,
            toAccountId = cash.id,
            amountMinor = 300_00L,
            dateMillis = initialDate,
            title = "Retiro ATM",
            notes = "Efectivo semanal"
        )

        val legsBefore = db.accountLedgerDao().getTransactionsByTransferGroup(transferRes.transferGroupId)
        assertEquals(2, legsBefore.size)
        val outLeg = legsBefore.first { it.entryKind == TransactionEntryKind.TRANSFER_OUT }

        val updatedDate = initialDate + 86_400_000L
        budgetRepo.updateTransaction(
            outLeg.withExactAmountMinor(450_00L).copy(dateMillis = updatedDate)
        )

        val legsAfter = db.accountLedgerDao().getTransactionsByTransferGroup(transferRes.transferGroupId)
        assertEquals(2, legsAfter.size)
        assertTrue(legsAfter.all { it.amountMinor == 450_00L && it.dateMillis == updatedDate })
        assertEquals(1_550_00L, accountsRepo.getAccountSnapshot(bank.id).currentBalanceMinor)
        assertEquals(650_00L, accountsRepo.getAccountSnapshot(cash.id).currentBalanceMinor)
    }

    private fun millisAt(year: Int, monthIndex: Int, day: Int, hour: Int, tz: TimeZone): Long {
        return Calendar.getInstance(tz).apply {
            clear()
            set(year, monthIndex, day, hour, 0, 0)
        }.timeInMillis
    }
}
