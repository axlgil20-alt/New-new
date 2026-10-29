package com.example.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.TransactionEntity
import com.example.ui.viewmodel.BudgetUiState

/**
 * DashboardScreen - Delegado directo a InicioScreen con ambientación guatemalteca completa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: BudgetUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleAllTime: () -> Unit,
    onToggleIncludeTentative: () -> Unit,
    onToggleDiscreetMode: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onAddAntExpenseClick: () -> Unit,
    onOpenAdvisorClick: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onHighlightTransaction: (TransactionEntity, Long) -> Unit,
    onToggleVerifyTransaction: (TransactionEntity) -> Unit,
    onDecideTentative: (TransactionEntity, String) -> Unit,
    onToggleDarkTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    InicioScreen(
        uiState = uiState,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        onToggleAllTime = onToggleAllTime,
        onToggleIncludeTentative = onToggleIncludeTentative,
        onToggleDiscreetMode = onToggleDiscreetMode,
        onAddTransactionClick = onAddTransactionClick,
        onAddAntExpenseClick = onAddAntExpenseClick,
        onOpenAdvisorClick = onOpenAdvisorClick,
        onEditTransaction = onEditTransaction,
        onDeleteTransaction = onDeleteTransaction,
        onHighlightTransaction = onHighlightTransaction,
        onToggleVerifyTransaction = onToggleVerifyTransaction,
        onDecideTentative = onDecideTentative,
        onToggleDarkTheme = onToggleDarkTheme,
        modifier = modifier
    )
}
