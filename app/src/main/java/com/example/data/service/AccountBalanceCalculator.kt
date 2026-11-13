package com.example.data.service

import com.example.data.model.AccountType
import com.example.data.model.CreditReservePolicy
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.InstallmentChargeStatus
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.InstallmentPlanStatus
import com.example.data.model.InstallmentScheduleItemEntity
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionEntryKind
import com.example.data.model.TransactionType
import com.example.data.model.effectiveAmountMinor
import com.example.data.model.isGastoSupuesto
import java.math.BigDecimal

/**
 * Snapshot contable exacto para una cuenta individual en unidades monetarias menores (`Long`)
 * y `BigDecimal` con escala 2 (`RoundingMode.HALF_UP`).
 */
data class AccountBalanceSnapshot(
    val accountId: Long,
    val accountType: String,
    val openingBalanceMinor: Long,
    val totalInflowsMinor: Long,
    val totalOutflowsMinor: Long,
    /** Para cuentas activas: saldo líquido disponible (`>=` o `< 0` si sobregiro). Para tarjetas: equivale a `-totalDebtMinor`. */
    val currentBalanceMinor: Long,
    /** Solo tarjetas: deuda facturada/rotativa acumulada (consumos + cuotas cargadas - abonos/pagos). */
    val revolvingDebtMinor: Long,
    /** Solo tarjetas: capital pendiente de cuotas futuras reservado según política de la institución. */
    val reservedInstallmentDebtMinor: Long,
    /** Solo tarjetas: deuda total comprometida (`revolvingDebtMinor + reservedInstallmentDebtMinor`). */
    val totalDebtMinor: Long,
    /** Solo tarjetas: crédito disponible real (`max(0, creditLimitMinor - max(0, totalDebtMinor))`). */
    val availableCreditMinor: Long,
    /** Solo tarjetas: saldo a favor si se pagó más que la deuda (`max(0, -totalDebtMinor)`). */
    val creditSurplusMinor: Long,
    /** Porcentaje de utilización del cupo (`0.00..100.00+`). */
    val utilizationPercent: BigDecimal,
    val movementsCount: Int
) {
    val currentBalance: BigDecimal get() = MoneyMinorUnits.toBigDecimal(currentBalanceMinor)
    val revolvingDebt: BigDecimal get() = MoneyMinorUnits.toBigDecimal(revolvingDebtMinor)
    val reservedInstallmentDebt: BigDecimal get() = MoneyMinorUnits.toBigDecimal(reservedInstallmentDebtMinor)
    val totalDebt: BigDecimal get() = MoneyMinorUnits.toBigDecimal(totalDebtMinor)
    val availableCredit: BigDecimal get() = MoneyMinorUnits.toBigDecimal(availableCreditMinor)
    val creditSurplus: BigDecimal get() = MoneyMinorUnits.toBigDecimal(creditSurplusMinor)
}

data class AccountsPortfolioSummary(
    val snapshotsByAccountId: Map<Long, AccountBalanceSnapshot>,
    val totalLiquidAssetsMinor: Long,
    val totalCreditCardDebtMinor: Long,
    val netWorthMinor: Long,
    val unassignedTransactionsCount: Int,
    val unassignedNetImpactMinor: Long
) {
    val totalLiquidAssets: BigDecimal get() = MoneyMinorUnits.toBigDecimal(totalLiquidAssetsMinor)
    val totalCreditCardDebt: BigDecimal get() = MoneyMinorUnits.toBigDecimal(totalCreditCardDebtMinor)
    val netWorth: BigDecimal get() = MoneyMinorUnits.toBigDecimal(netWorthMinor)
    val unassignedNetImpact: BigDecimal get() = MoneyMinorUnits.toBigDecimal(unassignedNetImpactMinor)
}

/**
 * Motor matemático puro para saldos por cuenta, deuda de tarjetas, reserva configurable de
 * Visacuotas/cuotas y resumen patrimonial consolidado.
 *
 * Todos los cálculos intermedios se ejecutan en `BigDecimal` desde unidades menores (`Long`).
 */
object AccountBalanceCalculator {

