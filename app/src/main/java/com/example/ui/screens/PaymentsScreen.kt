package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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

    val isDark = uiState.isDarkTheme
    val pageBackground = if (isDark) Color(0xFF14110E) else Color(0xFFFAF5ED)
    val backgroundScrim = if (isDark) Color(0xD914110E) else Color(0xD1FAF5ED)
    val cardSurface = if (isDark) Color(0xFF1E293B) else Color.White
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFD6E4DF)
    val accentEmeraldText = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
    val accentEmeraldTint = if (isDark) Color(0xFF34D399) else EmeraldGreen
    val heroTextShadow = Shadow(color = Color(0xCC000000), offset = Offset(0f, 2f), blurRadius = 6f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBackground)
    ) {
        // Fotografía de fondo para el cuerpo inferior: Parque Central (Plaza de la Constitución)
        Image(
            painter = painterResource(id = R.drawable.parque_central_card_1793567079147),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isDark) 0.14f else 0.20f)
        )
        // Velo opaco protector para preservar legibilidad sin eliminar la atmósfera del Parque Central
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundScrim)
        )

        // FOTOGRAFÍA HEROICA SUPERIOR DOMINANTE: Palacio Nacional de la Cultura y Plaza de la Constitución
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.palacio_nacional_header_1793567078293),
                contentDescription = "Palacio Nacional de la Cultura y Parque Central, Guatemala",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )
            // Gradiente vertical de protección: sólido arriba para lectura nítida de cabecera y botones,
            // ventana clara en el centro para apreciar el Palacio Nacional, y fusión limpia al fondo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xEB031F1A),
                            0.24f to Color(0x9E031F1A),
                            0.72f to Color(0x33000000),
                            1.0f to pageBackground
                        )
                    )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior de PAGOS integrada sobre la fotografía
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldGreen,
                            border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PAGOS",
                                style = MaterialTheme.typography.titleLarge.copy(shadow = heroTextShadow),
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Compromisos Familiares",
                                style = MaterialTheme.typography.labelSmall.copy(shadow = heroTextShadow),
                                color = Color(0xFFF1F5F9),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Botón Directorio de Beneficiarios con fondo sólido institucional de alto contraste
                    OutlinedButton(
                        onClick = { showBeneficiariesDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDark) Color(0xFF0E2921) else Color(0xFF063729),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.2.dp, Color(0xFF6EE7B7)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .padding(end = 6.dp)
                            .testTag("open_beneficiaries_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Beneficiarios",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Botón "+ Pago"
                    Button(
                        onClick = {
                            commitmentToEdit = null
                            showAddEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .padding(end = 8.dp)
                            .testTag("add_payment_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pago", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
                // A. RESUMEN SUPERIOR: 4 tarjetas métricas sólidas sobre la fotografía dominante del Palacio Nacional
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
                                    containerColor = if (isDark) Color(0xFF261D17) else Color(0xFFFFFBEB)
                                ),
                                border = BorderStroke(1.2.dp, if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Total Pendiente",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = FormatUtils.formatCurrency(uiState.totalPendingPayments, uiState.currencySymbol, uiState.isDiscreetMode),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF78350F)
                                    )
                                    Text(
                                        text = "Obligaciones por saldar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF475569)
                                    )
                                }
                            }

                            // Pagados este mes
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF13261E) else Color(0xFFF0FDF4)
                                ),
                                border = BorderStroke(1.2.dp, if (isDark) Color(0xFF34D399) else Color(0xFF059669)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Pagado este Mes",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = FormatUtils.formatCurrency(uiState.monthPaidPaymentsTotal, uiState.currencySymbol, uiState.isDiscreetMode),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFFD1FAE5) else Color(0xFF064E3B)
                                    )
                                    Text(
                                        text = "Ya saldado y real",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
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
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1B2433) else Color(0xFFEFF6FF)
                                ),
                                border = BorderStroke(1.2.dp, if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFFBFDBFE) else Color(0xFF1D4ED8),
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
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E3A8A)
                                        )
                                        Text(
                                            text = "Vencen en 7 días",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                                        )
                                    }
                                }
                            }

                            // Vencidos
                            val overdueCount = uiState.overduePaymentsCount
                            val overdueCardBg = if (overdueCount > 0) {
                                if (isDark) Color(0xFF3B1518) else Color(0xFFFEF2F2)
                            } else {
                                if (isDark) Color(0xFF13261E) else Color(0xFFF0FDF4)
                            }
                            val overdueCardBorder = if (overdueCount > 0) {
                                if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                            } else {
                                if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                            }
                            val overdueIconBg = if (overdueCount > 0) {
                                if (isDark) Color(0xFF5C1D24) else Color(0xFFFEE2E2)
                            } else {
                                if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                            }
                            val overdueAccentText = if (overdueCount > 0) {
                                if (isDark) Color(0xFFFECACA) else Color(0xFF991B1B)
                            } else {
                                if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = overdueCardBg),
                                border = BorderStroke(1.2.dp, overdueCardBorder),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = overdueIconBg,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (overdueCount > 0) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = overdueAccentText,
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
                                            color = overdueAccentText
                                        )
                                        Text(
                                            text = if (overdueCount > 0) "Atención requerida" else "Al día",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (overdueCount > 0) overdueAccentText else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // B. AGENDA DE PAGOS (Contenedor sólido opaco + chips opacos de alto contraste)
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardSurface),
                        border = BorderStroke(1.dp, cardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = accentEmeraldTint,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Agenda de Compromisos",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentEmeraldText
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                    val isSelected = selectedAgendaFilterIndex == index
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedAgendaFilterIndex = index },
                                        label = {
                                            Text(
                                                text = filters[index],
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                            labelColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                            selectedContainerColor = EmeraldGreen,
                                            selectedLabelColor = Color.White
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                                            selectedBorderColor = if (isDark) Color(0xFF34D399) else EmeraldGreen
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Buscador de Pagos con superficie sólida y bordes nítidos
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Buscar por beneficiario, concepto o monto...",
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = accentEmeraldTint
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar",
                                        tint = if (isDark) Color(0xFFE2E8F0) else Color(0xFF475569)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardSurface,
                            unfocusedContainerColor = cardSurface,
                            focusedTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                            focusedBorderColor = accentEmeraldTint,
                            unfocusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
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
                            colors = CardDefaults.cardColors(containerColor = cardSurface),
                            border = BorderStroke(1.dp, cardBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = null,
                                            tint = accentEmeraldText,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "Sin compromisos pendientes" else "Sin resultados",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "No hay pagos pendientes en este criterio." else "No se encontraron pagos para «$searchQuery».",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        commitmentToEdit = null
                                        showAddEditDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreen,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Registrar Primer Compromiso", color = Color.White, fontWeight = FontWeight.Bold)
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

    val statusContainerBg = when {
        isPaid -> if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5)
        isOverdue -> if (isDarkTheme) Color(0xFF5C1D24) else Color(0xFFFEE2E2)
        isToday -> if (isDarkTheme) Color(0xFF451A03) else Color(0xFFFEF3C7)
        isUpcoming -> if (isDarkTheme) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
        else -> if (isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9)
    }
    val statusContentColor = when {
        isPaid -> if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46)
        isOverdue -> if (isDarkTheme) Color(0xFFFECACA) else Color(0xFF991B1B)
        isToday -> if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E)
        isUpcoming -> if (isDarkTheme) Color(0xFFBFDBFE) else Color(0xFF1E40AF)
        else -> if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF334155)
    }
    val statusBorderColor = when {
        isPaid -> if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
        isOverdue -> if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)
        isToday -> if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)
        isUpcoming -> if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)
        else -> if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
    }
    val cardBorderColor = when {
        isOverdue -> if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)
        isToday -> if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)
        isPaid -> if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
        else -> if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
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

    val primaryText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentEmeraldText = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) {
                if (isDarkTheme) Color(0xFF14241D) else Color(0xFFF2FAF6)
            } else {
                if (isDarkTheme) Color(0xFF1E293B) else Color.White
            }
        ),
        border = BorderStroke(1.2.dp, cardBorderColor),
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
                        color = statusContainerBg,
                        border = BorderStroke(1.dp, statusBorderColor),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = statusBadgeIcon,
                            contentDescription = null,
                            tint = statusContentColor,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = commitment.beneficiaryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = commitment.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Insignia de Estado sólida opaca
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusContainerBg,
                    border = BorderStroke(1.dp, statusBorderColor)
                ) {
                    Text(
                        text = statusBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusContentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Concepto y referencia
            Text(
                text = commitment.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = primaryText
            )

            if (commitment.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = commitment.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText
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
                        color = secondaryText
                    )
                    Text(
                        text = dateFormatter.format(Date(commitment.dueDateMillis)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverdue) {
                            if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                        } else {
                            primaryText
                        }
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (commitment.isVariableAmount) {
                        Text(
                            text = "Monto Variable",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFF92400E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = FormatUtils.formatCurrency(commitment.amount, currencySymbol, isDiscreetMode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPaid) accentEmeraldText else primaryText
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
                        tint = accentEmeraldText,
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
                        color = accentEmeraldText,
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
                            tint = accentEmeraldText,
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
                            tint = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!isPaid) {
                    Button(
                        onClick = onMarkAsPaid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color.White
                        ),
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
                        color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pagado ✓ (Registrado en Historial)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46)
                            )
                        }
                    }
                }
            }
        }
    }
}
