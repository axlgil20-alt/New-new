package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TentativePurple
import com.example.ui.util.FormatUtils

@Composable
fun FinancialSummaryCard(
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double,
    confirmedNetBalance: Double,
    simulatedNetBalance: Double,
    tentativeExpensesPending: Double,
    discardedTentativeSavings: Double,
    savingsRate: Double,
    currencySymbol: String,
    includeTentativeInBalance: Boolean,
    onToggleIncludeTentative: () -> Unit,
    isDiscreetMode: Boolean = false,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardSurface = if (isDarkTheme) Color(0xFF201B17) else Color(0xFFFBF6ED)
    val incomeSurface = if (isDarkTheme) Color(0xFF1B291D).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.85f)
    val expenseSurface = if (isDarkTheme) Color(0xFF331E1C).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.85f)
    val positiveText = if (isDarkTheme) Color(0xFFA8D5A0) else Color(0xFF144D1E)
    val negativeText = if (isDarkTheme) Color(0xFFFF9A91) else Color(0xFFB91C1C)
    val tentativeText = if (isDarkTheme) Color(0xFFD2B6FF) else TentativePurple
    val cautionText = if (isDarkTheme) Color(0xFFFFC078) else Color(0xFF9A4D00)
    val cautionIndicator = if (isDarkTheme) Color(0xFFF5A753) else Color(0xFFE67E22)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.2.dp,
            color = if (isDarkTheme) Color(0xFF5A3E26) else Color(0xFFD9A05B).copy(alpha = 0.65f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Fondo fotográfico del Lago de Amatitlán y Teleférico
            Image(
                painter = painterResource(id = R.drawable.img_amatitlan_teleferico_card),
                contentDescription = "Lago de Amatitlán y Teleférico",
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(if (isDarkTheme) 0.68f else 0.74f)
            )

            // Capa de degradado sutil con tonos artesanales cálidos que garantiza 100% de legibilidad financiera
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDarkTheme) {
                                listOf(
                                    cardSurface.copy(alpha = 0.66f),
                                    cardSurface.copy(alpha = 0.42f),
                                    cardSurface.copy(alpha = 0.68f)
                                )
                            } else {
                                listOf(
                                    cardSurface.copy(alpha = 0.58f),
                                    cardSurface.copy(alpha = 0.35f),
                                    cardSurface.copy(alpha = 0.60f)
                                )
                            }
                        )
                    )
            )

            Column(modifier = Modifier.padding(20.dp)) {
                // Cabecera con identidad del Lago y Teleférico de Amatitlán y toggle de supuestos
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
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDarkTheme) Color(0xFF422814) else Color(0xFFFEF3C7),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Color(0xFF925225) else Color(0xFFD97706)),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RESUMEN DE CAJA · LAGO Y TELEFÉRICO DE AMATITLÁN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309),
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (includeTentativeInBalance) "Saldo Disponible (Con Supuestos)" else "Saldo Neto Confirmado",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (includeTentativeInBalance) tentativeText else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = if (includeTentativeInBalance) "Supuestos ON" else "Supuestos OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (includeTentativeInBalance) tentativeText else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = includeTentativeInBalance,
                            onCheckedChange = { onToggleIncludeTentative() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (isDarkTheme) Color(0xFF7B58B5) else TentativePurple,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = if (isDarkTheme) Color(0xFF596356) else Color.LightGray
                            )
                        )
                    }
                }

            Spacer(modifier = Modifier.height(12.dp))

            // Monto principal grande
            val isPositive = netBalance >= 0
            val balanceColor = if (isPositive) positiveText else negativeText

            Text(
                text = FormatUtils.formatCurrency(netBalance, currencySymbol, isDiscreetMode),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = balanceColor,
                letterSpacing = (-0.5).sp
            )

            // Comparativa de Confirmado vs Supuesto si hay compras supuestas
            if (tentativeExpensesPending > 0 || discardedTentativeSavings > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real confirmado: ${FormatUtils.formatCurrency(confirmedNetBalance, currencySymbol, isDiscreetMode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tentativeExpensesPending > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0xFF3B2E4F) else Color(0xFFEDE9FE)
                        ) {
                            Text(
                                text = "Supuestos: -${FormatUtils.formatCurrency(tentativeExpensesPending, currencySymbol, isDiscreetMode)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = tentativeText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (discardedTentativeSavings > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0xFF263629) else Color(0xFFEAF2E5)
                ) {
                    Text(
                        text = "¡Ahorraste ${FormatUtils.formatCurrency(discardedTentativeSavings, currencySymbol, isDiscreetMode)} al descartar compras supuestas!",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDarkTheme) Color(0xFFA8D5A0) else Color(0xFF2D5A27),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjetas de Ingresos vs Gastos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ingresos
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = incomeSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDarkTheme) Color(0xFF344A36).copy(alpha = 0.7f) else Color(0xFFDCD2BD).copy(alpha = 0.8f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(IncomeGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Ingreso",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Ingresos",
                                style = MaterialTheme.typography.labelSmall,
                                color = positiveText
                            )
                            Text(
                                text = FormatUtils.formatCurrency(totalIncome, currencySymbol, isDiscreetMode),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = positiveText
                            )
                        }
                    }
                }

                // Gastos
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = expenseSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDarkTheme) Color(0xFF4A2B28).copy(alpha = 0.7f) else Color(0xFFDCD2BD).copy(alpha = 0.8f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Gasto",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gastos",
                                style = MaterialTheme.typography.labelSmall,
                                color = negativeText
                            )
                            Text(
                                text = FormatUtils.formatCurrency(totalExpense, currencySymbol, isDiscreetMode),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = negativeText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra de progreso de Tasa de Ahorro / Excedente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Margen Libre tras Gastos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${"%.1f".format(savingsRate)}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (savingsRate >= 15) positiveText else if (savingsRate >= 0) cautionText else negativeText
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val progressValue = ((savingsRate / 100.0).toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (savingsRate >= 15) positiveText else if (savingsRate >= 0) cautionIndicator else negativeText,
                trackColor = if (isDarkTheme) Color(0xFF435143) else Color(0xFFE2E8D8)
            )
        }
    }
}
}

