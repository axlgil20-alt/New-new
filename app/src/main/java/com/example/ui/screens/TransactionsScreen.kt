package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DefaultData
import com.example.data.model.DuplicateFilterMode
import com.example.data.model.FilterPeriod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.isGastoSupuesto
import com.example.ui.components.AlfombraCenefa
import com.example.ui.components.AntiguaHeaderBackground
import com.example.ui.components.ExcelCsvImporter
import com.example.ui.components.HeaderVolcanoBackground
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.SafeQuetziAvatar
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.CoralRed
import com.example.ui.theme.CoralRedLight
import com.example.ui.theme.DeepNavyBlue
import com.example.ui.theme.DeepNavyLight
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.GoldenYellowLight
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenLight
import com.example.ui.theme.IvoryBorder
import com.example.ui.theme.IvoryWarm
import com.example.ui.theme.JadeGreen
import com.example.ui.theme.JadeGreenLight
import com.example.ui.theme.NazarenoPurple
import com.example.ui.theme.NazarenoPurpleLight
import com.example.ui.theme.TentativePurple
import com.example.ui.theme.TentativePurpleLight
import com.example.ui.theme.TurquoiseAccent
import com.example.ui.theme.TurquoiseLight
import com.example.ui.viewmodel.BudgetUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    // Controladores avanzados Quicken Simplifi
    onFilterPeriodChange: ((String) -> Unit)? = null,
    onFilterSpecificDateChange: ((Long?) -> Unit)? = null,
    onFilterDateRangeChange: ((Long?, Long?) -> Unit)? = null,
    onFilterQuincenaChange: ((Int) -> Unit)? = null,
    onFilterSubCategoryChange: ((String?) -> Unit)? = null,
    onFilterBeneficiaryChange: ((String?) -> Unit)? = null,
    onFilterStatusChange: ((String?) -> Unit)? = null,
    onFilterSourceChange: ((String?) -> Unit)? = null,
    onFilterDuplicatesModeChange: ((String) -> Unit)? = null,
    onClearAllFilters: (() -> Unit)? = null,
    onDeleteTransactions: ((List<Long>) -> Unit)? = null,
    onMarkTransactionsAsLegitimate: ((List<Long>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCsvDialog by remember { mutableStateOf(false) }
    var showAdvancedFiltersSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Selección múltiple para gestión y eliminación de duplicados
    val selectedIds = remember { mutableStateListOf<Long>() }
    val isDuplicatesReviewMode = uiState.filterDuplicatesMode == DuplicateFilterMode.ONLY_DUPLICATES

    if (showCsvDialog && onExportCsv != null) {
        ExcelCsvImporter(
            initialExportMode = true,
            onDismiss = { showCsvDialog = false },
            onImportText = { text -> onImportCsv?.invoke(text) },
            onExportRequest = onExportCsv
        )
    }

    // Diálogo de confirmación para eliminar duplicados seleccionados
    if (showDeleteConfirmDialog) {
        val countToDelete = selectedIds.size
        val totalAmountToDelete = uiState.filteredTransactions
            .filter { selectedIds.contains(it.id) }
            .sumOf { it.amount }

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Confirmar Eliminación",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "¿Deseas eliminar definitivamente $countToDelete registro(s) duplicado(s) por un monto total de ${uiState.currencySymbol}${"%,.2f".format(totalAmountToDelete)}?\n\nEsta acción no se puede deshacer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransactions?.invoke(selectedIds.toList())
                        selectedIds.clear()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Eliminar Definitivamente", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal Bottom Sheet de Filtros Avanzados Quicken Simplifi
    if (showAdvancedFiltersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAdvancedFiltersSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AdvancedFiltersSheetContent(
                uiState = uiState,
                onDismiss = { showAdvancedFiltersSheet = false },
                onFilterPeriodChange = { onFilterPeriodChange?.invoke(it) },
                onFilterSpecificDateChange = { onFilterSpecificDateChange?.invoke(it) },
                onFilterDateRangeChange = { s, e -> onFilterDateRangeChange?.invoke(s, e) },
                onFilterQuincenaChange = { onFilterQuincenaChange?.invoke(it) },
                onFilterTypeChange = onFilterTypeChange,
                onFilterCategoryChange = onFilterCategoryChange,
                onFilterSubCategoryChange = { onFilterSubCategoryChange?.invoke(it) },
                onFilterBeneficiaryChange = { onFilterBeneficiaryChange?.invoke(it) },
                onFilterStatusChange = { onFilterStatusChange?.invoke(it) },
                onFilterSourceChange = { onFilterSourceChange?.invoke(it) },
                onFilterDuplicatesModeChange = { onFilterDuplicatesModeChange?.invoke(it) },
                onClearAllFilters = {
                    onClearAllFilters?.invoke()
                    selectedIds.clear()
                }
            )
        }
    }

    Box(modifier = modifier.fillMaxSize().background(if (uiState.isDarkTheme) MaterialTheme.colorScheme.background else IvoryWarm)) {
        // Fondo icónico de Antigua Guatemala (Arco de Santa Catalina y Volcán de Agua) con cenefa
        AntiguaHeaderBackground(height = 260.dp, alpha = 0.90f)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Encabezado Oficial de Registros con Botón de Filtros, Excel y Nuevo
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = JadeGreen,
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldenYellow.copy(alpha = 0.7f)),
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = GoldenYellowLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Historial de Registros",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Antigua Guatemala • Finanzas Sin Clavos",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldenYellowLight
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    // Botón Filtros Avanzados con Badge de conteo
                    BadgedBox(
                        badge = {
                            if (uiState.activeFiltersCount > 0) {
                                Badge(
                                    containerColor = CoralRed,
                                    contentColor = Color.White
                                ) {
                                    Text("${uiState.activeFiltersCount}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x66000000),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            IconButton(
                                onClick = { showAdvancedFiltersSheet = true },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("advanced_filters_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filtros Avanzados",
                                    tint = if (uiState.activeFiltersCount > 0) GoldenYellow else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Botón Exportar / Importar Excel & CSV
                    if (onExportCsv != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x66000000),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            IconButton(
                                onClick = { showCsvDialog = true },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("export_csv_top_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Importar y Exportar Excel",
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Botón Nuevo Registro
                    Button(
                        onClick = onAddTransactionClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JadeGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("add_transaction_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = GoldenYellowLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Nuevo", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            // 2. Selector de Mes y Periodo
            item {
                MonthSelectorBar(
                    currentMonthLabel = uiState.currentMonthLabel,
                    isAllTime = uiState.isAllTime,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onToggleAllTime = onToggleAllTime
                )
            }

            // 3. Tarjeta de Resumen Financiero Completo (Inspiración Alfombras de Antigua)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("simplifi_summary_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, IvoryBorder)
                ) {
                    Column {
                        // Cenefa superior tradicional de alfombra de aserrín
                        AlfombraCenefa(height = 6.dp)

                        Column(modifier = Modifier.padding(16.dp)) {
                            // Fila de Encabezado: Título y Filtros Activos
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(JadeGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Resumen de Fondos • ${uiState.filteredTransactions.size} registro(s)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (uiState.activeFiltersCount > 0) {
                                    TextButton(
                                        text = "Limpiar (${uiState.activeFiltersCount})",
                                        onClick = {
                                            onClearAllFilters?.invoke()
                                            selectedIds.clear()
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // MÉTRICA 1: Monto Total para Gastar (Ingresos + Roll-Over)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = JadeGreenLight.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, JadeGreen.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "TOTAL DISPONIBLE PARA GASTAR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = JadeGreen,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (uiState.isDiscreetMode) "Q ****.**" else "${uiState.currencySymbol}${"%,.2f".format(uiState.totalAmountToSpend)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Black,
                                            color = if (uiState.totalAmountToSpend >= 0) JadeGreen else CoralRed
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = JadeGreenLight
                                        ) {
                                            Text(
                                                text = "Ingresos: +${uiState.currencySymbol}${"%,.0f".format(uiState.filteredRealIncome)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF065F46),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    // Indicador de Salario Roll-Over si existe remanente de la quincena previa
                                    if (uiState.quincenaRolloverAmount > 0) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = GoldenYellowLight,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldenYellow.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.TrendingUp,
                                                    contentDescription = null,
                                                    tint = GoldenYellow,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Incluye +${uiState.currencySymbol}${"%,.2f".format(uiState.quincenaRolloverAmount)} de Salario Roll-Over (Ahorro Quincenal Previo)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF92400E)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // MÉTRICAS 2, 3, 4: FILA DE 3 GASTOS (DÉBITOS)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 2. Gastos Reales (Confirmados)
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = CoralRedLight.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = CoralRed, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(text = "Gastos Reales", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9F1239), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (uiState.isDiscreetMode) "Q ****.**" else "-${uiState.currencySymbol}${"%,.2f".format(uiState.filteredConfirmedExpense)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = CoralRed
                                        )
                                        Text(text = "Confirmados", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                                    }
                                }

                                // 3. Gastos Supuestos (Pendientes de Aprobación)
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = NazarenoPurpleLight,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NazarenoPurple.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = NazarenoPurple, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(text = "Gastos Supuestos", style = MaterialTheme.typography.labelSmall, color = Color(0xFF581C87), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (uiState.isDiscreetMode) "Q ****.**" else "-${uiState.currencySymbol}${"%,.2f".format(uiState.filteredSupuestoExpense)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = NazarenoPurple
                                        )
                                        Text(text = "Por Aprobar", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                                    }
                                }

                                // 4. Total Ambos Gastos (Reales + Supuestos)
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = DeepNavyLight,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepNavyBlue.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(text = "Total Ambos", style = MaterialTheme.typography.labelSmall, color = DeepNavyBlue, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (uiState.isDiscreetMode) "Q ****.**" else "-${uiState.currencySymbol}${"%,.2f".format(uiState.filteredTotalBothExpenses)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = DeepNavyBlue
                                        )
                                        Text(text = "Real + Supuesto", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // MÉTRICAS 5 & 6: FILA DE 2 SALDOS RESIDUALES
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 5. Dinero Real en Mano (Sin Supuestos)
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFECFDF5),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = "Saldo Real en Mano", style = MaterialTheme.typography.labelSmall, color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = if (uiState.isDiscreetMode) "Q ****.**" else "${uiState.currencySymbol}${"%,.2f".format(uiState.filteredRealRemainingBalance)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = if (uiState.filteredRealRemainingBalance >= 0) JadeGreen else CoralRed
                                        )
                                        Text(text = "Sin supuestos (en mano)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF047857), fontSize = 10.sp)
                                    }
                                }

                                // 6. Pronóstico Proyectado Final si se aprueban todos los supuestos
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = NazarenoPurpleLight,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NazarenoPurple.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = "Saldo Proyectado", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4C1D95), fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = if (uiState.isDiscreetMode) "Q ****.**" else "${uiState.currencySymbol}${"%,.2f".format(uiState.filteredProjectedRemainingBalance)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = if (uiState.filteredProjectedRemainingBalance >= 0) NazarenoPurple else CoralRed
                                        )
                                        Text(text = "Si apruebas supuestos", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B21A8), fontSize = 10.sp)
                                    }
                                }
                            }

                            // Subtotales por Categoría
                            if (uiState.filteredCategorySubtotals.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    uiState.filteredCategorySubtotals.take(5).forEach { stat ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFF1F5F9),
                                            border = androidx.compose.foundation.BorderStroke(0.8.dp, IvoryBorder),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${stat.category}: ${uiState.currencySymbol}${"%,.0f".format(stat.totalAmount)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Banner de Detección de Duplicados (Alerta visible y accionable)
            if (uiState.suspectedDuplicatesCount > 0 && !isDuplicatesReviewMode) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .testTag("duplicates_alert_banner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${uiState.suspectedDuplicatesCount} posibles duplicados detectados",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFB45309)
                                    )
                                    Text(
                                        text = "Mismo monto y fecha. Toca para revisar y decidir.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onFilterDuplicatesModeChange?.invoke(DuplicateFilterMode.ONLY_DUPLICATES)
                                    selectedIds.clear()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Revisar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 5. Barra de Gestión y Eliminación Múltiple cuando está en modo "Solo Duplicados"
            if (isDuplicatesReviewMode) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .testTag("duplicate_review_controls"),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFEF3C7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Revisión de Duplicados (${selectedIds.size} de ${uiState.filteredTransactions.size} sel.)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF92400E)
                                )
                                IconButton(
                                    onClick = {
                                        onFilterDuplicatesModeChange?.invoke(DuplicateFilterMode.ALL)
                                        selectedIds.clear()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar revisión", tint = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (selectedIds.size == uiState.filteredTransactions.size) {
                                            selectedIds.clear()
                                        } else {
                                            selectedIds.clear()
                                            selectedIds.addAll(uiState.filteredTransactions.map { it.id })
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (selectedIds.size == uiState.filteredTransactions.size) "Deseleccionar" else "Seleccionar Todo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { showDeleteConfirmDialog = true },
                                    enabled = selectedIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ExpenseRed,
                                        disabledContainerColor = Color(0xFFFECDD3)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Eliminar (${selectedIds.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                if (onMarkTransactionsAsLegitimate != null) {
                                    OutlinedButton(
                                        onClick = {
                                            onMarkTransactionsAsLegitimate(selectedIds.toList())
                                            selectedIds.clear()
                                        },
                                        enabled = selectedIds.isNotEmpty(),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = "Legítimos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Barra de Búsqueda Superior en Tiempo Real
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Buscar por comercio, concepto o monto...", color = Color.Gray) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = JadeGreen)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.Gray)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transactions_search_bar"),
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JadeGreen,
                            unfocusedBorderColor = IvoryBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )
                }
            }

            // 7. Filtros Rápidos en Chips Horizontales (Quicken Simplifi Bar)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Todos
                    FilterChip(
                        selected = uiState.filterType == null &&
                                uiState.filterQuincena == 0 &&
                                uiState.filterDuplicatesMode == DuplicateFilterMode.ALL &&
                                !uiState.onlyHighlighted &&
                                uiState.filterSource == null,
                        onClick = {
                            onClearAllFilters?.invoke()
                            selectedIds.clear()
                        },
                        label = { Text("Todos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JadeGreen,
                            selectedLabelColor = Color.White
                        )
                    )

                    // Gastos Reales
                    FilterChip(
                        selected = uiState.filterType == "REAL_EXPENSE",
                        onClick = {
                            onFilterTypeChange(if (uiState.filterType == "REAL_EXPENSE") null else "REAL_EXPENSE")
                        },
                        label = { Text("Gastos Reales") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoralRedLight,
                            selectedLabelColor = CoralRed
                        )
                    )

                    // Gastos Supuestos
                    FilterChip(
                        selected = uiState.filterType == "SUPUESTO",
                        onClick = {
                            onFilterTypeChange(if (uiState.filterType == "SUPUESTO") null else "SUPUESTO")
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (uiState.filterType == "SUPUESTO") NazarenoPurple else Color.Gray)
                        },
                        label = { Text("Gastos Supuestos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NazarenoPurpleLight,
                            selectedLabelColor = NazarenoPurple
                        )
                    )

                    // Todos los Gastos
                    FilterChip(
                        selected = uiState.filterType == TransactionType.EXPENSE,
                        onClick = {
                            onFilterTypeChange(if (uiState.filterType == TransactionType.EXPENSE) null else TransactionType.EXPENSE)
                        },
                        label = { Text("Todos los Gastos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoralRedLight,
                            selectedLabelColor = CoralRed
                        )
                    )

                    // Ingresos
                    FilterChip(
                        selected = uiState.filterType == TransactionType.INCOME,
                        onClick = {
                            onFilterTypeChange(if (uiState.filterType == TransactionType.INCOME) null else TransactionType.INCOME)
                        },
                        label = { Text("Ingresos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JadeGreenLight,
                            selectedLabelColor = Color(0xFF065F46)
                        )
                    )

                    // 1ra Quincena (Fin mes previo - 14)
                    FilterChip(
                        selected = uiState.filterQuincena == 1,
                        onClick = {
                            onFilterQuincenaChange?.invoke(if (uiState.filterQuincena == 1) 0 else 1)
                        },
                        label = { Text("1ra Quincena (Fin de mes - 14)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JadeGreen,
                            selectedLabelColor = Color.White
                        )
                    )

                    // 2da Quincena (15 - Penúltimo día)
                    FilterChip(
                        selected = uiState.filterQuincena == 2,
                        onClick = {
                            onFilterQuincenaChange?.invoke(if (uiState.filterQuincena == 2) 0 else 2)
                        },
                        label = { Text("2da Quincena (15 - Penúltimo)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JadeGreen,
                            selectedLabelColor = Color.White
                        )
                    )

                    // Importados Excel
                    FilterChip(
                        selected = uiState.filterSource == TransactionSource.IMPORTED,
                        onClick = {
                            onFilterSourceChange?.invoke(if (uiState.filterSource == TransactionSource.IMPORTED) null else TransactionSource.IMPORTED)
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (uiState.filterSource == TransactionSource.IMPORTED) TurquoiseAccent else Color.Gray)
                        },
                        label = { Text("Importados Excel") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TurquoiseLight,
                            selectedLabelColor = TurquoiseAccent
                        )
                    )

                    // Duplicados
                    if (uiState.suspectedDuplicatesCount > 0) {
                        FilterChip(
                            selected = uiState.filterDuplicatesMode == DuplicateFilterMode.ONLY_DUPLICATES,
                            onClick = {
                                val next = if (uiState.filterDuplicatesMode == DuplicateFilterMode.ONLY_DUPLICATES) {
                                    DuplicateFilterMode.ALL
                                } else {
                                    DuplicateFilterMode.ONLY_DUPLICATES
                                }
                                onFilterDuplicatesModeChange?.invoke(next)
                                selectedIds.clear()
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldenYellow)
                            },
                            label = { Text("Duplicados (${uiState.suspectedDuplicatesCount})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldenYellowLight,
                                selectedLabelColor = Color(0xFFB45309)
                            )
                        )
                    }

                    // Resaltados
                    FilterChip(
                        selected = uiState.onlyHighlighted,
                        onClick = { onToggleOnlyHighlighted(!uiState.onlyHighlighted) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.FormatColorFill, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (uiState.onlyHighlighted) GoldenYellow else Color.Gray)
                        },
                        label = { Text("Resaltados") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldenYellowLight,
                            selectedLabelColor = Color(0xFFB45309)
                        )
                    )
                }
            }

            // 7.5. Salario Roll-Over de Quincena Previa (Ahorro transferido como ingreso disponible)
            if (uiState.quincenaRolloverAmount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = JadeGreenLight.copy(alpha = 0.45f)),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, JadeGreen.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = JadeGreen,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = GoldenYellowLight, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Salario Roll-Over (Ahorro Quincenal)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = GoldenYellowLight
                                    ) {
                                        Text(
                                            text = "Ahorro",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldenYellow,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Dinero no gastado del periodo anterior transferido a tu disponible de esta quincena.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF047857)
                                )
                            }
                            Text(
                                text = "+${uiState.currencySymbol}${"%,.2f".format(uiState.quincenaRolloverAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = JadeGreen
                            )
                        }
                    }
                }
            }

            // 8. Lista de Transacciones agrupadas
            if (uiState.filteredTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SafeQuetziAvatar(size = 80.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "¡Cero clavos por aquí!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (uiState.isDarkTheme) Color.White else JadeGreen
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No hay registros con los filtros seleccionados. Ajusta tu búsqueda o agrega un nuevo movimiento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { onClearAllFilters?.invoke() },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, JadeGreen)
                        ) {
                            Text("Restablecer Filtros", color = JadeGreen, fontWeight = FontWeight.Bold)
                        }
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
                        onToggleGastoSupuesto = {
                            val nextDec = if (tx.isGastoSupuesto) "CONFIRM" else "PENDING"
                            onDecideTentative(tx, nextDec)
                        },
                        isDiscreetMode = uiState.isDiscreetMode,
                        isSelectionMode = isDuplicatesReviewMode,
                        isSelected = selectedIds.contains(tx.id),
                        onToggleSelect = {
                            if (selectedIds.contains(tx.id)) {
                                selectedIds.remove(tx.id)
                            } else {
                                selectedIds.add(tx.id)
                            }
                        },
                        onMarkAsLegitimate = {
                            onMarkTransactionsAsLegitimate?.invoke(listOf(tx.id))
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

/**
 * Contenido del Modal BottomSheet para los 11 Criterios de Filtro Combinables
 */
@Composable
private fun AdvancedFiltersSheetContent(
    uiState: BudgetUiState,
    onDismiss: () -> Unit,
    onFilterPeriodChange: (String) -> Unit,
    onFilterSpecificDateChange: (Long?) -> Unit,
    onFilterDateRangeChange: (Long?, Long?) -> Unit,
    onFilterQuincenaChange: (Int) -> Unit,
    onFilterTypeChange: (String?) -> Unit,
    onFilterCategoryChange: (String?) -> Unit,
    onFilterSubCategoryChange: (String?) -> Unit,
    onFilterBeneficiaryChange: (String?) -> Unit,
    onFilterStatusChange: (String?) -> Unit,
    onFilterSourceChange: (String?) -> Unit,
    onFilterDuplicatesModeChange: (String) -> Unit,
    onClearAllFilters: () -> Unit
) {
    val context = LocalContext.current
    var beneficiaryInput by remember { mutableStateOf(uiState.filterBeneficiary ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filtros de Registros",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = EmeraldGreen
            )
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Criterio 1 y 3: Período y Fechas
        Text(text = "1. Período Calendario", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                FilterPeriod.DAY to "Día (Hoy)",
                FilterPeriod.WEEK to "Esta Semana",
                FilterPeriod.MONTH to "Este Mes",
                FilterPeriod.YEAR to "Este Año",
                FilterPeriod.ALL to "Todo el Historial"
            ).forEach { (p, label) ->
                FilterChip(
                    selected = uiState.filterPeriod == p && uiState.filterSpecificDateMillis == null && uiState.filterDateRangeStart == null,
                    onClick = { onFilterPeriodChange(p) },
                    label = { Text(label) }
                )
            }
        }

        // Criterio 1 Puntual: Fecha específica
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Fecha Puntual Específica", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val selectedCal = Calendar.getInstance()
                            selectedCal.set(y, m, d, 12, 0, 0)
                            onFilterSpecificDateChange(selectedCal.timeInMillis)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            ) {
                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (uiState.filterSpecificDateMillis != null) {
                        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(uiState.filterSpecificDateMillis))
                    } else "Elegir Día"
                )
            }
        }

        // Criterio 4 y 5: Quincenas
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "2. Quincena Guatemalteca", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.filterQuincena == 0,
                onClick = { onFilterQuincenaChange(0) },
                label = { Text("Todas") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = uiState.filterQuincena == 1,
                onClick = { onFilterQuincenaChange(1) },
                label = { Text("1ra (Fin mes - 14)") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = uiState.filterQuincena == 2,
                onClick = { onFilterQuincenaChange(2) },
                label = { Text("2da (15 - Penúltimo)") },
                modifier = Modifier.weight(1f)
            )
        }

        // Criterio 6: Tipo de Movimiento
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "3. Tipo de Movimiento", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                null to "Todos",
                "REAL_EXPENSE" to "Gastos Reales",
                "SUPUESTO" to "Gastos Supuestos",
                TransactionType.EXPENSE to "Todos los Gastos",
                TransactionType.INCOME to "Ingresos",
                "SAVING" to "Ahorros",
                "TRANSFER" to "Transferencias"
            ).forEach { (t, label) ->
                FilterChip(
                    selected = uiState.filterType == t,
                    onClick = { onFilterTypeChange(t) },
                    label = { Text(label) }
                )
            }
        }

        // Criterio 9: Estado Contable
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "4. Estado Contable", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                null to "Todos",
                TransactionStatus.REAL to "Real",
                TransactionStatus.PENDING to "Pendiente",
                TransactionStatus.PROJECTED to "Proyectado"
            ).forEach { (st, label) ->
                FilterChip(
                    selected = uiState.filterStatus == st,
                    onClick = { onFilterStatusChange(st) },
                    label = { Text(label) }
                )
            }
        }

        // Criterio 10: Origen del Dato
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "5. Origen del Registro", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                null to "Todos",
                TransactionSource.MANUAL to "Manual",
                TransactionSource.IMPORTED to "Importado Excel"
            ).forEach { (src, label) ->
                FilterChip(
                    selected = uiState.filterSource == src,
                    onClick = { onFilterSourceChange(src) },
                    label = { Text(label) }
                )
            }
        }

        // Criterio 11: Duplicados
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "6. Detección de Duplicados", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                DuplicateFilterMode.ALL to "Todos",
                DuplicateFilterMode.ONLY_DUPLICATES to "Solo Duplicados",
                DuplicateFilterMode.EXCLUDE_DUPLICATES to "Excluir Duplicados"
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = uiState.filterDuplicatesMode == mode,
                    onClick = { onFilterDuplicatesModeChange(mode) },
                    label = { Text(label) }
                )
            }
        }

        // Criterio 8: Beneficiario o Establecimiento
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "7. Beneficiario / Establecimiento", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = beneficiaryInput,
            onValueChange = {
                beneficiaryInput = it
                onFilterBeneficiaryChange(if (it.isBlank()) null else it)
            },
            placeholder = { Text("Ej: Súper La Torre, Shell, Empresa Eléctrica...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Criterio 7: Categoría
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "8. Categoría", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = uiState.filterCategory == null,
                onClick = { onFilterCategoryChange(null) },
                label = { Text("Todas") }
            )
            DefaultData.expenseCategories.forEach { cat ->
                FilterChip(
                    selected = uiState.filterCategory == cat.name,
                    onClick = { onFilterCategoryChange(if (uiState.filterCategory == cat.name) null else cat.name) },
                    label = { Text(cat.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Botones de acción inferiores
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    beneficiaryInput = ""
                    onClearAllFilters()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Limpiar Todo")
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Aplicar (${uiState.filteredTransactions.size})", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = EmeraldGreen,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
