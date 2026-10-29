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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DefaultData
import com.example.data.model.HighlightColorOption
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TentativeIndigo
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
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }

    val isIncome = transaction.type == TransactionType.INCOME
    val isTentative = transaction.isTentative
    val decision = transaction.tentativeDecision
    val isDiscarded = isTentative && decision == "DISCARDED"
    val isPendingTentative = isTentative && (decision.isBlank() || decision == "PENDING")

    // Colores según estado
    val baseCardColor = when {
        isDiscarded -> Color(0xFFF0FDF4) // Verde muy claro de dinero ahorrado
        isPendingTentative -> Color(0xFFFAF5FF) // Púrpura/Lavanda claro
        transaction.highlightColorHex != 0L -> Color(transaction.highlightColorHex).copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    val animatedCardColor by animateColorAsState(targetValue = baseCardColor, label = "cardColor")

    val borderColor = when {
        isDiscarded -> Color(0xFF86EFAC)
        isPendingTentative -> TentativePurple
        transaction.isVerified -> Color(0xFF93C5FD)
        else -> Color(0xFFE2E8F0)
    }

    val amountColor = when {
        isDiscarded -> IncomeGreen // El dinero no gastado es ahorro positivo
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
                width = if (isPendingTentative) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = animatedCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPendingTentative) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Fila superior: Categoría, Badges (Supuesto, Hormiga, Verificado) y Acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val icon = CategoryIconHelper.getIconForCategory(transaction.category)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isDiscarded -> Color(0xFFDCFCE7)
                                    isPendingTentative -> Color(0xFFEDE9FE)
                                    isIncome -> Color(0xFFD1FAE5)
                                    else -> Color(0xFFFEE2E2)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = transaction.category,
                            modifier = Modifier.size(16.dp),
                            tint = when {
                                isDiscarded -> IncomeGreen
                                isPendingTentative -> TentativePurple
                                isIncome -> IncomeGreen
                                else -> ExpenseRed
                            }
                        )
                    }

                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Badge de Gasto Supuesto
                    if (isPendingTentative) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TentativePurple
                        ) {
                            Text(
                                text = "¿Compro o No?",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isDiscarded) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldGreen
                        ) {
                            Text(
                                text = "Ahorrado (+)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

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

                // Acciones rápidas (Marcador, Verificado, Editar, Eliminar)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Checkbox de conciliado
                    IconButton(
                        onClick = onToggleVerify,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (transaction.isVerified) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (transaction.isVerified) "Verificado" else "Marcar Revisado",
                            tint = if (transaction.isVerified) Color(0xFF2563EB) else Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Menú de colores de subrayado
                    Box {
                        IconButton(
                            onClick = { showColorPicker = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatColorFill,
                                contentDescription = "Resaltar con color",
                                tint = if (transaction.highlightColorHex != 0L) Color(transaction.highlightColorHex) else Color.Gray,
                                modifier = Modifier.size(18.dp)
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

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color.Gray,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Título y Monto Principal
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
                        text = "$dateText • ${transaction.familyMember}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isDiscreetMode) "Q ****.**" else "$amountPrefix${"%,.2f".format(transaction.amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
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

            // BOTONES DE DECISIÓN PARA GASTOS SUPUESTOS (Solicitud directa del usuario)
            if (isPendingTentative) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEDE9FE)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "¿Vas a realizar este gasto de $currencySymbol${"%,.2f".format(transaction.amount)}?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4C1D95)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Opción 1: Confirmar compra (Resta del presupuesto)
                            Button(
                                onClick = { onDecideTentative("CONFIRM") },
                                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Comprar (Resta)", style = MaterialTheme.typography.labelSmall)
                            }

                            // Opción 2: Descartar / No comprar (Suma como dinero no gastado / ahorro)
                            Button(
                                onClick = { onDecideTentative("DISCARD") },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ahorrar (+ Suma)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
