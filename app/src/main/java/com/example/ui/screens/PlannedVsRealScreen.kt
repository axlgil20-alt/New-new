package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TransactionEntity
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
    val displayList = when (selectedCycleTab) {
        1 -> quincenaData.q1Transactions
        2 -> quincenaData.q2Transactions
        else -> quincenaData.monthTransactions
    }
    val backgroundScrim = if (displayList.isEmpty()) Color(0x7AF0F8FA) else Color(0xB3F0F8FA)

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
                            Brush.verticalGradient(listOf(Color(0x22102F46), Color(0xB807304B)))
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9BD9E2)),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onPreviousMonth, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes anterior", tint = LakeBlue)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = LakeJade)
                            Text(monthLabel, fontWeight = FontWeight.Bold, color = LakeText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = onNextMonth, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mes siguiente", tint = LakeBlue)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Mes", "1.ª · 1–15", "2.ª · 16–fin").forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedCycleTab == index,
                            onClick = { selectedCycleTab = index },
                            label = { Text(label, maxLines = 1, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = LakeText,
                                selectedContainerColor = LakeBlue,
                                selectedLabelColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedCycleTab == index) LakeTurquoise else Color(0xFF9BD9E2))
                        )
                    }
                }
            }

            if (selectedCycleTab == 0) item {
                val received = when (selectedCycleTab) {
                    1 -> quincenaData.q1Income
                    2 -> quincenaData.q2Income
                    else -> quincenaData.totalIncome
                }
                val actualExpenses = when (selectedCycleTab) {
                    1 -> quincenaData.q1Expense
                    2 -> quincenaData.q2Expense
                    else -> quincenaData.totalExpense
                }
                val pendingExpenses = when (selectedCycleTab) {
                    1 -> quincenaData.q1PendingExpense
                    2 -> quincenaData.q2PendingExpense
                    else -> quincenaData.totalPendingExpense
                }
                val available = when (selectedCycleTab) {
                    1 -> q1Available
                    2 -> q2Available
                    else -> q2Available
                }
                val projected = when (selectedCycleTab) {
                    1 -> q1Projected
                    2 -> q2Projected
                    else -> q2Projected
                }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FEFF)),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF62B9C8)),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (selectedCycleTab == 0) "RESUMEN DEL MES · SALDO AL CIERRE DE LA 2.ª QUINCENA" else "RESUMEN DE LA QUINCENA SELECCIONADA",
                            color = LakeNavy,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        FinanceLine("Dinero recibido", received, uiState.currencySymbol, uiState.isDiscreetMode, LakeBlue, Icons.Default.Payments)
                        FinanceLine("Gastos reales", actualExpenses, uiState.currencySymbol, uiState.isDiscreetMode, Color(0xFFB42332), Icons.Default.TrendingDown)
                        FinanceLine("Pendientes · proyección", pendingExpenses, uiState.currencySymbol, uiState.isDiscreetMode, Color(0xFF8A5A00), Icons.Default.Schedule)
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
                            color = LakeText
                        )
                    }
                }
            }

            if (selectedCycleTab == 1) {
                item {
                    CycleFinanceCard(
                        title = "1.ª quincena · días 1–15",
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
                        title = "2.ª quincena · días 16–fin de mes",
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

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Movimientos", color = LakeNavy, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${displayList.size}", color = Color.White, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(CircleShape).background(LakeBlue).padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
            }

            if (displayList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = LakeTurquoise, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Aún no hay movimientos en este ciclo", color = LakeText, fontWeight = FontWeight.Bold)
                            Text("Los registros nuevos aparecerán aquí automáticamente.", color = Color(0xFF456273), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                items(displayList, key = { it.id }) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        currencySymbol = uiState.currencySymbol,
                        onEdit = { onEditTransaction(tx) },
                        onDelete = { onDeleteTransaction(tx) },
                        onHighlight = { hex -> onHighlightTransaction(tx, hex) },
                        onToggleVerify = { onToggleVerifyTransaction(tx) },
                        onDecideTentative = { decision -> onDecideTentative(tx, decision) },
                        isDiscreetMode = uiState.isDiscreetMode
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
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4FBFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB6DFE5)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = LakeNavy, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
            Text(fundedBy, color = Color(0xFF31576A), style = MaterialTheme.typography.bodySmall)
            FinanceLine("Saldo inicial · traspaso", opening, uiState.currencySymbol, uiState.isDiscreetMode, LakeTurquoise, Icons.Default.TrendingUp)
            FinanceLine("Dinero recibido", received, uiState.currencySymbol, uiState.isDiscreetMode, LakeBlue, Icons.Default.Payments)
            FinanceLine("Gastos reales · una vez", actualExpenses, uiState.currencySymbol, uiState.isDiscreetMode, Color(0xFFB42332), Icons.Default.TrendingDown)
            FinanceLine("Gastos pendientes", pendingExpenses, uiState.currencySymbol, uiState.isDiscreetMode, Color(0xFF8A5A00), Icons.Default.Schedule)
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
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(label, color = LakeText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Text(
            FormatUtils.formatCurrency(amount, currencySymbol, discreetMode),
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
