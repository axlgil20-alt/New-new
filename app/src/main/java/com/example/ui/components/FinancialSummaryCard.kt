package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TentativePurple
import com.example.ui.util.FormatUtils

/**
 * FinancialSummaryCard - Tarjeta Principal de Resumen Financiero de QUETZI
 *
 * Arquitectura visual:
 * 1. Banner fotográfico independiente en la parte superior con el Lago de Amatitlán y Teleférico,
 *    totalmente separado del contenido de datos.
 * 2. Superficies financieras 100% opacas (alpha = 1.0f) tanto en modo claro como en modo oscuro.
 * 3. Ninguna fotografía, degradado o textura compite con los números, garantizando legibilidad total.
 */
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
    // Superficies financieras 100% sólidas y opacas (alpha = 1.0f) para máxima legibilidad
    val cardSurface = if (isDarkTheme) Color(0xFF201B17) else Color(0xFFFBF6ED)
    val incomeSurface = if (isDarkTheme) Color(0xFF1B291D) else Color(0xFFFFFFFF)
    val expenseSurface = if (isDarkTheme) Color(0xFF331E1C) else Color(0xFFFFFFFF)

    // Colores de contraste para textos e indicadores
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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.2.dp,
            color = if (isDarkTheme) Color(0xFF5A3E26) else Color(0xFFE8D3B7)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. BANNER VISUAL INDEPENDIENTE: Lago de Amatitlán y Teleférico
            // Ubicado como cabecera física separada, sin colocarse detrás de las cifras
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_amatitlan_teleferico_card),
                    contentDescription = "Lago de Amatitlán y Teleférico",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                )

                // Rótulo identificativo sobre el banner
                Surface(
                    color = Color(0xCC000000),
                    shape = RoundedCornerShape(topEnd = 8.dp),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "Lago y Teleférico de Amatitlán",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE68A),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Separador sutil de transición entre el banner fotográfico y el contenido financiero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (isDarkTheme) Color(0xFF5A3E26) else Color(0xFFE8D3B7))
            )

            // 2. PANEL DE DATOS FINANCIEROS (100% Opaco y Sólido)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardSurface)
                    .padding(18.dp)
            ) {
                // Cabecera de la sección financiera con toggle de supuestos
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
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDarkTheme) Color(0xFF925225) else Color(0xFFD97706)
                            ),
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
                                text = "RESUMEN DE CAJA",
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

                // Monto principal grande (100% sobre fondo sólido opaco)
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

                // Tarjetas de Ingresos vs Gastos con fondos 100% sólidos opacos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Ingresos (Superficie completamente opaca)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = incomeSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDarkTheme) Color(0xFF344A36) else Color(0xFFDCD2BD)
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

                    // Gastos (Superficie completamente opaca)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = expenseSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDarkTheme) Color(0xFF4A2B28) else Color(0xFFDCD2BD)
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

                // Barra de progreso de Margen Libre tras Gastos
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
