package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.IncomeGreen

/**
 * Calculadora de Remesas USD ↔ GTQ (RemittanceCalculator.kt)
 *
 * Características:
 * - Conversión rápida bidireccional (Dólares estadounidenses a Quetzales y viceversa).
 * - Tasa de cambio oficial / comercial ajustable (predeterminada en Q7.75 por USD).
 * - Botones de montos predeterminados ($50, $100, $200, $300, $500).
 * - Botón para guardar la remesa directamente como ingreso en el presupuesto familiar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemittanceCalculator(
    currencySymbol: String = "Q ",
    onDismiss: () -> Unit,
    onSaveAsIncome: (title: String, amountGtq: Double, note: String) -> Unit
) {
    var usdAmountText by remember { mutableStateOf("100") }
    var exchangeRateText by remember { mutableStateOf("7.75") }
    var senderNameText by remember { mutableStateOf("Familiar en EE.UU.") }
    var notesText by remember { mutableStateOf("") }
    var isUsdToGtq by remember { mutableStateOf(true) }

    val usdValue = usdAmountText.toDoubleOrNull() ?: 0.0
    val rate = exchangeRateText.toDoubleOrNull() ?: 7.75

    val gtqCalculated = if (isUsdToGtq) {
        usdValue * rate
    } else {
        if (rate > 0) usdValue else 0.0
    }

    val usdEquivalent = if (isUsdToGtq) {
        usdValue
    } else {
        if (rate > 0) usdValue / rate else 0.0
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("remittance_calculator_dialog"),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Cabecera
                Surface(
                    color = EmeraldGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyExchange,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Calculadora de Remesas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Conversión USD ↔ GTQ en tiempo real",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD1FAE5)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Selector de dirección de conversión
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isUsdToGtq) "Dólares a Quetzales (USD → GTQ)" else "Quetzales a Dólares (GTQ → USD)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedButton(
                            onClick = { isUsdToGtq = !isUsdToGtq },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Invertir", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Invertir", fontSize = 12.sp)
                        }
                    }

                    // Campo de Monto a convertir
                    OutlinedTextField(
                        value = usdAmountText,
                        onValueChange = { usdAmountText = it },
                        label = { Text(if (isUsdToGtq) "Monto de la Remesa en USD ($)" else "Monto en Quetzales (Q)") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isUsdToGtq) Icons.Default.AttachMoney else Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = EmeraldGreen
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("remittance_amount_input")
                    )

                    // Presets rápidos de remesas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(50, 100, 200, 300, 500).forEach { preset ->
                            FilterChip(
                                selected = (usdValue.toInt() == preset),
                                onClick = { usdAmountText = preset.toString() },
                                label = { Text("$$preset", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFD1FAE5),
                                    selectedLabelColor = EmeraldGreen
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Tasa de Cambio (Ajustable)
                    OutlinedTextField(
                        value = exchangeRateText,
                        onValueChange = { exchangeRateText = it },
                        label = { Text("Tipo de Cambio (Quetzales por 1 Dólar)") },
                        leadingIcon = {
                            Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = Color.Gray)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tarjeta de Resultado Grande
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "MONTO NETO A RECIBIR EN GUATEMALA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currencySymbol${"%,.2f".format(if (isUsdToGtq) gtqCalculated else usdValue)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = IncomeGreen
                            )
                            Text(
                                text = "Equivalente a $${"%,.2f".format(usdEquivalent)} USD a tasa Q$rate",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    // Quién envía y notas
                    OutlinedTextField(
                        value = senderNameText,
                        onValueChange = { senderNameText = it },
                        label = { Text("¿Quién envía la remisa o concepto?") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notas o destino (ej. víveres, construcción, ahorro)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Botón para Guardar la Remesa Directo en el Presupuesto
                    Button(
                        onClick = {
                            val finalGtq = if (isUsdToGtq) gtqCalculated else usdValue
                            val title = if (senderNameText.isNotBlank()) "Remesa: $senderNameText" else "Remesa Familiar USD"
                            val fullNote = "Remesa $${"%.2f".format(usdEquivalent)} USD @ Q$rate. ${notesText.trim()}".trim()
                            onSaveAsIncome(title, finalGtq, fullNote)
                            onDismiss()
                        },
                        enabled = gtqCalculated > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_remittance_income_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guardar en Presupuesto como Ingreso",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
