package com.example.ui.util

object FormatUtils {
    fun formatCurrency(amount: Double, currencySymbol: String = "Q ", isDiscreetMode: Boolean): String {
        return if (isDiscreetMode) "Q ****.**" else "$currencySymbol${"%,.2f".format(amount)}"
    }

    fun formatSignedCurrency(amount: Double, prefix: String, isDiscreetMode: Boolean): String {
        return if (isDiscreetMode) "Q ****.**" else "$prefix${"%,.2f".format(amount)}"
    }

    fun formatCurrencyRounded(amount: Double, currencySymbol: String = "Q ", isDiscreetMode: Boolean): String {
        return if (isDiscreetMode) "Q ****.**" else "$currencySymbol${"%,.0f".format(amount)}"
    }

    fun formatAmount(
        amount: Double,
        currencySymbol: String = "Q ",
        isDiscreet: Boolean = false
    ): String {
        return formatCurrency(amount, currencySymbol, isDiscreet)
    }

    fun formatMoney(
        currencySymbol: String = "Q",
        amount: Double,
        isDiscreetMode: Boolean = false
    ): String {
        val sym = if (currencySymbol.endsWith(" ")) currencySymbol else "$currencySymbol "
        return if (isDiscreetMode) "${sym}****.**" else "$sym${"%,.2f".format(amount)}"
    }
}
