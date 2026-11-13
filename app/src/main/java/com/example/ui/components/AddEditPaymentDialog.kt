package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BeneficiaryEntity
import com.example.data.model.DefaultData
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.model.PaymentFrequency
import com.example.ui.theme.EmeraldGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPaymentDialog(
    commitment: PaymentCommitmentEntity? = null,
    beneficiaries: List<BeneficiaryEntity>,
    currencySymbol: String = "Q ",
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        beneficiaryName: String,
        description: String,
        amount: Double,
        dueDateMillis: Long,
        category: String,
        frequency: String,
        isVariableAmount: Boolean,
        reminderDaysBefore: Int,
        notes: String
    ) -> Unit
) {
    var beneficiaryName by remember { mutableStateOf(commitment?.beneficiaryName ?: "") }
    var title by remember { mutableStateOf(commitment?.title ?: "") }
    var description by remember { mutableStateOf(commitment?.description ?: "") }
    var amountText by remember { mutableStateOf(commitment?.let { String.format(Locale.US, "%.2f", it.amount) } ?: "") }
    var selectedCategory by remember { mutableStateOf(commitment?.category ?: DefaultData.expenseCategories.first().name) }
    var selectedFrequency by remember { mutableStateOf(commitment?.frequency ?: PaymentFrequency.ONCE) }
    var isVariableAmount by remember { mutableStateOf(commitment?.isVariableAmount ?: false) }
    var reminderDaysBefore by remember { mutableIntStateOf(commitment?.reminderDaysBefore ?: 1) }
    var notes by remember { mutableStateOf(commitment?.notes ?: "") }

    var dueDateMillis by remember {
        mutableStateOf(commitment?.dueDateMillis ?: run {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 3)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        })
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember { SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "GT")) }

    val isDialogDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accentEmeraldText = if (isDialogDark) Color(0xFF6EE7B7) else EmeraldGreen

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = accentEmeraldText,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (commitment == null) "Nuevo Compromiso de Pago" else "Editar Compromiso de Pago",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sugerencias de Beneficiarios Frecuentes
                if (beneficiaries.isNotEmpty()) {
                    Text(
                        text = "Beneficiarios Frecuentes",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentEmeraldText
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(beneficiaries) { ben ->
                            FilterChip(
                                selected = beneficiaryName.equals(ben.name, ignoreCase = true),
                                onClick = {
                                    beneficiaryName = ben.name
                                    if (title.isBlank()) title = "Pago ${ben.name}"
                                    if (ben.defaultDescription.isNotBlank() && description.isBlank()) {
                                        description = ben.defaultDescription
                                    }
                                    if (ben.category.isNotBlank()) {
                                        selectedCategory = ben.category
                                    }
                                },
                                label = { Text(ben.name, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Nombre del Beneficiario o Comercio
                OutlinedTextField(
                    value = beneficiaryName,
                    onValueChange = {
                        beneficiaryName = it
                        if (title.isBlank()) title = "Pago $it"
                        errorMessage = null
                    },
                    label = { Text("Beneficiario o Empresa *") },
                    placeholder = { Text("Ej. EEGSA, Claro, Colegio, Alquiler") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_beneficiary_input")
                )

                // Concepto / Título del compromiso
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("Concepto o Título *") },
                    placeholder = { Text("Ej. Mensualidad luz, Renta casa") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_title_input")
                )

                // Monto en Quetzales
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { c -> c.isDigit() || c == '.' }
                        errorMessage = null
                    },
                    label = { Text("Monto a Pagar ($currencySymbol) *") },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Text(currencySymbol, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                // Selector de Fecha de Vencimiento
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Fecha de Vencimiento",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateFormatter.format(Date(dueDateMillis)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Seleccionar fecha",
                            tint = accentEmeraldText
                        )
                    }
                }

                // Selector de Categoría
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DefaultData.expenseCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat.name
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Frecuencia del Pago
                Column {
                    Text(
                        text = "Frecuencia del Compromiso",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentEmeraldText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val frequencies = listOf(
                            PaymentFrequency.ONCE to "Único",
                            PaymentFrequency.MONTHLY to "Mensual",
                            PaymentFrequency.BIWEEKLY to "Quincenal",
                            PaymentFrequency.WEEKLY to "Semanal",
                            PaymentFrequency.CUSTOM to "Personalizada"
                        )
                        items(frequencies) { (freq, label) ->
                            FilterChip(
                                selected = selectedFrequency == freq,
                                onClick = { selectedFrequency = freq },
                                label = { Text(label) },
                                leadingIcon = if (freq != PaymentFrequency.ONCE) {
                                    { Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Recordatorio de Vencimiento
                Column {
                    Text(
                        text = "Recordatorio",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentEmeraldText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val reminders = listOf(
                            0 to "Mismo día",
                            1 to "1 día antes",
                            2 to "2 días antes",
                            3 to "3 días antes",
                            -1 to "Sin aviso"
                        )
                        items(reminders) { (days, label) ->
                            FilterChip(
                                selected = reminderDaysBefore == days,
                                onClick = { reminderDaysBefore = days },
                                label = { Text(label) },
                                leadingIcon = if (days >= 0) {
                                    { Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGreen,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Toggle Monto Variable
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monto Variable",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Para servicios cuyo cobro cambia cada período (ej. luz, agua)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isVariableAmount,
                        onCheckedChange = { isVariableAmount = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldGreen
                        )
                    )
                }

                // Descripción / Referencia (Número de Contador / Contrato)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("No. Contador / Contrato / Referencia") },
                    placeholder = { Text("Ej. Contador #892348, Cuota 3/12") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Notas Adicionales
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas Familiares (Opcional)") },
                    placeholder = { Text("Instrucciones para pagar, enlace web, etc.") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Mensaje de Error
                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val benClean = beneficiaryName.trim()
                    val titleClean = title.trim()
                    val amt = amountText.toDoubleOrNull() ?: 0.0

                    if (benClean.isBlank()) {
                        errorMessage = "Por favor ingresa el nombre del beneficiario o empresa."
                        return@Button
                    }
                    if (titleClean.isBlank()) {
                        errorMessage = "Por favor ingresa un concepto para el pago."
                        return@Button
                    }
                    if (amt <= 0.0 && !isVariableAmount) {
                        errorMessage = "Ingresa un monto válido mayor a cero."
                        return@Button
                    }

                    onSave(
                        titleClean,
                        benClean,
                        description.trim(),
                        amt,
                        dueDateMillis,
                        selectedCategory,
                        selectedFrequency,
                        isVariableAmount,
                        reminderDaysBefore,
                        notes.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("save_payment_commitment_button")
            ) {
                Text("Guardar Pago", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancelar")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = it
                                set(Calendar.HOUR_OF_DAY, 12)
                            }
                            dueDateMillis = cal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
