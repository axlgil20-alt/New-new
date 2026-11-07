package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BeneficiaryEntity
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.model.PaymentFrequency
import com.example.data.model.PaymentStatus
import com.example.data.model.isDueToday
import com.example.data.model.isOverdue
import com.example.data.model.isUpcoming
import com.example.ui.components.AddEditPaymentDialog
import com.example.ui.components.BeneficiariesDialog
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.BudgetUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    uiState: BudgetUiState,
    onAddPayment: (
        title: String,
        beneficiaryName: String,
        description: String,
        amount: Double,
        dueDateMillis: Long,
        category: String,
        frequency: String,
        isVariableAmount: Boolean,
        reminderDaysBefore: Int,
        notes: String
    ) -> Unit,
    onMarkAsPaid: (Long) -> Unit,
    onUpdatePayment: (PaymentCommitmentEntity) -> Unit,
    onDeletePayment: (PaymentCommitmentEntity) -> Unit,
    onAddBeneficiary: (name: String, category: String, description: String, contact: String, notes: String) -> Unit,
    onUpdateBeneficiary: (BeneficiaryEntity) -> Unit,
    onDeleteBeneficiary: (BeneficiaryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedAgendaFilterIndex by remember { mutableIntStateOf(0) } // 0: Todos, 1: Hoy, 2: Próximos 7 días, 3: Este mes, 4: Vencidos
    var searchQuery by remember { mutableStateOf("") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var commitmentToEdit by remember { mutableStateOf<PaymentCommitmentEntity?>(null) }
    var showBeneficiariesDialog by remember { mutableStateOf(false) }

    val now = remember { System.currentTimeMillis() }
    val (todayStart, todayEnd) = remember(now) { com.example.ui.util.QuincenaFilterHelper.getDayRange(now) }
    val (monthStart, monthEnd) = remember(uiState.currentYear, uiState.currentMonthIndex) {
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, uiState.currentYear)
            set(Calendar.MONTH, uiState.currentMonthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        Pair(start, end)
    }

    val allPayments = uiState.paymentCommitments

    // Filtrado por Agenda
    val agendaFilteredList = remember(allPayments, selectedAgendaFilterIndex, now, monthStart, monthEnd) {
        when (selectedAgendaFilterIndex) {
            1 -> allPayments.filter { it.isDueToday(now) }
            2 -> allPayments.filter { it.isUpcoming(now, 7) }
            3 -> allPayments.filter { it.dueDateMillis in monthStart..monthEnd }
            4 -> allPayments.filter { it.isOverdue(now) }
            else -> allPayments
        }
    }

    // Filtrado adicional por búsqueda
    val displayList = remember(agendaFilteredList, searchQuery) {
        if (searchQuery.isBlank()) {
            agendaFilteredList
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            agendaFilteredList.filter { p ->
                p.title.lowercase(Locale.ROOT).contains(q) ||
                p.beneficiaryName.lowercase(Locale.ROOT).contains(q) ||
                p.description.lowercase(Locale.ROOT).contains(q) ||
                p.category.lowercase(Locale.ROOT).contains(q) ||
                "%.2f".format(p.amount).contains(q)
            }
        }
    }

    val pageBackground = if (uiState.isDarkTheme) Color(0xFF1B1612) else Color(0xFFFAF5ED)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior de PAGOS
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (uiState.isDarkTheme) Color(0xFF382313) else Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFD97706)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = if (uiState.isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PAGOS",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Administrador de Compromisos Familiares",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Botón Directorio de Beneficiarios
                    OutlinedButton(
                        onClick = { showBeneficiariesDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .padding(end = 6.dp)
                            .testTag("open_beneficiaries_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Beneficiarios", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Botón "+ Nuevo Pago"
                    Button(
                        onClick = {
                            commitmentToEdit = null
                            showAddEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .padding(end = 8.dp)
                            .testTag("add_payment_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pago", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 76.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // A. RESUMEN SUPERIOR (4 tarjetas métricas claras)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Fila 1: Total Pendiente & Pagados este mes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Pendiente
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (uiState.isDarkTheme) Color(0xFF261D17) else Color(0xFFFFFBEB)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Total Pendiente",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = FormatUtils.formatCurrency(uiState.totalPendingPayments, uiState.currencySymbol, uiState.isDiscreetMode),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (uiState.isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Obligaciones por saldar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Pagados este mes
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (uiState.isDarkTheme) Color(0xFF1B291D) else Color(0xFFF0FDF4)
                                ),
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Pagado este Mes",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.isDarkTheme) Color(0xFFA8D5A0) else Color(0xFF166534)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = FormatUtils.formatCurrency(uiState.monthPaidPaymentsTotal, uiState.currencySymbol, uiState.isDiscreetMode),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (uiState.isDarkTheme) Color(0xFFA8D5A0) else Color(0xFF166534)
                                    )
                                    Text(
                                        text = "Ya saldado y real",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Fila 2: Próximos (7 días) & Vencidos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Próximos
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${uiState.upcomingPaymentsCount} Próximos",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Vencen en 7 días",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Vencidos
                            val overdueCount = uiState.overduePaymentsCount
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (overdueCount > 0) {
                                        if (uiState.isDarkTheme) Color(0xFF3A1C1C) else Color(0xFFFEF2F2)
                                    } else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (overdueCount > 0) ExpenseRed else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (overdueCount > 0) ExpenseRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (overdueCount > 0) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (overdueCount > 0) ExpenseRed else EmeraldGreen,
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "$overdueCount Vencido(s)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (overdueCount > 0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (overdueCount > 0) "Atención requerida" else "Al día",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (overdueCount > 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // B. AGENDA DE PAGOS (Chips de filtro)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Agenda de Compromisos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val filters = listOf(
                                "Todos (${allPayments.size})",
                                "Hoy",
                                "Próximos 7 días",
                                "Este mes",
                                "Vencidos"
                            )
                            items(filters.size) { index ->
                                FilterChip(
                                    selected = selectedAgendaFilterIndex == index,
                                    onClick = { selectedAgendaFilterIndex = index },
                                    label = { Text(filters[index], fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldGreen,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                    }
                }

                // Buscador de Pagos
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por beneficiario, concepto o monto...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldGreen) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("payments_search_input")
                    )
                }

                // C. LISTA DE COMPROMISOS DE PAGO
                if (displayList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "No hay pagos pendientes en este criterio." else "No se encontraron pagos para «$searchQuery».",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        commitmentToEdit = null
                                        showAddEditDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Registrar Primer Compromiso", color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    items(displayList, key = { it.id }) { commitment ->
                        PaymentCommitmentCard(
                            commitment = commitment,
                            currencySymbol = uiState.currencySymbol,
                            isDiscreetMode = uiState.isDiscreetMode,
                            isDarkTheme = uiState.isDarkTheme,
                            onMarkAsPaid = { onMarkAsPaid(commitment.id) },
                            onEdit = {
                                commitmentToEdit = commitment
                                showAddEditDialog = true
                            },
                            onDelete = { onDeletePayment(commitment) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal Crear / Editar Pago
    if (showAddEditDialog) {
        AddEditPaymentDialog(
            commitment = commitmentToEdit,
            beneficiaries = uiState.paymentBeneficiaries,
            currencySymbol = uiState.currencySymbol,
            onDismiss = {
                showAddEditDialog = false
                commitmentToEdit = null
            },
            onSave = { title, ben, desc, amt, due, cat, freq, isVar, rem, notes ->
                if (commitmentToEdit == null) {
                    onAddPayment(title, ben, desc, amt, due, cat, freq, isVar, rem, notes)
                } else {
                    onUpdatePayment(
                        commitmentToEdit!!.copy(
                            title = title,
                            beneficiaryName = ben,
                            description = desc,
                            amount = amt,
                            dueDateMillis = due,
                            category = cat,
                            frequency = freq,
                            isVariableAmount = isVar,
                            reminderDaysBefore = rem,
                            notes = notes
                        )
                    )
                }
                showAddEditDialog = false
                commitmentToEdit = null
            }
        )
    }

    // Modal Directorio de Beneficiarios
    if (showBeneficiariesDialog) {
        BeneficiariesDialog(
            beneficiaries = uiState.paymentBeneficiaries,
            onDismiss = { showBeneficiariesDialog = false },
            onAddBeneficiary = onAddBeneficiary,
            onUpdateBeneficiary = onUpdateBeneficiary,
            onDeleteBeneficiary = onDeleteBeneficiary
        )
    }
}

@Composable
fun PaymentCommitmentCard(
    commitment: PaymentCommitmentEntity,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean,
    onMarkAsPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = remember { System.currentTimeMillis() }
    val isPaid = commitment.isPaid || commitment.status == PaymentStatus.PAID
    val isOverdue = commitment.isOverdue(now)
    val isToday = commitment.isDueToday(now)
    val isUpcoming = commitment.isUpcoming(now, 7)

    val dateFormatter = remember { SimpleDateFormat("dd 'de' MMM, yyyy", Locale("es", "GT")) }

    val statusBadgeColor = when {
        isPaid -> EmeraldGreen
        isOverdue -> ExpenseRed
        isToday -> Color(0xFFD97706)
        isUpcoming -> Color(0xFF2563EB)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusBadgeText = when {
        isPaid -> "PAGADO"
        isOverdue -> "VENCIDO"
        isToday -> "VENCE HOY"
        isUpcoming -> "PRÓXIMO"
        else -> "PENDIENTE"
    }

    val statusBadgeIcon = when {
        isPaid -> Icons.Default.CheckCircle
        isOverdue -> Icons.Default.ErrorOutline
        isToday -> Icons.Default.Warning
        isUpcoming -> Icons.Default.HourglassTop
        else -> Icons.Default.Schedule
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) {
                if (isDarkTheme) Color(0xFF1E281F) else Color(0xFFF7FAF7)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.2.dp,
            when {
                isOverdue -> ExpenseRed
                isToday -> Color(0xFFD97706)
                isPaid -> EmeraldGreen.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPaid) 1.dp else 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila superior: Beneficiario, Insignia de Estado y Acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBadgeColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = statusBadgeIcon,
                            contentDescription = null,
                            tint = statusBadgeColor,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = commitment.beneficiaryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = commitment.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Insignia de Estado
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBadgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, statusBadgeColor)
                ) {
                    Text(
                        text = statusBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusBadgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Concepto y referencia
            Text(
                text = commitment.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (commitment.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = commitment.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Monto y Fecha de Vencimiento
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Vencimiento",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormatter.format(Date(commitment.dueDateMillis)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverdue) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (commitment.isVariableAmount) {
                        Text(
                            text = "Monto Variable",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = FormatUtils.formatCurrency(commitment.amount, currencySymbol, isDiscreetMode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPaid) EmeraldGreen else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Frecuencia si es recurrente
            if (commitment.frequency != PaymentFrequency.ONCE) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val freqLabel = when (commitment.frequency) {
                        PaymentFrequency.MONTHLY -> "Recurrente Mensual"
                        PaymentFrequency.BIWEEKLY -> "Recurrente Quincenal"
                        PaymentFrequency.WEEKLY -> "Recurrente Semanal"
                        else -> "Recurrente"
                    }
                    Text(
                        text = "$freqLabel (autoprograma el siguiente vencimiento al pagar)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón de Acción Principal "Marcar como pagado" y Opciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!isPaid) {
                    Button(
                        onClick = onMarkAsPaid,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("mark_as_paid_button_${commitment.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Marcar como Pagado", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, EmeraldGreen)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pagado ✓ (Registrado en Historial)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }
                    }
                }
            }
        }
    }
}
