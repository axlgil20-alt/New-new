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
import com.example.data.model.BudgetLimitEntity
import com.example.data.model.DefaultData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetLimitDialog(
    initialLimit: BudgetLimitEntity? = null,
    currencySymbol: String = "Q ",
    onDismiss: () -> Unit,
    onSave: (category: String, limit: Double, reason: String, tip: String) -> Unit
) {
    var category by remember { mutableStateOf(initialLimit?.category ?: "Comida rápida") }
    var limitText by remember { mutableStateOf(initialLimit?.monthlyLimit?.let { "%.2f".format(it) } ?: "") }
    var reason by remember { mutableStateOf(initialLimit?.reason ?: "") }
    var tip by remember { mutableStateOf(initialLimit?.tipForControl ?: "") }
    var catExpanded by remember { mutableStateOf(false) }

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
                    text = if (initialLimit == null) "Nuevo Límite de Presupuesto" else "Editar Límite de Presupuesto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { if (initialLimit == null) catExpanded = !catExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría a Limitar") },
                        trailingIcon = { if (initialLimit == null) ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (initialLimit == null) {
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
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Límite Mensual Máximo ($currencySymbol)") },
                    placeholder = { Text("Ej: 800.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo / Acuerdo de Familia") },
                    placeholder = { Text("Ej: Evitar fugas en salidas de fin de semana") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = tip,
                    onValueChange = { tip = it },
                    label = { Text("Consejo de control") },
                    placeholder = { Text("Ej: No superar Q150 por quincena") },
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
                            val limitVal = limitText.toDoubleOrNull() ?: 0.0
                            if (category.isNotBlank() && limitVal > 0) {
                                onSave(category, limitVal, reason.trim(), tip.trim())
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = category.isNotBlank() && (limitText.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
