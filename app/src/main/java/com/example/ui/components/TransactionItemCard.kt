package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DefaultData
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.isGastoReal
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
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onMarkAsLegitimate: (() -> Unit)? = null,
    onToggleGastoSupuesto: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }

    val isIncome = transaction.type == TransactionType.INCOME
    val isTentative = transaction.isTentative
    val decision = transaction.tentativeDecision
    val isDiscarded = isTentative && decision == "DISCARDED"
    val isPendingTentative = transaction.isGastoSupuesto

    // Colores según estado y selección
    val baseCardColor = when {
        isSelected -> Color(0xFFE0F2FE) // Azul suave de selección
        isDiscarded -> Color(0xFFF0FDF4)
        isPendingTentative -> Color(0xFFFAF5FF)
        transaction.isSuspectedDuplicate -> Color(0xFFFFFBEB) // Fondo advertencia suave para duplicados
        transaction.highlightColorHex != 0L -> Color(transaction.highlightColorHex).copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    val animatedCardColor by animateColorAsState(targetValue = baseCardColor, label = "cardColor")

    val borderColor = when {
        isSelected -> Color(0xFF0284C7)
        transaction.isSuspectedDuplicate -> Color(0xFFF59E0B) // Borde ámbar para duplicado
        isDiscarded -> Color(0xFF86EFAC)
        isPendingTentative -> TentativePurple
        transaction.isVerified -> Color(0xFF93C5FD)
        else -> Color(0xFFE2E8F0)
    }

    val amountColor = when {
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

    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
    val dateText = sdf.format(Date(transaction.dateMillis))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .border(
                width = if (isSelected || transaction.isSuspectedDuplicate || isPendingTentative) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = isSelectionMode) { onToggleSelect() }
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = animatedCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPendingTentative || isSelected) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Fila superior: Checkbox (modo selección), Categoría, Badges y Acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = EmeraldGreen,
                                uncheckedColor = Color.Gray
                            ),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    val icon = CategoryIconHelper.getIconForCategory(transaction.category)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isDiscarded -> Color(0xFFDCFCE7)
                                    isPendingTentative -> Color(0xFFEDE9FE)
                                    isIncome -> Color(0xFFD1FAE5)
                                    transaction.isSuspectedDuplicate -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFFEE2E2)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = transaction.category,
                            modifier = Modifier.size(18.dp),
                            tint = when {
                                isDiscarded -> IncomeGreen
                                isPendingTentative -> TentativePurple
                                isIncome -> IncomeGreen
                                transaction.isSuspectedDuplicate -> Color(0xFFD97706)
                                else -> ExpenseRed
                            }
                        )
                    }

                    Column {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Acciones rápidas (Marcador, Verificado, Editar, Eliminar)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Checkbox de conciliado
                    IconButton(
                        onClick = onToggleVerify,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (transaction.isVerified) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (transaction.isVerified) "Verificado" else "Marcar Revisado",
                            tint = if (transaction.isVerified) Color(0xFF2563EB) else Color.LightGray,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Menú de colores de subrayado
                    Box {
                        IconButton(
                            onClick = { showColorPicker = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatColorFill,
                                contentDescription = "Resaltar con color",
                                tint = if (transaction.highlightColorHex != 0L) Color(transaction.highlightColorHex) else Color.Gray,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showColorPicker,
                            onDismissRequest = { showColorPicker = false }
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
                                        showColorPicker = false
                                    }
                                )
                            }
                        }
                    }

                    if (transaction.type == TransactionType.EXPENSE && onToggleGastoSupuesto != null) {
                        IconButton(
                            onClick = onToggleGastoSupuesto,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isPendingTentative) Icons.Default.CheckCircle else Icons.Default.HelpOutline,
                                contentDescription = if (isPendingTentative) "Confirmar como real" else "Marcar como supuesto",
                                tint = if (isPendingTentative) TentativePurple else Color.Gray,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Título / Establecimiento y Monto Principal (Estilo Simplifi)
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
                        textDecoration = if (isDiscarded) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = "$dateText • ${transaction.familyMember} • ${transaction.paymentMethod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
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
                            color = TentativePurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Fila de Badges y Metadatos (Origen, Estado, Duplicado, Hormiga)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge de Origen (Excel vs Manual)
                if (transaction.source == TransactionSource.IMPORTED) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFD1FAE5)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Excel",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Badge de Estado Contable
                when (transaction.status) {
                    TransactionStatus.PROJECTED -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEDE9FE)
                        ) {
                            Text(
                                text = "Proyectado",
                                style = MaterialTheme.typography.labelSmall,
                                color = TentativePurple,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    TransactionStatus.PENDING -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "Pendiente",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    else -> {}
                }

                // Alerta de Posible Duplicado con resolución directa
                if (transaction.isSuspectedDuplicate) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Posible Duplicado",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    if (onMarkAsLegitimate != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFD1FAE5),
                            modifier = Modifier.clickable { onMarkAsLegitimate() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Confirmar como legítimo",
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Es legítimo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Badge Hormiga
                if (transaction.isAntExpense) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFEDD5)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Gasto Hormiga",
                                tint = Color(0xFFC2410C),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Hormiga",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC2410C),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Notas si existen
            if (transaction.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            // BOTONES DE DECISIÓN PARA GASTOS SUPUESTOS
            if (isPendingTentative) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFAF5FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TentativePurple.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = TentativePurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Gasto Supuesto: ¿Aprobar débito de $currencySymbol${"%,.2f".format(transaction.amount)}?",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF581C87)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onDecideTentative("CONFIRM") },
                                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aprobar Gasto Real", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onDecideTentative("DISCARD") },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ahorrar / Descartar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
