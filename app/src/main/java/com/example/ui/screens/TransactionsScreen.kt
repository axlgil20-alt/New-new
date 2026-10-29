package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ReceiptLong
import com.example.ui.theme.EmeraldGreen
import com.example.data.model.DefaultData
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TentativePurple
import com.example.ui.viewmodel.BudgetUiState

import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.components.ExcelCsvImporter
import com.example.ui.components.HeaderVolcanoBackground

@Composable
fun TransactionsScreen(
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
    onExportCsv: (suspend () -> String)? = null,
    onImportCsv: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showCsvDialog by remember { mutableStateOf(false) }

    if (showCsvDialog && onExportCsv != null) {
        ExcelCsvImporter(
            initialExportMode = true,
            onDismiss = { showCsvDialog = false },
            onImportText = { text -> onImportCsv?.invoke(text) },
            onExportRequest = onExportCsv
        )
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Fondo sutil de volcanes y Tikal en la cabecera
        HeaderVolcanoBackground(height = 180.dp, alpha = 0.20f)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Encabezado Oficial de Registros con Identidad Esmeralda
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldGreen,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Historial de Registros",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (uiState.isDarkTheme) Color.White else EmeraldGreen
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onAddTransactionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nuevo",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            item {
                MonthSelectorBar(
                    currentMonthLabel = uiState.currentMonthLabel,
                    isAllTime = uiState.isAllTime,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onToggleAllTime = onToggleAllTime
                )
            }

            // Barra de Búsqueda Superior en Tiempo Real (Concepto o Monto) + Botón Exportar CSV
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Buscar por concepto o monto...") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = EmeraldGreen)
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("transactions_search_bar"),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        if (onExportCsv != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = EmeraldGreen,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("export_csv_button")
                            ) {
                                IconButton(
                                    onClick = { showCsvDialog = true },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Exportar Reporte CSV",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Encontrados: ${uiState.filteredTransactions.size} registro(s) para \"${uiState.searchQuery}\"",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Filtros de Tipo (Todos, Gastos, Ingresos, Supuestos, Resaltados)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.filterType == null && !uiState.onlyHighlighted,
                        onClick = {
                            onFilterTypeChange(null)
                            onToggleOnlyHighlighted(false)
                        },
                        label = { Text("Todos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen,
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = uiState.filterType == TransactionType.EXPENSE && !uiState.onlyHighlighted,
                        onClick = {
                            onFilterTypeChange(TransactionType.EXPENSE)
                            onToggleOnlyHighlighted(false)
                        },
                        label = { Text("Gastos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEE2E2),
                            selectedLabelColor = ExpenseRed
                        )
                    )

                    FilterChip(
                        selected = uiState.filterType == TransactionType.INCOME && !uiState.onlyHighlighted,
                        onClick = {
                            onFilterTypeChange(TransactionType.INCOME)
                            onToggleOnlyHighlighted(false)
                        },
                        label = { Text("Ingresos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD1FAE5),
                            selectedLabelColor = IncomeGreen
                        )
                    )

                    FilterChip(
                        selected = uiState.filterType == "TENTATIVE",
                        onClick = {
                            onFilterTypeChange(if (uiState.filterType == "TENTATIVE") null else "TENTATIVE")
                            onToggleOnlyHighlighted(false)
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Supuestos / Simulación") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEDE9FE),
                            selectedLabelColor = TentativePurple
                        )
                    )

                    FilterChip(
                        selected = uiState.onlyHighlighted,
                        onClick = { onToggleOnlyHighlighted(!uiState.onlyHighlighted) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.FormatColorFill, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Resaltados") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Filtro por Miembro Familiar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaultData.familyMembers.forEach { member ->
                        val isSelected = (uiState.filterMember == member) || (member == "Todos" && uiState.filterMember == null)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onFilterMemberChange(if (member == "Todos") null else member)
                            },
                            label = { Text(member) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Resumen de la lista
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mostrando ${uiState.filteredTransactions.size} registro(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
            }

            // Lista de transacciones
            if (uiState.filteredTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No hay transacciones en este filtro o periodo.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(
                    items = uiState.filteredTransactions,
                    key = { it.id }
                ) { tx ->
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

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
