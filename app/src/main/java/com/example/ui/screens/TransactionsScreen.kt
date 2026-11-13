package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.R
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
        Image(
            painter = painterResource(id = R.drawable.antigua_carpet_photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(if (uiState.isDarkTheme) 0.38f else 0.82f)
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                if (uiState.isDarkTheme) Color(0x770B1220) else IvoryWarm.copy(alpha = 0.34f)
            )
        )
        // Se conserva sin cambios la fotografía de Antigua del encabezado.
        AntiguaHeaderBackground(height = 260.dp, alpha = 0.90f)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Encabezado Oficial de Registros con acciones originales adaptadas a móvil
            item {
                val headerShadow = Shadow(
                    color = Color(0xD9022C22),
                    offset = Offset(0f, 2f),
                    blurRadius = 6f
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = JadeGreen,
                            border = BorderStroke(1.2.dp, GoldenYellow.copy(alpha = 0.7f)),
                            shadowElevation = 3.dp,
                            modifier = Modifier.size(48.dp)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Historial de registros",
                                style = MaterialTheme.typography.titleLarge.copy(shadow = headerShadow),
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tus finanzas sin clavos",
                                style = MaterialTheme.typography.labelSmall.copy(shadow = headerShadow),
                                fontWeight = FontWeight.SemiBold,
                                color = GoldenYellowLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onAddTransactionClick,
                            colors = ButtonDefaults.buttonColors(containerColor = JadeGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GoldenYellow),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("add_transaction_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = GoldenYellowLight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nuevo", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, maxLines = 1)
                        }
                    }

                    // Botón Filtros Avanzados con Badge de conteo
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BadgedBox(
                            modifier = Modifier.weight(1f),
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
                            Button(
                                onClick = { showAdvancedFiltersSheet = true },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("advanced_filters_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = JadeGreen, contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, GoldenYellow),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = null, tint = GoldenYellowLight, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Filtros", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Botón Exportar / Importar Excel & CSV
                        if (onExportCsv != null) {
                            Button(
                                onClick = { showCsvDialog = true },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("export_csv_top_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepNavyBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, TurquoiseAccent),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = TurquoiseLight, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Importar / Exportar", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // Búsqueda rápida: usa la misma consulta reactiva que se combina con filtros y registros de Room.
            item {
                val searchContainer = if (uiState.isDarkTheme) MaterialTheme.colorScheme.surface else Color.White
                val searchPlaceholderColor = if (uiState.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                val searchIconTint = if (uiState.isDarkTheme) Color(0xFF34D399) else JadeGreen
                val clearIconTint = if (uiState.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = "Beneficiario, comercio, concepto, categoría, monto o fecha...",
                                color = searchPlaceholderColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = searchIconTint)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar búsqueda", tint = clearIconTint)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("transactions_search_bar"),
                        shape = RoundedCornerShape(22.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = searchIconTint,
                            unfocusedBorderColor = if (uiState.isDarkTheme) Color(0xFF475569) else JadeGreen.copy(alpha = 0.65f),
                            focusedContainerColor = searchContainer,
                            unfocusedContainerColor = searchContainer,
                            cursorColor = searchIconTint
                        ),
                        singleLine = true
                    )
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
                val isDark = uiState.isDarkTheme
                val summaryAccentEmerald = if (isDark) Color(0xFF34D399) else EmeraldGreen
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("simplifi_summary_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF131D1B) else Color(0xFFFFFCF2)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.5.dp, if (isDark) Color(0xFF2B423C) else IvoryBorder)
                ) {
                    Column {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Fila de Encabezado: Título y Filtros Activos
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0xFF34D399) else JadeGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Resumen de Fondos • ${uiState.filteredTransactions.size} registro(s)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (uiState.activeFiltersCount > 0) {
                                    TextButton(
                                        text = "Limpiar (${uiState.activeFiltersCount})",
                                        textColor = summaryAccentEmerald,
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
                                color = if (isDark) Color(0xFF064E3B) else EmeraldGreen,
                                border = BorderStroke(1.2.dp, if (isDark) Color(0xFF10B981).copy(alpha = 0.65f) else JadeGreen)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "TOTAL DISPONIBLE PARA GASTAR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD1FAE5),
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (uiState.isDiscreetMode) "Q ****.**" else "${uiState.currencySymbol}${"%,.2f".format(uiState.totalAmountToSpend)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Black,
                                            color = if (uiState.totalAmountToSpend >= 0) Color.White else Color(0xFFFECACA),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.20f)
                                        ) {
                                            Text(
                                                text = "Ingresos: +${uiState.currencySymbol}${"%,.0f".format(uiState.filteredRealIncome)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Indicador de Salario Roll-Over si existe remanente de la quincena previa
                                    if (uiState.quincenaRolloverAmount > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDark) Color(0xFF3B2810) else GoldenYellowLight,
                                            border = BorderStroke(1.dp, GoldenYellow.copy(alpha = 0.55f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.TrendingUp,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Incluye +${uiState.currencySymbol}${"%,.2f".format(uiState.quincenaRolloverAmount)} de Salario Roll-Over (Ahorro Quincenal Previo)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF78350F)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // MÉTRICAS 2, 3, 4: FILA DE 3 GASTOS (DÉBITOS)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SummaryMetricCard(
                                    modifier = Modifier.weight(1f).widthIn(min = 140.dp),
                                    label = "Gastos Reales",
                                    amount = if (uiState.isDiscreetMode) "Q ****.**" else "-${uiState.currencySymbol}${"%,.2f".format(uiState.filteredConfirmedExpense)}",
                                    detail = "Confirmados",
                                    icon = Icons.Default.TrendingDown,
                                    accent = if (isDark) Color(0xFFFDA4AF) else Color(0xFF9F1239),
                                    container = if (isDark) Color(0xFF3B1822) else Color(0xFFFEE2E2),
                                    border = if (isDark) Color(0xFFFB7185).copy(alpha = 0.55f) else CoralRed.copy(alpha = 0.65f),
                                    detailColor = if (isDark) Color(0xFFFECDD3) else Color(0xFF881337)
                                )
                                SummaryMetricCard(
                                    modifier = Modifier.weight(1f).widthIn(min = 140.dp),
                                    label = "Gastos Supuestos",
                                    amount = if (uiState.isDiscreetMode) "Q ****.**" else "-${uiState.currencySymbol}${"%,.2f".format(uiState.filteredSupuestoExpense)}",
                                    detail = "Por aprobar",
                                    icon = Icons.Default.HelpOutline,
                                    accent = if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E),
                                    container = if (isDark) Color(0xFF3B2810) else Color(0xFFFEF3C7),
                                    border = if (isDark) Color(0xFFFBBF24).copy(alpha = 0.55f) else GoldenYellow,
                                    detailColor = if (isDark) Color(0xFFFDE68A) else Color(0xFF78350F)
                                )
                                SummaryMetricCard(
                                    modifier = Modifier.weight(1f).widthIn(min = 140.dp),
                                    label = "Total Ambos",
                                    amount = if (uiState.isDiscreetMode) "Q ****.**" else "-${uiState.currencySymbol}${"%,.2f".format(uiState.filteredTotalBothExpenses)}",
                                    detail = "Real + supuesto",
                                    icon = null,
                                    accent = if (isDark) Color(0xFFC7D2FE) else DeepNavyBlue,
                                    container = if (isDark) Color(0xFF1E2647) else Color(0xFFE0E7FF),
                                    border = if (isDark) Color(0xFF818CF8).copy(alpha = 0.55f) else DeepNavyBlue.copy(alpha = 0.55f),
                                    detailColor = if (isDark) Color(0xFFE0E7FF) else Color(0xFF312E81)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // MÉTRICAS 5 & 6: FILA DE 2 SALDOS RESIDUALES
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SummaryMetricCard(
                                    modifier = Modifier.weight(1f),
                                    label = "Saldo Real en Mano",
                                    amount = if (uiState.isDiscreetMode) "Q ****.**" else "${uiState.currencySymbol}${"%,.2f".format(uiState.filteredRealRemainingBalance)}",
                                    detail = "Sin supuestos (en mano)",
                                    icon = null,
                                    accent = if (isDark) Color(0xFF5EEAD4) else Color(0xFF115E59),
                                    container = if (isDark) Color(0xFF113336) else Color(0xFFCCFBF1),
                                    border = if (isDark) Color(0xFF2DD4BF).copy(alpha = 0.55f) else Color(0xFF0F766E).copy(alpha = 0.65f),
                                    detailColor = if (isDark) Color(0xFF99F6E4) else Color(0xFF134E4A)
                                )
                                SummaryMetricCard(
                                    modifier = Modifier.weight(1f),
                                    label = "Saldo Proyectado",
                                    amount = if (uiState.isDiscreetMode) "Q ****.**" else "${uiState.currencySymbol}${"%,.2f".format(uiState.filteredProjectedRemainingBalance)}",
                                    detail = "Si apruebas supuestos",
                                    icon = null,
                                    accent = if (isDark) Color(0xFFDDD6FE) else Color(0xFF5B21B6),
                                    container = if (isDark) Color(0xFF2E1D4A) else Color(0xFFEDE9FE),
                                    border = if (isDark) Color(0xFFA78BFA).copy(alpha = 0.55f) else NazarenoPurple.copy(alpha = 0.65f),
                                    detailColor = if (isDark) Color(0xFFEDE9FE) else Color(0xFF4C1D95)
                                )
                            }

                            // Subtotales por Categoría
                            if (uiState.filteredCategorySubtotals.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    uiState.filteredCategorySubtotals.take(5).forEach { stat ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                            border = BorderStroke(0.8.dp, if (isDark) Color(0xFF475569) else IvoryBorder),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${stat.category}: ${uiState.currencySymbol}${"%,.0f".format(stat.totalAmount)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
                    val isDark = uiState.isDarkTheme
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .testTag("duplicates_alert_banner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF3B2810) else Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFFFBBF24).copy(alpha = 0.65f) else Color(0xFFF59E0B))
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
                                    tint = if (isDark) Color(0xFFFCD34D) else Color(0xFFD97706),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${uiState.suspectedDuplicatesCount} posibles duplicados detectados",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Mismo monto y fecha. Toca para revisar y decidir.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF78350F)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    onFilterDuplicatesModeChange?.invoke(DuplicateFilterMode.ONLY_DUPLICATES)
                                    selectedIds.clear()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.heightIn(min = 42.dp)
                            ) {
                                Text("Revisar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 5. Barra de Gestión y Eliminación Múltiple cuando está en modo "Solo Duplicados"
            if (isDuplicatesReviewMode) {
                item {
                    val isDark = uiState.isDarkTheme
                    val bannerTextColor = if (isDark) Color(0xFFFDE68A) else Color(0xFF78350F)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .testTag("duplicate_review_controls"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF3B2810) else Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFFFBBF24).copy(alpha = 0.65f) else Color(0xFFF59E0B))
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
                                    color = bannerTextColor,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        onFilterDuplicatesModeChange?.invoke(DuplicateFilterMode.ALL)
                                        selectedIds.clear()
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar revisión", tint = bannerTextColor)
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
                                    border = BorderStroke(1.dp, bannerTextColor.copy(alpha = 0.55f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = bannerTextColor),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                ) {
                                    Text(
                                        text = if (selectedIds.size == uiState.filteredTransactions.size) "Ninguno" else "Sel. todo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = { showDeleteConfirmDialog = true },
                                    enabled = selectedIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ExpenseRed,
                                        contentColor = Color.White,
                                        disabledContainerColor = if (isDark) Color(0xFF4B252B) else Color(0xFFFECDD3),
                                        disabledContentColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFF9F1239)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Eliminar (${selectedIds.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (onMarkTransactionsAsLegitimate != null) {
                                    OutlinedButton(
                                        onClick = {
                                            onMarkTransactionsAsLegitimate(selectedIds.toList())
                                            selectedIds.clear()
                                        },
                                        enabled = selectedIds.isNotEmpty(),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (selectedIds.isNotEmpty()) bannerTextColor.copy(alpha = 0.55f) else bannerTextColor.copy(alpha = 0.25f)
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = bannerTextColor,
                                            disabledContentColor = bannerTextColor.copy(alpha = 0.45f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                    ) {
                                        Text(
                                            text = "Legítimos",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Filtros Rápidos en Chips Horizontales (Quicken Simplifi Bar)
            item {
                val isDark = uiState.isDarkTheme
                val chipContainer = if (isDark) Color(0xFF1E293B) else Color(0xFFF4ECD9)
                val chipLabel = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                val chipBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBBBA0)

                val allSelected = uiState.filterType == null &&
                    uiState.filterQuincena == 0 &&
                    uiState.filterDuplicatesMode == DuplicateFilterMode.ALL &&
                    !uiState.onlyHighlighted &&
                    uiState.filterSource == null

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Todos
                    FilterChip(
                        selected = allSelected,
                        onClick = {
                            onClearAllFilters?.invoke()
                            selectedIds.clear()
                        },
                        label = { Text("Todos", fontWeight = FontWeight.SemiBold) },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = allSelected,
                            borderColor = chipBorderColor,
                            selectedBorderColor = Color.Transparent,
                            borderWidth = 1.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = chipContainer,
                            labelColor = chipLabel,
                            selectedContainerColor = JadeGreen,
                            selectedLabelColor = Color.White
                        )
                    )

                    // Gastos Reales
                    val realExpenseSelected = uiState.filterType == "REAL_EXPENSE"
                    FilterChip(
                        selected = realExpenseSelected,
                        onClick = {
                            onFilterTypeChange(if (realExpenseSelected) null else "REAL_EXPENSE")
                        },
                        label = { Text("Gastos Reales", fontWeight = FontWeight.SemiBold) },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = realExpenseSelected,
                            borderColor = chipBorderColor,
                            selectedBorderColor = Color.Transparent,
                            borderWidth = 1.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = chipContainer,
                            labelColor = chipLabel,
                            selectedContainerColor = CoralRed,
                            selectedLabelColor = Color.White
                        )
                    )

                    // Gastos Supuestos
                    val supuestoSelected = uiState.filterType == "SUPUESTO"
                    FilterChip(
                        selected = supuestoSelected,
                        onClick = {
                            onFilterTypeChange(if (supuestoSelected) null else "SUPUESTO")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (supuestoSelected) Color.White else chipLabel
                            )
                        },
                        label = { Text("Gastos Supuestos", fontWeight = FontWeight.SemiBold) },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = supuestoSelected,
                            borderColor = chipBorderColor,
                            selectedBorderColor = Color.Transparent,
                            borderWidth = 1.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = chipContainer,
                            labelColor = chipLabel,
                            selectedContainerColor = NazarenoPurple,
                            selectedLabelColor = Color.White
                        )
                    )

                    // Todos los Gastos
                    val allExpenseSelected = uiState.filterType == TransactionType.EXPENSE
                    FilterChip(
                        selected = allExpenseSelected,
                        onClick = {
                            onFilterTypeChange(if (allExpenseSelected) null else TransactionType.EXPENSE)
                        },
                        label = { Text("Todos los Gastos", fontWeight = FontWeight.SemiBold) },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = allExpenseSelected,
                            borderColor = chipBorderColor,
                            selectedBorderColor = Color.Transparent,
                            borderWidth = 1.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = chipContainer,
                            labelColor = chipLabel,
                            selectedContainerColor = CoralRed,
                            selectedLabelColor = Color.White
                        )
                    )

                    // Ingresos
                    val incomeSelected = uiState.filterType == TransactionType.INCOME
                    FilterChip(
                        selected = incomeSelected,
                        onClick = {
                            onFilterTypeChange(if (incomeSelected) null else TransactionType.INCOME)
                        },
                        label = { Text("Ingresos", fontWeight = FontWeight.SemiBold) },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = incomeSelected,
                            borderColor = chipBorderColor,
                            selectedBorderColor = Color.Transparent,
                            borderWidth = 1.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = chipContainer,
                            labelColor = chipLabel,
                            selectedContainerColor = JadeGreen,
                            selectedLabelColor = Color.White
                        )
                    )

                    // 1ra Quincena (Fin mes previo - 14)
                    val q1Selected = uiState.filterQuincena == 1
                    FilterChip(
                        selected = q1Selected,
                        onClick = {
                            onFilterQuincenaChange?.invoke(if (q1Selected) 0 else 1)
                        },
                        label = { Text("1ra Quincena (Fin de mes - 14)", fontWeight = FontWeight.SemiBold) },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = q1Selected,
                            borderColor = chipBorderColor,
                            selectedBorderColor = Color.Transparent,
                            borderWidth = 1.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = chipContainer,
                            labelColor = chipLabel,
                            selectedContainerColor = JadeGreen,
                            selectedLabelColor = Color.White
                        )
                    )

                    // 2da Quincena (15 - Penúltimo día)
                    val q2Selected = uiState.filterQuincena == 2
                    FilterChip(
                        selected = q2Selected,
                        onClick = {
                            onFilterQuincenaChange?.invoke(if (q2Selected) 0 else 2)
                        },
                        label = { Text("2da Quincena (15 - Penúltimo)", fontWeight = FontWeight.SemiBold) },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = q2Selected,
                            borderColor = chipBorderColor,
                            selectedBorderColor = Color.Transparent,
                            borderWidth = 1.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = chipContainer,
                            labelColor = chipLabel,
                            selectedContainerColor = JadeGreen,
                            selectedLabelColor = Color.White
                        )
                    )

                    // Importados Excel
                    val importedSelected = uiState.filterSource == TransactionSource.IMPORTED
                    FilterChip(
                        selected = importedSelected,
                        onClick = {
                            onFilterSourceChange?.invoke(if (importedSelected) null else TransactionSource.IMPORTED)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (importedSelected) Color.White else chipLabel
                            )
                        },
                        label = { Text("Importados Excel", fontWeight = FontWeight.SemiBold) },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = importedSelected,
                            borderColor = chipBorderColor,
                            selectedBorderColor = Color.Transparent,
                            borderWidth = 1.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = chipContainer,
                            labelColor = chipLabel,
                            selectedContainerColor = Color(0xFF006B61),
                            selectedLabelColor = Color.White
                        )
                    )

                    // Duplicados
                    if (uiState.suspectedDuplicatesCount > 0) {
                        val dupSelected = uiState.filterDuplicatesMode == DuplicateFilterMode.ONLY_DUPLICATES
                        FilterChip(
                            selected = dupSelected,
                            onClick = {
                                val next = if (dupSelected) {
                                    DuplicateFilterMode.ALL
                                } else {
                                    DuplicateFilterMode.ONLY_DUPLICATES
                                }
                                onFilterDuplicatesModeChange?.invoke(next)
                                selectedIds.clear()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (dupSelected) Color.White else if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E)
                                )
                            },
                            label = { Text("Duplicados (${uiState.suspectedDuplicatesCount})", fontWeight = FontWeight.SemiBold) },
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = dupSelected,
                                borderColor = chipBorderColor,
                                selectedBorderColor = Color.Transparent,
                                borderWidth = 1.dp
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = chipContainer,
                                labelColor = chipLabel,
                                selectedContainerColor = Color(0xFF92400E),
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    // Resaltados
                    FilterChip(
                        selected = uiState.onlyHighlighted,
                        onClick = { onToggleOnlyHighlighted(!uiState.onlyHighlighted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FormatColorFill,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (uiState.onlyHighlighted) Color.White else chipLabel
                            )
                        },
                        label = { Text("Resaltados", fontWeight = FontWeight.SemiBold) },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = uiState.onlyHighlighted,
                            borderColor = chipBorderColor,
                            selectedBorderColor = Color.Transparent,
                            borderWidth = 1.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = chipContainer,
                            labelColor = chipLabel,
                            selectedContainerColor = Color(0xFF92400E),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // 7.5. Salario Roll-Over de Quincena Previa (Ahorro transferido como ingreso disponible)
            if (uiState.quincenaRolloverAmount > 0) {
                item {
                    val isDark = uiState.isDarkTheme
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF064E3B) else JadeGreenLight
                        ),
                        border = BorderStroke(1.2.dp, if (isDark) Color(0xFF10B981) else JadeGreen)
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Salario Roll-Over (Ahorro Quincenal)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF065F46),
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = GoldenYellowLight
                                    ) {
                                        Text(
                                            text = "Ahorro",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF78350F),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Dinero no gastado del periodo anterior transferido a tu disponible de esta quincena.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFFD1FAE5) else Color(0xFF064E3B)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+${uiState.currencySymbol}${"%,.2f".format(uiState.quincenaRolloverAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0xFF6EE7B7) else JadeGreen
                            )
                        }
                    }
                }
            }

            // 8. Lista de Transacciones agrupadas
            if (uiState.filteredTransactions.isEmpty()) {
                item {
                    val isDark = uiState.isDarkTheme
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xF0131D1B) else Color(0xF5FFFCF2)
                        ),
                        border = BorderStroke(1.2.dp, if (isDark) Color(0xFF2B423C) else IvoryBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SafeQuetziAvatar(size = 80.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "¡Cero clavos por aquí!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0xFF34D399) else JadeGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No hay registros con los filtros seleccionados. Ajusta tu búsqueda o agrega un nuevo movimiento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { onClearAllFilters?.invoke() },
                                modifier = Modifier.heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.2.dp, GoldenYellow),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = JadeGreen,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Restablecer Filtros", color = Color.White, fontWeight = FontWeight.Bold)
                            }
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
                        isDarkTheme = uiState.isDarkTheme,
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
@OptIn(ExperimentalLayoutApi::class)
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
    val sheetAccent = if (uiState.isDarkTheme) Color(0xFF34D399) else EmeraldGreen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
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
                color = sheetAccent
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            Text(
                text = "Fecha Puntual Específica",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                modifier = Modifier.heightIn(min = 44.dp),
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
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "2. Quincena Guatemalteca", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = uiState.filterQuincena == 0,
                onClick = { onFilterQuincenaChange(0) },
                label = { Text("Todas") }
            )
            FilterChip(
                selected = uiState.filterQuincena == 1,
                onClick = { onFilterQuincenaChange(1) },
                label = { Text("1ra (Fin mes - 14)") }
            )
            FilterChip(
                selected = uiState.filterQuincena == 2,
                onClick = { onFilterQuincenaChange(2) },
                label = { Text("2da (15 - Penúltimo)") }
            )
        }

        // Criterio 6: Tipo de Movimiento
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "3. Tipo de Movimiento", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "4. Estado Contable", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "5. Origen del Registro", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "6. Detección de Duplicados", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "7. Beneficiario / Establecimiento", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
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
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "8. Categoría", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Limpiar Todo", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Aplicar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    amount: String,
    detail: String,
    icon: ImageVector?,
    accent: Color,
    container: Color,
    border: Color,
    detailColor: Color
) {
    Surface(
        modifier = modifier.heightIn(min = 104.dp),
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = BorderStroke(1.2.dp, border)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.2).sp,
                color = accent,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = detailColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TextButton(
    text: String,
    onClick: () -> Unit,
    textColor: Color = EmeraldGreen,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
