package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.HeaderVolcanoBackground
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.BudgetUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PaymentDatePeriod(
    val title: String,
    val dayLabel: String,
    val income: Double,
    val expense: Double,
    val net: Double,
    val transactions: List<TransactionEntity>
)

@Composable
fun PlannedVsRealScreen(
    uiState: BudgetUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleAllTime: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onHighlightTransaction: (TransactionEntity, Long) -> Unit,
    onToggleVerifyTransaction: (TransactionEntity) -> Unit,
    onDecideTentative: (TransactionEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 0: Ambos, 1: 1a Quincena (15), 2: 2a Quincena (Fin de Mes)
    var selectedCycleTab by remember { mutableIntStateOf(0) }

    val quincenaData = remember(uiState.allTransactions, uiState.filteredTransactions, uiState.currentYear, uiState.currentMonthIndex) {
        val baseList = if (uiState.allTransactions.isNotEmpty()) uiState.allTransactions else uiState.filteredTransactions
        com.example.ui.util.QuincenaFilterHelper.filterByQuincena(
            transactions = baseList,
            targetYear = uiState.currentYear,
            targetMonthIndex = uiState.currentMonthIndex
        )
    }

    val monthCal = remember(uiState.currentYear, uiState.currentMonthIndex) {
        Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, uiState.currentYear)
            set(Calendar.MONTH, uiState.currentMonthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val currentMonthLabel = remember(monthCal) {
        SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(monthCal.time).replaceFirstChar { it.uppercase() }
    }

    val q1Income = quincenaData.q1Income
    val q1Expense = quincenaData.q1Expense
    val q1Net = quincenaData.q1Net

    val q2Income = quincenaData.q2Income
    val q2Expense = quincenaData.q2Expense
    val q2Net = quincenaData.q2Net

    val displayList = when (selectedCycleTab) {
        1 -> quincenaData.q1Transactions
        2 -> quincenaData.q2Transactions
        else -> quincenaData.monthTransactions
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Fondo sutil de volcanes y Tikal en la cabecera
        HeaderVolcanoBackground(height = 180.dp, alpha = 0.20f)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 76.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        item {
            MonthSelectorBar(
                currentMonthLabel = currentMonthLabel,
                isAllTime = false,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onToggleAllTime = onToggleAllTime
            )
        }

        // Selector de Pestaña de Quincena
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCycleTab == 0,
                    onClick = { selectedCycleTab = 0 },
                    label = { Text("Mes Completo") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedCycleTab == 1,
                    onClick = { selectedCycleTab = 1 },
                    label = { Text("1ª Quincena (15)") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedCycleTab == 2,
                    onClick = { selectedCycleTab = 2 },
                    label = { Text("2ª Quincena (Fin)") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Tarjetas Comparativas Quincenales
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tarjeta 1ª Quincena
                Card(
                    onClick = { selectedCycleTab = if (selectedCycleTab == 1) 0 else 1 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = if (selectedCycleTab == 1) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (selectedCycleTab == 1) 4.dp else 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "1ª Quincena",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Pago del día 15",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "+${FormatUtils.formatCurrency(q1Income, uiState.currencySymbol, uiState.isDiscreetMode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = IncomeGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "-${FormatUtils.formatCurrency(q1Expense, uiState.currencySymbol, uiState.isDiscreetMode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ExpenseRed,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Neto: ${FormatUtils.formatCurrency(q1Net, uiState.currencySymbol, uiState.isDiscreetMode)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (q1Net >= 0) Color(0xFF0F5132) else ExpenseRed
                        )
                    }
                }

                // Tarjeta 2ª Quincena
                Card(
                    onClick = { selectedCycleTab = if (selectedCycleTab == 2) 0 else 2 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = if (selectedCycleTab == 2) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (selectedCycleTab == 2) 4.dp else 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "2ª Quincena",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Fin de mes (30/31)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "+${FormatUtils.formatCurrency(q2Income, uiState.currencySymbol, uiState.isDiscreetMode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = IncomeGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "-${FormatUtils.formatCurrency(q2Expense, uiState.currencySymbol, uiState.isDiscreetMode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ExpenseRed,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Neto: ${FormatUtils.formatCurrency(q2Net, uiState.currencySymbol, uiState.isDiscreetMode)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (q2Net >= 0) Color(0xFF0F5132) else ExpenseRed
                        )
                    }
                }
            }
        }

        // Título de la lista y resumen activo
        item {
            val activeIncome = when (selectedCycleTab) {
                1 -> q1Income
                2 -> q2Income
                else -> quincenaData.totalIncome
            }
            val activeExpense = when (selectedCycleTab) {
                1 -> q1Expense
                2 -> q2Expense
                else -> quincenaData.totalExpense
            }
            val activeNet = when (selectedCycleTab) {
                1 -> q1Net
                2 -> q2Net
                else -> quincenaData.totalNet
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedCycleTab) {
                            1 -> "Detalle 1ª Quincena (Día 1 al 15)"
                            2 -> "Detalle 2ª Quincena (Día 16 al fin de mes)"
                            else -> "Detalle de Ambos Ciclos Quincenales"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${displayList.size} transacciones",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Totales filtrados: +${FormatUtils.formatCurrency(activeIncome, uiState.currencySymbol, uiState.isDiscreetMode)} | -${FormatUtils.formatCurrency(activeExpense, uiState.currencySymbol, uiState.isDiscreetMode)} | Saldo: ${FormatUtils.formatCurrency(activeNet, uiState.currencySymbol, uiState.isDiscreetMode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(displayList, key = { it.id }) { tx ->
            TransactionItemCard(
                transaction = tx,
                currencySymbol = uiState.currencySymbol,
                onEdit = { onEditTransaction(tx) },
                onDelete = { onDeleteTransaction(tx) },
                onHighlight = { hex: Long -> onHighlightTransaction(tx, hex) },
                onToggleVerify = { onToggleVerifyTransaction(tx) },
                onDecideTentative = { decision: String -> onDecideTentative(tx, decision) },
                isDiscreetMode = uiState.isDiscreetMode
            )
        }
    }
}
}
