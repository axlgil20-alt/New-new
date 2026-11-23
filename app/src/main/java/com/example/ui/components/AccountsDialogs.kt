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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountType
import com.example.data.model.CommitmentKind
import com.example.data.model.CreditReservePolicy
import com.example.data.model.DefaultData
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.model.PaymentStatus
import com.example.data.service.AccountBalanceSnapshot
import com.example.data.service.CreditCardStatementSummary
import com.example.ui.theme.EmeraldGreen
import com.example.ui.util.FormatUtils

object AccountUiMeta {
    fun displayTypeLabel(accountType: String): String = when (accountType) {
        AccountType.CASH -> "Efectivo"
        AccountType.BANK_CHECKING -> "Monetaria"
        AccountType.BANK_SAVINGS -> "Ahorro"
        AccountType.COOPERATIVE -> "Cooperativa"
        AccountType.CREDIT_CARD -> "Tarjeta Crédito"
        else -> "Cuenta"
    }

    fun iconForType(accountType: String): ImageVector = when (accountType) {
        AccountType.CASH -> Icons.Default.AccountBalanceWallet
        AccountType.BANK_CHECKING -> Icons.Default.AccountBalance
        AccountType.BANK_SAVINGS -> Icons.Default.Savings
        AccountType.COOPERATIVE -> Icons.Default.Groups
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
        else -> Icons.Default.AccountBalanceWallet
    }

    fun parseMinorUnitsOrNull(input: String): Long? {
        val cleaned = input.trim().replace(",", ".")
        if (cleaned.isBlank()) return null
        return runCatching {
            val bd = cleaned.toBigDecimalOrNull() ?: return null
            MoneyMinorUnits.fromBigDecimal(bd)
        }.getOrNull()
    }
}

internal fun cardPaymentCommitmentsForAccount(
    pendingCommitments: List<PaymentCommitmentEntity>,
    cardAccountId: Long?
): List<PaymentCommitmentEntity> {
    if (cardAccountId == null) return emptyList()
    return pendingCommitments.filter {
        it.commitmentKind == CommitmentKind.CREDIT_CARD_STATEMENT &&
            it.creditCardAccountId == cardAccountId &&
            !it.isPaid &&
            it.status != PaymentStatus.PAID &&
            it.status != PaymentStatus.CANCELLED
    }
}

