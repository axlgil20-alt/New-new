package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CreditScore
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountType
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.service.AccountBalanceSnapshot
import com.example.ui.components.AccountDetailPane
import com.example.ui.components.AccountTransferDialog
import com.example.ui.components.AccountUiMeta
import com.example.ui.components.AssignUnassignedTransactionsDialog
import com.example.ui.components.BalanceAdjustmentDialog
import com.example.ui.components.CreateEditAccountDialog
import com.example.ui.components.CreateInstallmentPlanDialog
import com.example.ui.components.ImportCsvToAccountDialog
import com.example.ui.components.InstallmentPlanDetailDialog
import com.example.ui.components.PayCreditCardDialog
import com.example.ui.components.RecordCardChargeDialog
import com.example.ui.theme.EmeraldGreen
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.AccountsUiState

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

    var showCreateEditDialog by remember { mutableStateOf(false) }
    var accountBeingEdited by remember { mutableStateOf<FinancialAccountEntity?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showPayCardDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var showCardChargeDialog by remember { mutableStateOf(false) }
    var showNewPlanDialog by remember { mutableStateOf(false) }
    var selectedPlanForDetail by remember { mutableStateOf<InstallmentPlanEntity?>(null) }
    var showUnassignedDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val pageBg = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF4F8F6)
    val cardBg = if (isDarkTheme) Color(0xFF1E293B) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
    val primaryText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentEmerald = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)

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

    Column(modifier = modifier.fillMaxSize().background(pageBg)) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("accounts_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            },
            title = {
                Column {
                    Text("Mis Cuentas", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "${accountsState.activeAccounts.size} activa(s) · ${accountsState.creditCardAccounts.size} tarjeta(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText
                    )
                }
            },
            actions = {
                Button(
                    onClick = { accountBeingEdited = null; showCreateEditDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 48.dp).padding(end = 8.dp).testTag("add_account_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cuenta", fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDarkTheme) Color(0xFF0B1F19) else Color(0xFFE9F5F0))
        )

        if (accountsState.isOperationInProgress) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EmeraldGreen)
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 840.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Resumen patrimonial sólido
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.2.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SummaryKpiChip("Liquidez Total", accountsState.totalLiquidAssetsMinor, currencySymbol, isDiscreetMode,
                                    if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5), accentEmerald, Modifier.weight(1f))
                                SummaryKpiChip("Deuda Tarjetas", accountsState.totalCreditCardDebtMinor, currencySymbol, isDiscreetMode,
                                    if (isDarkTheme) Color(0xFF4C1D24) else Color(0xFFFEE2E2), if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFF991B1B), Modifier.weight(1f))
                                SummaryKpiChip("Patrimonio Neto", accountsState.netWorthMinor, currencySymbol, isDiscreetMode,
                                    if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9), primaryText, Modifier.weight(1f))
                            }

                            // Barra de acciones rápidas
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    enabled = accountsState.liquidAccounts.size >= 2,
                                    onClick = { showTransferDialog = true },
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("open_transfer_button")
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Transferir")
                                }
                                OutlinedButton(
                                    enabled = accountsState.liquidAccounts.isNotEmpty() && accountsState.creditCardAccounts.isNotEmpty(),
                                    onClick = { showPayCardDialog = true },
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

                // Alerta de movimientos sin cuenta
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

                // Filtros por tipo de cuenta
                item {
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
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                // Maestro / Detalle adaptativo
                if (isWide) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(modifier = Modifier.weight(0.45f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AccountsListBlock(
                                    accounts = filteredAccounts,
                                    selectedId = selectedAccount?.id,
                                    snapshotsById = accountsState.snapshotsByAccountId,
                                    currencySymbol = currencySymbol,
                                    isDiscreetMode = isDiscreetMode,
                                    isDarkTheme = isDarkTheme,
                                    onSelect = { onSelectAccount(it) },
                                    onCreateFirst = { accountBeingEdited = null; showCreateEditDialog = true }
                                )
                            }
                            Column(modifier = Modifier.weight(0.55f)) {
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
                                        onPayCard = { showPayCardDialog = true },
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
                        AccountsListBlock(
                            accounts = filteredAccounts,
                            selectedId = selectedAccount?.id,
                            snapshotsById = accountsState.snapshotsByAccountId,
                            currencySymbol = currencySymbol,
                            isDiscreetMode = isDiscreetMode,
                            isDarkTheme = isDarkTheme,
                            onSelect = { onSelectAccount(it) },
                            onCreateFirst = { accountBeingEdited = null; showCreateEditDialog = true }
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
                                onPayCard = { showPayCardDialog = true },
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
            initialFromId = selectedAccount?.takeIf { AccountType.isLiquidAsset(it.accountType) }?.id,
            currencySymbol = currencySymbol,
            isDarkTheme = isDarkTheme,
            onDismiss = { showTransferDialog = false },
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
            initialCardId = selectedAccount?.takeIf { AccountType.isCreditCard(it.accountType) }?.id,
            currencySymbol = currencySymbol,
            isDarkTheme = isDarkTheme,
            onDismiss = { showPayCardDialog = false },
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
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(amountMinor), currencySymbol, isDiscreetMode),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AccountsListBlock(
    accounts: List<FinancialAccountEntity>,
    selectedId: Long?,
    snapshotsById: Map<Long, AccountBalanceSnapshot>,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean,
    onSelect: (Long) -> Unit,
    onCreateFirst: () -> Unit
) {
    val cardBg = if (isDarkTheme) Color(0xFF1E293B) else Color.White
    val borderNormal = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
    val primaryText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentEmerald = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)

    if (accounts.isEmpty()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderNormal),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = accentEmerald, modifier = Modifier.size(36.dp))
                Text("Sin cuentas en esta vista", fontWeight = FontWeight.Bold, color = primaryText)
                Text("Agrega tu efectivo, cuentas de banco, cooperativa o tarjetas de crédito.", style = MaterialTheme.typography.bodySmall, color = secondaryText)
                Button(
                    onClick = onCreateFirst,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Crear Primera Cuenta", fontWeight = FontWeight.Bold) }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.forEach { acc ->
                val isSelected = acc.id == selectedId
                val snap = snapshotsById[acc.id]
                val isCard = AccountType.isCreditCard(acc.accountType)
                val displayMinor = if (isCard) (snap?.totalDebtMinor ?: acc.openingBalanceMinor) else (snap?.currentBalanceMinor ?: acc.openingBalanceMinor)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) (if (isDarkTheme) Color(0xFF132A22) else Color(0xFFECFDF5)) else cardBg
                    ),
                    border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, if (isSelected) EmeraldGreen else borderNormal),
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(acc.id) }.testTag("account_card_${acc.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(AccountUiMeta.iconForType(acc.accountType), contentDescription = null, tint = accentEmerald)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(acc.name, fontWeight = FontWeight.Bold, color = primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = listOfNotNull(
                                        AccountUiMeta.displayTypeLabel(acc.accountType),
                                        acc.institutionName.takeIf { it.isNotBlank() }
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(displayMinor), currencySymbol, isDiscreetMode),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCard && displayMinor > 0L) (if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)) else accentEmerald
                            )
                            Text(
                                text = if (isCard) "Deuda" else "Disponible",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryText
                            )
                        }
                    }
                }
            }
        }
    }
}
