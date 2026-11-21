package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DefaultData
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionEntryKind
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.isGastoSupuesto
import com.example.data.model.source
import com.example.data.model.status
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TentativePurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onHighlight: (Long) -> Unit,
    onToggleVerify: () -> Unit,
    onDecideTentative: (String) -> Unit,
    isDiscreetMode: Boolean = false,
    isDarkTheme: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onMarkAsLegitimate: (() -> Unit)? = null,
    onToggleGastoSupuesto: (() -> Unit)? = null,
    accountName: String? = null,
    isCompactMode: Boolean = false,
    rowIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var compactExpanded by remember { mutableStateOf(false) }

    val isIncome = transaction.type == TransactionType.INCOME
    val isTentative = transaction.isTentative
    val decision = transaction.tentativeDecision
    val isDiscarded = isTentative && decision == "DISCARDED"
    val isPendingTentative = transaction.isGastoSupuesto

    // Superficie base 100% opaca con alternancia clara en filas (zebra striping)
    val opaqueFallbackBase = when {
        isDarkTheme && rowIndex % 2 == 0 -> Color(0xFF14211E)
        isDarkTheme -> Color(0xFF1C2C28)
        rowIndex % 2 == 0 -> Color(0xFFFFFFFF)
        else -> Color(0xFFF3EFE2)
    }

    val baseCardColor = when {
        isDarkTheme && isSelected -> Color(0xFF1E3A4C)
        isDarkTheme && isDiscarded -> Color(0xFF1B3328)
        isDarkTheme && isPendingTentative -> Color(0xFF2B2040)
        isDarkTheme && transaction.isSuspectedDuplicate -> Color(0xFF3A2D16)
        isDarkTheme && transaction.highlightColorHex != 0L ->
            Color(transaction.highlightColorHex).copy(alpha = 0.28f).compositeOver(opaqueFallbackBase)
        isSelected -> Color(0xFFE0F2FE)
        isDiscarded -> Color(0xFFECFDF5)
        isPendingTentative -> Color(0xFFF5F3FF)
        transaction.isSuspectedDuplicate -> Color(0xFFFEF3C7)
        transaction.highlightColorHex != 0L ->
            Color(transaction.highlightColorHex).copy(alpha = 0.30f).compositeOver(opaqueFallbackBase)
        else -> opaqueFallbackBase
    }

    val animatedCardColor by animateColorAsState(targetValue = baseCardColor, label = "cardColor")

    val borderColor = when {
        isSelected -> Color(0xFF0284C7)
        transaction.isSuspectedDuplicate -> Color(0xFFD97706)
        isDiscarded -> if (isDarkTheme) Color(0xFF34D399) else Color(0xFF10B981)
        isPendingTentative -> if (isDarkTheme) Color(0xFFA78BFA) else TentativePurple
        transaction.isVerified -> if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)
        isDarkTheme -> if (rowIndex % 2 == 0) Color(0xFF2A3E39) else Color(0xFF354E48)
        else -> if (rowIndex % 2 == 0) Color(0xFFE2E8F0) else Color(0xFFD6CBB2)
    }

    val amountColor = when {
        isDarkTheme && isDiscarded -> Color(0xFF6EE7B7)
        isDarkTheme && isPendingTentative -> Color(0xFFD8B4FE)
        isDarkTheme && isIncome -> Color(0xFF6EE7B7)
        isDarkTheme -> Color(0xFFFCA5A5)
        isDiscarded -> IncomeGreen
        isPendingTentative -> TentativePurple
        isIncome -> IncomeGreen
        else -> ExpenseRed
    }

    val amountPrefix = when {
        isDiscarded -> "+$currencySymbol"
        isIncome -> "+$currencySymbol"
        else -> "-$currencySymbol"
    }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale("es", "ES")) }
    val shortSdf = remember { SimpleDateFormat("dd MMM", Locale("es", "ES")) }
    val dateText = sdf.format(Date(transaction.dateMillis))
    val shortDateText = shortSdf.format(Date(transaction.dateMillis))

    val icon = CategoryIconHelper.getIconForCategory(transaction.category)
    val iconBoxBg = when {
        isDarkTheme && isDiscarded -> Color(0xFF064E3B)
        isDarkTheme && isPendingTentative -> Color(0xFF4C1D95)
        isDarkTheme && isIncome -> Color(0xFF065F46)
        isDarkTheme && transaction.isSuspectedDuplicate -> Color(0xFF78350F)
        isDarkTheme -> Color(0xFF7F1D1D)
        isDiscarded -> Color(0xFFDCFCE7)
        isPendingTentative -> Color(0xFFEDE9FE)
        isIncome -> Color(0xFFD1FAE5)
        transaction.isSuspectedDuplicate -> Color(0xFFFEF3C7)
        else -> Color(0xFFFEE2E2)
    }
    val iconTint = when {
        isDarkTheme && isDiscarded -> Color(0xFF6EE7B7)
        isDarkTheme && isPendingTentative -> Color(0xFFDDD6FE)
        isDarkTheme && isIncome -> Color(0xFFA7F3D0)
        isDarkTheme && transaction.isSuspectedDuplicate -> Color(0xFFFDE68A)
        isDarkTheme -> Color(0xFFFECACA)
        isDiscarded -> IncomeGreen
        isPendingTentative -> TentativePurple
        isIncome -> IncomeGreen
        transaction.isSuspectedDuplicate -> Color(0xFFB45309)
        else -> ExpenseRed
    }

    val shape = RoundedCornerShape(if (isCompactMode) 14.dp else 18.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = if (isCompactMode) 3.dp else 5.dp
            )
            .border(
                width = if (isSelected || transaction.isSuspectedDuplicate || isPendingTentative) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .clip(shape)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else if (isCompactMode) {
                        compactExpanded = !compactExpanded
                    }
                },
                onLongClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onToggleVerify()
                    }
                }
            )
            .semantics(mergeDescendants = false) {}
            .testTag("transaction_item_${transaction.id}"),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = animatedCardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPendingTentative || isSelected) 3.dp else if (isCompactMode) 1.dp else 2.dp
        )
    ) {
        if (isCompactMode) {
            // MODO COMPACTO: Alta densidad visual, targets >=48dp y panel reversible al tocar
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { onToggleSelect() },
                            contentAlignment = Alignment.Center
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleSelect() },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = EmeraldGreen,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconBoxBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = transaction.category,
                            modifier = Modifier.size(16.dp),
                            tint = iconTint
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transaction.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isDiscarded) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2
                        )
                        Text(
                            text = buildString {
                                append("$shortDateText · ${transaction.category}")
                                if (!accountName.isNullOrBlank()) append(" · $accountName")
                                if (isPendingTentative) append(" · Supuesto")
                                if (transaction.isSuspectedDuplicate) append(" · Duplicado")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isDiscreetMode) "Q ****" else "$amountPrefix${"%,.2f".format(transaction.amount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = amountColor
                        )
                        if (transaction.isVerified) {
                            Text(
                                text = "Conciliado",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Acción rápida 1: Verificar (target 48dp, 100% reversible)
                    IconButton(
                        onClick = onToggleVerify,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("tx_verify_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = if (transaction.isVerified) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (transaction.isVerified) "Quitar verificación" else "Marcar verificado",
                            tint = if (transaction.isVerified) {
                                if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Menú de más acciones (target 48dp)
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("tx_more_${transaction.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Acciones del registro",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        TransactionActionsOverflowMenu(
                            expanded = showMoreMenu,
                            onDismiss = { showMoreMenu = false },
                            transaction = transaction,
                            isPendingTentative = isPendingTentative,
                            onToggleVerify = onToggleVerify,
                            onOpenColorPicker = {
                                showMoreMenu = false
                                showColorPicker = true
                            },
                            onToggleGastoSupuesto = onToggleGastoSupuesto,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onMarkAsLegitimate = onMarkAsLegitimate
                        )
                    }
                }

                TransactionColorDropdown(
                    expanded = showColorPicker,
                    onDismiss = { showColorPicker = false },
                    onHighlight = onHighlight
                )

                // Tira reversible desplegable al tocar la fila en Compacto (o si es Gasto Supuesto pendiente)
                AnimatedVisibility(visible = compactExpanded || isPendingTentative) {
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                        if (isPendingTentative) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TentativeDecisionBanner(
                                currencySymbol = currencySymbol,
                                amount = transaction.amount,
                                isDarkTheme = isDarkTheme,
                                onDecideTentative = onDecideTentative
                            )
                        }
                        if (compactExpanded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TransactionActionBar(
                                transaction = transaction,
                                isDarkTheme = isDarkTheme,
                                isPendingTentative = isPendingTentative,
                                showColorPicker = showColorPicker,
                                onOpenColorPicker = { showColorPicker = true },
                                onDismissColorPicker = { showColorPicker = false },
                                onHighlight = onHighlight,
                                onToggleVerify = onToggleVerify,
                                onToggleGastoSupuesto = onToggleGastoSupuesto,
                                onEdit = onEdit,
                                onDelete = onDelete
                            )
                        }
                    }
                }
            }
        } else {
            // MODO CÓMODO: Tarjeta completa con jerarquía respirada, 0 truncamiento y targets >=48dp
            Column(modifier = Modifier.padding(14.dp)) {
                // Fila superior: Categoría completa sin compresión contra 5 botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { onToggleSelect() },
                                contentAlignment = Alignment.Center
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleSelect() },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = EmeraldGreen,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(iconBoxBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = transaction.category,
                                modifier = Modifier.size(19.dp),
                                tint = iconTint
                            )
                        }

                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }

                    // Botón principal de verificación rápida siempre accesible en esquina superior (48dp)
                    IconButton(
                        onClick = onToggleVerify,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("tx_verify_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = if (transaction.isVerified) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (transaction.isVerified) "Verificado" else "Marcar Revisado",
                            tint = if (transaction.isVerified) {
                                if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Título / Establecimiento y Monto Principal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transaction.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isDiscarded) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$dateText • ${transaction.familyMember} • ${transaction.paymentMethod}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isDiscreetMode) "Q ****.**" else "$amountPrefix${"%,.2f".format(transaction.amount)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = amountColor
                        )
                        if (isPendingTentative) {
                            Text(
                                text = "Gasto Supuesto",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDarkTheme) Color(0xFFD8B4FE) else TentativePurple,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Badges y Metadatos adaptados a modo claro/oscuro (100% opacos)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (transaction.source == TransactionSource.IMPORTED) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) Color(0xFF6EE7B7) else EmeraldGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Excel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    when (transaction.status) {
                        TransactionStatus.PROJECTED -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDarkTheme) Color(0xFF3B1D6A) else Color(0xFFEDE9FE)
                            ) {
                                Text(
                                    text = "Proyectado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkTheme) Color(0xFFDDD6FE) else TentativePurple,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        TransactionStatus.PENDING -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDarkTheme) Color(0xFF4B300B) else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "Pendiente",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        else -> {}
                    }

                    if (transaction.isSuspectedDuplicate) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) Color(0xFFFECACA) else Color(0xFFDC2626),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Posible Duplicado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkTheme) Color(0xFFFEE2E2) else Color(0xFFB91C1C),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    if (transaction.isAntExpense) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) Color(0xFF7C2D12) else Color(0xFFFFEDD5)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = "Gasto Hormiga",
                                    tint = if (isDarkTheme) Color(0xFFFDBA74) else Color(0xFFC2410C),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Hormiga",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkTheme) Color(0xFFFFEDD5) else Color(0xFFC2410C),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (!accountName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = accountName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    val specialKindLabel = when (transaction.entryKind) {
                        TransactionEntryKind.TRANSFER_OUT,
                        TransactionEntryKind.TRANSFER_IN -> "Transferencia"
                        TransactionEntryKind.CARD_PAYMENT -> "Pago Tarjeta"
                        TransactionEntryKind.INSTALLMENT_CHARGE ->
                            transaction.installmentNumber?.let { "Cuota #$it" } ?: "Visacuotas"
                        TransactionEntryKind.BALANCE_ADJUSTMENT -> "Conciliación"
                        else -> null
                    }
                    if (specialKindLabel != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                        ) {
                            Text(
                                text = specialKindLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDarkTheme) Color(0xFFBFDBFE) else Color(0xFF1E40AF),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Botón accesible >= 48dp para confirmar legítimo en caso de duplicado
                if (transaction.isSuspectedDuplicate && onMarkAsLegitimate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onMarkAsLegitimate,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF34D399) else EmeraldGreen),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFECFDF5),
                            contentColor = if (isDarkTheme) Color(0xFFA7F3D0) else EmeraldGreen
                        ),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirmar como legítimo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (transaction.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = transaction.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }

                if (isPendingTentative) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TentativeDecisionBanner(
                        currencySymbol = currencySymbol,
                        amount = transaction.amount,
                        isDarkTheme = isDarkTheme,
                        onDecideTentative = onDecideTentative
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = borderColor.copy(alpha = 0.55f))

                // Barra de acciones de 48dp en Cómodo: evita compresión de encabezados y cumple accesibilidad
                TransactionActionBar(
                    transaction = transaction,
                    isDarkTheme = isDarkTheme,
                    isPendingTentative = isPendingTentative,
                    showColorPicker = showColorPicker,
                    onOpenColorPicker = { showColorPicker = true },
                    onDismissColorPicker = { showColorPicker = false },
                    onHighlight = onHighlight,
                    onToggleVerify = onToggleVerify,
                    onToggleGastoSupuesto = onToggleGastoSupuesto,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun TransactionActionBar(
    transaction: TransactionEntity,
    isDarkTheme: Boolean,
    isPendingTentative: Boolean,
    showColorPicker: Boolean,
    onOpenColorPicker: () -> Unit,
    onDismissColorPicker: () -> Unit,
    onHighlight: (Long) -> Unit,
    onToggleVerify: () -> Unit,
    onToggleGastoSupuesto: (() -> Unit)?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggleVerify,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (transaction.isVerified) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (transaction.isVerified) "Quitar verificado" else "Marcar revisado",
                tint = if (transaction.isVerified) {
                    if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
        }

        Box {
            IconButton(
                onClick = onOpenColorPicker,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FormatColorFill,
                    contentDescription = "Resaltar con color",
                    tint = if (transaction.highlightColorHex != 0L) Color(transaction.highlightColorHex) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            TransactionColorDropdown(
                expanded = showColorPicker,
                onDismiss = onDismissColorPicker,
                onHighlight = onHighlight
            )
        }

        if (transaction.type == TransactionType.EXPENSE && onToggleGastoSupuesto != null) {
            IconButton(
                onClick = onToggleGastoSupuesto,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isPendingTentative) Icons.Default.CheckCircle else Icons.Default.HelpOutline,
                    contentDescription = if (isPendingTentative) "Confirmar como real" else "Marcar como supuesto",
                    tint = if (isPendingTentative) {
                        if (isDarkTheme) Color(0xFFC4B5FD) else TentativePurple
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        IconButton(
            onClick = onEdit,
            modifier = Modifier
                .size(48.dp)
                .testTag("tx_edit_${transaction.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Editar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(48.dp)
                .testTag("tx_delete_${transaction.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TransactionColorDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onHighlight: (Long) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DefaultData.highlightColors.forEach { option ->
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (option.colorHex != 0L) Color(option.colorHex) else Color.LightGray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.name)
                    }
                },
                onClick = {
                    onHighlight(option.colorHex)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun TransactionActionsOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    transaction: TransactionEntity,
    isPendingTentative: Boolean,
    onToggleVerify: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onToggleGastoSupuesto: (() -> Unit)?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsLegitimate: (() -> Unit)?
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(if (transaction.isVerified) "Quitar verificación" else "Marcar conciliado") },
            leadingIcon = {
                Icon(
                    imageVector = if (transaction.isVerified) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null
                )
            },
            onClick = {
                onToggleVerify()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Resaltar con color") },
            leadingIcon = { Icon(Icons.Default.FormatColorFill, contentDescription = null) },
            onClick = onOpenColorPicker
        )
        if (transaction.type == TransactionType.EXPENSE && onToggleGastoSupuesto != null) {
            DropdownMenuItem(
                text = { Text(if (isPendingTentative) "Confirmar como real" else "Marcar como supuesto") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isPendingTentative) Icons.Default.CheckCircle else Icons.Default.HelpOutline,
                        contentDescription = null
                    )
                },
                onClick = {
                    onToggleGastoSupuesto()
                    onDismiss()
                }
            )
        }
        if (transaction.isSuspectedDuplicate && onMarkAsLegitimate != null) {
            DropdownMenuItem(
                text = { Text("Confirmar como legítimo") },
                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                onClick = {
                    onMarkAsLegitimate()
                    onDismiss()
                }
            )
        }
        DropdownMenuItem(
            text = { Text("Editar registro") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = {
                onEdit()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Eliminar (con Deshacer)") },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            onClick = {
                onDelete()
                onDismiss()
            }
        )
    }
}

@Composable
private fun TentativeDecisionBanner(
    currencySymbol: String,
    amount: Double,
    isDarkTheme: Boolean,
    onDecideTentative: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkTheme) Color(0xFF2B1F3D) else Color(0xFFFAF5FF),
        border = BorderStroke(1.dp, (if (isDarkTheme) Color(0xFFA78BFA) else TentativePurple).copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = if (isDarkTheme) Color(0xFFD2B6FF) else TentativePurple,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Gasto Supuesto: ¿Aprobar débito de $currencySymbol${"%,.2f".format(amount)}?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color(0xFFE9D5FF) else Color(0xFF581C87),
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onDecideTentative("CONFIRM") },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Aprobar Real",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }

                Button(
                    onClick = { onDecideTentative("DISCARD") },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ahorrar / Descartar",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

