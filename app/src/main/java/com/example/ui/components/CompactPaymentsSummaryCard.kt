package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentCommitmentEntity
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CompactPaymentsSummaryCard(
    totalPending: Double,
    upcomingCount: Int,
    overdueCount: Int,
    nextPayment: PaymentCommitmentEntity?,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean,
    onOpenPayments: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("d 'de' MMM", Locale("es", "GT")) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF221A15) else Color(0xFFFFFDF8)
        ),
        border = BorderStroke(
            1.2.dp,
            if (overdueCount > 0) ExpenseRed.copy(alpha = 0.8f) else Color(0xFFD97706).copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenPayments() }
            .testTag("home_payments_summary_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera del resumen compacto
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFD97706).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFD97706)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "COMPROMISOS DE PAGO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Total pendiente: ${FormatUtils.formatCurrency(totalPending, currencySymbol, isDiscreetMode)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ir a Pagos",
                    tint = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Próximo pago o estado al día
            if (nextPayment != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) Color(0xFF2C221B) else Color(0xFFFEF3C7).copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Próximo: ${nextPayment.beneficiaryName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Vence el ${dateFormatter.format(Date(nextPayment.dueDateMillis))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = FormatUtils.formatCurrency(nextPayment.amount, currencySymbol, isDiscreetMode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E)
                        )
                    }
                }
            } else {
                Text(
                    text = "¡Todo al día! No tienes pagos pendientes registrados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            // Indicadores de Próximos y Vencidos
            if (upcomingCount > 0 || overdueCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (overdueCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ExpenseRed.copy(alpha = 0.15f),
                            border = BorderStroke(0.8.dp, ExpenseRed)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$overdueCount vencido(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }

                    if (upcomingCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                            border = BorderStroke(0.8.dp, Color(0xFF2563EB))
                        ) {
                            Text(
                                text = "$upcomingCount vencen en 7 días",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
