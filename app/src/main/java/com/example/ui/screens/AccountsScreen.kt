package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AccountType
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.PaymentCommitmentEntity
import com.example.ui.components.AccountCardStack
import com.example.ui.components.AccountDetailPane
import com.example.ui.components.AccountTransferDialog
import com.example.ui.components.AssignUnassignedTransactionsDialog
import com.example.ui.components.BalanceAdjustmentDialog
import com.example.ui.components.CreateEditAccountDialog
import com.example.ui.components.CreateInstallmentPlanDialog
import com.example.ui.components.ImportCsvToAccountDialog
import com.example.ui.components.InstallmentPlanDetailDialog
import com.example.ui.components.PayCreditCardDialog
import com.example.ui.components.RecordCardChargeDialog
import com.example.ui.components.SwipeableAccountCard
import com.example.ui.theme.EmeraldGreen
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.AccountsUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    accountsState: AccountsUiState,
    pendingCommitments: List<PaymentCommitmentEntity>,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onSelectAccount: (Long?) -> Unit,
    onCreateAccount: (
        name: String, accountType: String, institutionName: String, lastFourDigits: String,
        openingBalanceMinor: Long, creditLimitMinor: Long, cutoffDay: Int?, paymentDueDay: Int?,
        installmentReservePolicy: String, autoCreateReminder: Boolean, includeInTotalLiquidity: Boolean,
        isDefault: Boolean, notes: String
    ) -> Unit,
    onUpdateAccount: (FinancialAccountEntity) -> Unit,
    onArchiveAccount: (Long) -> Unit,
    onUnarchiveAccount: (Long) -> Unit,
    onAdjustBalance: (accountId: Long, targetMinor: Long, notes: String) -> Unit,
    onTransfer: (fromId: Long, toId: Long, amountMinor: Long, title: String, notes: String) -> Unit,
    onPayCard: (sourceId: Long, cardId: Long, amountMinor: Long, linkedCommitmentId: Long?, notes: String) -> Unit,
    onSyncCardReminder: (cardId: Long) -> Unit,
    onRecordCardPurchase: (cardId: Long, title: String, amountMinor: Long, category: String, familyMember: String, notes: String, enforceLimit: Boolean) -> Unit,
    onRecordCardInterest: (cardId: Long, title: String, amountMinor: Long, category: String, notes: String) -> Unit,
    onCreateInstallmentPlan: (cardId: Long, title: String, merchant: String, category: String, principalMinor: Long, count: Int, rateBps: Int, policyOverride: String?, postFirst: Boolean, member: String, notes: String) -> Unit,
    onPostInstallmentCharge: (planId: Long, installmentNumber: Int) -> Unit,
    onPostDueInstallments: (accountId: Long?) -> Unit,
    onCloseInstallmentPlan: (planId: Long, paidOffEarly: Boolean) -> Unit,
    onAssignTransaction: (txId: Long, accountId: Long) -> Unit,
    onImportCsvToAccount: (csvText: String, targetAccountId: Long) -> Unit,
    onExportCsv: suspend () -> String,
    onAddStandardMovementForAccount: (FinancialAccountEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var filterKey by remember { mutableStateOf("ALL") }
    var showArchived by remember { mutableStateOf(false) }
    var showListView by remember { mutableStateOf(false) }

    var showCreateEditDialog by remember { mutableStateOf(false) }
    var accountBeingEdited by remember { mutableStateOf<FinancialAccountEntity?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var preselectedTransferFromId by remember { mutableStateOf<Long?>(null) }
    var showPayCardDialog by remember { mutableStateOf(false) }
    var preselectedPayCardId by remember { mutableStateOf<Long?>(null) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var showCardChargeDialog by remember { mutableStateOf(false) }
    var showNewPlanDialog by remember { mutableStateOf(false) }
    var selectedPlanForDetail by remember { mutableStateOf<InstallmentPlanEntity?>(null) }
    var showUnassignedDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val pageBg = if (isDarkTheme) Color(0xFF0B1117) else Color(0xFFF3F6F4)
    val backgroundScrimBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            0.0f to Color(0x780A1118),
            0.5f to Color(0x720A1118),
            1.0f to Color(0x840A1118)
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color(0x28F6F8F7),
            0.5f to Color(0x20F6F8F7),
            1.0f to Color(0x2CF6F8F7)
        )
    }
    val cardBg = if (isDarkTheme) Color(0xFF1E293B).copy(alpha = 0.87f) else Color.White.copy(alpha = 0.89f)
    val cardBorder = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
    val primaryText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentEmerald = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)
    val heroTextShadow = Shadow(color = Color(0xD9000000), offset = Offset(0f, 2f), blurRadius = 6f)

    val sourceAccounts = if (showArchived) accountsState.allAccounts else accountsState.activeAccounts
    val filteredAccounts = remember(sourceAccounts, filterKey) {
        when (filterKey) {
            AccountType.CASH -> sourceAccounts.filter { it.accountType == AccountType.CASH }
            "BANKS" -> sourceAccounts.filter { it.accountType == AccountType.BANK_CHECKING || it.accountType == AccountType.BANK_SAVINGS }
            AccountType.COOPERATIVE -> sourceAccounts.filter { it.accountType == AccountType.COOPERATIVE }
            AccountType.CREDIT_CARD -> sourceAccounts.filter { it.accountType == AccountType.CREDIT_CARD }
            else -> sourceAccounts
        }
    }

    val selectedAccount = accountsState.selectedAccount

    // Sincronizar tarjeta frontal del stack y AccountDetailPane con las cuentas filtradas reales
    LaunchedEffect(filteredAccounts, selectedAccount?.id) {
        if (filteredAccounts.isNotEmpty() && (selectedAccount == null || filteredAccounts.none { it.id == selectedAccount.id })) {
            onSelectAccount(filteredAccounts.first().id)
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val detailItemIndex = 3 + (if (accountsState.unassignedTransactionsCount > 0) 1 else 0)

    val openDetailForAccount: (FinancialAccountEntity, Boolean) -> Unit = { acc, isWideLayout ->
        onSelectAccount(acc.id)
        if (!isWideLayout) {
            coroutineScope.launch {
                listState.animateScrollToItem(detailItemIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        // Fondo detrás de las tarjetas: Teatro Municipal de Quetzaltenango
        Image(
            painter = painterResource(id = R.drawable.img_xela_teatro_municipal_1795210177809),
            contentDescription = "Teatro Municipal de Quetzaltenango",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isDarkTheme) 0.62f else 0.90f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundScrimBrush)
        )

        // Encabezado con fotografía de Parque Central de Quetzaltenango
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(252.dp)
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
                painter = painterResource(id = R.drawable.img_xela_parque_central_1795210178864),
                contentDescription = "Parque Central de Quetzaltenango",
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isDarkTheme) 0.86f else 0.98f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xC2022C22),
                            0.42f to Color(0x72022C22),
                            0.80f to Color(0x20000000),
                            1.0f to Color.Transparent
                        )
                    )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp).testTag("accounts_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Mis Cuentas",
                            style = MaterialTheme.typography.titleLarge.copy(shadow = heroTextShadow),
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 2
                        )
                        Text(
                            text = "Quetzaltenango · ${accountsState.activeAccounts.size} activa(s) · ${accountsState.creditCardAccounts.size} tarjeta(s)",
                            style = MaterialTheme.typography.labelSmall.copy(shadow = heroTextShadow),
                            color = Color(0xFFE2E8F0),
                            maxLines = 2
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { accountBeingEdited = null; showCreateEditDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp).padding(end = 8.dp).testTag("add_account_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cuenta", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            if (accountsState.isOperationInProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EmeraldGreen)
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth >= 840.dp

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Resumen patrimonial integrado sobre la transición del Parque Central de Xela
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.2.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SummaryKpiChip(
                                        label = "Liquidez Total",
                                        amountMinor = accountsState.totalLiquidAssetsMinor,
                                        currencySymbol = currencySymbol,
                                        isDiscreetMode = isDiscreetMode,
                                        bgColor = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                                        textColor = accentEmerald,
                                        modifier = Modifier.weight(1f)
                                    )
                                    SummaryKpiChip(
                                        label = "Deuda Tarjetas",
                                        amountMinor = accountsState.totalCreditCardDebtMinor,
                                        currencySymbol = currencySymbol,
                                        isDiscreetMode = isDiscreetMode,
                                        bgColor = if (isDarkTheme) Color(0xFF4C1D24) else Color(0xFFFEE2E2),
                                        textColor = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                                        modifier = Modifier.weight(1f)
                                    )
                                    SummaryKpiChip(
                                        label = "Patrimonio Neto",
                                        amountMinor = accountsState.netWorthMinor,
                                        currencySymbol = currencySymbol,
                                        isDiscreetMode = isDiscreetMode,
                                        bgColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                        textColor = primaryText,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Barra de acciones rápidas globales
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        enabled = accountsState.liquidAccounts.size >= 2,
                                        onClick = {
                                            preselectedTransferFromId = selectedAccount?.takeIf { AccountType.isLiquidAsset(it.accountType) }?.id
                                            showTransferDialog = true
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("open_transfer_button")
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Transferir")
                                    }
                                    OutlinedButton(
                                        enabled = accountsState.liquidAccounts.isNotEmpty() && accountsState.creditCardAccounts.isNotEmpty(),
                                        onClick = {
                                            preselectedPayCardId = selectedAccount?.takeIf { AccountType.isCreditCard(it.accountType) }?.id
                                            showPayCardDialog = true
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("open_pay_card_button")
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pagar Tarjeta")
                                    }
                                    OutlinedButton(
                                        enabled = accountsState.creditCardAccounts.isNotEmpty(),
                                        onClick = { showNewPlanDialog = true },
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("open_installment_plan_button")
                                    ) {
                                        Icon(Icons.Default.CreditScore, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Visacuotas")
                                    }
                                    OutlinedButton(
                                        enabled = accountsState.activeAccounts.isNotEmpty(),
                                        onClick = { showImportDialog = true },
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("open_import_account_csv_button")
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Importar CSV/Excel")
                                    }
                                }
                            }
                        }
                    }

                    // Alerta de movimientos históricos sin cuenta
                    if (accountsState.unassignedTransactionsCount > 0) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDarkTheme) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${accountsState.unassignedTransactionsCount} movimiento(s) histórico(s) sin cuenta asignada",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFFDBEAFE) else Color(0xFF1E3A8A),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { showUnassignedDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("open_unassigned_transactions_button")
                                    ) {
                                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Vincular")
                                    }
                                }
                            }
                        }
                    }

                    // Filtros por tipo de cuenta + alternador Pila / Lista
                    item {
                        val chipSurface = if (isDarkTheme) Color(0xFF1E293B) else Color.White
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "ALL" to "Todas",
                                AccountType.CASH to "Efectivo",
                                "BANKS" to "Bancos",
                                AccountType.COOPERATIVE to "Cooperativas",
                                AccountType.CREDIT_CARD to "Tarjetas"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = filterKey == key,
                                    onClick = { filterKey = key },
                                    label = { Text(label, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = chipSurface,
                                        selectedContainerColor = EmeraldGreen,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                            FilterChip(
                                selected = showArchived,
                                onClick = { showArchived = !showArchived },
                                label = { Text("Archivadas (${accountsState.archivedAccounts.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = chipSurface,
                                    selectedContainerColor = EmeraldGreen,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                            if (filteredAccounts.size > 1) {
                                FilterChip(
                                    selected = showListView,
                                    onClick = { showListView = !showListView },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (showListView) Icons.Default.Layers else Icons.AutoMirrored.Filled.ViewList,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    label = { Text(if (showListView) "Ver Pila" else "Ver Lista") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = chipSurface,
                                        selectedContainerColor = EmeraldGreen,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("toggle_stack_list_chip")
                                )
                            }
                        }
                    }

                    // Contenido principal: Estado vacío de Xela vs Pila / Lista + Detalle adaptativo
                    if (filteredAccounts.isEmpty()) {
                        item {
                            XelaAccountsEmptyState(
                                hasAnyAccountAtAll = accountsState.allAccounts.isNotEmpty(),
                                isDarkTheme = isDarkTheme,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                accentEmerald = accentEmerald,
                                onCreateFirst = { accountBeingEdited = null; showCreateEditDialog = true },
                                onResetFilters = { filterKey = "ALL"; showArchived = false }
                            )
                        }
                    } else if (isWide) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(0.46f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    AccountsShowcaseSection(
                                        accounts = filteredAccounts,
                                        selectedAccountId = selectedAccount?.id,
                                        snapshotsById = accountsState.snapshotsByAccountId,
                                        statementsById = accountsState.cardStatementsByAccountId,
                                        canTransfer = accountsState.liquidAccounts.size >= 2,
                                        canPayCard = accountsState.liquidAccounts.isNotEmpty() && accountsState.creditCardAccounts.isNotEmpty(),
                                        showListView = showListView,
                                        currencySymbol = currencySymbol,
                                        isDiscreetMode = isDiscreetMode,
                                        isDarkTheme = isDarkTheme,
                                        onSelectAccount = { onSelectAccount(it) },
                                        onViewDetail = { openDetailForAccount(it, true) },
                                        onEditAccount = { acc ->
                                            onSelectAccount(acc.id)
                                            accountBeingEdited = acc
                                            showCreateEditDialog = true
                                        },
                                        onRequestTransfer = { acc ->
                                            onSelectAccount(acc.id)
                                            preselectedTransferFromId = acc.id
                                            showTransferDialog = true
                                        },
                                        onRequestPayCard = { acc ->
                                            onSelectAccount(acc.id)
                                            preselectedPayCardId = acc.id
                                            showPayCardDialog = true
                                        }
                                    )
                                }
                                Column(modifier = Modifier.weight(0.54f)) {
                                    if (selectedAccount != null) {
                                        AccountDetailPane(
                                            account = selectedAccount,
                                            snapshot = accountsState.selectedAccountSnapshot,
                                            statement = accountsState.selectedAccountStatement,
                                            accountTransactions = accountsState.selectedAccountTransactions,
                                            accountPlans = accountsState.installmentPlans.filter { it.accountId == selectedAccount.id },
                                            scheduleItemsByPlanId = accountsState.scheduleItemsByPlanId,
                                            currencySymbol = currencySymbol,
                                            isDiscreetMode = isDiscreetMode,
                                            isDarkTheme = isDarkTheme,
                                            onEditAccount = { accountBeingEdited = selectedAccount; showCreateEditDialog = true },
                                            onArchiveOrRestoreAccount = {
                                                if (selectedAccount.isArchived) onUnarchiveAccount(selectedAccount.id) else onArchiveAccount(selectedAccount.id)
                                            },
                                            onAdjustBalance = { showAdjustDialog = true },
                                            onPayCard = {
                                                preselectedPayCardId = selectedAccount.id
                                                showPayCardDialog = true
                                            },
                                            onSyncCardReminder = { onSyncCardReminder(selectedAccount.id) },
                                            onRecordCardCharge = { showCardChargeDialog = true },
                                            onNewInstallmentPlan = { showNewPlanDialog = true },
                                            onPostDueInstallments = { onPostDueInstallments(selectedAccount.id) },
                                            onOpenPlanDetail = { selectedPlanForDetail = it },
                                            onAddStandardMovement = { onAddStandardMovementForAccount(selectedAccount) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            AccountsShowcaseSection(
                                accounts = filteredAccounts,
                                selectedAccountId = selectedAccount?.id,
                                snapshotsById = accountsState.snapshotsByAccountId,
                                statementsById = accountsState.cardStatementsByAccountId,
                                canTransfer = accountsState.liquidAccounts.size >= 2,
                                canPayCard = accountsState.liquidAccounts.isNotEmpty() && accountsState.creditCardAccounts.isNotEmpty(),
                                showListView = showListView,
                                currencySymbol = currencySymbol,
                                isDiscreetMode = isDiscreetMode,
                                isDarkTheme = isDarkTheme,
                                onSelectAccount = { onSelectAccount(it) },
                                onViewDetail = { openDetailForAccount(it, false) },
                                onEditAccount = { acc ->
                                    onSelectAccount(acc.id)
                                    accountBeingEdited = acc
                                    showCreateEditDialog = true
                                },
                                onRequestTransfer = { acc ->
                                    onSelectAccount(acc.id)
                                    preselectedTransferFromId = acc.id
                                    showTransferDialog = true
                                },
                                onRequestPayCard = { acc ->
                                    onSelectAccount(acc.id)
                                    preselectedPayCardId = acc.id
                                    showPayCardDialog = true
                                }
                            )
                        }
                        if (selectedAccount != null) {
                            item {
                                AccountDetailPane(
                                    account = selectedAccount,
                                    snapshot = accountsState.selectedAccountSnapshot,
                                    statement = accountsState.selectedAccountStatement,
                                    accountTransactions = accountsState.selectedAccountTransactions,
                                    accountPlans = accountsState.installmentPlans.filter { it.accountId == selectedAccount.id },
                                    scheduleItemsByPlanId = accountsState.scheduleItemsByPlanId,
                                    currencySymbol = currencySymbol,
                                    isDiscreetMode = isDiscreetMode,
                                    isDarkTheme = isDarkTheme,
                                    onEditAccount = { accountBeingEdited = selectedAccount; showCreateEditDialog = true },
                                    onArchiveOrRestoreAccount = {
                                        if (selectedAccount.isArchived) onUnarchiveAccount(selectedAccount.id) else onArchiveAccount(selectedAccount.id)
                                    },
                                    onAdjustBalance = { showAdjustDialog = true },
                                    onPayCard = {
                                        preselectedPayCardId = selectedAccount.id
                                        showPayCardDialog = true
                                    },
                                    onSyncCardReminder = { onSyncCardReminder(selectedAccount.id) },
                                    onRecordCardCharge = { showCardChargeDialog = true },
                                    onNewInstallmentPlan = { showNewPlanDialog = true },
                                    onPostDueInstallments = { onPostDueInstallments(selectedAccount.id) },
                                    onOpenPlanDetail = { selectedPlanForDetail = it },
                                    onAddStandardMovement = { onAddStandardMovementForAccount(selectedAccount) }
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showCreateEditDialog) {
        CreateEditAccountDialog(
            initialAccount = accountBeingEdited,
            currencySymbol = currencySymbol,
            isDarkTheme = isDarkTheme,
            onDismiss = { showCreateEditDialog = false; accountBeingEdited = null },
            onCreateAccount = onCreateAccount,
            onUpdateAccount = onUpdateAccount
        )
    }

    if (showTransferDialog) {
        AccountTransferDialog(
            liquidAccounts = accountsState.liquidAccounts,
            snapshotsById = accountsState.snapshotsByAccountId,
            initialFromId = preselectedTransferFromId ?: selectedAccount?.takeIf { AccountType.isLiquidAsset(it.accountType) }?.id,
            currencySymbol = currencySymbol,
            isDarkTheme = isDarkTheme,
            onDismiss = {
                showTransferDialog = false
                preselectedTransferFromId = null
            },
            onConfirmTransfer = onTransfer
        )
    }

    if (showPayCardDialog) {
        PayCreditCardDialog(
            liquidAccounts = accountsState.liquidAccounts,
            creditCards = accountsState.creditCardAccounts,
            snapshotsById = accountsState.snapshotsByAccountId,
            statementsById = accountsState.cardStatementsByAccountId,
            pendingCommitments = pendingCommitments,
            initialCardId = preselectedPayCardId ?: selectedAccount?.takeIf { AccountType.isCreditCard(it.accountType) }?.id,
            currencySymbol = currencySymbol,
            isDarkTheme = isDarkTheme,
            onDismiss = {
                showPayCardDialog = false
                preselectedPayCardId = null
            },
            onConfirmPay = onPayCard
        )
    }

    if (showAdjustDialog && selectedAccount != null) {
        val currentVal = if (AccountType.isCreditCard(selectedAccount.accountType)) {
            accountsState.selectedAccountSnapshot?.revolvingDebtMinor ?: selectedAccount.openingBalanceMinor
        } else {
            accountsState.selectedAccountSnapshot?.currentBalanceMinor ?: selectedAccount.openingBalanceMinor
        }
        BalanceAdjustmentDialog(
            account = selectedAccount,
            currentBalanceOrDebtMinor = currentVal,
            currencySymbol = currencySymbol,
            isDarkTheme = isDarkTheme,
            onDismiss = { showAdjustDialog = false },
            onConfirmAdjustment = { targetMinor, notes -> onAdjustBalance(selectedAccount.id, targetMinor, notes) }
        )
    }

    if (showCardChargeDialog && selectedAccount != null && AccountType.isCreditCard(selectedAccount.accountType)) {
        RecordCardChargeDialog(
            cardAccount = selectedAccount,
            currencySymbol = currencySymbol,
            isDarkTheme = isDarkTheme,
            onDismiss = { showCardChargeDialog = false },
            onRecordPurchase = { t, amt, cat, member, notes, enforce -> onRecordCardPurchase(selectedAccount.id, t, amt, cat, member, notes, enforce) },
            onRecordInterestOrFee = { t, amt, cat, notes -> onRecordCardInterest(selectedAccount.id, t, amt, cat, notes) }
        )
    }

    if (showNewPlanDialog) {
        CreateInstallmentPlanDialog(
            creditCards = accountsState.creditCardAccounts,
            initialCardId = selectedAccount?.takeIf { AccountType.isCreditCard(it.accountType) }?.id,
            currencySymbol = currencySymbol,
            isDarkTheme = isDarkTheme,
            onDismiss = { showNewPlanDialog = false },
            onCreatePlan = onCreateInstallmentPlan
        )
    }

    selectedPlanForDetail?.let { plan ->
        val freshPlan = accountsState.installmentPlans.firstOrNull { it.id == plan.id } ?: plan
        InstallmentPlanDetailDialog(
            plan = freshPlan,
            scheduleItems = accountsState.scheduleItemsByPlanId[freshPlan.id].orEmpty(),
            currencySymbol = currencySymbol,
            isDarkTheme = isDarkTheme,
            onDismiss = { selectedPlanForDetail = null },
            onPostInstallment = onPostInstallmentCharge,
            onClosePlan = onCloseInstallmentPlan
        )
    }

    if (showUnassignedDialog) {
        AssignUnassignedTransactionsDialog(
            unassignedTransactions = accountsState.unassignedTransactions,
            activeAccounts = accountsState.activeAccounts,
            currencySymbol = currencySymbol,
            isDarkTheme = isDarkTheme,
            onDismiss = { showUnassignedDialog = false },
            onAssignTransaction = onAssignTransaction
        )
    }

    if (showImportDialog) {
        ImportCsvToAccountDialog(
            activeAccounts = accountsState.activeAccounts,
            initialTargetAccountId = selectedAccount?.takeIf { !it.isArchived }?.id,
            isDarkTheme = isDarkTheme,
            onDismiss = { showImportDialog = false },
            onImportToAccount = onImportCsvToAccount,
            onExportCsv = onExportCsv
        )
    }
}

@Composable
private fun AccountsShowcaseSection(
    accounts: List<FinancialAccountEntity>,
    selectedAccountId: Long?,
    snapshotsById: Map<Long, com.example.data.service.AccountBalanceSnapshot>,
    statementsById: Map<Long, com.example.data.service.CreditCardStatementSummary>,
    canTransfer: Boolean,
    canPayCard: Boolean,
    showListView: Boolean,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean,
    onSelectAccount: (Long) -> Unit,
    onViewDetail: (FinancialAccountEntity) -> Unit,
    onEditAccount: (FinancialAccountEntity) -> Unit,
    onRequestTransfer: (FinancialAccountEntity) -> Unit,
    onRequestPayCard: (FinancialAccountEntity) -> Unit
) {
    if (!showListView) {
        AccountCardStack(
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            snapshotsById = snapshotsById,
            statementsById = statementsById,
            canTransfer = canTransfer,
            canPayCard = canPayCard,
            currencySymbol = currencySymbol,
            isDiscreetMode = isDiscreetMode,
            isDarkTheme = isDarkTheme,
            onSelectAccount = onSelectAccount,
            onViewDetail = onViewDetail,
            onEditAccount = onEditAccount,
            onRequestTransfer = onRequestTransfer,
            onRequestPayCard = onRequestPayCard
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            accounts.forEach { acc ->
                SwipeableAccountCard(
                    account = acc,
                    snapshot = snapshotsById[acc.id],
                    statement = statementsById[acc.id],
                    isSelected = acc.id == selectedAccountId,
                    canTransfer = canTransfer && !acc.isArchived && AccountType.isLiquidAsset(acc.accountType),
                    canPayCard = canPayCard && !acc.isArchived && AccountType.isCreditCard(acc.accountType),
                    currencySymbol = currencySymbol,
                    isDiscreetMode = isDiscreetMode,
                    onCardTap = { onSelectAccount(acc.id) },
                    onViewDetail = { onViewDetail(acc) },
                    onEditAccount = { onEditAccount(acc) },
                    onRequestTransfer = { onRequestTransfer(acc) },
                    onRequestPayCard = { onRequestPayCard(acc) },
                    cardTestTag = "account_card_${acc.id}"
                )
            }
        }
    }
}

@Composable
private fun XelaAccountsEmptyState(
    hasAnyAccountAtAll: Boolean,
    isDarkTheme: Boolean,
    cardBg: Color,
    cardBorder: Color,
    primaryText: Color,
    secondaryText: Color,
    accentEmerald: Color,
    onCreateFirst: () -> Unit,
    onResetFilters: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.2.dp, cardBorder),
        modifier = Modifier.fillMaxWidth().testTag("accounts_empty_state_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_xela_parque_central_1795210178864),
                    contentDescription = "Quetzaltenango, Parque Central",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color(0x28022C22),
                                1.0f to Color(0xBA022C22)
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldGreen
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(6.dp).size(18.dp)
                        )
                    }
                    Text(
                        text = if (hasAnyAccountAtAll) "Sin resultados en este filtro" else "Tu portafolio en Xela está listo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            Text(
                text = if (hasAnyAccountAtAll) {
                    "No hay cuentas registradas bajo la categoría seleccionada. Cambia el filtro o agrega una nueva cuenta."
                } else {
                    "Aún no has registrado cuentas reales. Agrega tu efectivo, cuentas bancarias, cooperativas o tarjetas de crédito para ver tu pila interactiva."
                },
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasAnyAccountAtAll) {
                    OutlinedButton(
                        onClick = onResetFilters,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Ver Todas", fontWeight = FontWeight.Bold, color = accentEmerald)
                    }
                }
                Button(
                    onClick = onCreateFirst,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("empty_state_create_account_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Crear Primera Cuenta", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SummaryKpiChip(
    label: String,
    amountMinor: Long,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = textColor,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(amountMinor), currencySymbol, isDiscreetMode),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = textColor,
                maxLines = 2
            )
        }
    }
}
