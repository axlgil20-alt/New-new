package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.util.AguinaldoCalculator
import com.example.ui.util.FormatUtils
import java.util.Calendar

/**
 * Diálogo interactivo: Calculadora Legal de Aguinaldo (Decreto 76-78)
 *
 * Permite a los usuarios de Guatemala calcular su aguinaldo completo o proporcional
 * respetando el período de ley (1 de diciembre al 30 de noviembre), con desglose
 * del 50% en diciembre y 50% en enero, validación de entradas y soporte para modo discreto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AguinaldoCalculatorDialog(
    currencySymbol: String,
    isDiscreetMode: Boolean,
    onDismiss: () -> Unit,
    onSaveAsIncome: ((title: String, amount: Double, note: String) -> Unit)? = null
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    var salaryInput by remember { mutableStateOf("4250.00") }
    var isFullYearSelected by remember { mutableStateOf(true) }

    // Fecha de inicio (por defecto 1 de diciembre del año anterior)
    var startDay by remember { mutableIntStateOf(1) }
    var startMonth by remember { mutableIntStateOf(12) }
    var startYear by remember { mutableIntStateOf(currentYear - 1) }

    // Fecha de corte (por defecto 30 de noviembre del año en curso)
    var endDay by remember { mutableIntStateOf(30) }
    var endMonth by remember { mutableIntStateOf(11) }
    var endYear by remember { mutableIntStateOf(currentYear) }

    // Cálculo reactivo
    val calculationResult = remember(
        salaryInput,
        isFullYearSelected,
        startDay,
        startMonth,
        startYear,
        endDay,
        endMonth,
        endYear
    ) {
        if (isFullYearSelected) {
            AguinaldoCalculator.calculate(
                salaryStr = salaryInput,
                startDay = 1,
                startMonth = 12,
                startYear = endYear - 1,
                endDay = 30,
                endMonth = 11,
                endYear = endYear
            )
        } else {
            AguinaldoCalculator.calculate(
                salaryStr = salaryInput,
                startDay = startDay,
                startMonth = startMonth,
                startYear = startYear,
                endDay = endDay,
                endMonth = endMonth,
                endYear = endYear
            )
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.5.dp, Color(0xFF065F46).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Cabecera con identidad guatemalteca
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF064E3B),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Calculadora de Aguinaldo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "🇬🇹", fontSize = 14.sp)
                            }
                            Text(
                                text = "Decreto 76-78 del Congreso de Guatemala",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6EE7B7)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de Modalidad (Año Completo vs Proporcional)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isFullYearSelected,
                        onClick = { isFullYearSelected = true },
                        label = { Text("Año Completo (100%)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF022C22),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = !isFullYearSelected,
                        onClick = { isFullYearSelected = false },
                        label = { Text("Proporcional", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF022C22),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Campo: Salario Mensual Ordinario
                OutlinedTextField(
                    value = salaryInput,
                    onValueChange = { salaryInput = it },
                    label = { Text("Salario Mensual Ordinario / Promedio") },
                    prefix = { Text("$currencySymbol ", color = Color(0xFF34D399), fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF022C22),
                        unfocusedContainerColor = Color(0xFF022C22),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF6EE7B7),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedIndicatorColor = Color(0xFF34D399),
                        unfocusedIndicatorColor = Color(0xFF065F46)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Fechas personalizadas si es cálculo proporcional
                if (!isFullYearSelected) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF022C22)),
                        border = BorderStroke(1.dp, Color(0xFF065F46).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Fecha de Inicio de Labores en el Ciclo:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Día
                                OutlinedTextField(
                                    value = startDay.toString(),
                                    onValueChange = { startDay = it.toIntOrNull()?.coerceIn(1, 31) ?: 1 },
                                    label = { Text("Día", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                // Mes
                                OutlinedTextField(
                                    value = startMonth.toString(),
                                    onValueChange = { startMonth = it.toIntOrNull()?.coerceIn(1, 12) ?: 1 },
                                    label = { Text("Mes (1-12)", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1.3f)
                                )

                                // Año
                                OutlinedTextField(
                                    value = startYear.toString(),
                                    onValueChange = { startYear = it.toIntOrNull() ?: currentYear },
                                    label = { Text("Año", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1.3f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tarjeta de Resultado en Verde Esmeralda Oscuro de Alto Contraste
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.2.dp, Color(0xFF34D399).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF022C22),
                                        Color(0xFF0F172A),
                                        Color(0xFF064E3B)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            if (!calculationResult.isValid) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = calculationResult.errorMessage ?: "Entrada no válida",
                                        color = Color(0xFFFCA5A5),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text(
                                    text = "MONTO ESTIMADO DE AGUINALDO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp,
                                    color = Color(0xFF94A3B8)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val displayedAmount = if (isDiscreetMode) {
                                    "$currencySymbol ••••••"
                                } else {
                                    "$currencySymbol${"%,.2f".format(calculationResult.totalAguinaldo)}"
                                }

                                Text(
                                    text = displayedAmount,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF6EE7B7) // Verde menta neón
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Desglose de las dos cuotas de ley (50% Diciembre, 50% Enero)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF021C16),
                                        border = BorderStroke(1.dp, Color(0xFF065F46)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "1ª Cuota (Dic):",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFFDE047),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isDiscreetMode) "$currencySymbol ••••" else "$currencySymbol${"%,.2f".format(calculationResult.firstPaymentDecember)}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "1ª quincena dic",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF021C16),
                                        border = BorderStroke(1.dp, Color(0xFF065F46)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "2ª Cuota (Ene):",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFFDE047),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isDiscreetMode) "$currencySymbol ••••" else "$currencySymbol${"%,.2f".format(calculationResult.secondPaymentJanuary)}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "2ª quincena ene",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Detalle de días computados
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0F172A).copy(alpha = 0.8f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = calculationResult.calculationSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Período legal aplicado: ${calculationResult.periodStartFormatted} al ${calculationResult.periodEndFormatted}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nota Legal Informativa
                Text(
                    text = "⚖️ Nota Legal: Cálculo informativo basado en el Decreto 76-78 del Congreso de Guatemala. No sustituye la liquidación oficial de tu empleador.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontStyle = FontStyle.Italic,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Acciones: Botón Guardar en Presupuesto y Cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (calculationResult.isValid && calculationResult.totalAguinaldo > 0.0 && onSaveAsIncome != null) {
                        Button(
                            onClick = {
                                onSaveAsIncome(
                                    "Aguinaldo 2026 (Decreto 76-78)",
                                    calculationResult.totalAguinaldo,
                                    "Proyección de Aguinaldo calculado según Decreto 76-78 (${calculationResult.computedDays} días)."
                                )
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar en Presupuesto", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = if (onSaveAsIncome != null) Modifier.weight(0.7f) else Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
