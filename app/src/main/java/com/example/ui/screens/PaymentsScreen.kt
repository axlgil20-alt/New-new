package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BeneficiaryEntity
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.model.PaymentFrequency
import com.example.data.model.PaymentStatus
import com.example.data.model.isDueToday
import com.example.data.model.isOverdue
import com.example.data.model.isUpcoming
import com.example.data.model.withExactAmountMinor
import com.example.ui.components.AddEditPaymentDialog
import com.example.ui.components.BeneficiariesDialog
import com.example.ui.components.PaymentCommitmentStack
import com.example.ui.components.PaymentCommitmentStackLogic
import com.example.ui.components.PostponePreset
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JadeGreen
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
        notes: String,
        creditCardAccountId: Long?,
        paymentSourceAccountId: Long?
    ) -> Unit,
    onMarkAsPaid: (Long) -> Unit,
    onPostponePayment: (PaymentCommitmentEntity, Long, String) -> Unit = { _, _, _ -> },
    onUpdatePayment: (PaymentCommitmentEntity) -> Unit,
    onDeletePayment: (PaymentCommitmentEntity) -> Unit,
    onAddBeneficiary: (name: String, category: String, description: String, contact: String, notes: String) -> Unit,
    onUpdateBeneficiary: (BeneficiaryEntity) -> Unit,
    onDeleteBeneficiary: (BeneficiaryEntity) -> Unit,
    accountNameById: Map<Long, String> = emptyMap(),
    accounts: List<com.example.data.model.FinancialAccountEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedAgendaFilterIndex by remember { mutableIntStateOf(0) } // 0: Todos, 1: Hoy, 2: Próximos 7 días, 3: Este mes, 4: Vencidos
    var searchQuery by remember { mutableStateOf("") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var commitmentToEdit by remember { mutableStateOf<PaymentCommitmentEntity?>(null) }
    var showBeneficiariesDialog by remember { mutableStateOf(false) }
    var selectedCommitmentId by remember { mutableStateOf<Long?>(null) }
    var commitmentForDetail by remember { mutableStateOf<PaymentCommitmentEntity?>(null) }
    var commitmentForPayConfirmation by remember { mutableStateOf<PaymentCommitmentEntity?>(null) }
    var commitmentForPostponeConfirmation by remember { mutableStateOf<PaymentCommitmentEntity?>(null) }
    var showFullCommitmentsList by remember { mutableStateOf(false) }

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

    val sortedDisplayList = remember(displayList, now) {
        PaymentCommitmentStackLogic.sortCommitmentsForStack(displayList, now)
    }

    LaunchedEffect(sortedDisplayList) {
        selectedCommitmentId = PaymentCommitmentStackLogic.resolveSelectedCommitmentId(
            commitments = sortedDisplayList,
            currentSelectedId = selectedCommitmentId
        )
    }

    val swipeResetKey = remember(
        selectedAgendaFilterIndex,
        searchQuery,
        allPayments.size,
        allPayments.count { it.isPaid }
    ) {
        "${selectedAgendaFilterIndex}|${searchQuery}|${allPayments.size}|${allPayments.count { it.isPaid }}"
    }

    val isDark = uiState.isDarkTheme
    val pageBackground = if (isDark) Color(0xFF101418) else Color(0xFFF6F1E7)
    val backgroundScrimBrush = if (isDark) {
        Brush.verticalGradient(
            0.0f to Color(0x780D1318),
            0.5f to Color(0x700D1318),
            1.0f to Color(0x820D1318)
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color(0x28FAF5ED),
            0.5f to Color(0x22FAF5ED),
            1.0f to Color(0x2CFAF5ED)
        )
    }
    val secondaryCardSurface = if (isDark) Color(0xFF0E2620) else Color(0xFFF6FBF9)
    val searchFieldSurface = if (isDark) Color(0xFF0A241E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF10B981) else Color(0xFF86C5B0)
    val accentEmeraldText = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
    val accentEmeraldTint = if (isDark) Color(0xFF34D399) else EmeraldGreen
    val heroTextShadow = Shadow(color = Color(0xD9000000), offset = Offset(0f, 2f), blurRadius = 6f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBackground)
    ) {
        // Fotografía de fondo a pantalla completa: Parque Central de Antigua Guatemala
        Image(
            painter = painterResource(id = R.drawable.img_antigua_parque_central_1795208568243),
            contentDescription = "Parque Central de Antigua Guatemala",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isDark) 0.62f else 0.92f)
        )
        // Velo ligero para balancear contraste sin ocultar el Parque Central
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundScrimBrush)
        )

        // FOTOGRAFÍA DE ENCABEZADO: Palacio Nacional de la Cultura de Guatemala
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(232.dp)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Black,
                            0.74f to Color.Black,
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            Image(
                painter = painterResource(id = R.drawable.palacio_nacional_header_1793567078293),
                contentDescription = "Palacio Nacional de la Cultura, Guatemala",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isDark) 0.86f else 0.98f)
            )
            // Protección localizada únicamente detrás del TopAppBar para lectura nítida sin tapar el edificio
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xB8031F1A),
                            0.36f to Color(0x52031F1A),
                            0.75f to Color(0x14000000),
                            1.0f to Color.Transparent
                        )
                    )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior de PAGOS sin encimamientos ni truncamientos sobre Palacio Nacional
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
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Compromisos Familiares",
                                style = MaterialTheme.typography.labelSmall.copy(shadow = heroTextShadow),
                                color = Color(0xFFF1F5F9)
                            )
                        }
                    }
                },
                actions = {
                    // Icono compacto para Beneficiarios (sin competir por texto en TopAppBar)
                    OutlinedButton(
                        onClick = { showBeneficiariesDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDark) Color(0xFF0A241E) else Color(0xFF063729),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.2.dp, Color(0xFF6EE7B7)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .padding(end = 6.dp)
                            .testTag("open_beneficiaries_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Directorio de Beneficiarios",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Benef.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 18.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // A. RESUMEN SUPERIOR: 4 tarjetas métricas 100% opacas con acento Mazo de Jade
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
                                    containerColor = if (isDark) Color(0xFF2B200C) else Color(0xFFFFFBEB)
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

                            // Pagados este mes (100% opaco)
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF0A241E) else Color(0xFFECFDF5)
                                ),
                                border = BorderStroke(1.2.dp, if (isDark) Color(0xFF34D399) else JadeGreen),
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
                            // Próximos (100% opaco)
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF0D2D27) else Color(0xFFF0FDFA)
                                ),
                                border = BorderStroke(1.2.dp, if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E)),
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
                                        color = if (isDark) Color(0xFF064E3B) else Color(0xFFCCFBF1),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF99F6E4) else Color(0xFF0F766E),
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
                                            color = if (isDark) Color(0xFFCCFBF1) else Color(0xFF115E59)
                                        )
                                        Text(
                                            text = "Vencen en 7 días",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                                        )
                                    }
                                }
                            }

                            // Vencidos (100% opaco)
                            val overdueCount = uiState.overduePaymentsCount
                            val overdueCardBg = if (overdueCount > 0) {
                                if (isDark) Color(0xFF2B1418) else Color(0xFFFEF2F2)
                            } else {
                                if (isDark) Color(0xFF0A241E) else Color(0xFFECFDF5)
                            }
                            val overdueCardBorder = if (overdueCount > 0) {
                                if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                            } else {
                                if (isDark) Color(0xFF34D399) else JadeGreen
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

                // B. AGENDA DE PAGOS (Superficie secundaria translúcida + chips legibles)
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = secondaryCardSurface),
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
                                            containerColor = if (isDark) Color(0xFF07231C) else Color(0xFFEBF7F2),
                                            labelColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF064E3B),
                                            selectedContainerColor = EmeraldGreen,
                                            selectedLabelColor = Color.White
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isDark) Color(0xFF10B981) else Color(0xFF86C5B0),
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
                            focusedContainerColor = searchFieldSurface,
                            unfocusedContainerColor = searchFieldSurface,
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

                // C. STACK INTELIGENTE + LISTA COMPLETA SINCRONIZADA DE COMPROMISOS DE PAGO
                if (sortedDisplayList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("payments_empty_state_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = secondaryCardSurface),
                            border = BorderStroke(1.dp, cardBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
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
                    item {
                        PaymentCommitmentStack(
                            commitments = sortedDisplayList,
                            selectedCommitmentId = selectedCommitmentId,
                            accountNameById = accountNameById,
                            currencySymbol = uiState.currencySymbol,
                            isDiscreetMode = uiState.isDiscreetMode,
                            isDarkTheme = uiState.isDarkTheme,
                            nowMillis = now,
                            resetActionsKey = swipeResetKey,
                            onSelectCommitment = { id -> selectedCommitmentId = id },
                            onRequestPay = { commitment -> commitmentForPayConfirmation = commitment },
                            onRequestPostpone = { commitment -> commitmentForPostponeConfirmation = commitment },
                            onViewDetail = { commitment ->
                                selectedCommitmentId = commitment.id
                                commitmentForDetail = commitment
                            },
                            onEditCommitment = { commitment ->
                                selectedCommitmentId = commitment.id
                                commitmentToEdit = commitment
                                showAddEditDialog = true
                            },
                            onAddNewPayment = {
                                commitmentToEdit = null
                                showAddEditDialog = true
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (sortedDisplayList.size > 1) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = secondaryCardSurface,
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Lista completa (${sortedDisplayList.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFECFDF5) else Color(0xFF022C22),
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = showFullCommitmentsList,
                                        onClick = { showFullCommitmentsList = !showFullCommitmentsList },
                                        label = {
                                            Text(
                                                text = if (showFullCommitmentsList) "Ocultar lista" else "Mostrar todos",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = if (isDark) Color(0xFF07231C) else Color(0xFFEBF7F2),
                                            selectedContainerColor = EmeraldGreen,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("toggle_payments_full_list")
                                    )
                                }
                            }
                        }

                        if (showFullCommitmentsList) {
                            items(sortedDisplayList, key = { it.id }) { commitment ->
                                val isSelected = commitment.id == selectedCommitmentId
                                PaymentCommitmentCard(
                                    commitment = commitment,
                                    currencySymbol = uiState.currencySymbol,
                                    isDiscreetMode = uiState.isDiscreetMode,
                                    isDarkTheme = uiState.isDarkTheme,
                                    isHighlighted = isSelected,
                                    onCardClick = { selectedCommitmentId = commitment.id },
                                    onViewDetail = {
                                        selectedCommitmentId = commitment.id
                                        commitmentForDetail = commitment
                                    },
                                    onMarkAsPaid = {
                                        selectedCommitmentId = commitment.id
                                        commitmentForPayConfirmation = commitment
                                    },
                                    onPostpone = {
                                        selectedCommitmentId = commitment.id
                                        commitmentForPostponeConfirmation = commitment
                                    },
                                    onEdit = {
                                        selectedCommitmentId = commitment.id
                                        commitmentToEdit = commitment
                                        showAddEditDialog = true
                                    },
                                    onDelete = { onDeletePayment(commitment) },
                                    creditCardAccountName = commitment.creditCardAccountId?.let { accountNameById[it] },
                                    paymentSourceAccountName = commitment.paymentSourceAccountId?.let { accountNameById[it] },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Confirmación Segura de Pago
    commitmentForPayConfirmation?.let { target ->
        val latest = allPayments.firstOrNull { it.id == target.id } ?: target
        ConfirmPaymentDialog(
            commitment = latest,
            currencySymbol = uiState.currencySymbol,
            isDiscreetMode = uiState.isDiscreetMode,
            isDarkTheme = uiState.isDarkTheme,
            creditCardAccountName = latest.creditCardAccountId?.let { accountNameById[it] },
            paymentSourceAccountName = latest.paymentSourceAccountId?.let { accountNameById[it] },
            onDismiss = { commitmentForPayConfirmation = null },
            onConfirmPay = {
                if (PaymentCommitmentStackLogic.canPayCommitment(latest)) {
                    onMarkAsPaid(latest.id)
                }
                commitmentForPayConfirmation = null
            }
        )
    }

    // Modal Posponer Compromiso con Opciones y Confirmación
    commitmentForPostponeConfirmation?.let { target ->
        val latest = allPayments.firstOrNull { it.id == target.id } ?: target
        ConfirmPostponePaymentDialog(
            commitment = latest,
            isDarkTheme = uiState.isDarkTheme,
            nowMillis = now,
            onDismiss = { commitmentForPostponeConfirmation = null },
            onConfirmPostpone = { newDueMillis, optionLabel ->
                if (PaymentCommitmentStackLogic.canPayCommitment(latest)) {
                    onPostponePayment(latest, newDueMillis, optionLabel)
                }
                commitmentForPostponeConfirmation = null
            }
        )
    }

    // Modal Detalle de Compromiso
    commitmentForDetail?.let { target ->
        val latest = allPayments.firstOrNull { it.id == target.id } ?: target
        PaymentCommitmentDetailDialog(
            commitment = latest,
            currencySymbol = uiState.currencySymbol,
            isDiscreetMode = uiState.isDiscreetMode,
            isDarkTheme = uiState.isDarkTheme,
            creditCardAccountName = latest.creditCardAccountId?.let { accountNameById[it] },
            paymentSourceAccountName = latest.paymentSourceAccountId?.let { accountNameById[it] },
            onDismiss = { commitmentForDetail = null },
            onRequestPay = {
                commitmentForDetail = null
                commitmentForPayConfirmation = latest
            },
            onRequestPostpone = {
                commitmentForDetail = null
                commitmentForPostponeConfirmation = latest
            },
            onEdit = {
                commitmentForDetail = null
                commitmentToEdit = latest
                showAddEditDialog = true
            },
            onDelete = {
                commitmentForDetail = null
                onDeletePayment(latest)
            }
        )
    }

    // Modal Crear / Editar Pago
    if (showAddEditDialog) {
        AddEditPaymentDialog(
            commitment = commitmentToEdit,
            beneficiaries = uiState.paymentBeneficiaries,
            currencySymbol = uiState.currencySymbol,
            accounts = accounts,
            onDismiss = {
                showAddEditDialog = false
                commitmentToEdit = null
            },
            onSave = { title, ben, desc, amt, due, cat, freq, isVar, rem, notes, creditCardId, sourceAccountId ->
                if (commitmentToEdit == null) {
                    onAddPayment(
                        title, ben, desc, amt, due, cat, freq, isVar, rem, notes,
                        creditCardId, sourceAccountId
                    )
                } else {
                    val exactMinor = com.example.data.model.MoneyMinorUnits.fromDouble(amt)
                    val existing = commitmentToEdit!!
                    val nextKind = when {
                        creditCardId != null -> com.example.data.model.CommitmentKind.CREDIT_CARD_STATEMENT
                        existing.commitmentKind == com.example.data.model.CommitmentKind.CREDIT_CARD_STATEMENT -> com.example.data.model.CommitmentKind.BILL
                        else -> existing.commitmentKind
                    }
                    onUpdatePayment(
                        existing.withExactAmountMinor(exactMinor).copy(
                            title = title,
                            beneficiaryName = ben,
                            description = desc,
                            dueDateMillis = due,
                            category = cat,
                            frequency = freq,
                            isVariableAmount = isVar,
                            reminderDaysBefore = rem,
                            notes = notes,
                            creditCardAccountId = creditCardId,
                            paymentSourceAccountId = sourceAccountId,
                            commitmentKind = nextKind
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
    creditCardAccountName: String? = null,
    paymentSourceAccountName: String? = null,
    isHighlighted: Boolean = false,
    onCardClick: (() -> Unit)? = null,
    onViewDetail: (() -> Unit)? = null,
    onPostpone: (() -> Unit)? = null,
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
        isHighlighted -> EmeraldGreen
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
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier
            )
            .testTag("payment_list_card_${commitment.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) {
                if (isDarkTheme) Color(0xFF0B241D) else Color(0xFFECFDF5)
            } else {
                if (isDarkTheme) Color(0xFF0E2620) else Color(0xFFF7FCFA)
            }
        ),
        border = BorderStroke(if (isHighlighted) 2.dp else 1.2.dp, cardBorderColor),
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
                            color = primaryText
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

            if (!creditCardAccountName.isNullOrBlank() || !paymentSourceAccountName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!creditCardAccountName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB))
                        ) {
                            Text(
                                text = "Tarjeta: $creditCardAccountName",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFFBFDBFE) else Color(0xFF1E40AF),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!paymentSourceAccountName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF34D399) else EmeraldGreen)
                        ) {
                            Text(
                                text = "Desde: $paymentSourceAccountName",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
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
                        text = FormatUtils.formatCurrency(
                            PaymentCommitmentStackLogic.displayAmountForCommitment(commitment),
                            currencySymbol,
                            isDiscreetMode
                        ),
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
                    if (onViewDetail != null) {
                        IconButton(
                            onClick = onViewDetail,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Ver detalle",
                                tint = secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

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
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (onPostpone != null) {
                            OutlinedButton(
                                onClick = onPostpone,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("list_postpone_button_${commitment.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Update,
                                    contentDescription = "Posponer",
                                    tint = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
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
                            Text("Pagar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
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

@Composable
private fun ConfirmPaymentDialog(
    commitment: PaymentCommitmentEntity,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean,
    creditCardAccountName: String?,
    paymentSourceAccountName: String?,
    onDismiss: () -> Unit,
    onConfirmPay: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd 'de' MMM, yyyy", Locale("es", "GT")) }
    val dialogBg = if (isDarkTheme) Color(0xFF0E2620) else Color(0xFFF7FCFA)
    val primaryText = if (isDarkTheme) Color(0xFFECFDF5) else Color(0xFF022C22)
    val secondaryText = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF335C4F)
    val canPay = PaymentCommitmentStackLogic.canPayCommitment(commitment)
    val displayAmount = PaymentCommitmentStackLogic.displayAmountForCommitment(commitment)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        titleContentColor = primaryText,
        modifier = Modifier.testTag("confirm_payment_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar Pago", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = commitment.beneficiaryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = primaryText
                )
                Text(
                    text = commitment.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryText
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) Color(0xFF072019) else Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF10B981) else Color(0xFF86C5B0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Monto a confirmar: ${FormatUtils.formatCurrency(displayAmount, currencySymbol, isDiscreetMode)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)
                        )
                        Text(
                            text = "Vencimiento: ${dateFormatter.format(Date(commitment.dueDateMillis))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryText
                        )
                        if (!paymentSourceAccountName.isNullOrBlank()) {
                            Text(
                                text = "Cuenta de origen: $paymentSourceAccountName",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryText
                            )
                        }
                        if (!creditCardAccountName.isNullOrBlank()) {
                            Text(
                                text = "Tarjeta vinculada: $creditCardAccountName",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryText
                            )
                        }
                    }
                }
                if (commitment.frequency != PaymentFrequency.ONCE) {
                    Text(
                        text = "Compromiso recurrente: al confirmar se programará automáticamente el siguiente vencimiento.",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)
                    )
                }
                if (!canPay) {
                    Text(
                        text = "Este compromiso ya fue confirmado previamente.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmPay,
                enabled = canPay,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_payment_execute_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirmar Pago", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmPostponePaymentDialog(
    commitment: PaymentCommitmentEntity,
    isDarkTheme: Boolean,
    nowMillis: Long,
    onDismiss: () -> Unit,
    onConfirmPostpone: (newDueMillis: Long, optionLabel: String) -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd 'de' MMM, yyyy", Locale("es", "GT")) }
    val dialogBg = if (isDarkTheme) Color(0xFF0E2620) else Color(0xFFF7FCFA)
    val primaryText = if (isDarkTheme) Color(0xFFECFDF5) else Color(0xFF022C22)
    val secondaryText = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF335C4F)

    var selectedPreset by remember(commitment.id) { mutableStateOf<PostponePreset?>(PostponePreset.PLUS_3_DAYS) }
    var customDateMillis by remember(commitment.id) {
        mutableLongStateOf(
            PaymentCommitmentStackLogic.computePostponedDueDate(
                currentDueDateMillis = commitment.dueDateMillis,
                preset = PostponePreset.PLUS_3_DAYS,
                nowMillis = nowMillis
            )
        )
    }
    var selectedLabel by remember(commitment.id) { mutableStateOf(PostponePreset.PLUS_3_DAYS.label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        titleContentColor = primaryText,
        modifier = Modifier.testTag("confirm_postpone_payment_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Update, contentDescription = null, tint = Color(0xFFD97706))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Posponer Compromiso", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${commitment.beneficiaryName} · ${commitment.title}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )
                Text(
                    text = "Vencimiento actual: ${dateFormatter.format(Date(commitment.dueDateMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText
                )

                Text(
                    text = "Elige el nuevo plazo:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PostponePreset.entries.forEach { preset ->
                        val isSelected = selectedPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPreset = preset
                                customDateMillis = PaymentCommitmentStackLogic.computePostponedDueDate(
                                    currentDueDateMillis = commitment.dueDateMillis,
                                    preset = preset,
                                    nowMillis = nowMillis
                                )
                                selectedLabel = preset.label
                            },
                            label = { Text(preset.label, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (isDarkTheme) Color(0xFF07231C) else Color(0xFFEBF7F2),
                                selectedContainerColor = EmeraldGreen,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("postpone_preset_${preset.name}")
                        )
                    }

                    FilterChip(
                        selected = selectedPreset == null,
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = customDateMillis }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val chosen = Calendar.getInstance().apply {
                                        set(y, m, d, 12, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    selectedPreset = null
                                    customDateMillis = chosen.timeInMillis
                                    selectedLabel = "Fecha personalizada"
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Elegir fecha", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (isDarkTheme) Color(0xFF07231C) else Color(0xFFEBF7F2),
                            selectedContainerColor = EmeraldGreen,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("postpone_custom_date_button")
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) Color(0xFF063729) else Color(0xFFD1FAE5),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF34D399) else JadeGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Nueva fecha confirmada",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46)
                        )
                        Text(
                            text = dateFormatter.format(Date(customDateMillis)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) Color(0xFFECFDF5) else Color(0xFF022C22)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPostpone(customDateMillis, selectedLabel) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_postpone_execute_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirmar Posponer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun PaymentCommitmentDetailDialog(
    commitment: PaymentCommitmentEntity,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean,
    creditCardAccountName: String?,
    paymentSourceAccountName: String?,
    onDismiss: () -> Unit,
    onRequestPay: () -> Unit,
    onRequestPostpone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd 'de' MMM, yyyy", Locale("es", "GT")) }
    val dialogBg = if (isDarkTheme) Color(0xFF0E2620) else Color(0xFFF7FCFA)
    val primaryText = if (isDarkTheme) Color(0xFFECFDF5) else Color(0xFF022C22)
    val secondaryText = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF335C4F)
    val canPay = PaymentCommitmentStackLogic.canPayCommitment(commitment)
    val displayAmount = PaymentCommitmentStackLogic.displayAmountForCommitment(commitment)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        titleContentColor = primaryText,
        modifier = Modifier.testTag("payment_detail_dialog"),
        title = {
            Column {
                Text(commitment.beneficiaryName, fontWeight = FontWeight.Black)
                Text(commitment.category, style = MaterialTheme.typography.labelSmall, color = secondaryText)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(commitment.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = primaryText)
                if (commitment.description.isNotBlank()) {
                    Text(commitment.description, style = MaterialTheme.typography.bodySmall, color = secondaryText)
                }
                Text(
                    text = "Monto: ${FormatUtils.formatCurrency(displayAmount, currencySymbol, isDiscreetMode)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)
                )
                Text(
                    text = "Vencimiento: ${dateFormatter.format(Date(commitment.dueDateMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryText
                )
                Text(
                    text = "Estado: ${if (commitment.isPaid || commitment.status == PaymentStatus.PAID) "Pagado" else "Pendiente"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText
                )
                if (!creditCardAccountName.isNullOrBlank()) {
                    Text("Tarjeta: $creditCardAccountName", style = MaterialTheme.typography.bodySmall, color = primaryText)
                }
                if (!paymentSourceAccountName.isNullOrBlank()) {
                    Text("Cuenta origen: $paymentSourceAccountName", style = MaterialTheme.typography.bodySmall, color = primaryText)
                }
                if (commitment.notes.isNotBlank()) {
                    Text("Notas: ${commitment.notes}", style = MaterialTheme.typography.bodySmall, color = secondaryText)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("detail_edit_payment_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar")
                    }
                    if (canPay) {
                        OutlinedButton(
                            onClick = onRequestPostpone,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("detail_postpone_payment_button")
                        ) {
                            Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Posponer")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("detail_delete_payment_button")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar", color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C))
                        }
                    }
                }
                if (canPay) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("detail_delete_payment_button")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar compromiso", color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C))
                    }
                }
            }
        },
        confirmButton = {
            if (canPay) {
                Button(
                    onClick = onRequestPay,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("detail_pay_payment_button")
                ) {
                    Text("Pagar Ahora", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (canPay) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Cerrar")
                }
            }
        }
    )
}
