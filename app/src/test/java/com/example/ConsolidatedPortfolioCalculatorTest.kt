package com.example

import com.example.data.model.AccountType
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.TransactionEntryKind
import com.example.data.model.TransactionType
import com.example.data.model.TransactionEntity
import com.example.data.model.affectsBudgetTotals
import com.example.data.service.ConsolidatedPortfolioCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsolidatedPortfolioCalculatorTest {
    @Test
    fun missingRate_keepsNativeBalance_andMarksPortfolioPartial() {
        val gtq = FinancialAccountEntity(
            id = 1L,
            name = "Cuenta GTQ",
            accountType = AccountType.BANK_CHECKING,
            currencyCode = "GTQ",
            openingBalanceMinor = 1_000L
        )
        val usd = FinancialAccountEntity(
            id = 2L,
            name = "Cuenta USD",
            accountType = AccountType.BANK_SAVINGS,
            currencyCode = "USD",
            openingBalanceMinor = 10_000L
        )
        val eur = FinancialAccountEntity(
            id = 3L,
            name = "Cuenta EUR",
            accountType = AccountType.CASH,
            currencyCode = "EUR",
            openingBalanceMinor = 5_000L
        )
        val result = ConsolidatedPortfolioCalculator.calculate(
            primaryCurrencyCode = "GTQ",
            accounts = listOf(gtq, usd, eur),
            transactions = emptyList(),
            rates = listOf(
                ExchangeRateEntity(
                    baseCurrencyCode = "USD",
                    quoteCurrencyCode = "GTQ",
                    rateNumerator = "7.8",
                    effectiveAtMillis = 1L,
                    observedAtMillis = 1L,
                    source = "MANUAL"
                )
            )
        )

        assertTrue(result.isPartial)
        assertEquals(setOf("EUR"), result.missingCurrencyCodes)
        assertEquals(2, result.valuedAccounts.size)
        assertEquals(79_000L, result.consolidatedLiquidAssetsMinor)
        assertEquals(5_000L, result.unvaluedAccounts.single { it.accountId == 3L }.nativeBalanceMinor)
    }

    @Test
    fun transferCharges_remainRealLedgerEntries_withoutBudgetImpact() {
        val fee = TransactionEntity(
            title = "Comisión bancaria",
            amount = 1.50,
            type = TransactionType.EXPENSE,
            category = "Comisión bancaria",
            dateMillis = 1L,
            amountMinor = 150L,
            entryKind = TransactionEntryKind.TRANSFER_FEE
        )
        val fx = fee.copy(
            title = "Diferencia cambiaria",
            category = "Diferencia cambiaria",
            entryKind = TransactionEntryKind.TRANSFER_FX_DIFFERENCE
        )
        assertTrue(!fee.affectsBudgetTotals)
        assertTrue(!fx.affectsBudgetTotals)
    }
}
