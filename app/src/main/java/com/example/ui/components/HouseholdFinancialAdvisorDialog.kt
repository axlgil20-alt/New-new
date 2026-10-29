package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.ui.viewmodel.BudgetUiState

/**
 * Asesor Financiero Familiar (HouseholdFinancialAdvisorDialog).
 * Delega directamente al Asesor Financiero IA conversacional con buscador e ícono de lupa (🔍).
 */
@Composable
fun HouseholdFinancialAdvisorDialog(
    uiState: BudgetUiState,
    onDismiss: () -> Unit
) {
    FinancialAdvisorDialog(
        uiState = uiState,
        onDismiss = onDismiss
    )
}
