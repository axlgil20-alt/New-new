package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AccountType
import com.example.data.model.CreditReservePolicy
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.ImportSummary
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.InstallmentScheduleItemEntity
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.TransactionEntity
import com.example.data.repository.AccountsPortfolioState
import com.example.data.repository.AccountsRepository
import com.example.data.repository.BudgetRepository
import com.example.data.service.AccountBalanceSnapshot
import com.example.data.service.CreditCardStatementSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections

sealed class AccountsOperationStatus {
    data object Idle : AccountsOperationStatus()
    data class Loading(val actionLabel: String = "Procesando operación...") : AccountsOperationStatus()
    data class Success(val message: String, val timestampMillis: Long = System.currentTimeMillis()) : AccountsOperationStatus()
    data class Error(val message: String, val timestampMillis: Long = System.currentTimeMillis()) : AccountsOperationStatus()
}

data class AccountsUiState(
    val isLoading: Boolean = true,
    val operationStatus: AccountsOperationStatus = AccountsOperationStatus.Idle,
    val isOperationInProgress: Boolean = false,
    val allAccounts: List<FinancialAccountEntity> = emptyList(),
    val activeAccounts: List<FinancialAccountEntity> = emptyList(),
    val archivedAccounts: List<FinancialAccountEntity> = emptyList(),
    val liquidAccounts: List<FinancialAccountEntity> = emptyList(),
    val creditCardAccounts: List<FinancialAccountEntity> = emptyList(),
    val snapshotsByAccountId: Map<Long, AccountBalanceSnapshot> = emptyMap(),
    val cardStatementsByAccountId: Map<Long, CreditCardStatementSummary> = emptyMap(),
    val installmentPlans: List<InstallmentPlanEntity> = emptyList(),
    val scheduleItemsByPlanId: Map<Long, List<InstallmentScheduleItemEntity>> = emptyMap(),
    val selectedAccountId: Long? = null,
    val selectedAccount: FinancialAccountEntity? = null,
    val selectedAccountSnapshot: AccountBalanceSnapshot? = null,
    val selectedAccountStatement: CreditCardStatementSummary? = null,
    val selectedAccountTransactions: List<TransactionEntity> = emptyList(),
    val unassignedTransactions: List<TransactionEntity> = emptyList(),
    val unassignedTransactionsCount: Int = 0,
    val unassignedNetImpactMinor: Long = 0L,
    val totalLiquidAssetsMinor: Long = 0L,
    val totalCreditCardDebtMinor: Long = 0L,
    val netWorthMinor: Long = 0L,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel dedicado del módulo MIS CUENTAS.
 *
 * Mantiene la lógica financiera desacoplada de la capa visual, expone estados reactivos
 * (`Loading`, `Success`, `Error`) y blinda contra eventos duplicados en curso o ráfagas rápidas.
 */
class AccountsViewModel(
    application: Application,
    private val accountsRepository: AccountsRepository,
    private val budgetRepository: BudgetRepository? = null,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val duplicateGuardWindowMs: Long = 800L
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        accountsRepository = AccountsRepository.fromDatabase(AppDatabase.getDatabase(application)),
        budgetRepository = AppDatabase.getDatabase(application).let { db ->
            BudgetRepository(
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
    )

    private val selectedAccountIdFlow = MutableStateFlow<Long?>(null)
    private val operationStatusFlow = MutableStateFlow<AccountsOperationStatus>(AccountsOperationStatus.Idle)

    private val inFlightOperationKeys: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
    private val recentOperationTimestamps: MutableMap<String, Long> = Collections.synchronizedMap(mutableMapOf())

    val uiState: StateFlow<AccountsUiState> = combine(
        selectedAccountIdFlow,
        accountsRepository.observePortfolio(referenceDateMillisProvider = clockMillis),
        operationStatusFlow
    ) { selectedId, portfolio, opStatus ->
        buildUiState(
            selectedId = selectedId,
            portfolio = portfolio,
            opStatus = opStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsUiState(isLoading = true)
    )

    fun selectAccount(accountId: Long?) {
        selectedAccountIdFlow.value = accountId
    }

    fun clearMessages() {
        val current = operationStatusFlow.value
        if (current is AccountsOperationStatus.Success || current is AccountsOperationStatus.Error) {
            operationStatusFlow.value = AccountsOperationStatus.Idle
        }
    }

    // =========================================================================
    // CUENTAS: CRUD, ARCHIVADO Y AJUSTES DE SALDO
    // =========================================================================

    fun createAccount(
        name: String,
        accountType: String = AccountType.CASH,
        institutionName: String = "",
        lastFourDigits: String = "",
        currencyCode: String = "GTQ",
        openingBalanceMinor: Long = 0L,
        creditLimitMinor: Long = 0L,
        cutoffDay: Int? = null,
        paymentDueDay: Int? = null,
        installmentReservePolicy: String = CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL,
        autoCreatePaymentReminder: Boolean = true,
        includeInTotalLiquidity: Boolean = true,
        isDefault: Boolean = false,
        colorHex: Long = 0xFF0F5132L,
        iconKey: String = "account_balance_wallet",
        notes: String = ""
    ): Job? = launchGuarded(
        operationSignature = "create_account:${name.trim().lowercase()}:$accountType:$openingBalanceMinor",
        loadingLabel = "Creando cuenta..."
    ) {
        val created = accountsRepository.createAccount(
            FinancialAccountEntity(
                name = name,
                accountType = accountType,
                institutionName = institutionName,
                lastFourDigits = lastFourDigits,
                currencyCode = currencyCode,
                openingBalanceMinor = openingBalanceMinor,
                creditLimitMinor = creditLimitMinor,
                cutoffDay = cutoffDay,
                paymentDueDay = paymentDueDay,
                installmentReservePolicy = installmentReservePolicy,
                autoCreatePaymentReminder = autoCreatePaymentReminder,
                includeInTotalLiquidity = includeInTotalLiquidity,
                isDefault = isDefault,
                colorHex = colorHex,
                iconKey = iconKey,
                notes = notes
            )
        )
        if (selectedAccountIdFlow.value == null) {
            selectedAccountIdFlow.value = created.id
        }
        "Cuenta «${created.name}» guardada correctamente."
    }

    fun updateAccount(account: FinancialAccountEntity): Job? = launchGuarded(
        operationSignature = "update_account:${account.id}:${account.name.trim()}",
        loadingLabel = "Actualizando cuenta..."
    ) {
        val updated = accountsRepository.updateAccount(account)
        "Cuenta «${updated.name}» actualizada."
    }

    fun archiveAccount(accountId: Long): Job? = launchGuarded(
        operationSignature = "archive_account:$accountId",
        loadingLabel = "Archivando cuenta..."
    ) {
        accountsRepository.archiveAccount(accountId)
        if (selectedAccountIdFlow.value == accountId) {
            selectedAccountIdFlow.value = null
        }
        "Cuenta archivada conservando su historial."
    }

    fun unarchiveAccount(accountId: Long): Job? = launchGuarded(
        operationSignature = "unarchive_account:$accountId",
        loadingLabel = "Reactivando cuenta..."
    ) {
        accountsRepository.unarchiveAccount(accountId)
        "Cuenta reactivada."
    }

    fun adjustAccountBalance(
        accountId: Long,
        targetBalanceOrDebtMinor: Long,
        notes: String = "",
        dateMillis: Long = clockMillis(),
        operationKey: String? = null
    ): Job? = launchGuarded(
        operationSignature = "adjust:$accountId:$targetBalanceOrDebtMinor:${operationKey.orEmpty()}",
        loadingLabel = "Registrando conciliación de saldo..."
    ) {
        val res = accountsRepository.recordBalanceAdjustment(
            accountId = accountId,
            targetBalanceOrDebtMinor = targetBalanceOrDebtMinor,
            notes = notes,
            dateMillis = dateMillis,
            operationKey = operationKey
        )
        if (res.transactionId == null) {
            "El saldo ya coincide exactamente con Q${MoneyMinorUnits.toBigDecimal(targetBalanceOrDebtMinor)}."
        } else {
            "Ajuste de saldo registrado sin alterar el presupuesto."
        }
    }

    fun assignAccountToTransaction(transactionId: Long, accountId: Long?): Job? = launchGuarded(
        operationSignature = "assign_tx:$transactionId:${accountId ?: -1L}",
        loadingLabel = "Vinculando movimiento..."
    ) {
        accountsRepository.assignAccountToTransaction(transactionId, accountId)
        if (accountId == null) "Movimiento desvinculado de cuenta." else "Movimiento asociado a la cuenta."
    }

    // =========================================================================
    // TRANSFERENCIAS Y TARJETAS DE CRÉDITO
    // =========================================================================

    fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        amountMinor: Long,
        title: String = "",
        notes: String = "",
        dateMillis: Long = clockMillis(),
        operationKey: String? = null
    ): Job? = launchGuarded(
        operationSignature = "transfer:$fromAccountId:$toAccountId:$amountMinor:${operationKey.orEmpty()}",
        loadingLabel = "Registrando transferencia..."
    ) {
        val result = accountsRepository.transferBetweenAccounts(
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            amountMinor = amountMinor,
            title = title,
            notes = notes,
            dateMillis = dateMillis,
            operationKey = operationKey
        )
        if (result.wasDeduplicated) {
            "Esta transferencia ya había sido registrada."
        } else {
            "Transferencia de Q${MoneyMinorUnits.toBigDecimal(result.amountMinor)} registrada entre tus cuentas."
        }
    }

    fun recordCreditCardPurchase(
        cardAccountId: Long,
        title: String,
        amountMinor: Long,
        category: String = "Otros Gastos",
        dateMillis: Long = clockMillis(),
        familyMember: String = "Personal",
        notes: String = "",
        enforceCreditLimit: Boolean = false,
        operationKey: String? = null
    ): Job? = launchGuarded(
        operationSignature = "cc_purchase:$cardAccountId:${title.trim()}:$amountMinor:${operationKey.orEmpty()}",
        loadingLabel = "Registrando consumo..."
    ) {
        val res = accountsRepository.recordCreditCardCharge(
            cardAccountId = cardAccountId,
            title = title,
            amountMinor = amountMinor,
            category = category,
            dateMillis = dateMillis,
            familyMember = familyMember,
            notes = notes,
            enforceCreditLimit = enforceCreditLimit,
            operationKey = operationKey
        )
        when {
            res.wasDeduplicated -> "Este consumo ya había sido registrado."
            res.exceedsAvailableCredit -> "Consumo registrado (alerta: excede el crédito disponible)."
            else -> "Consumo registrado en tarjeta por Q${MoneyMinorUnits.toBigDecimal(res.amountMinor)}."
        }
    }

    fun recordCreditCardInterestOrFee(
        cardAccountId: Long,
        title: String,
        amountMinor: Long,
        category: String = "Otros Gastos",
        dateMillis: Long = clockMillis(),
        notes: String = "",
        operationKey: String? = null
    ): Job? = launchGuarded(
        operationSignature = "cc_fee:$cardAccountId:${title.trim()}:$amountMinor:${operationKey.orEmpty()}",
        loadingLabel = "Registrando cargo financiero..."
    ) {
        val res = accountsRepository.recordCreditCardInterestOrFee(
            cardAccountId = cardAccountId,
            title = title,
            amountMinor = amountMinor,
            category = category,
            dateMillis = dateMillis,
            notes = notes,
            operationKey = operationKey
        )
        if (res.wasDeduplicated) {
            "Este cargo financiero ya había sido registrado."
        } else {
            "Cargo financiero registrado por Q${MoneyMinorUnits.toBigDecimal(res.amountMinor)}."
        }
    }

    fun payCreditCard(
        sourceAccountId: Long,
        cardAccountId: Long,
        amountMinor: Long,
        dateMillis: Long = clockMillis(),
        notes: String = "",
        linkedCommitmentId: Long? = null,
        operationKey: String? = null
    ): Job? = launchGuarded(
        operationSignature = "cc_pay:$sourceAccountId:$cardAccountId:$amountMinor:${operationKey.orEmpty()}",
        loadingLabel = "Aplicando abono a tarjeta..."
    ) {
        val res = accountsRepository.payCreditCard(
            sourceAccountId = sourceAccountId,
            cardAccountId = cardAccountId,
            amountMinor = amountMinor,
            dateMillis = dateMillis,
            notes = notes,
            linkedCommitmentId = linkedCommitmentId,
            operationKey = operationKey
        )
        when {
            res.wasDeduplicated -> "Este pago a tarjeta ya había sido registrado."
            res.validation.isPartialPayment -> "Abono parcial de Q${MoneyMinorUnits.toBigDecimal(res.amountMinor)} aplicado sin duplicar gastos."
            else -> "Pago de tarjeta por Q${MoneyMinorUnits.toBigDecimal(res.amountMinor)} registrado."
        }
    }

    fun syncCreditCardStatementReminder(
        cardAccountId: Long,
        paymentSourceAccountId: Long? = null
    ): Job? = launchGuarded(
        operationSignature = "cc_sync_reminder:$cardAccountId:${paymentSourceAccountId ?: -1L}",
        loadingLabel = "Actualizando recordatorio de corte..."
    ) {
        val commitment = accountsRepository.syncCreditCardStatementCommitment(
            cardAccountId = cardAccountId,
            paymentSourceAccountId = paymentSourceAccountId,
            referenceDateMillis = clockMillis()
        )
        if (commitment == null) {
            "Sin saldo pendiente del último corte cerrado."
        } else {
            "Recordatorio de tarjeta sincronizado por Q${MoneyMinorUnits.toBigDecimal(commitment.amountMinor)}."
        }
    }

    // =========================================================================
    // VISACUOTAS Y CUOTAS EN FINANCIAMIENTO
    // =========================================================================

    fun createInstallmentPlan(
        cardAccountId: Long,
        title: String,
        merchantName: String = "",
        category: String = "Otros Gastos",
        principalTotalMinor: Long,
        installmentsCount: Int,
        annualInterestRateBps: Int = 0,
        purchaseDateMillis: Long = clockMillis(),
        firstChargeDateMillis: Long = purchaseDateMillis,
        policyOverride: String? = null,
        postFirstInstallmentImmediately: Boolean = false,
        familyMember: String = "Personal",
        notes: String = ""
    ): Job? = launchGuarded(
        operationSignature = "inst_plan:$cardAccountId:${title.trim()}:$principalTotalMinor:$installmentsCount:$annualInterestRateBps",
        loadingLabel = "Generando calendario de cuotas..."
    ) {
        val created = accountsRepository.createInstallmentPlan(
            cardAccountId = cardAccountId,
            title = title,
            merchantName = merchantName,
            category = category,
            principalTotalMinor = principalTotalMinor,
            installmentsCount = installmentsCount,
            annualInterestRateBps = annualInterestRateBps,
            purchaseDateMillis = purchaseDateMillis,
            firstChargeDateMillis = firstChargeDateMillis,
            policyOverride = policyOverride,
            postFirstInstallmentImmediately = postFirstInstallmentImmediately,
            familyMember = familyMember,
            notes = notes
        )
        "Plan «${created.plan.title}» creado con ${created.scheduleItems.size} cuotas."
    }

    fun postInstallmentCharge(planId: Long, installmentNumber: Int): Job? = launchGuarded(
        operationSignature = "post_inst:$planId:$installmentNumber",
        loadingLabel = "Contabilizando cuota #$installmentNumber..."
    ) {
        val result = accountsRepository.postInstallmentItem(
            planId = planId,
            installmentNumber = installmentNumber,
            postedAtMillis = clockMillis()
        )
        when {
            result.alreadyPosted -> "La cuota #$installmentNumber ya estaba contabilizada."
            result.planCompleted -> "Última cuota #$installmentNumber contabilizada. Plan liquidado."
            else -> "Cuota #$installmentNumber contabilizada en tu tarjeta y presupuesto."
        }
    }

    fun postDueInstallments(
        upToMillis: Long = clockMillis(),
        accountId: Long? = null
    ): Job? = launchGuarded(
        operationSignature = "post_due:${accountId ?: -1L}",
        loadingLabel = "Contabilizando cuotas vencidas..."
    ) {
        val results = accountsRepository.postDueInstallments(upToMillis = upToMillis, accountId = accountId)
        val newPosts = results.count { !it.alreadyPosted }
        if (newPosts == 0) {
            "No hay cuotas nuevas pendientes por contabilizar."
        } else {
            "Se contabilizaron $newPosts cuota(s) de Visacuotas."
        }
    }

    fun cancelOrPayOffInstallmentPlan(planId: Long, paidOffEarly: Boolean): Job? = launchGuarded(
        operationSignature = "close_plan:$planId:$paidOffEarly",
        loadingLabel = if (paidOffEarly) "Liquidando plan..." else "Cancelando plan..."
    ) {
        val updated = accountsRepository.cancelOrPayOffInstallmentPlan(planId, paidOffEarly)
        if (paidOffEarly) {
            "Plan «${updated.title}» marcado como completado; cupo reservado liberado."
        } else {
            "Plan «${updated.title}» cancelado; cuotas futuras liberadas."
        }
    }

    fun importCsvToAccount(
        csvText: String,
        targetAccountId: Long,
        onResult: ((ImportSummary) -> Unit)? = null
    ): Job? = launchGuarded(
        operationSignature = "import_csv:$targetAccountId:${csvText.hashCode()}",
        loadingLabel = "Importando movimientos a cuenta..."
    ) {
        val repo = budgetRepository
            ?: throw IllegalStateException("El importador CSV requiere BudgetRepository configurado.")
        // Valida que la cuenta exista y no esté archivada antes de importar
        val snapshot = accountsRepository.getPortfolioSnapshot(clockMillis())
        val account = snapshot.activeAccounts.firstOrNull { it.id == targetAccountId }
            ?: throw IllegalArgumentException("Selecciona una cuenta activa válida para importar.")
        val summary = repo.importFromCsv(csvText, targetAccountId = account.id)
        onResult?.invoke(summary)
        "Importación a «${account.name}»: ${summary.newCount} nuevos, ${summary.duplicateCount} duplicados omitidos."
    }

    // =========================================================================
    // MOTOR INTERNO DE BLOQUEO CONTRA EVENTOS REPETIDOS Y ESTADOS
    // =========================================================================

    private fun launchGuarded(
        operationSignature: String,
        loadingLabel: String,
        block: suspend () -> String
    ): Job? {
        val now = clockMillis()
        synchronized(this) {
            if (!inFlightOperationKeys.add(operationSignature)) {
                return null
            }
            val lastExecution = recentOperationTimestamps[operationSignature]
            if (lastExecution != null && (now - lastExecution) in 0L until duplicateGuardWindowMs) {
                inFlightOperationKeys.remove(operationSignature)
                return null
            }
            recentOperationTimestamps[operationSignature] = now
        }

        return viewModelScope.launch {
            operationStatusFlow.value = AccountsOperationStatus.Loading(loadingLabel)
            try {
                val successMsg = block()
                operationStatusFlow.value = AccountsOperationStatus.Success(
                    message = successMsg,
                    timestampMillis = clockMillis()
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                operationStatusFlow.value = AccountsOperationStatus.Error(
                    message = t.message?.takeIf { it.isNotBlank() } ?: "No se pudo completar la operación.",
                    timestampMillis = clockMillis()
                )
            } finally {
                inFlightOperationKeys.remove(operationSignature)
            }
        }
    }

    private fun buildUiState(
        selectedId: Long?,
        portfolio: AccountsPortfolioState,
        opStatus: AccountsOperationStatus
    ): AccountsUiState {
        val effectiveSelectedId = when {
            selectedId != null && portfolio.allAccounts.any { it.id == selectedId } -> selectedId
            else -> portfolio.activeAccounts.firstOrNull { it.isDefault }?.id
                ?: portfolio.activeAccounts.firstOrNull()?.id
        }
        val selectedAcc = effectiveSelectedId?.let { id -> portfolio.allAccounts.firstOrNull { it.id == id } }
        val selectedSnap = effectiveSelectedId?.let { id -> portfolio.summary.snapshotsByAccountId[id] }
        val selectedStmt = effectiveSelectedId?.let { id -> portfolio.cardStatementsByAccountId[id] }
        val selectedAccountTxs = if (effectiveSelectedId != null) {
            portfolio.allTransactions.filter { it.accountId == effectiveSelectedId }
        } else emptyList()

        return AccountsUiState(
            isLoading = false,
            operationStatus = opStatus,
            isOperationInProgress = opStatus is AccountsOperationStatus.Loading,
            allAccounts = portfolio.allAccounts,
            activeAccounts = portfolio.activeAccounts,
            archivedAccounts = portfolio.archivedAccounts,
            liquidAccounts = portfolio.activeAccounts.filter { AccountType.isLiquidAsset(it.accountType) },
            creditCardAccounts = portfolio.activeAccounts.filter { AccountType.isCreditCard(it.accountType) },
            snapshotsByAccountId = portfolio.summary.snapshotsByAccountId,
            cardStatementsByAccountId = portfolio.cardStatementsByAccountId,
            installmentPlans = portfolio.installmentPlans,
            scheduleItemsByPlanId = portfolio.scheduleItemsByPlanId,
            selectedAccountId = effectiveSelectedId,
            selectedAccount = selectedAcc,
            selectedAccountSnapshot = selectedSnap,
            selectedAccountStatement = selectedStmt,
            selectedAccountTransactions = selectedAccountTxs,
            unassignedTransactions = portfolio.unassignedTransactions,
            unassignedTransactionsCount = portfolio.summary.unassignedTransactionsCount,
            unassignedNetImpactMinor = portfolio.summary.unassignedNetImpactMinor,
            totalLiquidAssetsMinor = portfolio.summary.totalLiquidAssetsMinor,
            totalCreditCardDebtMinor = portfolio.summary.totalCreditCardDebtMinor,
            netWorthMinor = portfolio.summary.netWorthMinor,
            errorMessage = (opStatus as? AccountsOperationStatus.Error)?.message,
            successMessage = (opStatus as? AccountsOperationStatus.Success)?.message
        )
    }
}
