package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AccountLedgerDao
import com.example.data.local.AppDatabase
import com.example.data.local.FinancialAccountDao
import com.example.data.local.InstallmentPlanDao
import com.example.data.local.InstallmentScheduleDao
import com.example.data.local.PaymentCommitmentDao
import com.example.data.local.TransactionDao
import com.example.data.model.AccountType
import com.example.data.model.CommitmentKind
import com.example.data.model.CreditReservePolicy
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.InstallmentChargeStatus
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.InstallmentPlanStatus
import com.example.data.model.InstallmentScheduleItemEntity
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.model.PaymentFrequency
import com.example.data.model.PaymentStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionEntryKind
import com.example.data.model.TransactionType
import com.example.data.model.effectiveAmountMinor
import com.example.data.model.withExactAmountMinor
import com.example.data.service.AccountBalanceCalculator
import com.example.data.service.AccountBalanceSnapshot
import com.example.data.service.AccountsPortfolioSummary
import com.example.data.service.CardPaymentValidation
import com.example.data.service.CreditCardCycleCalculator
import com.example.data.service.CreditCardStatementSummary
import com.example.data.service.InstallmentScheduleEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.TimeZone
import java.util.UUID

data class AccountsPortfolioState(
    val allAccounts: List<FinancialAccountEntity>,
    val activeAccounts: List<FinancialAccountEntity>,
    val archivedAccounts: List<FinancialAccountEntity>,
    val summary: AccountsPortfolioSummary,
    val cardStatementsByAccountId: Map<Long, CreditCardStatementSummary>,
    val installmentPlans: List<InstallmentPlanEntity>,
    val scheduleItemsByPlanId: Map<Long, List<InstallmentScheduleItemEntity>>,
    val unassignedTransactions: List<TransactionEntity>,
    val allTransactions: List<TransactionEntity> = emptyList()
)

data class TransferResult(
    val transferGroupId: String,
    val outgoingTransactionId: Long,
    val incomingTransactionId: Long,
    val amountMinor: Long,
    val wasDeduplicated: Boolean = false
)

data class CreditCardPaymentResult(
    val transferGroupId: String,
    val sourceOutflowTransactionId: Long,
    val cardPaymentTransactionId: Long,
    val amountMinor: Long,
    val validation: CardPaymentValidation,
    val settledCommitmentId: Long? = null,
    val wasDeduplicated: Boolean = false
)

data class CreditCardChargeResult(
    val transactionId: Long,
    val amountMinor: Long,
    val exceedsAvailableCredit: Boolean,
    val newAvailableCreditMinor: Long,
    val wasDeduplicated: Boolean = false
)

data class BalanceAdjustmentResult(
    val transactionId: Long?,
    val previousBalanceOrDebtMinor: Long,
    val targetBalanceOrDebtMinor: Long,
    val deltaMinor: Long
)

data class InstallmentPlanCreationResult(
    val plan: InstallmentPlanEntity,
    val scheduleItems: List<InstallmentScheduleItemEntity>,
    val postedResults: List<InstallmentPostResult>
)

data class InstallmentPostResult(
    val scheduleItem: InstallmentScheduleItemEntity,
    val transactionId: Long,
    val alreadyPosted: Boolean,
    val planCompleted: Boolean
)

/**
 * Repositorio dedicado del módulo MIS CUENTAS.
 *
 * Garantías contables y offline-first:
 * - `amountMinor: Long` es la fuente primaria de verdad; `amount: Double` se deriva en cada escritura.
 * - Operaciones compuestas ejecutadas en `database.withTransaction` (o secuencial si `database == null`).
 * - Idempotencia por clave externa y restricciones únicas en Room para transferencias, pagos y Visacuotas.
 */
