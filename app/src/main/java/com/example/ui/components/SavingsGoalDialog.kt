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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.data.model.SavingsGoalEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalDialog(
    initialGoal: SavingsGoalEntity? = null,
    currencySymbol: String = "Q ",
    onDismiss: () -> Unit,
    onSave: (name: String, target: Double, initial: Double, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(initialGoal?.name ?: "") }
    var targetText by remember { mutableStateOf(initialGoal?.targetAmount?.let { "%.2f".format(it) } ?: "") }
    var initialText by remember { mutableStateOf(initialGoal?.currentAmount?.let { "%.2f".format(it) } ?: "") }
    var notes by remember { mutableStateOf(initialGoal?.notes ?: "") }

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
                    text = if (initialGoal == null) "Nueva Meta de Ahorro" else "Editar Meta de Ahorro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la Meta") },
                    placeholder = { Text("Ej: Fondo de Emergencia, Vacaciones, Aguinaldo") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Monto Objetivo ($currencySymbol)") },
                    placeholder = { Text("Ej: 5000.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = initialText,
                    onValueChange = { initialText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Monto Actual Ahorrado ($currencySymbol)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Propósito o Notas") },
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
                            val targetVal = targetText.toDoubleOrNull() ?: 0.0
                            val initVal = initialText.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && targetVal > 0) {
                                onSave(name.trim(), targetVal, initVal, notes.trim())
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = name.isNotBlank() && (targetText.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
