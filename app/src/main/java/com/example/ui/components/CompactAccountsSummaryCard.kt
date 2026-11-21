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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MoneyMinorUnits
import com.example.ui.theme.EmeraldGreen
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.AccountsUiState

@Composable
fun CompactAccountsSummaryCard(
    accountsState: AccountsUiState,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean,
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) Color(0xFF13221C) else Color(0xFFF6FBF9)
    val cardBorder = if (isDarkTheme) Color(0xFF10B981) else Color(0xFF059669)
    val primaryText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentEmerald = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)

    val liquidityDisplay = FormatUtils.formatCurrency(
        MoneyMinorUnits.toDisplayDouble(accountsState.totalLiquidAssetsMinor),
        currencySymbol,
        isDiscreetMode
    )
    val debtDisplay = FormatUtils.formatCurrency(
        MoneyMinorUnits.toDisplayDouble(accountsState.totalCreditCardDebtMinor),
        currencySymbol,
        isDiscreetMode
    )
    val netWorthDisplay = FormatUtils.formatCurrency(
        MoneyMinorUnits.toDisplayDouble(accountsState.netWorthMinor),
        currencySymbol,
        isDiscreetMode
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.4.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenAccounts() }
            .testTag("home_accounts_summary_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = accentEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MIS CUENTAS Y PATRIMONIO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentEmerald,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (accountsState.activeAccounts.isEmpty()) {
                                "Configura tus cuentas y tarjetas"
                            } else {
                                "${accountsState.activeAccounts.size} cuenta(s) activa(s) · ${accountsState.creditCardAccounts.size} tarjeta(s)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryText,
                            maxLines = 2
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ir a Mis Cuentas",
                    tint = accentEmerald,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (accountsState.activeAccounts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Administra efectivo, bancos, cooperativas, tarjetas de crédito y Visacuotas en un solo lugar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = primaryText,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountsMetricTile(
                        label = "Liquidez",
                        value = liquidityDisplay,
                        containerColor = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                        borderColor = if (isDarkTheme) Color(0xFF34D399) else EmeraldGreen,
                        labelColor = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46),
                        valueColor = if (isDarkTheme) Color(0xFFECFDF5) else Color(0xFF022C22),
                        modifier = Modifier.weight(1f)
                    )
                    AccountsMetricTile(
                        label = "Deuda Tarjetas",
                        value = debtDisplay,
                        containerColor = if (isDarkTheme) Color(0xFF4C1D24) else Color(0xFFFEE2E2),
                        borderColor = if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626),
                        labelColor = if (isDarkTheme) Color(0xFFFECACA) else Color(0xFF991B1B),
                        valueColor = if (isDarkTheme) Color(0xFFFFF1F2) else Color(0xFF7F1D1D),
                        modifier = Modifier.weight(1f)
                    )
                    AccountsMetricTile(
                        label = "Patrimonio Neto",
                        value = netWorthDisplay,
                        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                        borderColor = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8),
                        labelColor = secondaryText,
                        valueColor = primaryText,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (accountsState.unassignedTransactionsCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkTheme) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDarkTheme) Color(0xFFBFDBFE) else Color(0xFF1E40AF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${accountsState.unassignedTransactionsCount} movimiento(s) sin cuenta asignada · Toca para vincular",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFFDBEAFE) else Color(0xFF1E3A8A),
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountsMetricTile(
    label: String,
    value: String,
    containerColor: Color,
    borderColor: Color,
    labelColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 2
            )
        }
    }
}