class AccountsRepository(
    private val financialAccountDao: FinancialAccountDao,
    private val installmentPlanDao: InstallmentPlanDao,
    private val installmentScheduleDao: InstallmentScheduleDao,
    private val accountLedgerDao: AccountLedgerDao,
    private val transactionDao: TransactionDao,
    private val paymentCommitmentDao: PaymentCommitmentDao? = null,
    private val database: AppDatabase? = null
) {
    companion object {
        private val SUPPORTED_ACCOUNT_TYPES = setOf(
            AccountType.CASH,
            AccountType.BANK_CHECKING,
            AccountType.BANK_SAVINGS,
            AccountType.COOPERATIVE,
            AccountType.CREDIT_CARD
        )
        private val SUPPORTED_RESERVE_POLICIES = setOf(
            CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL,
            CreditReservePolicy.RESERVE_BILLED_INSTALLMENTS_ONLY
        )

        fun fromDatabase(db: AppDatabase): AccountsRepository = AccountsRepository(
            financialAccountDao = db.financialAccountDao(),
            installmentPlanDao = db.installmentPlanDao(),
            installmentScheduleDao = db.installmentScheduleDao(),
            accountLedgerDao = db.accountLedgerDao(),
            transactionDao = db.transactionDao(),
            paymentCommitmentDao = db.paymentCommitmentDao(),
            database = db
        )
    }

    suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return if (database != null) {
            database.withTransaction { block() }
        } else {
            block()
        }
    }

    // =========================================================================
    // CONSULTAS REACTIVAS Y SNAPSHOTS DE SALDOS
    // =========================================================================

    fun observePortfolio(
        referenceDateMillisProvider: () -> Long = { System.currentTimeMillis() },
        timeZone: TimeZone = TimeZone.getDefault()
    ): Flow<AccountsPortfolioState> {
        return combine(
            financialAccountDao.getAllAccounts(),
            transactionDao.getAllTransactions(),
            installmentPlanDao.getAllPlans(),
            installmentScheduleDao.getAllScheduleItems()
        ) { accounts, txs, plans, scheduleItems ->
            buildPortfolioState(
                accounts = accounts,
                transactions = txs,
                plans = plans,
                scheduleItems = scheduleItems,
                referenceDateMillis = referenceDateMillisProvider(),
                timeZone = timeZone
            )
        }
    }

    fun observeTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>> =
        accountLedgerDao.getTransactionsForAccount(accountId)

    fun observeUnassignedTransactions(): Flow<List<TransactionEntity>> =
        accountLedgerDao.getUnassignedTransactions()

    suspend fun getPortfolioSnapshot(
        referenceDateMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): AccountsPortfolioState {
        val accounts = financialAccountDao.getAllAccountsList()
        val txs = transactionDao.getAllTransactionsList()
        val plans = installmentPlanDao.getAllPlansList()
        val items = installmentScheduleDao.getItemsListSnapshot()
        return buildPortfolioState(accounts, txs, plans, items, referenceDateMillis, timeZone)
    }

    suspend fun getAccountSnapshot(accountId: Long): AccountBalanceSnapshot {
        val account = financialAccountDao.getAccountById(accountId)
            ?: throw IllegalArgumentException("La cuenta #$accountId no existe.")
        val txs = transactionDao.getAllTransactionsList()
        val plans = installmentPlanDao.getAllPlansList()
        val items = installmentScheduleDao.getItemsListSnapshot()
        return AccountBalanceCalculator.calculateAccountSnapshot(account, txs, plans, items)
    }

    suspend fun getCreditCardStatementSummary(
        cardAccountId: Long,
        referenceDateMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): CreditCardStatementSummary {
        val card = financialAccountDao.getAccountById(cardAccountId)
            ?: throw IllegalArgumentException("La tarjeta #$cardAccountId no existe.")
        require(AccountType.isCreditCard(card.accountType)) {
            "La cuenta «${card.name}» no es una tarjeta de crédito."
        }
        val txs = accountLedgerDao.getTransactionsForAccountList(cardAccountId)
        return CreditCardCycleCalculator.calculateStatementSummary(card, txs, referenceDateMillis, timeZone)
    }

    // =========================================================================
    // CRUD Y ARCHIVADO DE CUENTAS
    // =========================================================================

    suspend fun createAccount(account: FinancialAccountEntity): FinancialAccountEntity = runInTransaction {
        val sanitized = validateAndNormalizeAccount(account.copy(id = 0L))
        val newId = financialAccountDao.insertAccount(sanitized)
        if (sanitized.isDefault) {
            financialAccountDao.clearDefaultExcept(newId)
        }
        sanitized.copy(id = newId)
    }

    suspend fun updateAccount(account: FinancialAccountEntity): FinancialAccountEntity = runInTransaction {
        require(account.id > 0L) { "Se requiere un ID válido para actualizar la cuenta." }
        val existing = financialAccountDao.getAccountById(account.id)
            ?: throw IllegalArgumentException("La cuenta #${account.id} no existe.")
        val sanitized = validateAndNormalizeAccount(
            account.copy(
                createdAtMillis = existing.createdAtMillis,
                isDefault = if (account.isArchived) false else account.isDefault
            )
        )
        financialAccountDao.updateAccount(sanitized)
        if (sanitized.isDefault) {
            financialAccountDao.clearDefaultExcept(sanitized.id)
        }
        sanitized
    }

    suspend fun archiveAccount(accountId: Long) = runInTransaction {
        financialAccountDao.getAccountById(accountId)
            ?: throw IllegalArgumentException("La cuenta #$accountId no existe.")
        // Archivado lógico: preserva íntegramente los movimientos y compromisos históricos
        financialAccountDao.archiveAccount(accountId)
    }

    suspend fun unarchiveAccount(accountId: Long) = runInTransaction {
        financialAccountDao.getAccountById(accountId)
            ?: throw IllegalArgumentException("La cuenta #$accountId no existe.")
        financialAccountDao.unarchiveAccount(accountId)
    }

    // =========================================================================
    // AJUSTES DE SALDO Y VINCULACIÓN DE HISTORIAL HEREDADO
    // =========================================================================

    /**
     * Registra un asiento de conciliación ([TransactionEntryKind.BALANCE_ADJUSTMENT]) para que
     * el saldo real de la cuenta (o deuda rotativa si es tarjeta) coincida exactamente con
     * [targetBalanceOrDebtMinor] sin distorsionar ingresos ni gastos del presupuesto.
     */
    suspend fun recordBalanceAdjustment(
        accountId: Long,
        targetBalanceOrDebtMinor: Long,
        notes: String = "",
        dateMillis: Long = System.currentTimeMillis(),
        operationKey: String? = null
    ): BalanceAdjustmentResult = runInTransaction {
        val extKey = operationKey?.takeIf { it.isNotBlank() }?.let { "adj_$it" }
        if (extKey != null) {
            val existing = accountLedgerDao.getTransactionByExternalId(extKey)
            if (existing != null) {
                val snap = getAccountSnapshot(accountId)
                return@runInTransaction BalanceAdjustmentResult(
                    transactionId = existing.id,
                    previousBalanceOrDebtMinor = snap.currentBalanceMinor,
                    targetBalanceOrDebtMinor = targetBalanceOrDebtMinor,
                    deltaMinor = existing.effectiveAmountMinor
                )
            }
        }

        val account = financialAccountDao.getAccountById(accountId)
            ?: throw IllegalArgumentException("La cuenta #$accountId no existe.")
        require(!account.isArchived) { "No se pueden registrar ajustes en la cuenta archivada «${account.name}»." }

        val snap = getAccountSnapshot(accountId)
        val isCard = AccountType.isCreditCard(account.accountType)
        val currentMinor = if (isCard) snap.revolvingDebtMinor else snap.currentBalanceMinor
        val deltaMinor = targetBalanceOrDebtMinor - currentMinor

        if (deltaMinor == 0L) {
            return@runInTransaction BalanceAdjustmentResult(
                transactionId = null,
                previousBalanceOrDebtMinor = currentMinor,
                targetBalanceOrDebtMinor = targetBalanceOrDebtMinor,
                deltaMinor = 0L
            )
        }

        val txType = if (isCard) {
            // En tarjeta: aumentar deuda = OUTFLOW (EXPENSE); reducir deuda = INFLOW (INCOME)
            if (deltaMinor > 0L) TransactionType.EXPENSE else TransactionType.INCOME
        } else {
            if (deltaMinor > 0L) TransactionType.INCOME else TransactionType.EXPENSE
        }

        val absMinor = kotlin.math.abs(deltaMinor)
        val tx = TransactionEntity(
            title = "Ajuste de saldo · ${account.name}",
            amount = MoneyMinorUnits.toDisplayDouble(absMinor),
            type = txType,
            category = "Ajuste de Saldo",
            dateMillis = dateMillis,
            paymentMethod = account.name,
            notes = notes.trim().ifBlank { "Conciliación de saldo sin impacto presupuestario" },
            isVerified = true,
            isTentative = false,
            tentativeDecision = "CONFIRMED",
            externalId = extKey,
            amountMinor = absMinor,
            accountId = account.id,
            entryKind = TransactionEntryKind.BALANCE_ADJUSTMENT
        )
        val txId = transactionDao.insertTransaction(tx)
        BalanceAdjustmentResult(
            transactionId = txId,
            previousBalanceOrDebtMinor = currentMinor,
            targetBalanceOrDebtMinor = targetBalanceOrDebtMinor,
            deltaMinor = deltaMinor
        )
    }

    /**
     * Asocia o desasocia manualmente un movimiento histórico ordinario a una cuenta activa,
     * protegiendo la integridad de transferencias enlazadas y cuotas generadas por plan.
     */
    suspend fun assignAccountToTransaction(transactionId: Long, accountId: Long?) = runInTransaction {
        val tx = accountLedgerDao.getTransactionById(transactionId)
            ?: throw IllegalArgumentException("El movimiento #$transactionId no existe.")
        require(tx.transferGroupId == null && tx.installmentPlanId == null) {
            "No se puede reasignar individualmente un movimiento enlazado a una transferencia o plan de cuotas."
        }
        val account = if (accountId != null) {
            val found = financialAccountDao.getAccountById(accountId)
                ?: throw IllegalArgumentException("La cuenta #$accountId no existe.")
            require(!found.isArchived) { "La cuenta «${found.name}» está archivada." }
            found
        } else null

        val exactMinor = tx.effectiveAmountMinor
        val updated = tx.withExactAmountMinor(exactMinor).copy(
            accountId = account?.id
        )
        transactionDao.updateTransaction(updated)
    }

    // =========================================================================
    // TRANSFERENCIAS ATÓMICAS ENTRE CUENTAS PROPIAS
    // =========================================================================

    /**
     * Ejecuta una transferencia atómica mediante dos asientos relacionados (`TRANSFER_OUT` y
     * `TRANSFER_IN`) compartiendo `transferGroupId`. Ninguno afecta ingresos ni gastos de presupuesto.
     */
    suspend fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        amountMinor: Long,
        title: String = "",
        notes: String = "",
        dateMillis: Long = System.currentTimeMillis(),
        operationKey: String? = null
    ): TransferResult = runInTransaction {
        require(amountMinor > 0L) { "El monto de la transferencia debe ser mayor a Q0.00." }
        require(fromAccountId != toAccountId) { "La cuenta origen y destino deben ser distintas." }

        val normalizedKey = operationKey?.trim()?.takeIf { it.isNotEmpty() }
        val outExtId = normalizedKey?.let { "transfer_${it}_out" }
        val inExtId = normalizedKey?.let { "transfer_${it}_in" }
        if (outExtId != null) {
            val existingOut = accountLedgerDao.getTransactionByExternalId(outExtId)
            if (existingOut != null) {
                val groupTxs = existingOut.transferGroupId
                    ?.let { accountLedgerDao.getTransactionsByTransferGroup(it) }
                    .orEmpty()
                val existingIn = groupTxs.firstOrNull { it.entryKind == TransactionEntryKind.TRANSFER_IN }
                    ?: accountLedgerDao.getTransactionByExternalId(inExtId!!)
                return@runInTransaction TransferResult(
                    transferGroupId = existingOut.transferGroupId.orEmpty(),
                    outgoingTransactionId = existingOut.id,
                    incomingTransactionId = existingIn?.id ?: 0L,
                    amountMinor = existingOut.effectiveAmountMinor,
                    wasDeduplicated = true
                )
            }
        }

        val fromAccount = financialAccountDao.getAccountById(fromAccountId)
            ?: throw IllegalArgumentException("La cuenta origen #$fromAccountId no existe.")
        val toAccount = financialAccountDao.getAccountById(toAccountId)
            ?: throw IllegalArgumentException("La cuenta destino #$toAccountId no existe.")

        require(!fromAccount.isArchived) { "La cuenta origen «${fromAccount.name}» está archivada." }
        require(!toAccount.isArchived) { "La cuenta destino «${toAccount.name}» está archivada." }
        require(AccountType.isLiquidAsset(fromAccount.accountType)) {
            "No se puede transferir directamente desde la tarjeta «${fromAccount.name}» como cuenta líquida."
        }
        require(AccountType.isLiquidAsset(toAccount.accountType)) {
            "Para abonar a la tarjeta «${toAccount.name}» utiliza la operación de pago de tarjeta."
        }

        val transferGroupId = normalizedKey?.let { "trf_$it" } ?: UUID.randomUUID().toString()
        val displayAmount = MoneyMinorUnits.toDisplayDouble(amountMinor)
        val cleanTitle = title.trim()

        val outgoingTx = TransactionEntity(
            title = cleanTitle.ifBlank { "Transferencia hacia ${toAccount.name}" },
            amount = displayAmount,
            type = TransactionType.EXPENSE,
            category = "Transferencia entre cuentas",
            dateMillis = dateMillis,
            paymentMethod = fromAccount.name,
            notes = notes.trim().ifBlank { "Salida hacia ${toAccount.name}" },
            isVerified = true,
            isTentative = false,
            tentativeDecision = "CONFIRMED",
            externalId = outExtId,
            amountMinor = amountMinor,
            accountId = fromAccount.id,
            entryKind = TransactionEntryKind.TRANSFER_OUT,
            transferGroupId = transferGroupId,
            counterpartAccountId = toAccount.id
        )

        val incomingTx = TransactionEntity(
            title = cleanTitle.ifBlank { "Transferencia desde ${fromAccount.name}" },
            amount = displayAmount,
            type = TransactionType.INCOME,
            category = "Transferencia entre cuentas",
            dateMillis = dateMillis,
            paymentMethod = toAccount.name,
            notes = notes.trim().ifBlank { "Ingreso desde ${fromAccount.name}" },
            isVerified = true,
            isTentative = false,
            tentativeDecision = "CONFIRMED",
            externalId = inExtId,
            amountMinor = amountMinor,
            accountId = toAccount.id,
            entryKind = TransactionEntryKind.TRANSFER_IN,
            transferGroupId = transferGroupId,
            counterpartAccountId = fromAccount.id
        )

        val outId = transactionDao.insertTransaction(outgoingTx)
        val inId = transactionDao.insertTransaction(incomingTx)

        TransferResult(
            transferGroupId = transferGroupId,
            outgoingTransactionId = outId,
            incomingTransactionId = inId,
            amountMinor = amountMinor,
            wasDeduplicated = false
        )
    }

    suspend fun deleteTransferGroup(transferGroupId: String) = runInTransaction {
        require(transferGroupId.isNotBlank()) { "El identificador de transferencia es requerido." }
        accountLedgerDao.deleteByTransferGroup(transferGroupId)
    }

    // =========================================================================
    // OPERACIONES DE TARJETA DE CRÉDITO (COMPRAS, CARGOS, PAGOS Y CICLOS)
    // =========================================================================

    suspend fun recordCreditCardCharge(
        cardAccountId: Long,
        title: String,
        amountMinor: Long,
        category: String = "Otros Gastos",
        dateMillis: Long = System.currentTimeMillis(),
        familyMember: String = "Personal",
        notes: String = "",
        enforceCreditLimit: Boolean = false,
        operationKey: String? = null
    ): CreditCardChargeResult = recordCardDebitInternal(
        cardAccountId = cardAccountId,
        title = title,
        amountMinor = amountMinor,
        category = category,
        dateMillis = dateMillis,
        familyMember = familyMember,
        notes = notes,
        enforceCreditLimit = enforceCreditLimit,
        extPrefix = "cardchg",
        operationKey = operationKey
    )

    suspend fun recordCreditCardInterestOrFee(
        cardAccountId: Long,
        title: String,
        amountMinor: Long,
        category: String = "Otros Gastos",
        dateMillis: Long = System.currentTimeMillis(),
        notes: String = "",
        operationKey: String? = null
    ): CreditCardChargeResult = recordCardDebitInternal(
        cardAccountId = cardAccountId,
        title = title.trim().ifBlank { "Interés / Cargo financiero" },
        amountMinor = amountMinor,
        category = category,
        dateMillis = dateMillis,
        familyMember = "Hogar",
        notes = notes.trim().ifBlank { "Cargo financiero o interés de tarjeta" },
        enforceCreditLimit = false,
        extPrefix = "cardfee",
        operationKey = operationKey
    )

    private suspend fun recordCardDebitInternal(
        cardAccountId: Long,
        title: String,
        amountMinor: Long,
        category: String,
        dateMillis: Long,
        familyMember: String,
        notes: String,
        enforceCreditLimit: Boolean,
        extPrefix: String,
        operationKey: String?
    ): CreditCardChargeResult = runInTransaction {
        require(amountMinor > 0L) { "El monto del cargo debe ser mayor a Q0.00." }
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "La descripción del cargo es obligatoria." }

        val normalizedKey = operationKey?.trim()?.takeIf { it.isNotEmpty() }
        val extId = normalizedKey?.let { "${extPrefix}_$it" }
        if (extId != null) {
            val existing = accountLedgerDao.getTransactionByExternalId(extId)
            if (existing != null) {
                val snap = getAccountSnapshot(cardAccountId)
                return@runInTransaction CreditCardChargeResult(
                    transactionId = existing.id,
                    amountMinor = existing.effectiveAmountMinor,
                    exceedsAvailableCredit = false,
                    newAvailableCreditMinor = snap.availableCreditMinor,
                    wasDeduplicated = true
                )
            }
        }

        val card = financialAccountDao.getAccountById(cardAccountId)
            ?: throw IllegalArgumentException("La tarjeta #$cardAccountId no existe.")
        require(AccountType.isCreditCard(card.accountType)) {
            "La cuenta «${card.name}» no es una tarjeta de crédito."
        }
        require(!card.isArchived) { "La tarjeta «${card.name}» está archivada." }

        val snapBefore = getAccountSnapshot(cardAccountId)
        val exceedsLimit = card.creditLimitMinor > 0L && amountMinor > snapBefore.availableCreditMinor
        if (enforceCreditLimit && exceedsLimit) {
            throw IllegalStateException(
                "El monto excede el crédito disponible de ${card.name} (Q${MoneyMinorUnits.toBigDecimal(snapBefore.availableCreditMinor)})."
            )
        }

        val tx = TransactionEntity(
            title = cleanTitle,
            amount = MoneyMinorUnits.toDisplayDouble(amountMinor),
            type = TransactionType.EXPENSE,
            category = category.trim().ifBlank { "Otros Gastos" },
            dateMillis = dateMillis,
            paymentMethod = card.name,
            familyMember = familyMember.ifBlank { "Personal" },
            notes = notes.trim(),
            isVerified = true,
            isTentative = false,
            tentativeDecision = "CONFIRMED",
            externalId = extId,
            amountMinor = amountMinor,
            accountId = card.id,
            entryKind = TransactionEntryKind.STANDARD
        )
        val txId = transactionDao.insertTransaction(tx)
        val newAvailableMinor = if (card.creditLimitMinor > 0L) {
            (snapBefore.availableCreditMinor - amountMinor).coerceAtLeast(0L)
        } else 0L

        CreditCardChargeResult(
            transactionId = txId,
            amountMinor = amountMinor,
            exceedsAvailableCredit = exceedsLimit,
            newAvailableCreditMinor = newAvailableMinor,
            wasDeduplicated = false
        )
    }

    /**
     * Registra el pago/abono de una tarjeta de crédito separando la cuenta origen ([sourceAccountId])
     * de la tarjeta destino ([cardAccountId]), sin volver a contar las compras como gasto presupuestario.
     */
    suspend fun payCreditCard(
        sourceAccountId: Long,
        cardAccountId: Long,
        amountMinor: Long,
        dateMillis: Long = System.currentTimeMillis(),
        notes: String = "",
        linkedCommitmentId: Long? = null,
        operationKey: String? = null
    ): CreditCardPaymentResult = runInTransaction {
        require(amountMinor > 0L) { "El monto del abono debe ser mayor a Q0.00." }

        val normalizedKey = operationKey?.trim()?.takeIf { it.isNotEmpty() }
        val outExtId = normalizedKey?.let { "cardpay_${it}_out" }
        val inExtId = normalizedKey?.let { "cardpay_${it}_in" }
        if (outExtId != null) {
            val existingOut = accountLedgerDao.getTransactionByExternalId(outExtId)
            if (existingOut != null) {
                val groupTxs = existingOut.transferGroupId
                    ?.let { accountLedgerDao.getTransactionsByTransferGroup(it) }
                    .orEmpty()
                val existingIn = groupTxs.firstOrNull { it.entryKind == TransactionEntryKind.CARD_PAYMENT }
                    ?: accountLedgerDao.getTransactionByExternalId(inExtId!!)
                return@runInTransaction CreditCardPaymentResult(
                    transferGroupId = existingOut.transferGroupId.orEmpty(),
                    sourceOutflowTransactionId = existingOut.id,
                    cardPaymentTransactionId = existingIn?.id ?: 0L,
                    amountMinor = existingOut.effectiveAmountMinor,
                    validation = CardPaymentValidation(isValid = true),
                    settledCommitmentId = linkedCommitmentId,
                    wasDeduplicated = true
                )
            }
        }

        val sourceAccount = financialAccountDao.getAccountById(sourceAccountId)
            ?: throw IllegalArgumentException("La cuenta de pago #$sourceAccountId no existe.")
        val targetCard = financialAccountDao.getAccountById(cardAccountId)
            ?: throw IllegalArgumentException("La tarjeta de crédito #$cardAccountId no existe.")

        require(!sourceAccount.isArchived) { "La cuenta «${sourceAccount.name}» está archivada." }
        require(!targetCard.isArchived) { "La tarjeta «${targetCard.name}» está archivada." }
        require(AccountType.isLiquidAsset(sourceAccount.accountType)) {
            "La cuenta de pago «${sourceAccount.name}» debe ser una cuenta líquida (efectivo, banco o cooperativa)."
        }

        val allTxs = transactionDao.getAllTransactionsList()
        val plans = installmentPlanDao.getAllPlansList()
        val items = installmentScheduleDao.getItemsListSnapshot()
        val sourceSnap = AccountBalanceCalculator.calculateAccountSnapshot(sourceAccount, allTxs, plans, items)
        val cardSnap = AccountBalanceCalculator.calculateAccountSnapshot(targetCard, allTxs, plans, items)

        val validation = AccountBalanceCalculator.validateCreditCardPayment(
            sourceAccount = sourceAccount,
            sourceSnapshot = sourceSnap,
            targetCard = targetCard,
            targetSnapshot = cardSnap,
            paymentAmountMinor = amountMinor
        )
        if (!validation.isValid) {
            throw IllegalArgumentException(validation.reason)
        }

        val transferGroupId = normalizedKey?.let { "ccpay_$it" } ?: UUID.randomUUID().toString()
        val displayAmount = MoneyMinorUnits.toDisplayDouble(amountMinor)
        val cleanNotes = notes.trim()

        val sourceOutflow = TransactionEntity(
            title = "Pago tarjeta ${targetCard.name}",
            amount = displayAmount,
            type = TransactionType.EXPENSE,
            category = "Pago de Tarjeta",
            dateMillis = dateMillis,
            paymentMethod = sourceAccount.name,
            notes = cleanNotes.ifBlank { "Abono a ${targetCard.name} desde ${sourceAccount.name}" },
            isVerified = true,
            isTentative = false,
            tentativeDecision = "CONFIRMED",
            externalId = outExtId,
            amountMinor = amountMinor,
            accountId = sourceAccount.id,
            entryKind = TransactionEntryKind.TRANSFER_OUT,
            transferGroupId = transferGroupId,
            counterpartAccountId = targetCard.id
        )

        val cardInflow = TransactionEntity(
            title = "Abono recibido · ${targetCard.name}",
            amount = displayAmount,
            type = TransactionType.INCOME,
            category = "Pago de Tarjeta",
            dateMillis = dateMillis,
            paymentMethod = sourceAccount.name,
            notes = cleanNotes.ifBlank { "Pago recibido desde ${sourceAccount.name}" },
            isVerified = true,
            isTentative = false,
            tentativeDecision = "CONFIRMED",
            externalId = inExtId,
            amountMinor = amountMinor,
            accountId = targetCard.id,
            entryKind = TransactionEntryKind.CARD_PAYMENT,
            transferGroupId = transferGroupId,
            counterpartAccountId = sourceAccount.id
        )

        val outId = transactionDao.insertTransaction(sourceOutflow)
        val inId = transactionDao.insertTransaction(cardInflow)

        var closedCommitmentId: Long? = null
        if (linkedCommitmentId != null && paymentCommitmentDao != null) {
            val commitment = paymentCommitmentDao.getCommitmentById(linkedCommitmentId)
            if (commitment != null && !commitment.isPaid) {
                require(commitment.creditCardAccountId == null || commitment.creditCardAccountId == targetCard.id) {
                    "El compromiso #$linkedCommitmentId pertenece a otra tarjeta de crédito."
                }
                commitment.linkedTransactionId?.let { tentativeTxId ->
                    val linkedTx = accountLedgerDao.getTransactionById(tentativeTxId)
                    if (linkedTx != null && linkedTx.isTentative) {
                        transactionDao.deleteById(tentativeTxId)
                    }
                }
                paymentCommitmentDao.updateCommitment(
                    commitment.copy(
                        isPaid = true,
                        status = PaymentStatus.PAID,
                        paidDateMillis = dateMillis,
                        creditCardAccountId = targetCard.id,
                        paymentSourceAccountId = sourceAccount.id,
                        linkedTransactionId = inId
                    )
                )
                closedCommitmentId = commitment.id
            }
        }

        CreditCardPaymentResult(
            transferGroupId = transferGroupId,
            sourceOutflowTransactionId = outId,
            cardPaymentTransactionId = inId,
            amountMinor = amountMinor,
            validation = validation,
            settledCommitmentId = closedCommitmentId,
            wasDeduplicated = false
        )
    }

    /**
     * Sincroniza o crea el recordatorio de pago del estado de cuenta cerrado para [cardAccountId]
     * separando explícitamente `creditCardAccountId` y `paymentSourceAccountId`, sin duplicar
     * recordatorios del mismo ciclo ni inflar los gastos del presupuesto.
     */
    suspend fun syncCreditCardStatementCommitment(
        cardAccountId: Long,
        paymentSourceAccountId: Long? = null,
        referenceDateMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): PaymentCommitmentEntity? = runInTransaction {
        if (paymentCommitmentDao == null) return@runInTransaction null

        val card = financialAccountDao.getAccountById(cardAccountId)
            ?: throw IllegalArgumentException("La tarjeta #$cardAccountId no existe.")
        require(AccountType.isCreditCard(card.accountType)) {
            "La cuenta «${card.name}» no es una tarjeta de crédito."
        }
        if (card.isArchived || !card.autoCreatePaymentReminder) return@runInTransaction null

        if (paymentSourceAccountId != null) {
            val source = financialAccountDao.getAccountById(paymentSourceAccountId)
                ?: throw IllegalArgumentException("La cuenta de pago #$paymentSourceAccountId no existe.")
            require(AccountType.isLiquidAsset(source.accountType) && !source.isArchived) {
                "La cuenta origen debe ser una cuenta líquida activa."
            }
        }

        val txs = accountLedgerDao.getTransactionsForAccountList(card.id)
        val summary = CreditCardCycleCalculator.calculateStatementSummary(card, txs, referenceDateMillis, timeZone)
        val cycleTag = "Ciclo ${summary.cycleWindow.lastStatementCycleKey}"
        val existingCommitments = paymentCommitmentDao.getAllCommitmentsList()
        val existingForCycle = existingCommitments.firstOrNull {
            it.creditCardAccountId == card.id &&
                it.commitmentKind == CommitmentKind.CREDIT_CARD_STATEMENT &&
                !it.isPaid &&
                it.status != PaymentStatus.PAID &&
                (it.description.contains(cycleTag) || it.notes.contains(cycleTag))
        }

        if (!summary.hasStatementBalanceDue) {
            if (existingForCycle != null) {
                paymentCommitmentDao.updateCommitment(
                    existingForCycle.withExactAmountMinor(0L).copy(
                        isPaid = true,
                        status = PaymentStatus.PAID,
                        paidDateMillis = referenceDateMillis
                    )
                )
            }
            return@runInTransaction null
        }

        val amountMinor = summary.closedStatementRemainingDueMinor
        val dueMillis = summary.cycleWindow.paymentDueDateMillis

        if (existingForCycle != null) {
            val updated = existingForCycle.withExactAmountMinor(amountMinor).copy(
                dueDateMillis = dueMillis,
                paymentSourceAccountId = paymentSourceAccountId ?: existingForCycle.paymentSourceAccountId
            )
            paymentCommitmentDao.updateCommitment(updated)
            return@runInTransaction updated
        }

        val newCommitment = PaymentCommitmentEntity(
            title = "Pago ${card.name}",
            beneficiaryName = card.institutionName.ifBlank { card.name },
            description = "$cycleTag · Corte tarjeta ${card.lastFourDigits}".trim(),
            amount = MoneyMinorUnits.toDisplayDouble(amountMinor),
            dueDateMillis = dueMillis,
            category = "Pago de Tarjeta",
            frequency = PaymentFrequency.ONCE,
            isVariableAmount = true,
            reminderDaysBefore = 3,
            notes = "Recordatorio automático de corte ($cycleTag)",
            amountMinor = amountMinor,
            creditCardAccountId = card.id,
            paymentSourceAccountId = paymentSourceAccountId,
            commitmentKind = CommitmentKind.CREDIT_CARD_STATEMENT
        )
        val savedId = paymentCommitmentDao.insertCommitment(newCommitment)
        newCommitment.copy(id = savedId)
    }

    // =========================================================================
    // VISACUOTAS Y PLANES DE CUOTAS CON IDEMPOTENCIA ESTRICTA
    // =========================================================================

    suspend fun createInstallmentPlan(
        cardAccountId: Long,
        title: String,
        merchantName: String = "",
        category: String = "Otros Gastos",
        principalTotalMinor: Long,
        installmentsCount: Int,
        annualInterestRateBps: Int = 0,
        purchaseDateMillis: Long = System.currentTimeMillis(),
        firstChargeDateMillis: Long = purchaseDateMillis,
        policyOverride: String? = null,
        postDueUntilMillis: Long? = null,
        postFirstInstallmentImmediately: Boolean = false,
        familyMember: String = "Personal",
        notes: String = "",
        timeZone: TimeZone = TimeZone.getDefault()
    ): InstallmentPlanCreationResult = runInTransaction {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "El concepto de la compra en cuotas es obligatorio." }
        require(principalTotalMinor > 0L) { "El monto financiado debe ser mayor a Q0.00." }
        require(installmentsCount in 1..120) { "La cantidad de cuotas debe estar entre 1 y 120." }
        require(annualInterestRateBps >= 0) { "La tasa de interés anual no puede ser negativa." }

        val card = financialAccountDao.getAccountById(cardAccountId)
            ?: throw IllegalArgumentException("La tarjeta #$cardAccountId no existe.")
        require(AccountType.isCreditCard(card.accountType)) {
            "Los planes de Visacuotas deben asociarse a una tarjeta de crédito."
        }
        require(!card.isArchived) { "La tarjeta «${card.name}» está archivada." }

        val effectivePolicy = policyOverride?.takeIf { it.isNotBlank() } ?: card.installmentReservePolicy
        require(effectivePolicy in SUPPORTED_RESERVE_POLICIES) {
            "Política de reserva de crédito inválida: $effectivePolicy"
        }

        val previewSchedule = InstallmentScheduleEngine.generateSchedule(
            planId = 0L,
            accountId = card.id,
            principalTotalMinor = principalTotalMinor,
            installmentsCount = installmentsCount,
            annualInterestRateBps = annualInterestRateBps,
            firstChargeDateMillis = firstChargeDateMillis,
            cutoffDay = card.cutoffDay,
            timeZone = timeZone
        )

        val planToInsert = InstallmentPlanEntity(
            accountId = card.id,
            title = cleanTitle,
            merchantName = merchantName.trim(),
            category = category.trim().ifBlank { "Otros Gastos" },
            principalTotalMinor = principalTotalMinor,
            totalPlannedMinor = previewSchedule.totalPlannedMinor,
            installmentsCount = installmentsCount,
            annualInterestRateBps = annualInterestRateBps,
            installmentAmountMinor = previewSchedule.referenceInstallmentAmountMinor,
            creditReservePolicy = effectivePolicy,
            originTransactionId = null,
            purchaseDateMillis = purchaseDateMillis,
            firstChargeDateMillis = firstChargeDateMillis,
            status = InstallmentPlanStatus.ACTIVE,
            familyMember = familyMember.ifBlank { "Personal" },
            notes = notes.trim()
        )
        val planId = installmentPlanDao.insertPlan(planToInsert)
        var persistedPlan = planToInsert.copy(id = planId)

        val finalScheduleItems = previewSchedule.items.map { item ->
            item.copy(
                planId = planId,
                idempotencyKey = InstallmentScheduleEngine.buildIdempotencyKey(planId, item.installmentNumber)
            )
        }
        installmentScheduleDao.insertItemsIdempotent(finalScheduleItems)
        val storedItems = installmentScheduleDao.getItemsForPlanList(planId)

        val postedResults = mutableListOf<InstallmentPostResult>()
        for (item in storedItems) {
            val shouldPost = (postFirstInstallmentImmediately && item.installmentNumber == 1) ||
                (postDueUntilMillis != null && item.dueDateMillis <= postDueUntilMillis)
            if (shouldPost) {
                postedResults.add(
                    postSingleInstallmentInternal(
                        plan = persistedPlan,
                        item = item,
                        cardName = card.name,
                        postedAtMillis = item.dueDateMillis
                    )
                )
            }
        }

        persistedPlan = installmentPlanDao.getPlanById(planId) ?: persistedPlan
        val updatedItems = installmentScheduleDao.getItemsForPlanList(planId)
        InstallmentPlanCreationResult(
            plan = persistedPlan,
            scheduleItems = updatedItems,
            postedResults = postedResults
        )
    }

    /**
     * Contabiliza de forma idempotente una cuota específica en el libro mayor (`transactions`).
     * Reintentar la misma cuota nunca duplica el asiento ni el cargo presupuestario.
     */
    suspend fun postInstallmentItem(
        planId: Long,
        installmentNumber: Int,
        postedAtMillis: Long = System.currentTimeMillis()
    ): InstallmentPostResult = runInTransaction {
        val plan = installmentPlanDao.getPlanById(planId)
            ?: throw IllegalArgumentException("El plan de cuotas #$planId no existe.")
        val item = installmentScheduleDao.getItemByPlanAndNumber(planId, installmentNumber)
            ?: throw IllegalArgumentException("La cuota #$installmentNumber del plan #$planId no existe.")
        val card = financialAccountDao.getAccountById(plan.accountId)
            ?: throw IllegalArgumentException("La tarjeta #${plan.accountId} no existe.")

        postSingleInstallmentInternal(plan, item, card.name, postedAtMillis)
    }

    /**
     * Contabiliza todas las cuotas programadas con fecha `<= upToMillis` (todas las cuentas o una específica).
     */
    suspend fun postDueInstallments(
        upToMillis: Long = System.currentTimeMillis(),
        accountId: Long? = null
    ): List<InstallmentPostResult> = runInTransaction {
        val dueItems = if (accountId != null) {
            installmentScheduleDao.getDueScheduledItemsForAccount(accountId, upToMillis)
        } else {
            installmentScheduleDao.getDueScheduledItems(upToMillis)
        }
        if (dueItems.isEmpty()) return@runInTransaction emptyList()

        val plansCache = mutableMapOf<Long, InstallmentPlanEntity>()
        val cardsCache = mutableMapOf<Long, FinancialAccountEntity>()
        val results = mutableListOf<InstallmentPostResult>()

        for (item in dueItems) {
            val plan = plansCache[item.planId] ?: installmentPlanDao.getPlanById(item.planId)?.also {
                plansCache[item.planId] = it
            } ?: continue
            if (plan.status != InstallmentPlanStatus.ACTIVE) continue
            val card = cardsCache[plan.accountId] ?: financialAccountDao.getAccountById(plan.accountId)?.also {
                cardsCache[plan.accountId] = it
            } ?: continue
            if (card.isArchived) continue

            val res = postSingleInstallmentInternal(plan, item, card.name, item.dueDateMillis)
            results.add(res)
            if (res.planCompleted) {
                plansCache[plan.id] = plan.copy(status = InstallmentPlanStatus.COMPLETED)
            }
        }
        results
    }

    suspend fun cancelOrPayOffInstallmentPlan(
        planId: Long,
        paidOffEarly: Boolean
    ): InstallmentPlanEntity = runInTransaction {
        val plan = installmentPlanDao.getPlanById(planId)
            ?: throw IllegalArgumentException("El plan de cuotas #$planId no existe.")
        if (plan.status != InstallmentPlanStatus.ACTIVE) return@runInTransaction plan

        val targetItemStatus = if (paidOffEarly) {
            InstallmentChargeStatus.PAID_OFF_EARLY
        } else {
            InstallmentChargeStatus.CANCELLED
        }
        val targetPlanStatus = if (paidOffEarly) {
            InstallmentPlanStatus.COMPLETED
        } else {
            InstallmentPlanStatus.CANCELLED
        }

        val items = installmentScheduleDao.getItemsForPlanList(planId)
        for (item in items) {
            if (item.status == InstallmentChargeStatus.SCHEDULED) {
                installmentScheduleDao.updateItem(item.copy(status = targetItemStatus))
            }
        }
        val updatedPlan = plan.copy(status = targetPlanStatus)
        installmentPlanDao.updatePlan(updatedPlan)
        updatedPlan
    }

    private suspend fun postSingleInstallmentInternal(
        plan: InstallmentPlanEntity,
        item: InstallmentScheduleItemEntity,
        cardName: String,
        postedAtMillis: Long
    ): InstallmentPostResult {
        if (item.status == InstallmentChargeStatus.POSTED && item.linkedTransactionId != null) {
            return InstallmentPostResult(
                scheduleItem = item,
                transactionId = item.linkedTransactionId,
                alreadyPosted = true,
                planCompleted = plan.status == InstallmentPlanStatus.COMPLETED
            )
        }
        require(plan.status == InstallmentPlanStatus.ACTIVE) {
            "El plan «${plan.title}» no está activo (${plan.status})."
        }
        require(item.status == InstallmentChargeStatus.SCHEDULED) {
            "La cuota #${item.installmentNumber} está en estado ${item.status} y no puede contabilizarse."
        }

        // Doble barrera idempotente: verificar asiento previo por (planId, installmentNumber)
        val existingTx = accountLedgerDao.getTransactionForInstallment(plan.id, item.installmentNumber)
        val (txId, wasAlreadyInLedger) = if (existingTx != null) {
            Pair(existingTx.id, true)
        } else {
            val chargeTx = InstallmentScheduleEngine.buildInstallmentChargeTransaction(plan, item, cardName)
                .withExactAmountMinor(item.totalChargeMinor)
            Pair(transactionDao.insertTransaction(chargeTx), false)
        }

        val updatedItem = item.copy(
            status = InstallmentChargeStatus.POSTED,
            linkedTransactionId = txId,
            postedDateMillis = existingTx?.dateMillis ?: postedAtMillis
        )
        installmentScheduleDao.updateItem(updatedItem)

        val remainingScheduled = installmentScheduleDao.getItemsForPlanList(plan.id)
            .any { it.status == InstallmentChargeStatus.SCHEDULED }
        val planCompleted = !remainingScheduled
        if (planCompleted && plan.status == InstallmentPlanStatus.ACTIVE) {
            installmentPlanDao.updatePlan(plan.copy(status = InstallmentPlanStatus.COMPLETED))
        }

        return InstallmentPostResult(
            scheduleItem = updatedItem,
            transactionId = txId,
            alreadyPosted = wasAlreadyInLedger,
            planCompleted = planCompleted
        )
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    private suspend fun InstallmentScheduleDao.getItemsListSnapshot(): List<InstallmentScheduleItemEntity> =
        getAllScheduleItemsList()

    private fun buildPortfolioState(
        accounts: List<FinancialAccountEntity>,
        transactions: List<TransactionEntity>,
        plans: List<InstallmentPlanEntity>,
        scheduleItems: List<InstallmentScheduleItemEntity>,
        referenceDateMillis: Long,
        timeZone: TimeZone
    ): AccountsPortfolioState {
        val activeAccounts = accounts.filter { !it.isArchived }
        val archivedAccounts = accounts.filter { it.isArchived }
        val summary = AccountBalanceCalculator.calculatePortfolioSummary(
            accounts = accounts,
            transactions = transactions,
            installmentPlans = plans,
            scheduleItems = scheduleItems
        )
        val cardStatements = LinkedHashMap<Long, CreditCardStatementSummary>()
        for (acc in activeAccounts) {
            if (AccountType.isCreditCard(acc.accountType)) {
                cardStatements[acc.id] = CreditCardCycleCalculator.calculateStatementSummary(
                    card = acc,
                    transactions = transactions,
                    referenceDateMillis = referenceDateMillis,
                    timeZone = timeZone
                )
            }
        }
        return AccountsPortfolioState(
            allAccounts = accounts,
            activeAccounts = activeAccounts,
            archivedAccounts = archivedAccounts,
            summary = summary,
            cardStatementsByAccountId = cardStatements,
            installmentPlans = plans,
            scheduleItemsByPlanId = scheduleItems.groupBy { it.planId },
            unassignedTransactions = transactions.filter { it.accountId == null },
            allTransactions = transactions
        )
    }

    private fun validateAndNormalizeAccount(account: FinancialAccountEntity): FinancialAccountEntity {
        val cleanName = account.name.trim()
        require(cleanName.isNotEmpty()) { "El nombre de la cuenta es obligatorio." }
        require(account.accountType in SUPPORTED_ACCOUNT_TYPES) {
            "Tipo de cuenta inválido: ${account.accountType}"
        }
        require(account.creditLimitMinor >= 0L) {
            "El límite de crédito no puede ser negativo."
        }
        if (AccountType.isCreditCard(account.accountType)) {
            require(account.openingBalanceMinor >= 0L) {
                "La deuda inicial de una tarjeta de crédito no puede ser negativa."
            }
        }
        account.cutoffDay?.let {
            require(it in 1..31) { "El día de corte debe estar entre 1 y 31." }
        }
        account.paymentDueDay?.let {
            require(it in 1..31) { "El día límite de pago debe estar entre 1 y 31." }
        }
        val effectivePolicy = account.installmentReservePolicy.ifBlank {
            CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL
        }
        require(effectivePolicy in SUPPORTED_RESERVE_POLICIES) {
            "Política de reserva de Visacuotas inválida: $effectivePolicy"
        }

        val cleanDigits = account.lastFourDigits.filter { it.isDigit() }.takeLast(4)
        return account.copy(
            name = cleanName,
            institutionName = account.institutionName.trim(),
            lastFourDigits = cleanDigits,
            currencyCode = account.currencyCode.trim().ifBlank { "GTQ" }.uppercase(),
            installmentReservePolicy = effectivePolicy,
            notes = account.notes.trim()
        )
    }
}
