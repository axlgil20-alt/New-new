package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TransactionEntity
import com.example.data.model.isGastoSupuesto
import com.example.ui.components.TransactionItemCard
import com.example.ui.util.FormatUtils
import com.example.ui.util.QuincenaFilterHelper
import com.example.ui.viewmodel.BudgetUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val LakeNavy = Color(0xFF073B5C)
private val LakeBlue = Color(0xFF087EAA)
private val LakeTurquoise = Color(0xFF0F9D9A)
private val LakeJade = Color(0xFF087F68)
private val LakePale = Color(0xFFEAF7F8)
private val LakeText = Color(0xFF143449)

@Composable
fun PlannedVsRealScreen(
    uiState: BudgetUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onHighlightTransaction: (TransactionEntity, Long) -> Unit,
    onToggleVerifyTransaction: (TransactionEntity) -> Unit,
    onDecideTentative: (TransactionEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCycleTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val transactions = uiState.allTransactions
    val quincenaData = remember(transactions, uiState.currentYear, uiState.currentMonthIndex) {
        QuincenaFilterHelper.filterByQuincena(
            transactions = transactions,
            targetYear = uiState.currentYear,
            targetMonthIndex = uiState.currentMonthIndex
        )
    }
    val q1Opening = remember(transactions, uiState.currentYear, uiState.currentMonthIndex) {
        QuincenaFilterHelper.calculateQuincenaRollover(transactions, uiState.currentYear, uiState.currentMonthIndex, 1)
    }
    val q2Opening = remember(transactions, uiState.currentYear, uiState.currentMonthIndex) {
        QuincenaFilterHelper.calculateQuincenaRollover(transactions, uiState.currentYear, uiState.currentMonthIndex, 2)
    }
    val q1Available = q1Opening + quincenaData.q1Income - quincenaData.q1Expense
    val q2Available = q2Opening + quincenaData.q2Income - quincenaData.q2Expense
    val q1Projected = q1Available - quincenaData.q1PendingExpense
    val q2Projected = q2Available - quincenaData.q2PendingExpense
    val monthCal = remember(uiState.currentYear, uiState.currentMonthIndex) {
        Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, uiState.currentYear)
            set(Calendar.MONTH, uiState.currentMonthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val monthLabel = remember(monthCal) {
        SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(monthCal.time).replaceFirstChar { it.uppercase() }
    }
    val rawCycleList = when (selectedCycleTab) {
        1 -> quincenaData.q1Transactions
        2 -> quincenaData.q2Transactions
        else -> quincenaData.monthTransactions
    }
    val displayList = remember(rawCycleList, searchQuery) {
        if (searchQuery.isBlank()) {
            rawCycleList
        } else {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            rawCycleList.filter { tx ->
                tx.title.lowercase(Locale.getDefault()).contains(q) ||
                tx.category.lowercase(Locale.getDefault()).contains(q) ||
                tx.notes?.lowercase(Locale.getDefault())?.contains(q) == true ||
                "%.2f".format(tx.amount).contains(q)
            }
        }
    }
    val isDark = uiState.isDarkTheme
    val backgroundScrim = if (isDark) {
        if (displayList.isEmpty()) Color(0x9909131C) else Color(0xB809131C)
    } else {
        if (displayList.isEmpty()) Color(0x6AF0F8FA) else Color(0x9EF0F8FA)
    }
    val cardContainer = if (isDark) Color(0xFF0E1F2A) else Color.White
    val summaryContainer = if (isDark) Color(0xFF0B1C26) else Color(0xFFF8FEFF)
    val cycleCardContainer = if (isDark) Color(0xFF0D212D) else Color(0xFFF4FBFC)
    val cardBorder = if (isDark) Color(0xFF285970) else Color(0xFF9BD9E2)
    val accentBorder = if (isDark) Color(0xFF38BDF8) else Color(0xFF62B9C8)
    val titleColor = if (isDark) Color(0xFF7DD3FC) else LakeNavy
    val primaryTextColor = if (isDark) Color(0xFFF1F5F9) else LakeText
    val secondaryTextColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF31576A)
    val iconNavColor = if (isDark) Color(0xFF38BDF8) else LakeBlue

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.atitlan_quincenas_street),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(backgroundScrim))

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(bottom = 76.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                    Image(
                        painter = painterResource(R.drawable.atitlan_quincenas_header),
                        contentDescription = "Lago de Atitlán, volcanes y muelle",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color(0x33102F46), Color(0xCC07304B)))
                        )
                    )
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "QUINCENAS",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Tu dinero, claro en cada ciclo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE0F7FA),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onPreviousMonth, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes anterior", tint = iconNavColor)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = if (isDark) Color(0xFF34D399) else LakeJade)
                            Text(monthLabel, fontWeight = FontWeight.Bold, color = primaryTextColor, maxLines = 2)
                        }
                        IconButton(onClick = onNextMonth, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mes siguiente", tint = iconNavColor)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Mes completo", "1.ª · Fin mes–14", "2.ª · 15–Penúltimo").forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedCycleTab == index,
                            onClick = { selectedCycleTab = index },
                            label = { Text(label, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = cardContainer,
                                labelColor = primaryTextColor,
                                selectedContainerColor = LakeBlue,
                                selectedLabelColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedCycleTab == index) LakeTurquoise else cardBorder)
                        )
                    }
                }
            }

            if (selectedCycleTab == 0) item {
                val received = quincenaData.totalIncome
                val actualExpenses = quincenaData.totalExpense
                val pendingExpenses = quincenaData.totalPendingExpense
                val available = q2Available
                val projected = q2Projected
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = summaryContainer),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, accentBorder),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "RESUMEN DEL MES · SALDO AL CIERRE DE LA 2.ª QUINCENA",
                            color = titleColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (q1Opening > 0) {
                            FinanceLine("Saldo inicial · traspaso", q1Opening, uiState.currencySymbol, uiState.isDiscreetMode, if (isDark) Color(0xFF2DD4BF) else LakeTurquoise, Icons.Default.TrendingUp, primaryTextColor)
                        }
                        FinanceLine("Dinero recibido", received, uiState.currencySymbol, uiState.isDiscreetMode, if (isDark) Color(0xFF38BDF8) else LakeBlue, Icons.Default.Payments, primaryTextColor)
                        FinanceLine("Gastos reales", actualExpenses, uiState.currencySymbol, uiState.isDiscreetMode, if (isDark) Color(0xFFFCA5A5) else Color(0xFFB42332), Icons.Default.TrendingDown, primaryTextColor)
                        FinanceLine("Pendientes · proyección", pendingExpenses, uiState.currencySymbol, uiState.isDiscreetMode, if (isDark) Color(0xFFFCD34D) else Color(0xFF8A5A00), Icons.Default.Schedule, primaryTextColor)
                        Spacer(Modifier.height(2.dp))
                        Surface(shape = RoundedCornerShape(14.dp), color = LakeJade) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Disponible real", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Proyección tras pendientes", color = Color(0xFFD4F5EF), style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(FormatUtils.formatCurrency(available, uiState.currencySymbol, uiState.isDiscreetMode), color = Color.White, fontWeight = FontWeight.Black)
                                    Text(FormatUtils.formatCurrency(projected, uiState.currencySymbol, uiState.isDiscreetMode), color = Color(0xFFD4F5EF), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            "El saldo que queda pasa al siguiente ciclo como saldo inicial; no se cuenta como ingreso nuevo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor
                        )
                    }
                }
            }

            if (selectedCycleTab == 1) {
                item {
                    CycleFinanceCard(
                        title = "1.ª quincena · Fin del mes previo al día 14",
                        fundedBy = "Financiada por el pago de fin del mes anterior",
                        opening = q1Opening,
                        received = quincenaData.q1Income,
                        actualExpenses = quincenaData.q1Expense,
                        pendingExpenses = quincenaData.q1PendingExpense,
                        actualAvailable = q1Available,
                        projected = q1Projected,
                        uiState = uiState
                    )
                }
            }
            if (selectedCycleTab == 2) {
                item {
                    CycleFinanceCard(
                        title = "2.ª quincena · Día 15 al penúltimo día",
                        fundedBy = "Financiada por el pago recibido el día 15",
                        opening = q2Opening,
                        received = quincenaData.q2Income,
                        actualExpenses = quincenaData.q2Expense,
                        pendingExpenses = quincenaData.q2PendingExpense,
                        actualAvailable = q2Available,
                        projected = q2Projected,
                        uiState = uiState
                    )
                }
            }

            // Barra de Búsqueda integrada dentro del ciclo seleccionado
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            when (selectedCycleTab) {
                                1 -> "Buscar en 1.ª quincena..."
                                2 -> "Buscar en 2.ª quincena..."
                                else -> "Buscar en el mes..."
                            },
                            color = secondaryTextColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = iconNavColor)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda", tint = secondaryTextColor)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardContainer,
                        unfocusedContainerColor = cardContainer,
                        focusedBorderColor = iconNavColor,
                        unfocusedBorderColor = cardBorder,
                        focusedTextColor = primaryTextColor,
                        unfocusedTextColor = primaryTextColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .testTag("quincenas_search_bar")
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Movimientos", color = titleColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${displayList.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(CircleShape).background(LakeBlue).padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (displayList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = cardContainer),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = if (isDark) Color(0xFF2DD4BF) else LakeTurquoise, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            if (searchQuery.isNotBlank()) {
                                Text("No se encontraron movimientos", color = primaryTextColor, fontWeight = FontWeight.Bold)
                                Text("No hay coincidencias para \"$searchQuery\" en este ciclo.", color = secondaryTextColor, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { searchQuery = "" },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, iconNavColor)
                                ) {
                                    Text("Limpiar búsqueda", color = iconNavColor, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("Aún no hay movimientos en este ciclo", color = primaryTextColor, fontWeight = FontWeight.Bold)
                                Text("Los registros nuevos aparecerán aquí automáticamente.", color = secondaryTextColor, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(displayList, key = { _, it -> it.id }) { index, tx ->
                    TransactionItemCard(
                        transaction = tx,
                        currencySymbol = uiState.currencySymbol,
                        onEdit = { onEditTransaction(tx) },
                        onDelete = { onDeleteTransaction(tx) },
                        onHighlight = { hex -> onHighlightTransaction(tx, hex) },
                        onToggleVerify = { onToggleVerifyTransaction(tx) },
                        onDecideTentative = { decision -> onDecideTentative(tx, decision) },
                        onToggleGastoSupuesto = {
                            val nextDec = if (tx.isGastoSupuesto) "CONFIRM" else "PENDING"
                            onDecideTentative(tx, nextDec)
                        },
                        isDiscreetMode = uiState.isDiscreetMode,
                        isDarkTheme = isDark,
                        rowIndex = index
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }
}

@Composable
private fun CycleFinanceCard(
    title: String,
    fundedBy: String,
    opening: Double,
    received: Double,
    actualExpenses: Double,
    pendingExpenses: Double,
    actualAvailable: Double,
    projected: Double,
    uiState: BudgetUiState
) {
    val isDark = uiState.isDarkTheme
    val cardContainer = if (isDark) Color(0xFF0D212D) else Color(0xFFF4FBFC)
    val cardBorder = if (isDark) Color(0xFF285970) else Color(0xFFB6DFE5)
    val titleColor = if (isDark) Color(0xFF7DD3FC) else LakeNavy
    val subtitleColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF31576A)
    val primaryText = if (isDark) Color(0xFFF1F5F9) else LakeText

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = titleColor, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
            Text(fundedBy, color = subtitleColor, style = MaterialTheme.typography.bodySmall)
            FinanceLine("Saldo inicial · traspaso", opening, uiState.currencySymbol, uiState.isDiscreetMode, if (isDark) Color(0xFF2DD4BF) else LakeTurquoise, Icons.Default.TrendingUp, primaryText)
            FinanceLine("Dinero recibido", received, uiState.currencySymbol, uiState.isDiscreetMode, if (isDark) Color(0xFF38BDF8) else LakeBlue, Icons.Default.Payments, primaryText)
            FinanceLine("Gastos reales · una vez", actualExpenses, uiState.currencySymbol, uiState.isDiscreetMode, if (isDark) Color(0xFFFCA5A5) else Color(0xFFB42332), Icons.Default.TrendingDown, primaryText)
            FinanceLine("Gastos pendientes", pendingExpenses, uiState.currencySymbol, uiState.isDiscreetMode, if (isDark) Color(0xFFFCD34D) else Color(0xFF8A5A00), Icons.Default.Schedule, primaryText)
            Surface(shape = RoundedCornerShape(12.dp), color = LakeJade) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Disponible real", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Proyectado", color = Color(0xFFD4F5EF), style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(FormatUtils.formatCurrency(actualAvailable, uiState.currencySymbol, uiState.isDiscreetMode), color = Color.White, fontWeight = FontWeight.Black)
                        Text(FormatUtils.formatCurrency(projected, uiState.currencySymbol, uiState.isDiscreetMode), color = Color(0xFFD4F5EF), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceLine(
    label: String,
    amount: Double,
    currencySymbol: String,
    discreetMode: Boolean,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelColor: Color = LakeText
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(label, color = labelColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            FormatUtils.formatCurrency(amount, currencySymbol, discreetMode),
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2
        )
    }
}
