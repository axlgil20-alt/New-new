package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.RecurringExpenseEntity
import com.example.data.model.SavingsGoalEntity
import com.example.ui.viewmodel.BudgetUiState

/**
 * Pantalla de Límites de Presupuesto, Gastos Fijos y Metas de Ahorro.
 * Botones uniformes con altura fija de 48dp y Material Icons (+) limpios.
 */
@Composable
fun LimitsScreen(
    uiState: BudgetUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleAllTime: () -> Unit,
    onAddBudgetLimitClick: () -> Unit,
    onDeleteBudgetLimit: (String) -> Unit,
    onAddRecurringExpenseClick: () -> Unit,
    onDeleteRecurringExpense: (RecurringExpenseEntity) -> Unit,
    onToggleRecurringExpense: (RecurringExpenseEntity) -> Unit,
    onAddSavingsGoalClick: () -> Unit,
    onDeleteSavingsGoal: (SavingsGoalEntity) -> Unit,
    onContributeSavingsGoal: (SavingsGoalEntity, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    BudgetsScreen(
        uiState = uiState,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        onToggleAllTime = onToggleAllTime,
        onAddBudgetLimitClick = onAddBudgetLimitClick,
        onDeleteBudgetLimit = onDeleteBudgetLimit,
        onAddRecurringExpenseClick = onAddRecurringExpenseClick,
        onDeleteRecurringExpense = onDeleteRecurringExpense,
        onToggleRecurringExpense = onToggleRecurringExpense,
        onAddSavingsGoalClick = onAddSavingsGoalClick,
        onDeleteSavingsGoal = onDeleteSavingsGoal,
        onContributeSavingsGoal = onContributeSavingsGoal,
        modifier = modifier
    )
}
