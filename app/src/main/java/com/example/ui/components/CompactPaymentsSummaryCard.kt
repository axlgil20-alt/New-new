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
            1.4.dp,
            if (overdueCount > 0) {
                if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)
            } else {
                if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)
            }
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
                        color = if (isDarkTheme) Color(0xFF3B2810) else Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E),
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
                            color = if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFF92400E),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Total pendiente: ${FormatUtils.formatCurrency(totalPending, currencySymbol, isDiscreetMode)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ir a Pagos",
                    tint = if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFF92400E),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Próximo pago o estado al día
            if (nextPayment != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) Color(0xFF2C221B) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF57402D) else Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Próximo: ${nextPayment.beneficiaryName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Vence el ${dateFormatter.format(Date(nextPayment.dueDateMillis))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        }

                        Text(
                            text = FormatUtils.formatCurrency(nextPayment.amount, currencySymbol, isDiscreetMode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF78350F)
                        )
                    }
                }
            } else {
                Text(
                    text = "¡Todo al día! No tienes pagos pendientes registrados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46),
                    fontWeight = FontWeight.SemiBold
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
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0xFF5C1D24) else Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) Color(0xFFFECACA) else Color(0xFF991B1B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$overdueCount vencido(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFFFECACA) else Color(0xFF991B1B)
                                )
                            }
                        }
                    }

                    if (upcomingCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB))
                        ) {
                            Text(
                                text = "$upcomingCount vencen en 7 días",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFFBFDBFE) else Color(0xFF1E40AF),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
