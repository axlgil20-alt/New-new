package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.affectsBudgetTotals
import com.example.data.model.isCancelled
import com.example.data.model.isGastoReal
import com.example.data.model.isGastoSupuesto
import com.example.data.model.isIngresoReal
import com.example.ui.components.TransactionItemCard
import com.example.ui.util.FormatUtils
import com.example.ui.util.QuincenaFilterHelper
import com.example.ui.viewmodel.BudgetUiState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Jade = Color(0xFF087F68)
private val Emerald = Color(0xFF0F9D75)
private val Deep = Color(0xFF0B1C26)
private val GuatemalaDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("es", "GT"))

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
    val month = remember(uiState.currentYear, uiState.currentMonthIndex) { YearMonth.of(uiState.currentYear, uiState.currentMonthIndex + 1) }
    val initialSelectedDate = LocalDate.now(QuincenaFilterHelper.FINANCIAL_ZONE)
        .takeIf { it.year == month.year && it.monthValue == month.monthValue } ?: month.atDay(1)
    var selectedDate by remember(month) { mutableStateOf(initialSelectedDate) }
    var selectedCycle by remember(month) {
        mutableStateOf(QuincenaFilterHelper.getBudgetPeriod(QuincenaFilterHelper.atStartOfDay(initialSelectedDate)).quincena)
    }
    var searchQuery by remember { mutableStateOf("") }
    val transactions = uiState.allTransactions.filter { it.affectsBudgetTotals }
    val commitments = uiState.paymentCommitments
    val monthTransactions = transactions.filter { QuincenaFilterHelper.localDate(it.dateMillis).let { d -> d.year == month.year && d.monthValue == month.monthValue } }
    val selectedPeriod = QuincenaFilterHelper.getBudgetPeriod(QuincenaFilterHelper.atStartOfDay(selectedDate))
    val q = QuincenaFilterHelper.filterByQuincena(transactions, selectedPeriod.year, selectedPeriod.monthIndex)
    val today = LocalDate.now(QuincenaFilterHelper.FINANCIAL_ZONE)
    val cyclePeriod = selectedPeriod
    val cycleTransactions = if (cyclePeriod.quincena == 1) q.q1Transactions else q.q2Transactions
    val dayTransactions = monthTransactions.filter { QuincenaFilterHelper.localDate(it.dateMillis) == selectedDate }
    val dayCommitments = commitments.filter { QuincenaFilterHelper.localDate(it.dueDateMillis) == selectedDate && !it.isCancelled }
    val searchedTransactions = cycleTransactions.filter { tx ->
        val query = searchQuery.trim()
        query.isBlank() || tx.title.contains(query, true) || tx.category.contains(query, true) || tx.notes.contains(query, true) || tx.amount.toString().contains(query)
    }
    val cycleRange = QuincenaFilterHelper.getBudgetPeriodRange(cyclePeriod.year, cyclePeriod.monthIndex, cyclePeriod.quincena)
    val cycleIncome = if (cyclePeriod.quincena == 1) q.q1Income else q.q2Income
    val cyclePendingIncome = if (cyclePeriod.quincena == 1) q.q1PendingIncome else q.q2PendingIncome
    val cycleExpense = if (cyclePeriod.quincena == 1) q.q1Expense else q.q2Expense
    val cycleRollover = QuincenaFilterHelper.calculateQuincenaRollover(transactions, cyclePeriod.year, cyclePeriod.monthIndex, cyclePeriod.quincena)
    val cycleCommitments = commitments.filter { !it.isCancelled && !it.isPaid && it.status != com.example.data.model.PaymentStatus.PAID && it.dueDateMillis in cycleRange.first..cycleRange.second }
    val cyclePendingExpenses = cycleTransactions.filter { it.isGastoSupuesto }
    val projectedOutflow = QuincenaFilterHelper.calculateProjectedPendingOutflow(cycleCommitments, cyclePendingExpenses)
    val currentPeriod = QuincenaFilterHelper.getBudgetPeriod(QuincenaFilterHelper.atStartOfDay(today))
    val daysRemaining = if (currentPeriod == cyclePeriod) {
        Chronology.daysBetweenInclusive(today, QuincenaFilterHelper.localDate(cycleRange.second)).coerceAtLeast(0)
    } else null
    val realAvailable = cycleRollover + cycleIncome - cycleExpense
    val projected = realAvailable + cyclePendingIncome - projectedOutflow
    val monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "GT"))).replaceFirstChar { it.uppercase() }
    val isDark = uiState.isDarkTheme
    val card = if (isDark) Color(0xFF102832) else Color.White
    val text = if (isDark) Color(0xFFF1F5F9) else Color(0xFF143449)
    val muted = if (isDark) Color(0xFFB7CBD2) else Color(0xFF466773)

    LazyColumn(modifier = modifier.fillMaxSize().background(if (isDark) Deep else Color(0xFFF2F8F6)).padding(bottom = 72.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text("QUINCENAS 2.0", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = if (isDark) Color(0xFF6EE7B7) else Jade)
                Text("Tu dinero, claro en cada ciclo", color = muted)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2).forEach { cycle ->
                    val chipRange = QuincenaFilterHelper.getBudgetPeriodRange(selectedPeriod.year, selectedPeriod.monthIndex, cycle)
                    FilterChip(selected = selectedCycle == cycle, onClick = {
                        selectedCycle = cycle
                        val periodMonth = YearMonth.of(selectedPeriod.year, selectedPeriod.monthIndex + 1)
                        selectedDate = if (cycle == 1) periodMonth.atDay(1) else periodMonth.atDay(15)
                    }, label = { Text("Ciclo $cycle de ${periodMonthLabel(selectedPeriod)}: ${shortDate(chipRange.first)} - ${shortDate(chipRange.second)}") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Jade, selectedLabelColor = Color.White), modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(card), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CENTRO DE CONTROL", color = Jade, fontWeight = FontWeight.ExtraBold)
                    Text("Ciclo actual · ${cyclePeriod.quincena}.ª · ${cyclePeriod.year}-${cyclePeriod.monthIndex + 1}", color = text, fontWeight = FontWeight.Bold)
                    Text(if (daysRemaining != null) "$daysRemaining días restantes en el ciclo" else "Consulta un mes para ver su ciclo", color = muted)
                    MetricLine("Saldo disponible real", realAvailable, uiState, Emerald, Icons.Default.Payments, text)
                    MetricLine("Ingresos confirmados", cycleIncome, uiState, Color(0xFF168AAD), Icons.Default.TrendingUp, text)
                    if (cyclePendingIncome > 0.0) MetricLine("Ingresos supuestos · proyección", cyclePendingIncome, uiState, Color(0xFF8B5CF6), Icons.Default.Schedule, text)
                    MetricLine("Gastos reales", cycleExpense, uiState, Color(0xFFC2414B), Icons.Default.TrendingDown, text)
                    MetricLine("Salidas proyectadas", projectedOutflow, uiState, Color(0xFFB7791F), Icons.Default.Schedule, text)
                    MetricLine("Rollover recibido", cycleRollover, uiState, Jade, Icons.Default.TrendingUp, text)
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Jade) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Proyección de cierre", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(FormatUtils.formatCurrency(projected, uiState.currencySymbol, uiState.isDiscreetMode), color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(card), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onPreviousMonth) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Mes anterior", tint = Jade) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CalendarMonth, null, tint = Emerald); Spacer(Modifier.size(8.dp)); Text(monthLabel, color = text, fontWeight = FontWeight.Bold) }
                    IconButton(onClick = onNextMonth) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Mes siguiente", tint = Jade) }
                }
            }
        }
        item {
            val previousDay = if (cyclePeriod.quincena == 1) {
                QuincenaFilterHelper.localDate(cycleRange.first)
            } else {
                QuincenaFilterHelper.localDate(cycleRange.first).minusDays(1)
            }
            val previousDayTransactions = transactions.filter { QuincenaFilterHelper.localDate(it.dateMillis) == previousDay }
            val previousIncome = previousDayTransactions.filter { it.isIngresoReal }.sumOf { it.amount }
            val previousExpense = previousDayTransactions.filter { it.isGastoReal }.sumOf { it.amount }
            PreviousCycleDayCard(previousDay, previousDayTransactions.size, previousIncome, previousExpense, uiState, muted)
        }
        item {
            MonthlyCalendar(month, monthTransactions, commitments, selectedDate, {
                selectedDate = it
                selectedCycle = QuincenaFilterHelper.getBudgetPeriod(QuincenaFilterHelper.atStartOfDay(it)).quincena
            }, isDark)
        }
        item {
            DailyDetail(selectedDate, dayTransactions, dayCommitments, uiState, text, muted, onEditTransaction, onDeleteTransaction, onHighlightTransaction, onToggleVerifyTransaction, onDecideTentative)
        }
        item {
            TransactionList(searchQuery, { searchQuery = it }, searchedTransactions, uiState, muted, onEditTransaction, onDeleteTransaction, onHighlightTransaction, onToggleVerifyTransaction, onDecideTentative)
        }
        item { UpcomingAgenda(today, transactions, commitments, uiState, text, muted) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun PreviousCycleDayCard(date: LocalDate, movementCount: Int, income: Double, expense: Double, uiState: BudgetUiState, muted: Color) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("quincenas_previous_cycle_day"), colors = CardDefaults.cardColors(if (uiState.isDarkTheme) Color(0xFF102832) else Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text("INICIO DEL CICLO 1", color = Jade, fontWeight = FontWeight.ExtraBold)
            Text("${date.format(GuatemalaDateFormatter).replaceFirstChar { it.uppercase() }} · $movementCount movimientos registrados", color = muted)
            Text("Ingresos confirmados: ${FormatUtils.formatCurrency(income, uiState.currencySymbol, uiState.isDiscreetMode)}", color = Emerald)
            Text("Gastos reales: ${FormatUtils.formatCurrency(expense, uiState.currencySymbol, uiState.isDiscreetMode)}", color = Color(0xFFC2414B))
            Text("Balance neto: ${FormatUtils.formatCurrency(income - expense, uiState.currencySymbol, uiState.isDiscreetMode)}", color = Jade, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TransactionList(query: String, onQueryChange: (String) -> Unit, transactions: List<TransactionEntity>, uiState: BudgetUiState, muted: Color, onEdit: (TransactionEntity) -> Unit, onDelete: (TransactionEntity) -> Unit, onHighlight: (TransactionEntity, Long) -> Unit, onVerify: (TransactionEntity) -> Unit, onDecide: (TransactionEntity, String) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(if (uiState.isDarkTheme) Color(0xFF102832) else Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MOVIMIENTOS DEL MES", color = Jade, fontWeight = FontWeight.ExtraBold)
            OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth().testTag("quincenas_search_bar"), singleLine = true, placeholder = { Text("Buscar por concepto, categoría o monto", color = muted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Jade) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Jade, unfocusedBorderColor = muted))
            transactions.forEachIndexed { index, tx ->
                TransactionItemCard(tx, uiState.currencySymbol, onEdit = { onEdit(tx) }, onDelete = { onDelete(tx) }, onHighlight = { onHighlight(tx, it) }, onToggleVerify = { onVerify(tx) }, onDecideTentative = { onDecide(tx, it) }, onToggleGastoSupuesto = { onDecide(tx, if (tx.isGastoSupuesto) "CONFIRM" else "PENDING") }, isDiscreetMode = uiState.isDiscreetMode, isDarkTheme = uiState.isDarkTheme, rowIndex = index)
            }
            if (transactions.isEmpty()) Text(if (query.isBlank()) "No hay movimientos registrados en este mes." else "No se encontraron coincidencias.", color = muted)
        }
    }
}

@Composable
private fun MonthlyCalendar(month: YearMonth, transactions: List<TransactionEntity>, commitments: List<PaymentCommitmentEntity>, selected: LocalDate, onSelect: (LocalDate) -> Unit, isDark: Boolean) {
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val leading = month.atDay(1).dayOfWeek.value - 1
    val cells = List(leading) { null } + days
    val bg = if (isDark) Color(0xFF102832) else Color.White
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("quincenas_month_calendar"), colors = CardDefaults.cardColors(bg), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("CALENDARIO FINANCIERO", color = Jade, fontWeight = FontWeight.ExtraBold)
            Row(Modifier.fillMaxWidth()) { listOf("L","M","X","J","V","S","D").forEach { Text(it, Modifier.weight(1f).padding(vertical = 8.dp), color = Color.Gray, fontWeight = FontWeight.Bold) } }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).height(54.dp).padding(2.dp).clickable(enabled = date != null) { date?.let(onSelect) }.background(if (date == selected) Color(0xFFB7E4D5) else Color.Transparent, RoundedCornerShape(8.dp)), contentAlignment = Alignment.TopCenter) {
                            if (date != null) {
                                val txs = transactions.filter { QuincenaFilterHelper.localDate(it.dateMillis) == date }
                                val pays = commitments.filter { !it.isCancelled && QuincenaFilterHelper.localDate(it.dueDateMillis) == date }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${date.dayOfMonth}", color = if (date.dayOfMonth == 15 || date.dayOfMonth == month.lengthOfMonth()) Jade else Color.Unspecified, fontWeight = if (date.dayOfMonth == 15 || date.dayOfMonth == month.lengthOfMonth()) FontWeight.Black else FontWeight.Normal)
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (txs.any { it.isIngresoReal }) Text("↑", color = Emerald)
                                        if (txs.any { it.isGastoReal }) Text("↓", color = Color(0xFFC2414B))
                                        if (txs.any { it.isGastoSupuesto }) Text("·", color = Color(0xFFB7791F))
                                        if (pays.any { !it.isPaid && it.status != com.example.data.model.PaymentStatus.PAID }) Text("$", color = Color(0xFF8B5CF6))
                                        if (pays.any { it.isPaid || it.status == com.example.data.model.PaymentStatus.PAID }) Text("✓", color = Emerald)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("↑ ingreso confirmado  ↓ gasto real  · supuesto  $ compromiso · 15 y último día inician ciclo", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
private fun DailyDetail(date: LocalDate, transactions: List<TransactionEntity>, commitments: List<PaymentCommitmentEntity>, uiState: BudgetUiState, text: Color, muted: Color, onEdit: (TransactionEntity) -> Unit, onDelete: (TransactionEntity) -> Unit, onHighlight: (TransactionEntity, Long) -> Unit, onVerify: (TransactionEntity) -> Unit, onDecide: (TransactionEntity, String) -> Unit) {
    val income = transactions.filter { it.isIngresoReal }.sumOf { it.amount }
    val expenses = transactions.filter { it.isGastoReal }.sumOf { it.amount }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(if (uiState.isDarkTheme) Color(0xFF102832) else Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("DETALLE DEL DÍA", color = Jade, fontWeight = FontWeight.ExtraBold)
            Text(date.format(GuatemalaDateFormatter).replaceFirstChar { it.uppercase() }, color = text, fontWeight = FontWeight.Bold)
            Text("Ingresos ${FormatUtils.formatCurrency(income, uiState.currencySymbol, uiState.isDiscreetMode)} · Gastos ${FormatUtils.formatCurrency(expenses, uiState.currencySymbol, uiState.isDiscreetMode)} · Compromisos ${commitments.size}", color = muted)
            transactions.forEachIndexed { index, tx -> TransactionItemCard(tx, uiState.currencySymbol, onEdit = { onEdit(tx) }, onDelete = { onDelete(tx) }, onHighlight = { onHighlight(tx, it) }, onToggleVerify = { onVerify(tx) }, onDecideTentative = { onDecide(tx, it) }, onToggleGastoSupuesto = { onDecide(tx, if (tx.isGastoSupuesto) "CONFIRM" else "PENDING") }, isDiscreetMode = uiState.isDiscreetMode, isDarkTheme = uiState.isDarkTheme, rowIndex = index) }
            commitments.forEach {
                val paid = it.isPaid || it.status == com.example.data.model.PaymentStatus.PAID
                Text("${if (paid) "Pago registrado" else "Pago pendiente"} · ${it.title} · ${FormatUtils.formatCurrency(it.amount, uiState.currencySymbol, uiState.isDiscreetMode)}", color = if (paid) Emerald else Color(0xFF8B5CF6))
            }
            if (transactions.isEmpty() && commitments.isEmpty()) Text("No hay movimientos ni compromisos registrados para este día.", color = muted)
        }
    }
}

@Composable
private fun UpcomingAgenda(today: LocalDate, transactions: List<TransactionEntity>, commitments: List<PaymentCommitmentEntity>, uiState: BudgetUiState, text: Color, muted: Color) {
    val end = today.plusDays(6)
    val tx = transactions.filter { val d = QuincenaFilterHelper.localDate(it.dateMillis); d in today..end }
    val pay = commitments.filter { !it.isCancelled && !it.isPaid && it.status != com.example.data.model.PaymentStatus.PAID && QuincenaFilterHelper.localDate(it.dueDateMillis) in today..end }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(if (uiState.isDarkTheme) Color(0xFF102832) else Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Event, null, tint = Jade); Spacer(Modifier.size(8.dp)); Text("AGENDA · PRÓXIMOS SIETE DÍAS", color = Jade, fontWeight = FontWeight.ExtraBold) }
            pay.sortedBy { it.dueDateMillis }.forEach { Text("Pago · ${it.title} · ${QuincenaFilterHelper.localDate(it.dueDateMillis).format(GuatemalaDateFormatter)}", color = text) }
            tx.sortedBy { it.dateMillis }.forEach { Text("${if (it.isIngresoReal) "Ingreso confirmado" else if (it.isGastoSupuesto) "Supuesto" else "Movimiento"} · ${it.title} · ${QuincenaFilterHelper.localDate(it.dateMillis).format(GuatemalaDateFormatter)}", color = text) }
            if (pay.isEmpty() && tx.isEmpty()) Text("No hay eventos registrados para los próximos siete días.", color = muted)
        }
    }
}

@Composable
private fun MetricLine(label: String, amount: Double, state: BudgetUiState, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, text: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text(label, color = text) }
        Text(FormatUtils.formatCurrency(amount, state.currencySymbol, state.isDiscreetMode), color = color, fontWeight = FontWeight.Bold)
    }
}

private fun shortDate(timeMillis: Long): String =
    QuincenaFilterHelper.localDate(timeMillis).format(DateTimeFormatter.ofPattern("d MMM", Locale("es", "GT")))

private fun periodMonthLabel(period: com.example.ui.util.BudgetPeriod): String =
    YearMonth.of(period.year, period.monthIndex + 1)
        .format(DateTimeFormatter.ofPattern("MMMM", Locale("es", "GT")))
        .replaceFirstChar { it.uppercase() }

private object Chronology {
    fun daysBetweenInclusive(start: LocalDate, end: LocalDate): Int = if (end.isBefore(start)) 0 else java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
}