internal fun findCardPaymentCommitment(
    pendingCommitments: List<PaymentCommitmentEntity>,
    cardAccountId: Long?
): PaymentCommitmentEntity? = cardPaymentCommitmentsForAccount(pendingCommitments, cardAccountId).firstOrNull()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditAccountDialog(
    initialAccount: FinancialAccountEntity? = null,
    currencySymbol: String = "Q ",
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onCreateAccount: (
        name: String,
        accountType: String,
        institutionName: String,
        lastFourDigits: String,
        openingBalanceMinor: Long,
        creditLimitMinor: Long,
        cutoffDay: Int?,
        paymentDueDay: Int?,
        installmentReservePolicy: String,
        autoCreatePaymentReminder: Boolean,
        includeInTotalLiquidity: Boolean,
        isDefault: Boolean,
        notes: String
    ) -> Unit,
    onUpdateAccount: (FinancialAccountEntity) -> Unit
) {
    val isEditing = initialAccount != null
    var accountType by remember { mutableStateOf(initialAccount?.accountType ?: AccountType.CASH) }
    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var institutionName by remember { mutableStateOf(initialAccount?.institutionName ?: "") }
    var lastFourDigits by remember { mutableStateOf(initialAccount?.lastFourDigits ?: "") }
    var openingBalanceText by remember {
        mutableStateOf(
            initialAccount?.let { MoneyMinorUnits.toBigDecimal(it.openingBalanceMinor).toPlainString() } ?: "0.00"
        )
    }
    var creditLimitText by remember {
        mutableStateOf(
            initialAccount?.takeIf { it.creditLimitMinor > 0L }?.let {
                MoneyMinorUnits.toBigDecimal(it.creditLimitMinor).toPlainString()
            } ?: ""
        )
    }
    var cutoffDayText by remember { mutableStateOf(initialAccount?.cutoffDay?.toString() ?: "15") }
    var paymentDueDayText by remember { mutableStateOf(initialAccount?.paymentDueDay?.toString() ?: "5") }
    var reservePolicy by remember {
        mutableStateOf(initialAccount?.installmentReservePolicy ?: CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL)
    }
    var autoCreateReminder by remember { mutableStateOf(initialAccount?.autoCreatePaymentReminder ?: true) }
    var includeInLiquidity by remember { mutableStateOf(initialAccount?.includeInTotalLiquidity ?: true) }
    var isDefault by remember { mutableStateOf(initialAccount?.isDefault ?: false) }
    var notes by remember { mutableStateOf(initialAccount?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val isCard = AccountType.isCreditCard(accountType)
    val dialogBg = if (isDarkTheme) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        titleContentColor = textPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AccountUiMeta.iconForType(accountType), contentDescription = null, tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Editar Cuenta" else "Nueva Cuenta / Tarjeta",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tipo de Cuenta",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        AccountType.CASH,
                        AccountType.BANK_CHECKING,
                        AccountType.BANK_SAVINGS,
                        AccountType.COOPERATIVE,
                        AccountType.CREDIT_CARD
                    ).forEach { type ->
                        val selected = accountType == type
                        FilterChip(
                            selected = selected,
                            enabled = !isEditing,
                            onClick = { accountType = type },
                            label = { Text(AccountUiMeta.displayTypeLabel(type), fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreen,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Nombre de la cuenta *") },
                    placeholder = { Text(if (isCard) "Ej. Visa Bi Cashback" else "Ej. Cuenta Monetaria BI") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("account_name_input")
                )

                if (accountType != AccountType.CASH) {
                    OutlinedTextField(
                        value = institutionName,
                        onValueChange = { institutionName = it },
                        label = { Text("Banco / Cooperativa / Emisor") },
                        placeholder = { Text("Ej. Banco Industrial, Banrural, Micoope") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = lastFourDigits,
                        onValueChange = { lastFourDigits = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Últimos 4 dígitos (opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = openingBalanceText,
                    onValueChange = { openingBalanceText = it.filter { c -> c.isDigit() || c == '.' || c == '-' }; errorMsg = null },
                    label = { Text(if (isCard) "Deuda inicial previa ($currencySymbol)" else "Saldo inicial ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("account_opening_balance_input")
                )

                if (isCard) {
                    OutlinedTextField(
                        value = creditLimitText,
                        onValueChange = { creditLimitText = it.filter { c -> c.isDigit() || c == '.' }; errorMsg = null },
                        label = { Text("Límite / Cupo de crédito ($currencySymbol) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("account_credit_limit_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = cutoffDayText,
                            onValueChange = { cutoffDayText = it.filter { c -> c.isDigit() }.take(2); errorMsg = null },
                            label = { Text("Día corte (1-31)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paymentDueDayText,
                            onValueChange = { paymentDueDayText = it.filter { c -> c.isDigit() }.take(2); errorMsg = null },
                            label = { Text("Día pago (1-31)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Política de cupo en Visacuotas", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = reservePolicy == CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL,
                            onClick = { reservePolicy = CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL },
                            label = { Text("Total pendiente") },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        )
                        FilterChip(
                            selected = reservePolicy == CreditReservePolicy.RESERVE_BILLED_INSTALLMENTS_ONLY,
                            onClick = { reservePolicy = CreditReservePolicy.RESERVE_BILLED_INSTALLMENTS_ONLY },
                            label = { Text("Solo facturadas") },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Crear recordatorio automático en Pagos", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Switch(checked = autoCreateReminder, onCheckedChange = { autoCreateReminder = it })
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Incluir en Liquidez Total", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Switch(checked = includeInLiquidity, onCheckedChange = { includeInLiquidity = it })
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cuenta predeterminada", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(checked = isDefault, onCheckedChange = { isDefault = it })
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isBlank()) {
                        errorMsg = "Ingresa el nombre de la cuenta."
                        return@Button
                    }
                    val openingMinor = AccountUiMeta.parseMinorUnitsOrNull(openingBalanceText) ?: 0L
                    if (isCard && openingMinor < 0L) {
                        errorMsg = "La deuda inicial de la tarjeta no puede ser negativa."
                        return@Button
                    }
                    val limitMinor = if (isCard) (AccountUiMeta.parseMinorUnitsOrNull(creditLimitText) ?: 0L) else 0L
                    if (isCard && limitMinor <= 0L) {
                        errorMsg = "Ingresa un límite de crédito válido mayor a cero."
                        return@Button
                    }
                    val cutoff = if (isCard) cutoffDayText.toIntOrNull()?.takeIf { it in 1..31 } else null
                    val dueDay = if (isCard) paymentDueDayText.toIntOrNull()?.takeIf { it in 1..31 } else null
                    if (isCard && (cutoff == null || dueDay == null)) {
                        errorMsg = "Los días de corte y pago deben estar entre 1 y 31."
                        return@Button
                    }

                    if (initialAccount == null) {
                        onCreateAccount(
                            cleanName, accountType, institutionName.trim(), lastFourDigits.trim(),
                            openingMinor, limitMinor, cutoff, dueDay, reservePolicy,
                            autoCreateReminder, includeInLiquidity, isDefault, notes.trim()
                        )
                    } else {
                        onUpdateAccount(
                            initialAccount.copy(
                                name = cleanName,
                                institutionName = institutionName.trim(),
                                lastFourDigits = lastFourDigits.trim(),
                                openingBalanceMinor = openingMinor,
                                creditLimitMinor = limitMinor,
                                cutoffDay = cutoff,
                                paymentDueDay = dueDay,
                                installmentReservePolicy = reservePolicy,
                                autoCreatePaymentReminder = autoCreateReminder,
                                includeInTotalLiquidity = if (isCard) false else includeInLiquidity,
                                isDefault = isDefault,
                                notes = notes.trim()
                            )
                        )
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                modifier = Modifier.heightIn(min = 48.dp).testTag("save_account_button")
            ) {
                Text(if (isEditing) "Guardar Cambios" else "Crear Cuenta", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTransferDialog(
    liquidAccounts: List<FinancialAccountEntity>,
    snapshotsById: Map<Long, AccountBalanceSnapshot>,
    initialFromId: Long? = null,
    currencySymbol: String = "Q ",
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirmTransfer: (fromId: Long, toId: Long, amountMinor: Long, title: String, notes: String) -> Unit
) {
    var fromAccount by remember {
        mutableStateOf(liquidAccounts.firstOrNull { it.id == initialFromId } ?: liquidAccounts.firstOrNull())
    }
    var toAccount by remember {
        mutableStateOf(liquidAccounts.firstOrNull { it.id != fromAccount?.id })
    }
    var amountText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expandFrom by remember { mutableStateOf(false) }
    var expandTo by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val dialogBg = if (isDarkTheme) Color(0xFF1E293B) else Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transferencia entre Cuentas", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("No duplica gastos ni ingresos en tu presupuesto.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                ExposedDropdownMenuBox(expanded = expandFrom, onExpandedChange = { expandFrom = !expandFrom }) {
                    val bal = fromAccount?.let { snapshotsById[it.id]?.currentBalanceMinor ?: 0L } ?: 0L
                    OutlinedTextField(
                        value = fromAccount?.let { "${it.name} (${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(bal), currencySymbol, false)})" } ?: "Sin cuentas origen",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cuenta Origen *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandFrom) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandFrom, onDismissRequest = { expandFrom = false }) {
                        liquidAccounts.forEach { acc ->
                            val accBal = snapshotsById[acc.id]?.currentBalanceMinor ?: 0L
                            DropdownMenuItem(
                                text = { Text("${acc.name} · ${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(accBal), currencySymbol, false)}") },
                                onClick = {
                                    fromAccount = acc
                                    if (toAccount?.id == acc.id) {
                                        toAccount = liquidAccounts.firstOrNull { it.id != acc.id }
                                    }
                                    expandFrom = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = expandTo, onExpandedChange = { expandTo = !expandTo }) {
                    val bal = toAccount?.let { snapshotsById[it.id]?.currentBalanceMinor ?: 0L } ?: 0L
                    OutlinedTextField(
                        value = toAccount?.let { "${it.name} (${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(bal), currencySymbol, false)})" } ?: "Sin cuentas destino",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cuenta Destino *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandTo) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandTo, onDismissRequest = { expandTo = false }) {
                        liquidAccounts.filter { it.id != fromAccount?.id }.forEach { acc ->
                            val accBal = snapshotsById[acc.id]?.currentBalanceMinor ?: 0L
                            DropdownMenuItem(
                                text = { Text("${acc.name} · ${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(accBal), currencySymbol, false)}") },
                                onClick = { toAccount = acc; expandTo = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }; errorMsg = null },
                    label = { Text("Monto a transferir ($currencySymbol) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input")
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Concepto (opcional)") },
                    placeholder = { Text("Ej. Traslado a cuenta ahorro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val from = fromAccount
                    val to = toAccount
                    val amtMinor = AccountUiMeta.parseMinorUnitsOrNull(amountText) ?: 0L
                    if (from == null || to == null || from.id == to.id) {
                        errorMsg = "Selecciona cuentas de origen y destino distintas."
                        return@Button
                    }
                    if (amtMinor <= 0L) {
                        errorMsg = "Ingresa un monto mayor a cero."
                        return@Button
                    }
                    onConfirmTransfer(from.id, to.id, amtMinor, title.trim(), notes.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_transfer_button")
            ) {
                Text("Transferir", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayCreditCardDialog(
    liquidAccounts: List<FinancialAccountEntity>,
    creditCards: List<FinancialAccountEntity>,
    snapshotsById: Map<Long, AccountBalanceSnapshot>,
    statementsById: Map<Long, CreditCardStatementSummary>,
    pendingCommitments: List<PaymentCommitmentEntity>,
    initialCardId: Long? = null,
    currencySymbol: String = "Q ",
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirmPay: (sourceId: Long, cardId: Long, amountMinor: Long, linkedCommitmentId: Long?, notes: String) -> Unit
) {
    var selectedCard by remember { mutableStateOf(creditCards.firstOrNull { it.id == initialCardId } ?: creditCards.firstOrNull()) }
    var selectedSource by remember { mutableStateOf(liquidAccounts.firstOrNull { it.isDefault } ?: liquidAccounts.firstOrNull()) }
    val eligibleCommitments = remember(pendingCommitments, selectedCard?.id) {
        cardPaymentCommitmentsForAccount(pendingCommitments, selectedCard?.id)
    }
    val eligibleCommitmentIds = remember(eligibleCommitments) { eligibleCommitments.map { it.id } }
    var selectedCommitmentId by remember(selectedCard?.id, eligibleCommitmentIds) {
        mutableStateOf(eligibleCommitments.firstOrNull()?.id)
    }
    val selectedCommitment = eligibleCommitments.firstOrNull { it.id == selectedCommitmentId }
    val statementDueMinor = selectedCard?.let { statementsById[it.id]?.closedStatementRemainingDueMinor ?: 0L } ?: 0L
    val revolvingMinor = selectedCard?.let { (snapshotsById[it.id]?.revolvingDebtMinor ?: 0L).coerceAtLeast(0L) } ?: 0L

    var amountText by remember(selectedCard?.id) {
        val suggested = if (statementDueMinor > 0L) statementDueMinor else revolvingMinor
        mutableStateOf(if (suggested > 0L) MoneyMinorUnits.toBigDecimal(suggested).toPlainString() else "")
    }
    var notes by remember { mutableStateOf("") }
    var expandCard by remember { mutableStateOf(false) }
    var expandSource by remember { mutableStateOf(false) }
    var expandCommitment by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val dialogBg = if (isDarkTheme) Color(0xFF1E293B) else Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pagar Tarjeta de Crédito", fontWeight = FontWeight.Bold)
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
                        label = { Text("Tarjeta a Pagar *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandCard) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandCard, onDismissRequest = { expandCard = false }) {
                        creditCards.forEach { card ->
                            DropdownMenuItem(text = { Text(card.name) }, onClick = { selectedCard = card; expandCard = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = expandSource, onExpandedChange = { expandSource = !expandSource }) {
                    val srcBal = selectedSource?.let { snapshotsById[it.id]?.currentBalanceMinor ?: 0L } ?: 0L
                    OutlinedTextField(
                        value = selectedSource?.let { "${it.name} (${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(srcBal), currencySymbol, false)})" } ?: "Sin cuentas de origen",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pagar desde *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandSource) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandSource, onDismissRequest = { expandSource = false }) {
                        liquidAccounts.forEach { acc ->
                            val bal = snapshotsById[acc.id]?.currentBalanceMinor ?: 0L
                            DropdownMenuItem(
                                text = { Text("${acc.name} · ${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(bal), currencySymbol, false)}") },
                                onClick = { selectedSource = acc; expandSource = false }
                            )
                        }
                    }
                }

                if (statementDueMinor > 0L || revolvingMinor > 0L) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (statementDueMinor > 0L) {
                            OutlinedButton(
                                onClick = { amountText = MoneyMinorUnits.toBigDecimal(statementDueMinor).toPlainString() },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Text("Corte: $currencySymbol${MoneyMinorUnits.toBigDecimal(statementDueMinor).toPlainString()}", maxLines = 1)
                            }
                        }
                        if (revolvingMinor > 0L) {
                            OutlinedButton(
                                onClick = { amountText = MoneyMinorUnits.toBigDecimal(revolvingMinor).toPlainString() },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Text("Total: $currencySymbol${MoneyMinorUnits.toBigDecimal(revolvingMinor).toPlainString()}", maxLines = 1)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }; errorMsg = null },
                    label = { Text("Monto a abonar ($currencySymbol) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("pay_card_amount_input")
                )

                if (eligibleCommitments.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expandCommitment,
                        onExpandedChange = { expandCommitment = !expandCommitment }
                    ) {
                        OutlinedTextField(
                            value = selectedCommitment?.title ?: "Sin vincular",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Compromiso a liquidar (opcional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandCommitment) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandCommitment,
                            onDismissRequest = { expandCommitment = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sin vincular") },
                                onClick = {
                                    selectedCommitmentId = null
                                    expandCommitment = false
                                }
                            )
                            eligibleCommitments.forEach { commitment ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${commitment.title} · ${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(commitment.amountMinor), currencySymbol, false)}",
                                            maxLines = 1
                                        )
                                    },
                                    onClick = {
                                        selectedCommitmentId = commitment.id
                                        expandCommitment = false
                                    }
                                )
                            }
                        }
                    }
                }

                selectedCommitment?.let { commitment ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                        border = BorderStroke(1.dp, EmeraldGreen)
                    ) {
                        Text(
                            text = "También actualizará «${commitment.title}» en Pagos; solo quedará pagado cuando cubras el saldo pendiente.",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Referencia (opcional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val src = selectedSource
                    val card = selectedCard
                    val amt = AccountUiMeta.parseMinorUnitsOrNull(amountText) ?: 0L
                    if (src == null || card == null) {
                        errorMsg = "Necesitas al menos una cuenta de origen y una tarjeta."
                        return@Button
                    }
                    if (amt <= 0L) {
                        errorMsg = "Ingresa un monto de abono válido mayor a cero."
                        return@Button
                    }
                    onConfirmPay(src.id, card.id, amt, selectedCommitment?.id, notes.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_pay_card_button")
            ) {
                Text("Aplicar Pago", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancelar") }
        }
    )
}

@Composable
fun BalanceAdjustmentDialog(
    account: FinancialAccountEntity,
    currentBalanceOrDebtMinor: Long,
    currencySymbol: String = "Q ",
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirmAdjustment: (targetMinor: Long, notes: String) -> Unit
) {
    val isCard = AccountType.isCreditCard(account.accountType)
    var targetText by remember { mutableStateOf(MoneyMinorUnits.toBigDecimal(currentBalanceOrDebtMinor).toPlainString()) }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Conciliar Saldo", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isCard) "Ajusta la deuda rotativa real de «${account.name}» según tu estado de cuenta sin duplicar gastos."
                    else "Ajusta el saldo real disponible en «${account.name}» tras arqueo sin alterar presupuestos.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Actual en app: ${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(currentBalanceOrDebtMinor), currencySymbol, false)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' || c == '-' }; errorMsg = null },
                    label = { Text(if (isCard) "Nueva deuda rotativa ($currencySymbol)" else "Nuevo saldo real ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("adjust_balance_input")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Motivo del ajuste (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = AccountUiMeta.parseMinorUnitsOrNull(targetText)
                    if (parsed == null) {
                        errorMsg = "Ingresa un monto numérico válido."
                        return@Button
                    }
                    onConfirmAdjustment(parsed, notes.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Conciliar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordCardChargeDialog(
    cardAccount: FinancialAccountEntity,
    currencySymbol: String = "Q ",
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onRecordPurchase: (title: String, amountMinor: Long, category: String, familyMember: String, notes: String, enforceLimit: Boolean) -> Unit,
    onRecordInterestOrFee: (title: String, amountMinor: Long, category: String, notes: String) -> Unit
) {
    var isInterestOrFee by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(DefaultData.expenseCategories.first().name) }
    var familyMember by remember { mutableStateOf("Personal") }
    var enforceLimit by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var expandCat by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
        title = { Text("Cargo en ${cardAccount.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isInterestOrFee,
                        onClick = { isInterestOrFee = false },
                        label = { Text("Consumo") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                    FilterChip(
                        selected = isInterestOrFee,
                        onClick = { isInterestOrFee = true },
                        label = { Text("Interés / Cargo") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text(if (isInterestOrFee) "Descripción del cargo *" else "Comercio / Concepto *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }; errorMsg = null },
                    label = { Text("Monto ($currencySymbol) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                if (!isInterestOrFee) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bloquear si supera cupo disponible", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Switch(checked = enforceLimit, onCheckedChange = { enforceLimit = it })
                    }
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
                    val cleanTitle = title.trim()
                    val amt = AccountUiMeta.parseMinorUnitsOrNull(amountText) ?: 0L
                    if (cleanTitle.isBlank()) { errorMsg = "Ingresa una descripción."; return@Button }
                    if (amt <= 0L) { errorMsg = "Ingresa un monto mayor a cero."; return@Button }
                    if (isInterestOrFee) {
                        onRecordInterestOrFee(cleanTitle, amt, category, notes.trim())
                    } else {
                        onRecordPurchase(cleanTitle, amt, category, familyMember, notes.trim(), enforceLimit)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Registrar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancelar") }
        }
    )
}
