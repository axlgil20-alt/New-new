package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CreditReservePolicy
import com.example.data.model.DefaultData
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.InstallmentChargeStatus
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.InstallmentPlanStatus
import com.example.data.model.InstallmentScheduleItemEntity
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.EmeraldGreen
import com.example.ui.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateInstallmentPlanDialog(
    creditCards: List<FinancialAccountEntity>,
    initialCardId: Long? = null,
    currencySymbol: String = "Q ",
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onCreatePlan: (
        cardAccountId: Long,
        title: String,
        merchantName: String,
        category: String,
        principalTotalMinor: Long,
        installmentsCount: Int,
        annualInterestRateBps: Int,
        policyOverride: String?,
        postFirstImmediately: Boolean,
        familyMember: String,
        notes: String
    ) -> Unit
) {
    var selectedCard by remember { mutableStateOf(creditCards.firstOrNull { it.id == initialCardId } ?: creditCards.firstOrNull()) }
    var title by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(DefaultData.expenseCategories.first().name) }
    var principalText by remember { mutableStateOf("") }
    var countText by remember { mutableStateOf("6") }
    var ratePercentText by remember { mutableStateOf("0") }
    var policyOverride by remember { mutableStateOf<String?>(null) }
    var postFirstImmediately by remember { mutableStateOf(false) }
    var familyMember by remember { mutableStateOf("Personal") }
    var notes by remember { mutableStateOf("") }
    var expandCard by remember { mutableStateOf(false) }
    var expandCat by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditScore, contentDescription = null, tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nueva Compra en Cuotas / Visacuotas", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(expanded = expandCard, onExpandedChange = { expandCard = !expandCard }) {
                    OutlinedTextField(
                        value = selectedCard?.name ?: "Sin tarjetas activas",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tarjeta de Crédito *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandCard) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandCard, onDismissRequest = { expandCard = false }) {
                        creditCards.forEach { card ->
                            DropdownMenuItem(text = { Text(card.name) }, onClick = { selectedCard = card; expandCard = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text("Concepto de compra *") },
                    placeholder = { Text("Ej. Laptop trabajo, Refrigeradora") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("installment_title_input")
                )

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Comercio (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = principalText,
                    onValueChange = { principalText = it.filter { c -> c.isDigit() || c == '.' }; errorMsg = null },
                    label = { Text("Monto total financiado ($currencySymbol) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("installment_principal_input")
                )

                Text("Cantidad de cuotas", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3, 6, 12, 18, 24).forEach { preset ->
                        FilterChip(
                            selected = countText == preset.toString(),
                            onClick = { countText = preset.toString() },
                            label = { Text("$preset c.") },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it.filter { c -> c.isDigit() }.take(3); errorMsg = null },
                        label = { Text("Cuotas (1-120) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ratePercentText,
                        onValueChange = { ratePercentText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Tasa anual %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                ExposedDropdownMenuBox(expanded = expandCat, onExpandedChange = { expandCat = !expandCat }) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandCat) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandCat, onDismissRequest = { expandCat = false }) {
                        DefaultData.expenseCategories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name) }, onClick = { category = cat.name; expandCat = false })
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = policyOverride == null,
                        onClick = { policyOverride = null },
                        label = { Text("Política tarjeta") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                    FilterChip(
                        selected = policyOverride == CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL,
                        onClick = { policyOverride = CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL },
                        label = { Text("Reservar todo") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                    FilterChip(
                        selected = policyOverride == CreditReservePolicy.RESERVE_BILLED_INSTALLMENTS_ONLY,
                        onClick = { policyOverride = CreditReservePolicy.RESERVE_BILLED_INSTALLMENTS_ONLY },
                        label = { Text("Solo facturadas") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Contabilizar 1.ª cuota de inmediato", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(checked = postFirstImmediately, onCheckedChange = { postFirstImmediately = it })
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val card = selectedCard
                    val cleanTitle = title.trim()
                    val principal = AccountUiMeta.parseMinorUnitsOrNull(principalText) ?: 0L
                    val count = countText.toIntOrNull() ?: 0
                    val rateBps = ((ratePercentText.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO)
                        .multiply(java.math.BigDecimal(100))).toInt().coerceIn(0, 20000)

                    if (card == null) { errorMsg = "Selecciona una tarjeta de crédito."; return@Button }
                    if (cleanTitle.isBlank()) { errorMsg = "Ingresa el concepto de la compra."; return@Button }
                    if (principal <= 0L) { errorMsg = "Ingresa un monto financiado mayor a cero."; return@Button }
                    if (count !in 1..120) { errorMsg = "La cantidad de cuotas debe estar entre 1 y 120."; return@Button }

                    onCreatePlan(
                        card.id, cleanTitle, merchant.trim(), category,
                        principal, count, rateBps, policyOverride,
                        postFirstImmediately, familyMember, notes.trim()
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_create_installment_plan_button")
            ) { Text("Crear Plan", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancelar") }
        }
    )
}

@Composable
fun InstallmentPlanDetailDialog(
    plan: InstallmentPlanEntity,
    scheduleItems: List<InstallmentScheduleItemEntity>,
    currencySymbol: String = "Q ",
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onPostInstallment: (planId: Long, installmentNumber: Int) -> Unit,
    onClosePlan: (planId: Long, paidOffEarly: Boolean) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale("es", "GT")) }
    val postedCount = scheduleItems.count { it.status == InstallmentChargeStatus.POSTED }
    val isActive = plan.status == InstallmentPlanStatus.ACTIVE

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
        title = {
            Column {
                Text(plan.title, fontWeight = FontWeight.Bold)
                Text(
                    text = "${postedCount}/${plan.installmentsCount} cuotas cargadas · ${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(plan.principalTotalMinor), currencySymbol, false)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isActive) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onClosePlan(plan.id, true); onDismiss() },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) { Text("Liquidar Plan", maxLines = 1) }
                        OutlinedButton(
                            onClick = { onClosePlan(plan.id, false); onDismiss() },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) { Text("Cancelar Plan", maxLines = 1) }
                    }
                }

                scheduleItems.forEach { item ->
                    val isPosted = item.status == InstallmentChargeStatus.POSTED
                    val isScheduled = item.status == InstallmentChargeStatus.SCHEDULED
                    val rowBg = when {
                        isPosted -> if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                        isScheduled -> if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                        else -> if (isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = rowBg,
                        border = BorderStroke(1.dp, if (isPosted) EmeraldGreen else Color(0xFF64748B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cuota #${item.installmentNumber} · ${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(item.totalChargeMinor), currencySymbol, false)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ciclo ${item.statementCycleKey} · Vence ${sdf.format(Date(item.dueDateMillis))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isScheduled && isActive) {
                                Button(
                                    onClick = { onPostInstallment(plan.id, item.installmentNumber) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("post_installment_${plan.id}_${item.installmentNumber}")
                                ) { Text("Contabilizar", fontWeight = FontWeight.Bold) }
                            } else {
                                Text(
                                    text = when (item.status) {
                                        InstallmentChargeStatus.POSTED -> "Cargada"
                                        InstallmentChargeStatus.PAID_OFF_EARLY -> "Liquidada"
                                        InstallmentChargeStatus.CANCELLED -> "Cancelada"
                                        else -> "Programada"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Cerrar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignUnassignedTransactionsDialog(
    unassignedTransactions: List<TransactionEntity>,
    activeAccounts: List<FinancialAccountEntity>,
    currencySymbol: String = "Q ",
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onAssignTransaction: (transactionId: Long, accountId: Long) -> Unit
) {
    var selectedTargetAccount by remember { mutableStateOf(activeAccounts.firstOrNull { it.isDefault } ?: activeAccounts.firstOrNull()) }
    var expandAcc by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Link, contentDescription = null, tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Movimientos Sin Cuenta (${unassignedTransactions.size})", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeAccounts.isEmpty()) {
                    Text("Primero crea al menos una cuenta activa para vincular tus registros históricos.")
                } else {
                    ExposedDropdownMenuBox(expanded = expandAcc, onExpandedChange = { expandAcc = !expandAcc }) {
                        OutlinedTextField(
                            value = selectedTargetAccount?.name ?: "Selecciona cuenta",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cuenta destino para vincular") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandAcc) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expandAcc, onDismissRequest = { expandAcc = false }) {
                            activeAccounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${AccountUiMeta.displayTypeLabel(acc.accountType)})") },
                                    onClick = { selectedTargetAccount = acc; expandAcc = false }
                                )
                            }
                        }
                    }

                    if (unassignedTransactions.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Todos los movimientos ya tienen cuenta asignada.", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        unassignedTransactions.take(40).forEach { tx ->
                            val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "$sign${FormatUtils.formatCurrency(tx.amount, currencySymbol, false)} · ${tx.category}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { selectedTargetAccount?.let { acc -> onAssignTransaction(tx.id, acc.id) } },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) { Text("Vincular") }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Listo") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCsvToAccountDialog(
    activeAccounts: List<FinancialAccountEntity>,
    initialTargetAccountId: Long? = null,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onImportToAccount: (csvText: String, targetAccountId: Long) -> Unit,
    onExportCsv: suspend () -> String
) {
    var selectedAccount by remember {
        mutableStateOf(activeAccounts.firstOrNull { it.id == initialTargetAccountId } ?: activeAccounts.firstOrNull())
    }
    var expandAcc by remember { mutableStateOf(false) }
    var openFileImporter by remember { mutableStateOf(false) }

    if (openFileImporter && selectedAccount != null) {
        ExcelCsvImporter(
            initialExportMode = false,
            onDismiss = {
                openFileImporter = false
                onDismiss()
            },
            onImportText = { csv ->
                selectedAccount?.let { acc -> onImportToAccount(csv, acc.id) }
            },
            onExportRequest = onExportCsv
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = EmeraldGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar CSV / Excel a Cuenta", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (activeAccounts.isEmpty()) {
                        Text("Crea primero una cuenta activa para importar sus movimientos.")
                    } else {
                        Text("Los movimientos nuevos se asociarán automáticamente a la cuenta elegida, sin duplicar registros existentes.")
                        ExposedDropdownMenuBox(expanded = expandAcc, onExpandedChange = { expandAcc = !expandAcc }) {
                            OutlinedTextField(
                                value = selectedAccount?.let { "${it.name} (${AccountUiMeta.displayTypeLabel(it.accountType)})" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Cuenta Destino *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandAcc) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = expandAcc, onDismissRequest = { expandAcc = false }) {
                                activeAccounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.name} (${AccountUiMeta.displayTypeLabel(acc.accountType)})") },
                                        onClick = { selectedAccount = acc; expandAcc = false }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedAccount != null,
                    onClick = { openFileImporter = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Seleccionar Archivo", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancelar") }
            }
        )
    }
}
