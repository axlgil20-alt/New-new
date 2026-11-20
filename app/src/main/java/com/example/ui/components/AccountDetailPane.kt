package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountType
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.InstallmentChargeStatus
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.InstallmentScheduleItemEntity
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionEntryKind
import com.example.data.model.TransactionType
import com.example.data.service.AccountBalanceSnapshot
import com.example.data.service.CreditCardStatementSummary
import com.example.ui.theme.EmeraldGreen
import com.example.ui.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountDetailPane(
    account: FinancialAccountEntity,
    snapshot: AccountBalanceSnapshot?,
    statement: CreditCardStatementSummary?,
    accountTransactions: List<TransactionEntity>,
    accountPlans: List<InstallmentPlanEntity>,
    scheduleItemsByPlanId: Map<Long, List<InstallmentScheduleItemEntity>>,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean,
    onEditAccount: () -> Unit,
    onArchiveOrRestoreAccount: () -> Unit,
    onAdjustBalance: () -> Unit,
    onPayCard: () -> Unit,
    onSyncCardReminder: () -> Unit,
    onRecordCardCharge: () -> Unit,
    onNewInstallmentPlan: () -> Unit,
    onPostDueInstallments: () -> Unit,
    onOpenPlanDetail: (InstallmentPlanEntity) -> Unit,
    onAddStandardMovement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCard = AccountType.isCreditCard(account.accountType)
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale("es", "GT")) }
    val cardBg = if (isDarkTheme) Color(0xFF1E293B).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.90f)
    val subSurfaceBg = if (isDarkTheme) Color(0xFF0F172A).copy(alpha = 0.84f) else Color(0xFFF8FAFC).copy(alpha = 0.88f)
    val cardBorder = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
    val primaryText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentEmerald = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Cabecera del detalle de cuenta
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.2.dp, cardBorder),
            modifier = Modifier.fillMaxWidth().testTag("account_detail_card_${account.id}")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(AccountUiMeta.iconForType(account.accountType), contentDescription = null, tint = accentEmerald)
                            Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = primaryText)
                        }
                        val metaParts = listOfNotNull(
                            AccountUiMeta.displayTypeLabel(account.accountType),
                            account.institutionName.takeIf { it.isNotBlank() },
                            account.lastFourDigits.takeIf { it.isNotBlank() }?.let { "•••• $it" }
                        ).joinToString(" · ")
                        Text(metaParts, style = MaterialTheme.typography.bodySmall, color = secondaryText)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (account.isArchived) Color(0xFF475569) else if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                    ) {
                        Text(
                            text = if (account.isArchived) "Archivada" else if (account.isDefault) "Principal" else "Activa",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (account.isArchived) Color.White else accentEmerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Saldo o Deuda principal
                val headlineAmount = if (isCard) {
                    MoneyMinorUnits.toDisplayDouble(snapshot?.totalDebtMinor ?: account.openingBalanceMinor)
                } else {
                    MoneyMinorUnits.toDisplayDouble(snapshot?.currentBalanceMinor ?: account.openingBalanceMinor)
                }
                Text(
                    text = if (isCard) "Deuda total comprometida" else "Saldo disponible",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryText
                )
                Text(
                    text = FormatUtils.formatCurrency(headlineAmount, currencySymbol, isDiscreetMode),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = if (isCard && (snapshot?.totalDebtMinor ?: 0L) > 0L) {
                        if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                    } else accentEmerald
                )

                // Información específica de Tarjeta de Crédito
                if (isCard && snapshot != null) {
                    val utilRatio = (snapshot.utilizationPercent.toFloat() / 100f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { utilRatio },
                        color = if (utilRatio >= 0.85f) Color(0xFFDC2626) else EmeraldGreen,
                        trackColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    Text(
                        text = "Rotativa: ${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(snapshot.revolvingDebtMinor), currencySymbol, isDiscreetMode)} · " +
                            "Visacuotas res.: ${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(snapshot.reservedInstallmentDebtMinor), currencySymbol, isDiscreetMode)} · " +
                            "Disponible: ${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(snapshot.availableCreditMinor), currencySymbol, isDiscreetMode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryText
                    )
                    if (statement != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = subSurfaceBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Ciclo ${statement.cycleWindow.lastStatementCycleKey} · Vence pago: ${sdf.format(Date(statement.cycleWindow.paymentDueDateMillis))}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText
                                )
                                Text(
                                    text = "Corte cerrado por pagar: ${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(statement.closedStatementRemainingDueMinor), currencySymbol, isDiscreetMode)} · " +
                                        "Nuevo ciclo: ${FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(statement.currentCycleNewChargesMinor), currencySymbol, isDiscreetMode)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryText
                                )
                            }
                        }
                    }
                }

                // Acciones de cuenta
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCard) {
                        Button(
                            onClick = onPayCard,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pagar Tarjeta")
                        }
                        OutlinedButton(onClick = onRecordCardCharge, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Consumo / Interés")
                        }
                        OutlinedButton(onClick = onNewInstallmentPlan, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.Default.CreditScore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Visacuotas")
                        }
                        OutlinedButton(onClick = onSyncCardReminder, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recordatorio Pagos")
                        }
                    } else {
                        Button(
                            onClick = onAddStandardMovement,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Registrar Movimiento")
                        }
                    }
                    OutlinedButton(onClick = onAdjustBalance, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Conciliar")
                    }
                    OutlinedButton(onClick = onEditAccount, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar")
                    }
                    OutlinedButton(onClick = onArchiveOrRestoreAccount, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(
                            if (account.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (account.isArchived) "Reactivar" else "Archivar")
                    }
                }
            }
        }

        // Sección Visacuotas de esta tarjeta
        if (isCard) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Planes de Cuotas / Visacuotas (${accountPlans.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = primaryText)
                        if (accountPlans.isNotEmpty()) {
                            OutlinedButton(onClick = onPostDueInstallments, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text("Cargar vencidas")
                            }
                        }
                    }
                    if (accountPlans.isEmpty()) {
                        Text("Sin planes en cuotas activos en esta tarjeta.", style = MaterialTheme.typography.bodySmall, color = secondaryText)
                    } else {
                        accountPlans.forEach { plan ->
                            val items = scheduleItemsByPlanId[plan.id].orEmpty()
                            val posted = items.count { it.status == InstallmentChargeStatus.POSTED }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = subSurfaceBg,
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth().clickable { onOpenPlanDetail(plan) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(plan.title, fontWeight = FontWeight.Bold, color = primaryText)
                                        Text(
                                            text = "$posted/${plan.installmentsCount} cuotas · Estado: ${plan.status}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = secondaryText
                                        )
                                    }
                                    Text(
                                        text = FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(plan.principalTotalMinor), currencySymbol, isDiscreetMode),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accentEmerald
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Movimientos vinculados en el libro mayor único (`transactions`)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Movimientos de la Cuenta (${accountTransactions.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = primaryText)
                if (accountTransactions.isEmpty()) {
                    Text("Aún no hay movimientos vinculados a esta cuenta.", style = MaterialTheme.typography.bodySmall, color = secondaryText)
                } else {
                    accountTransactions.take(25).forEach { tx ->
                        val isInflow = tx.entryKind == TransactionEntryKind.CARD_PAYMENT ||
                            tx.entryKind == TransactionEntryKind.TRANSFER_IN ||
                            (tx.entryKind != TransactionEntryKind.TRANSFER_OUT && tx.type == TransactionType.INCOME)
                        val kindTag = when (tx.entryKind) {
                            TransactionEntryKind.TRANSFER_OUT -> "Transferencia enviada"
                            TransactionEntryKind.TRANSFER_IN -> "Transferencia recibida"
                            TransactionEntryKind.CARD_PAYMENT -> "Abono a tarjeta"
                            TransactionEntryKind.INSTALLMENT_CHARGE -> "Cuota Visacuotas"
                            TransactionEntryKind.BALANCE_ADJUSTMENT -> "Conciliación saldo"
                            else -> tx.category
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = subSurfaceBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.title, fontWeight = FontWeight.Bold, color = primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = "${sdf.format(Date(tx.dateMillis))} · $kindTag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isDiscreetMode) "Q ****.**" else "${if (isInflow) "+" else "-"}${FormatUtils.formatCurrency(tx.amount, currencySymbol, false)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isInflow) accentEmerald else if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
