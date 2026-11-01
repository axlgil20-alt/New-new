package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.FormatUtils

@Composable
fun MonthlyExecutiveSummaryCard(
    currentMonthLabel: String,
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double,
    savingsRate: Double,
    healthScore: Int,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val warmSurface = if (isDarkTheme) Color(0xFF1E261D) else Color(0xFFFFF9E6)
    val insetSurface = if (isDarkTheme) Color(0xFF293629) else Color(0xFFFFF9E6)
    val softTrack = if (isDarkTheme) Color(0xFF435143) else Color(0xFFE2E8D8)
    val incomeText = if (isDarkTheme) Color(0xFFA8D5A0) else Color(0xFF2D5A27)
    val expenseText = if (isDarkTheme) Color(0xFFFF9A91) else Color(0xFFD9534F)
    val cautionIndicator = if (isDarkTheme) Color(0xFFF5A753) else Color(0xFFE67E22)
    val netText = if (netBalance >= 0) incomeText else expenseText
    val animatedProgress by animateFloatAsState(
        targetValue = (healthScore / 100f).coerceIn(0f, 1f),
        animationSpec = tween(1200),
        label = "health_progress"
    )

    val (scoreColor, scoreLabel, scoreDesc) = when {
        healthScore >= 80 -> Triple(
            incomeText,
            "Excelente",
            "¡Tus finanzas van sobre ruedas! Gran margen de ahorro en Quetzales."
        )
        healthScore >= 55 -> Triple(
            if (isDarkTheme) Color(0xFF8BB9FF) else Color(0xFF2563A6),
            "Saludable",
            "Balance positivo. Continúa cuidando los gastos hormiga diarios."
        )
        healthScore >= 35 -> Triple(
            if (isDarkTheme) Color(0xFFFFC078) else Color(0xFF9A4D00),
            "Atención",
            "Tus gastos consumen la mayor parte de tus ingresos. Modera compras extras."
        )
        else -> Triple(
            expenseText,
            "Alerta",
            "Gastos elevados en el periodo. Revisa tus límites y recorta compras."
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("monthly_executive_summary_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = warmSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Color(0xFF435143) else Color(0xFFEADBCE))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Cabecera: Título y Badge del Mes
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isDarkTheme) Color(0xFF344433) else Color(0xFFE5EEDB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = if (isDarkTheme) incomeText else EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Resumen Mensual Ejecutivo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                        text = "Salud y balance financiero de $currentMonthLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = scoreColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = scoreLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                        modifier = Modifier.widthIn(min = 62.dp).padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sección de Indicador de Salud Financiera y Comparación Rápida
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(insetSurface)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Medidor Circular de Salud Financiera (%)
                Box(
                    modifier = Modifier.size(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(76.dp),
                        color = scoreColor,
                        trackColor = softTrack,
                        strokeWidth = 7.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$healthScore%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = scoreColor
                        )
                        Text(
                        text = "Salud",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Mensaje Ejecutivo y Diagnóstico
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Diagnóstico del Periodo",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = scoreDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    if (savingsRate > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🪙 Tasa de ahorro actual: ${"%.1f".format(savingsRate)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = incomeText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Comparativa Visual de Ingresos vs Gastos (Barra Proporcional)
            val expenseRatio = if (totalIncome > 0) (totalExpense / totalIncome).toFloat().coerceIn(0f, 1f) else if (totalExpense > 0) 1f else 0f
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Consumo de Ingresos (${"%.0f".format(expenseRatio * 100)}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (expenseRatio < 1f) "Disponible: ${"%.0f".format((1f - expenseRatio) * 100)}%" else "Sin excedente",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (expenseRatio < 0.7f) incomeText else expenseText
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { expenseRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (expenseRatio > 0.85f) expenseText else if (expenseRatio > 0.6f) cautionIndicator else incomeText,
                    trackColor = softTrack
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ingresos y gastos en paralelo; saldo neto a ancho completo para evitar recortes.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ingresos
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) Color(0xFF263629) else Color(0xFFEAF2E5)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = incomeText, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Ingresos", style = MaterialTheme.typography.labelSmall, color = incomeText)
                        }
                        Text(
                            text = FormatUtils.formatCurrency(totalIncome, currencySymbol, isDiscreetMode),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = incomeText,
                            maxLines = 1
                        )
                    }
                }

                // Gastos
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) Color(0xFF3A2927) else Color(0xFFFCE8E4)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = expenseText, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Gastos", style = MaterialTheme.typography.labelSmall, color = expenseText)
                        }
                        Text(
                            text = FormatUtils.formatCurrency(totalExpense, currencySymbol, isDiscreetMode),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = expenseText,
                            maxLines = 1
                        )
                    }
                }

            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkTheme) Color(0xFF344433) else Color(0xFFE5EEDB)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = netText, modifier = Modifier.size(18.dp))
                        Text("Neto", style = MaterialTheme.typography.titleSmall, color = netText, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = FormatUtils.formatCurrency(netBalance, currencySymbol, isDiscreetMode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = netText,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
