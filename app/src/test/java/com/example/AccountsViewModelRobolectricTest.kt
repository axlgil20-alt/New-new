package com.example

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import com.example.data.repository.AccountsRepository
import com.example.data.repository.BudgetRepository
import com.example.ui.viewmodel.AccountsOperationStatus
import com.example.ui.viewmodel.AccountsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountsViewModelRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var accountsRepo: AccountsRepository
    private lateinit var budgetRepo: BudgetRepository
    private lateinit var viewModel: AccountsViewModel
    private var fakeClockMillis: Long = 1_760_000_000_000L

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
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
        viewModel = AccountsViewModel(
            application = app,
            accountsRepository = accountsRepo,
            budgetRepository = budgetRepo,
            clockMillis = { fakeClockMillis },
            duplicateGuardWindowMs = 800L
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun awaitJobAndIdle(job: Job?) {
        if (job != null) {
            runBlocking {
                withTimeout(5_000L) {
                    while (!job.isCompleted) {
                        shadowOf(Looper.getMainLooper()).idle()
                        kotlinx.coroutines.delay(10L)
                    }
                }
            }
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun awaitStateCondition(predicate: () -> Boolean) {
        runBlocking {
            withTimeout(5_000L) {
                while (!predicate()) {
                    shadowOf(Looper.getMainLooper()).idle()
                    kotlinx.coroutines.delay(10L)
                }
            }
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun reactiveUiState_successAndErrorHandling_andDuplicateEventProtection() = runBlocking {
        val collectorJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.uiState.collect {}
        }
        try {
            // 1. Crear cuentas líquidas y tarjeta de crédito vía ViewModel
            awaitJobAndIdle(
                viewModel.createAccount(
                    name = "Banco Monetaria",
                    accountType = AccountType.BANK_CHECKING,
                    openingBalanceMinor = 3_000_00L,
                    isDefault = true
                )
            )
            awaitStateCondition { viewModel.uiState.value.activeAccounts.size == 1 }
            assertTrue(viewModel.uiState.value.operationStatus is AccountsOperationStatus.Success)
            val checkingId = viewModel.uiState.value.activeAccounts.first().id

            awaitJobAndIdle(
                viewModel.createAccount(
                    name = "Efectivo Cartera",
                    accountType = AccountType.CASH,
                    openingBalanceMinor = 500_00L
                )
            )
            awaitJobAndIdle(
                viewModel.createAccount(
                    name = "Visa Cashback",
                    accountType = AccountType.CREDIT_CARD,
                    creditLimitMinor = 8_000_00L,
                    cutoffDay = 15,
                    paymentDueDay = 5
                )
            )
            awaitStateCondition { viewModel.uiState.value.activeAccounts.size == 3 }

            val cashId = viewModel.uiState.value.activeAccounts.first { it.name == "Efectivo Cartera" }.id
            val cardId = viewModel.uiState.value.creditCardAccounts.first().id

            // 2. Doble evento idéntico dentro de la ventana de protección (800 ms) se bloquea
            val job1 = viewModel.transferBetweenAccounts(
                fromAccountId = checkingId,
                toAccountId = cashId,
                amountMinor = 400_00L,
                title = "Retiro cajero"
            )
            awaitJobAndIdle(job1)

            fakeClockMillis += 200L
            val jobDuplicateTap = viewModel.transferBetweenAccounts(
                fromAccountId = checkingId,
                toAccountId = cashId,
                amountMinor = 400_00L,
                title = "Retiro cajero"
            )
            assertNull("Tap repetido rápido debe bloquearse antes de lanzar operación", jobDuplicateTap)

            awaitStateCondition {
                viewModel.uiState.value.snapshotsByAccountId[checkingId]?.currentBalanceMinor == 2_600_00L
            }
            val cashSnap = viewModel.uiState.value.snapshotsByAccountId[cashId]!!
            assertEquals(900_00L, cashSnap.currentBalanceMinor)
            assertEquals(3_500_00L, viewModel.uiState.value.totalLiquidAssetsMinor)

            // 3. Seleccionar cuenta actualiza movimientos específicos de esa cuenta
            viewModel.selectAccount(cashId)
            awaitStateCondition { viewModel.uiState.value.selectedAccountId == cashId }
            assertEquals(1, viewModel.uiState.value.selectedAccountTransactions.size)

            // 4. Error controlado cuando se intenta operación financiera inválida
            awaitJobAndIdle(
                viewModel.transferBetweenAccounts(
                    fromAccountId = checkingId,
                    toAccountId = checkingId,
                    amountMinor = 100_00L
                )
            )
            assertTrue(viewModel.uiState.value.operationStatus is AccountsOperationStatus.Error)
            assertNotNull(viewModel.uiState.value.errorMessage)

            viewModel.clearMessages()
            awaitStateCondition { viewModel.uiState.value.operationStatus == AccountsOperationStatus.Idle }
            assertNull(viewModel.uiState.value.errorMessage)

            // 5. Compra con tarjeta + Visacuotas + abono vía ViewModel actualizan patrimonio neto
            awaitJobAndIdle(
                viewModel.recordCreditCardPurchase(
                    cardAccountId = cardId,
                    title = "Gasolinera Shell",
                    amountMinor = 250_00L,
                    category = "Transporte & Combustible"
                )
            )
            awaitJobAndIdle(
                viewModel.createInstallmentPlan(
                    cardAccountId = cardId,
                    title = "Celular en cuotas",
                    principalTotalMinor = 600_00L,
                    installmentsCount = 3,
                    postFirstInstallmentImmediately = false
                )
            )

            awaitStateCondition { viewModel.uiState.value.totalCreditCardDebtMinor == 850_00L }
            assertEquals(2_650_00L, viewModel.uiState.value.netWorthMinor) // 3500 - 850

            awaitJobAndIdle(
                viewModel.payCreditCard(
                    sourceAccountId = checkingId,
                    cardAccountId = cardId,
                    amountMinor = 250_00L
                )
            )

            awaitStateCondition { viewModel.uiState.value.totalLiquidAssetsMinor == 3_250_00L }
            assertEquals(600_00L, viewModel.uiState.value.totalCreditCardDebtMinor)
            assertEquals(2_650_00L, viewModel.uiState.value.netWorthMinor)

            // Confirmar que ningún pago o transferencia infló gastos reales presupuestarios (solo Q250 de gasolina)
            val budgetExpenseMinor = db.transactionDao().getAllTransactions().first()
                .filter { it.type == TransactionType.EXPENSE && it.entryKind != "TRANSFER_OUT" }
                .sumOf { it.amountMinor }
            assertEquals(250_00L, budgetExpenseMinor)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            collectorJob.cancel()
        }
    }
}
