package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.TransactionEntity
import com.example.ui.viewmodel.BudgetUiState

/**
 * Pantalla de Historial de Registros y Transacciones (TransactionHistoryScreen).
 * Incluye barra de búsqueda superior en tiempo real para filtrar por concepto o monto.
 */
@Composable
fun TransactionHistoryScreen(
    uiState: BudgetUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleAllTime: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterTypeChange: (String?) -> Unit,
    onFilterCategoryChange: (String?) -> Unit,
    onFilterMemberChange: (String?) -> Unit,
    onToggleOnlyHighlighted: (Boolean) -> Unit,
    onAddTransactionClick: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onHighlightTransaction: (TransactionEntity, Long) -> Unit,
    onToggleVerifyTransaction: (TransactionEntity) -> Unit,
    onDecideTentative: (TransactionEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    TransactionsScreen(
        uiState = uiState,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        onToggleAllTime = onToggleAllTime,
        onSearchQueryChange = onSearchQueryChange,
        onFilterTypeChange = onFilterTypeChange,
        onFilterCategoryChange = onFilterCategoryChange,
        onFilterMemberChange = onFilterMemberChange,
        onToggleOnlyHighlighted = onToggleOnlyHighlighted,
        onAddTransactionClick = onAddTransactionClick,
        onEditTransaction = onEditTransaction,
        onDeleteTransaction = onDeleteTransaction,
        onHighlightTransaction = onHighlightTransaction,
        onToggleVerifyTransaction = onToggleVerifyTransaction,
        onDecideTentative = onDecideTentative,
        modifier = modifier
    )
}