    fun calculateAccountSnapshot(
        account: FinancialAccountEntity,
        transactions: List<TransactionEntity>,
        installmentPlans: List<InstallmentPlanEntity> = emptyList(),
        scheduleItems: List<InstallmentScheduleItemEntity> = emptyList()
    ): AccountBalanceSnapshot {
        val accountTxs = transactions.asSequence()
            .filter { tx ->
                tx.accountId == account.id &&
                    !tx.isGastoSupuesto &&
                    tx.tentativeDecision != "DISCARDED" &&
                    !tx.isSuspectedDuplicate
            }
            .toList()

        var inflowsBd = BigDecimal.ZERO.setScale(MoneyMinorUnits.CURRENCY_SCALE, MoneyMinorUnits.ROUNDING_MODE)
        var outflowsBd = BigDecimal.ZERO.setScale(MoneyMinorUnits.CURRENCY_SCALE, MoneyMinorUnits.ROUNDING_MODE)

        for (tx in accountTxs) {
            val amtBd = MoneyMinorUnits.toBigDecimal(tx.effectiveAmountMinor)
            val isInflow = when {
                tx.entryKind == TransactionEntryKind.CARD_PAYMENT -> true
                tx.entryKind == TransactionEntryKind.TRANSFER_IN -> true
                tx.entryKind == TransactionEntryKind.TRANSFER_OUT -> false
                else -> tx.type == TransactionType.INCOME
            }
            if (isInflow) {
                inflowsBd = inflowsBd.add(amtBd)
            } else {
                outflowsBd = outflowsBd.add(amtBd)
            }
        }

        val totalInflowsMinor = MoneyMinorUnits.fromBigDecimal(inflowsBd)
        val totalOutflowsMinor = MoneyMinorUnits.fromBigDecimal(outflowsBd)
        val openingBd = MoneyMinorUnits.toBigDecimal(account.openingBalanceMinor)

        if (AccountType.isLiquidAsset(account.accountType)) {
            val balanceBd = openingBd.add(inflowsBd).subtract(outflowsBd)
            val balanceMinor = MoneyMinorUnits.fromBigDecimal(balanceBd)
            return AccountBalanceSnapshot(
                accountId = account.id,
                accountType = account.accountType,
                openingBalanceMinor = account.openingBalanceMinor,
                totalInflowsMinor = totalInflowsMinor,
                totalOutflowsMinor = totalOutflowsMinor,
                currentBalanceMinor = balanceMinor,
                revolvingDebtMinor = 0L,
                reservedInstallmentDebtMinor = 0L,
                totalDebtMinor = 0L,
                availableCreditMinor = 0L,
                creditSurplusMinor = 0L,
                utilizationPercent = BigDecimal.ZERO.setScale(MoneyMinorUnits.CURRENCY_SCALE, MoneyMinorUnits.ROUNDING_MODE),
                movementsCount = accountTxs.size
            )
        }

        // Tarjeta de Crédito:
        // openingBalanceMinor representa deuda previa existente (>= 0).
        // Los cargos (outflows: compras STANDARD + cuotas ya cargadas INSTALLMENT_CHARGE + intereses) suman deuda.
        // Los abonos (inflows: CARD_PAYMENT + reembolsos) reducen deuda.
        val revolvingDebtBd = openingBd.add(outflowsBd).subtract(inflowsBd)
        val revolvingDebtMinor = MoneyMinorUnits.fromBigDecimal(revolvingDebtBd)

        val accountPlans = installmentPlans.filter {
            it.accountId == account.id && it.status == InstallmentPlanStatus.ACTIVE
        }
        val itemsByPlan = scheduleItems.filter { it.accountId == account.id }.groupBy { it.planId }

        var reservedPrincipalBd = BigDecimal.ZERO.setScale(MoneyMinorUnits.CURRENCY_SCALE, MoneyMinorUnits.ROUNDING_MODE)
        for (plan in accountPlans) {
            val effectivePolicy = plan.creditReservePolicy.ifBlank { account.installmentReservePolicy }
            val reservedForPlanMinor = InstallmentScheduleEngine.calculateReservedPrincipalMinor(
                plan = plan,
                scheduleItems = itemsByPlan[plan.id].orEmpty(),
                postedTransactions = accountTxs,
                policyOverride = effectivePolicy
            )
            reservedPrincipalBd = reservedPrincipalBd.add(MoneyMinorUnits.toBigDecimal(reservedForPlanMinor))
        }
        val reservedInstallmentDebtMinor = MoneyMinorUnits.fromBigDecimal(reservedPrincipalBd)

        val totalDebtBd = revolvingDebtBd.add(reservedPrincipalBd)
        val totalDebtMinor = MoneyMinorUnits.fromBigDecimal(totalDebtBd)

        val creditSurplusMinor = if (totalDebtMinor < 0L) -totalDebtMinor else 0L
        val positiveDebtMinor = totalDebtMinor.coerceAtLeast(0L)
        val availableCreditMinor = (account.creditLimitMinor - positiveDebtMinor).coerceAtLeast(0L)
        val utilizationPercent = if (account.creditLimitMinor > 0L && positiveDebtMinor > 0L) {
            MoneyMinorUnits.percentOf(positiveDebtMinor, account.creditLimitMinor)
        } else {
            BigDecimal.ZERO.setScale(MoneyMinorUnits.CURRENCY_SCALE, MoneyMinorUnits.ROUNDING_MODE)
        }

        return AccountBalanceSnapshot(
            accountId = account.id,
            accountType = account.accountType,
            openingBalanceMinor = account.openingBalanceMinor,
            totalInflowsMinor = totalInflowsMinor,
            totalOutflowsMinor = totalOutflowsMinor,
            currentBalanceMinor = -totalDebtMinor,
            revolvingDebtMinor = revolvingDebtMinor,
            reservedInstallmentDebtMinor = reservedInstallmentDebtMinor,
            totalDebtMinor = totalDebtMinor,
            availableCreditMinor = availableCreditMinor,
            creditSurplusMinor = creditSurplusMinor,
            utilizationPercent = utilizationPercent,
            movementsCount = accountTxs.size
        )
    }

