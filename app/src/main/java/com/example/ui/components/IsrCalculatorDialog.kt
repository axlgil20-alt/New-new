package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.util.FormatUtils
import com.example.ui.util.IsrCalculator

/**
 * Diálogo Interactivo de la Calculadora de ISR según Tablas Oficiales de la SAT (Guatemala)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsrCalculatorDialog(
    currencySymbol: String = "Q",
    isDiscreetMode: Boolean = false,
    onDismiss: () -> Unit,
    onSaveAsExpense: ((title: String, amount: Double, note: String) -> Unit)? = null
) {
    var salaryText by remember { mutableStateOf("") }
    var bonusText by remember { mutableStateOf("") }
    var ivaCreditText by remember { mutableStateOf("") }

    val monthlySalary = remember(salaryText) {
        val s = salaryText.replace(",", ".").trim()
        val num = s.toDoubleOrNull() ?: 0.0
        if (num < 0.0) 0.0 else num
    }

    val monthlyBonus = remember(bonusText) {
        val s = bonusText.replace(",", ".").trim()
        val num = s.toDoubleOrNull() ?: 0.0
        if (num < 0.0) 0.0 else num
    }

    val ivaCredit = remember(ivaCreditText) {
        val s = ivaCreditText.replace(",", ".").trim()
        val num = s.toDoubleOrNull() ?: 0.0
        if (num < 0.0) 0.0 else num
    }

    val isrResult by remember(monthlySalary, monthlyBonus, ivaCredit) {
        derivedStateOf {
            IsrCalculator.calculateIsr(
                monthlySalary = monthlySalary,
                otherMonthlyTaxableIncome = monthlyBonus,
                estimatedIvaCredit = ivaCredit
            )
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.2.dp, EmeraldGreen.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("isr_calculator_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldGreen,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Calculadora de ISR",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tablas Oficiales SAT • D. 10-2012",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Campos de entrada
                OutlinedTextField(
                    value = salaryText,
                    onValueChange = { salaryText = it },
                    label = { Text("Sueldo mensual ordinario") },
                    prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("Ej: 8500.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = bonusText,
                        onValueChange = { bonusText = it },
                        label = { Text("Bonos afectos/mes") },
                        prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = ivaCreditText,
                        onValueChange = { ivaCreditText = it },
                        label = { Text("Crédito IVA anual") },
                        prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("Máx 12,000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta de Resultados Destacados
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Retención Mensual Estimada",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFA7F3D0),
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.20f)
                            ) {
                                Text(
                                    text = "SAT",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val displayMonthly = FormatUtils.formatAmount(
                            amount = isrResult.monthlyRetention,
                            currencySymbol = currencySymbol,
                            isDiscreet = isDiscreetMode
                        )
                        Text(
                            text = displayMonthly,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val displayAnnual = FormatUtils.formatAmount(
                            amount = isrResult.netAnnualTax,
                            currencySymbol = currencySymbol,
                            isDiscreet = isDiscreetMode
                        )
                        Text(
                            text = "Impuesto Anual Total: $displayAnnual • Tasa Efectiva: ${"%.2f".format(isrResult.effectiveRatePercentage)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD1FAE5)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Desglose de Deducciones y Cálculos
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Desglose Legal SAT Guatemala:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Renta Bruta Anual:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                FormatUtils.formatAmount(isrResult.annualGrossSalary, currencySymbol, isDiscreetMode),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Deducción Única Legal (Art. 72):", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "-${FormatUtils.formatAmount(isrResult.annualPersonalDeduction, currencySymbol, isDiscreetMode)}",
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cuota IGSS Laboral (4.83%):", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "-${FormatUtils.formatAmount(isrResult.annualIgssDeduction, currencySymbol, isDiscreetMode)}",
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (isrResult.ivaCreditApplied > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Crédito Fiscal IVA Aplicado:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "-${FormatUtils.formatAmount(isrResult.ivaCreditApplied, currencySymbol, isDiscreetMode)}",
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Renta Imponible Neta:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(
                                FormatUtils.formatAmount(isrResult.taxableIncome, currencySymbol, isDiscreetMode),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cerrar")
                    }

                    if (onSaveAsExpense != null && isrResult.monthlyRetention > 0.0) {
                        Button(
                            onClick = {
                                onSaveAsExpense(
                                    "Retención ISR Mensual",
                                    isrResult.monthlyRetention,
                                    "Retención de ISR proyectada según tablas SAT (${isrResult.bracketDescription})"
                                )
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Guardar Gasto")
                        }
                    }
                }
            }
        }
    }
}
