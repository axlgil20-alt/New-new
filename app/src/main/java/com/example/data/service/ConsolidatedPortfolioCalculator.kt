package com.example.data.service

import com.example.data.model.ConsolidatedAccountValue
import com.example.data.model.ConsolidatedPortfolioResult
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.InstallmentScheduleItemEntity
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.AccountType
import com.example.data.model.TransactionEntity
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Consolida patrimonio únicamente para presentación. Nunca cambia el saldo nativo de una cuenta
 * ni escribe conversiones en transactions. El llamador debe entregar en [rates] únicamente tasas
 * válidas para la fecha y política solicitadas; este cálculo no usa una tasa antigua como actual.
 */
object ConsolidatedPortfolioCalculator {
    private val ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)

    fun calculate(
        primaryCurrencyCode: String,
        accounts: List<FinancialAccountEntity>,
        transactions: List<TransactionEntity>,
        rates: List<ExchangeRateEntity>,
        installmentPlans: List<InstallmentPlanEntity> = emptyList(),
        scheduleItems: List<InstallmentScheduleItemEntity> = emptyList()
    ): ConsolidatedPortfolioResult {
        val valued = mutableListOf<ConsolidatedAccountValue>()
        val unvalued = mutableListOf<ConsolidatedAccountValue>()
        var liquidAssets = ZERO
        var debt = ZERO
        val missingCurrencies = linkedSetOf<String>()

        accounts.filter { !it.isArchived }.forEach { account ->
            val snapshot = AccountBalanceCalculator.calculateAccountSnapshot(
                account = account,
                transactions = transactions,
                installmentPlans = installmentPlans,
                scheduleItems = scheduleItems
            )
            val nativeBalanceMinor = if (AccountType.isCreditCard(account.accountType)) {
                snapshot.totalDebtMinor
            } else {
                snapshot.currentBalanceMinor
            }
            val signedNative = if (AccountType.isCreditCard(account.accountType)) {
                -snapshot.totalDebtMinor
            } else {
                snapshot.currentBalanceMinor
            }
            val converted = convertMinor(
                amountMinor = signedNative,
                fromCurrency = account.currencyCode,
                toCurrency = primaryCurrencyCode,
                rates = rates
            )
            val value = ConsolidatedAccountValue(
                accountId = account.id,
                accountCurrencyCode = account.currencyCode,
                nativeBalanceMinor = nativeBalanceMinor,
                primaryValueMinor = converted?.let { MoneyMinorUnits.fromBigDecimal(it) },
                isValued = converted != null,
                reason = if (converted == null) "RATE_MISSING" else null
            )
            if (converted == null) {
                unvalued += value
                missingCurrencies += account.currencyCode
            } else {
                valued += value
                if (AccountType.isCreditCard(account.accountType)) {
                    debt = debt.add(converted.negate())
                } else if (account.includeInTotalLiquidity) {
                    liquidAssets = liquidAssets.add(converted)
                }
            }
        }

        val liquidMinor = MoneyMinorUnits.fromBigDecimal(liquidAssets.max(ZERO))
        val debtMinor = MoneyMinorUnits.fromBigDecimal(debt.max(ZERO))
        val netWorthMinor = MoneyMinorUnits.fromBigDecimal(liquidAssets.subtract(debt))
        return ConsolidatedPortfolioResult(
            primaryCurrencyCode = primaryCurrencyCode,
            valuedAccounts = valued,
            unvaluedAccounts = unvalued,
            consolidatedLiquidAssetsMinor = liquidMinor,
            consolidatedDebtMinor = debtMinor,
            consolidatedNetWorthMinor = netWorthMinor,
            isPartial = unvalued.isNotEmpty(),
            missingCurrencyCodes = missingCurrencies
        )
    }

    private fun convertMinor(
        amountMinor: Long,
        fromCurrency: String,
        toCurrency: String,
        rates: List<ExchangeRateEntity>
    ): BigDecimal? {
        if (fromCurrency == toCurrency) return MoneyMinorUnits.toBigDecimal(amountMinor)
        val rate = rates.firstOrNull {
            it.baseCurrencyCode == fromCurrency && it.quoteCurrencyCode == toCurrency
        } ?: return null
        val converted = MoneyMinorUnits.toBigDecimal(amountMinor).multiply(rate.rate())
        return converted.setScale(MoneyMinorUnits.CURRENCY_SCALE, RoundingMode.HALF_UP)
    }
}