    fun calculatePortfolioSummary(
        accounts: List<FinancialAccountEntity>,
        transactions: List<TransactionEntity>,
        installmentPlans: List<InstallmentPlanEntity> = emptyList(),
        scheduleItems: List<InstallmentScheduleItemEntity> = emptyList()
    ): AccountsPortfolioSummary {
        val activeAccounts = accounts.filter { !it.isArchived }
        val snapshots = LinkedHashMap<Long, AccountBalanceSnapshot>(activeAccounts.size)

        var liquidAssetsBd = BigDecimal.ZERO.setScale(MoneyMinorUnits.CURRENCY_SCALE, MoneyMinorUnits.ROUNDING_MODE)
        var cardDebtBd = BigDecimal.ZERO.setScale(MoneyMinorUnits.CURRENCY_SCALE, MoneyMinorUnits.ROUNDING_MODE)

        for (account in activeAccounts) {
            val snap = calculateAccountSnapshot(account, transactions, installmentPlans, scheduleItems)
            snapshots[account.id] = snap
            if (AccountType.isLiquidAsset(account.accountType)) {
                if (account.includeInTotalLiquidity) {
                    liquidAssetsBd = liquidAssetsBd.add(snap.currentBalance)
                }
            } else {
                val effectiveDebtBd = if (snap.totalDebtMinor > 0L) snap.totalDebt else BigDecimal.ZERO
                cardDebtBd = cardDebtBd.add(effectiveDebtBd)
                if (snap.creditSurplusMinor > 0L && account.includeInTotalLiquidity) {
                    liquidAssetsBd = liquidAssetsBd.add(snap.creditSurplus)
                }
            }
        }

        val unassignedTxs = transactions.filter {
            it.accountId == null &&
                !it.isGastoSupuesto &&
                it.tentativeDecision != "DISCARDED" &&
                !it.isSuspectedDuplicate
        }
        var unassignedNetBd = BigDecimal.ZERO.setScale(MoneyMinorUnits.CURRENCY_SCALE, MoneyMinorUnits.ROUNDING_MODE)
        for (tx in unassignedTxs) {
            val amtBd = MoneyMinorUnits.toBigDecimal(tx.effectiveAmountMinor)
            unassignedNetBd = if (tx.type == TransactionType.INCOME) {
                unassignedNetBd.add(amtBd)
            } else {
                unassignedNetBd.subtract(amtBd)
            }
        }

        val totalLiquidMinor = MoneyMinorUnits.fromBigDecimal(liquidAssetsBd)
        val totalDebtMinor = MoneyMinorUnits.fromBigDecimal(cardDebtBd)
        val netWorthMinor = MoneyMinorUnits.fromBigDecimal(liquidAssetsBd.subtract(cardDebtBd))

        return AccountsPortfolioSummary(
            snapshotsByAccountId = snapshots,
            totalLiquidAssetsMinor = totalLiquidMinor,
            totalCreditCardDebtMinor = totalDebtMinor,
            netWorthMinor = netWorthMinor,
            unassignedTransactionsCount = unassignedTxs.size,
            unassignedNetImpactMinor = MoneyMinorUnits.fromBigDecimal(unassignedNetBd)
        )
    }

    /** Verifica si un pago de tarjeta cubre el saldo o si es un abono parcial válido. */
    fun validateCreditCardPayment(
        sourceAccount: FinancialAccountEntity,
        sourceSnapshot: AccountBalanceSnapshot,
        targetCard: FinancialAccountEntity,
        targetSnapshot: AccountBalanceSnapshot,
        paymentAmountMinor: Long
    ): CardPaymentValidation {
        if (sourceAccount.id == targetCard.id) {
            return CardPaymentValidation(isValid = false, reason = "La cuenta origen y destino deben ser distintas.")
        }
        if (targetCard.accountType != AccountType.CREDIT_CARD) {
            return CardPaymentValidation(isValid = false, reason = "La cuenta destino debe ser una tarjeta de crédito.")
        }
        if (paymentAmountMinor <= 0L) {
            return CardPaymentValidation(isValid = false, reason = "El monto del abono debe ser mayor a Q0.00.")
        }
        val resultingRevolvingDebtMinor = targetSnapshot.revolvingDebtMinor - paymentAmountMinor
        val isPartial = resultingRevolvingDebtMinor > 0L
        val isOverpayment = resultingRevolvingDebtMinor < 0L
        val sourceRemainingMinor = sourceSnapshot.currentBalanceMinor - paymentAmountMinor
        return CardPaymentValidation(
            isValid = true,
            isPartialPayment = isPartial,
            isOverpayment = isOverpayment,
            resultingRevolvingDebtMinor = resultingRevolvingDebtMinor,
            sourceRemainingBalanceMinor = sourceRemainingMinor
        )
    }
}

data class CardPaymentValidation(
    val isValid: Boolean,
    val reason: String = "",
    val isPartialPayment: Boolean = false,
    val isOverpayment: Boolean = false,
    val resultingRevolvingDebtMinor: Long = 0L,
    val sourceRemainingBalanceMinor: Long = 0L
)
