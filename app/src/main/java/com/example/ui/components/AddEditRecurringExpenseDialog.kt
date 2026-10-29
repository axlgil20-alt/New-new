package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.DefaultData
import com.example.data.model.RecurringExpenseEntity
import com.example.data.model.RecurringFrequency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringExpenseDialog(
    initialExpense: RecurringExpenseEntity? = null,
    currencySymbol: String = "Q ",
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: String, frequency: String, member: String, paymentMethod: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(initialExpense?.title ?: "") }
    var amountText by remember { mutableStateOf(initialExpense?.amount?.let { "%.2f".format(it) } ?: "") }
    var category by remember { mutableStateOf(initialExpense?.category ?: "Esposa desde octubre") }
    var frequency by remember { mutableStateOf(initialExpense?.frequency ?: RecurringFrequency.BIWEEKLY) }
    var member by remember { mutableStateOf(initialExpense?.familyMember ?: "Todos") }
    var notes by remember { mutableStateOf(initialExpense?.notes ?: "") }

    var catExpanded by remember { mutableStateOf(false) }
    var freqExpanded by remember { mutableStateOf(false) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (initialExpense == null) "Programar Gasto Recurrente" else "Editar Gasto Recurrente",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre del compromiso") },
                    placeholder = { Text("Ej: Esposa quincenal, Pasajes, Colegio") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Monto ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Frecuencia
                ExposedDropdownMenuBox(
                    expanded = freqExpanded,
                    onExpandedChange = { freqExpanded = !freqExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val freqLabel = when (frequency) {
                        RecurringFrequency.BIWEEKLY -> "Quincenal (Cada quincena)"
                        RecurringFrequency.MONTHLY_15TH -> "Mensual (Día 15)"
                        RecurringFrequency.MONTHLY_END -> "Mensual (Fin de mes)"
                        else -> "Mensual"
                    }
                    OutlinedTextField(
                        value = freqLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frecuencia de Pago") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = freqExpanded,
                        onDismissRequest = { freqExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Quincenal (Cada quincena)") },
                            onClick = {
                                frequency = RecurringFrequency.BIWEEKLY
                                freqExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mensual (Día 15)") },
                            onClick = {
                                frequency = RecurringFrequency.MONTHLY_15TH
                                freqExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mensual (Fin de mes)") },
                            onClick = {
                                frequency = RecurringFrequency.MONTHLY_END
                                freqExpanded = false
                            }
                        )
                    }
                }

                // Categoría
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        DefaultData.expenseCategories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    category = c.name
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amt > 0) {
                                onSave(title.trim(), amt, category, frequency, member, "Efectivo", notes.trim())
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
