package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.math.RoundingMode

/** Estados persistidos de una conversión; no implican que el importe original haya cambiado. */
object ConversionStatus {
    const val NOT_REQUIRED = "NOT_REQUIRED"
    const val CONVERTED = "CONVERTED"
    const val RATE_MISSING = "RATE_MISSING"
    const val RATE_STALE = "RATE_STALE"
}

/** Tipos de operación compuesta soportados en esta etapa. */
object CurrencyOperationType {
    const val TRANSFER = "TRANSFER"
    const val CARD_PAYMENT = "CARD_PAYMENT"
}

/**
 * Historial local de una cotización. La tasa se representa como decimal textual para evitar
 * pérdida de precisión por Double. Semántica: 1 unidad de base = numerator / denominator de quote.
 */
@Entity(
    tableName = "exchange_rates",
    indices = [
        Index(
            name = "idx_exchange_rates_pair_effective",
            value = ["baseCurrencyCode", "quoteCurrencyCode", "effectiveAtMillis"]
        ),
        Index(
            name = "idx_exchange_rates_source_reference",
            value = ["source", "sourceReference"],
            unique = true
        )
    ]
)
data class ExchangeRateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val baseCurrencyCode: String,
    val quoteCurrencyCode: String,
    val rateNumerator: String,
    val rateDenominator: String = "1",
    val effectiveAtMillis: Long,
    val observedAtMillis: Long,
    val source: String,
    val sourceReference: String? = null,
    val isManual: Boolean = false,
    val isEstimated: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun rate(): BigDecimal {
        val numerator = BigDecimal(rateNumerator)
        val denominator = BigDecimal(rateDenominator)
        require(denominator.signum() > 0) { "El denominador de la tasa debe ser positivo." }
        require(numerator.signum() > 0) { "El numerador de la tasa debe ser positivo." }
        return numerator.divide(denominator, 16, RoundingMode.HALF_UP)
    }
}

/**
 * Detalle contable de una operación multicurrency. Los asientos de transactions siguen siendo
 * el libro mayor; esta fila explica la relación origen/destino/tasa/comisión.
 */
@Entity(
    tableName = "currency_operation_details",
    indices = [
        Index(name = "idx_currency_operation_group_unique", value = ["operationGroupId"], unique = true),
        Index(name = "idx_currency_operation_rate", value = ["rateId"])
    ]
)
data class CurrencyOperationDetailEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val operationGroupId: String,
    val operationType: String,
    val sourceAccountId: Long,
    val destinationAccountId: Long,
    val sourceAmountMinor: Long,
    val sourceCurrencyCode: String,
    val destinationAmountMinor: Long,
    val destinationCurrencyCode: String,
    val feeAmountMinor: Long = 0L,
    val feeCurrencyCode: String? = null,
    val fxDifferenceAmountMinor: Long = 0L,
    val fxDifferenceCurrencyCode: String? = null,
    val rateId: Long? = null,
    val rateNumerator: String,
    val rateDenominator: String = "1",
    val rateAsOfMillis: Long,
    val rateSource: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class CurrencyTransferInput(
    val sourceAccountId: Long,
    val destinationAccountId: Long,
    val sourceAmountMinor: Long,
    val destinationAmountMinor: Long,
    val rateId: Long? = null,
    val rateNumerator: String,
    val rateDenominator: String = "1",
    val rateAsOfMillis: Long,
    val rateSource: String,
    val feeAmountMinor: Long = 0L,
    val feeCurrencyCode: String? = null,
    val fxDifferenceAmountMinor: Long = 0L,
    val fxDifferenceCurrencyCode: String? = null,
    val title: String = "",
    val notes: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val operationKey: String? = null
)

data class ConsolidatedAccountValue(
    val accountId: Long,
    val accountCurrencyCode: String,
    val nativeBalanceMinor: Long,
    val primaryValueMinor: Long?,
    val isValued: Boolean,
    val reason: String? = null
)

data class ConsolidatedPortfolioResult(
    val primaryCurrencyCode: String,
    val valuedAccounts: List<ConsolidatedAccountValue>,
    val unvaluedAccounts: List<ConsolidatedAccountValue>,
    val consolidatedLiquidAssetsMinor: Long,
    val consolidatedDebtMinor: Long,
    val consolidatedNetWorthMinor: Long,
    val isPartial: Boolean,
    val missingCurrencyCodes: Set<String>
)
