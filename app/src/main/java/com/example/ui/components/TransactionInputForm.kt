package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.DefaultData
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TentativePurple
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionInputForm(
    initialTransaction: TransactionEntity? = null,
    currencySymbol: String = "Q ",
    isAntExpenseInitial: Boolean = false,
    onCancel: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTransaction?.title ?: "") }
    var amountText by remember { mutableStateOf(initialTransaction?.amount?.let { "%.2f".format(it) } ?: "") }
    var type by remember { mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE) }
    var category by remember { mutableStateOf(initialTransaction?.category ?: (if (type == TransactionType.INCOME) "Ingreso" else "Alimentación & Súper")) }
    var familyMember by remember { mutableStateOf(initialTransaction?.familyMember ?: "Todos") }
    var paymentMethod by remember { mutableStateOf(initialTransaction?.paymentMethod ?: "Efectivo") }
    var notes by remember { mutableStateOf(initialTransaction?.notes ?: "") }
    var isAntExpense by remember { mutableStateOf(initialTransaction?.isAntExpense ?: isAntExpenseInitial) }
    var isTentative by remember { mutableStateOf(initialTransaction?.isTentative ?: false) }
    var selectedHighlightHex by remember { mutableLongStateOf(initialTransaction?.highlightColorHex ?: 0L) }
    var dateMillis by remember { mutableLongStateOf(initialTransaction?.dateMillis ?: System.currentTimeMillis()) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var memberDropdownExpanded by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES")) }
    val displayDate = sdf.format(Date(dateMillis))

    val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            cal.set(Calendar.HOUR_OF_DAY, 12)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            dateMillis = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selector Tipo: Ingreso vs Gasto
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = type == TransactionType.EXPENSE,
                onClick = {
                    type = TransactionType.EXPENSE
                    if (category == "Ingreso") category = "Alimentación & Súper"
                },
                label = { Text("Gasto", fontWeight = FontWeight.Bold) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFEE2E2),
                    selectedLabelColor = ExpenseRed
                )
            )

            FilterChip(
                selected = type == TransactionType.INCOME,
                onClick = {
                    type = TransactionType.INCOME
                    category = "Ingreso"
                    isTentative = false
                },
                label = { Text("Ingreso", fontWeight = FontWeight.Bold) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFD1FAE5),
                    selectedLabelColor = IncomeGreen
                )
            )
        }

        // TARJETA DE GASTO SUPUESTO / SIMULACIÓN ("¿Me alcanzará? ¿Compro o no compro?")
        if (type == TransactionType.EXPENSE) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isTentative) Color(0xFFEDE9FE) else Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isTentative) 1.5.dp else 1.dp,
                    color = if (isTentative) TentativePurple else Color(0xFFE2E8F0)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isTentative) TentativePurple else Color(0xFFCBD5E1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Transacción Supuesta (Simulación)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isTentative) TentativePurple else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "¿Dudas si comprar o si te alcanzará? Se mostrará en púrpura y podrás confirmarla o descartarla sumándola como ahorro.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = isTentative,
                        onCheckedChange = { isTentative = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TentativePurple
                        )
                    )
                }
            }
        }

        // Título / Concepto
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Concepto o Título") },
            placeholder = { Text(if (isTentative) "Ej: Smartphone nuevo, Microondas, Zapatos" else "Ej: Supermercado, Salario, Colegio") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Monto
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Monto ($currencySymbol)") },
            placeholder = { Text("0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Categoría (Exposed Dropdown)
        val availableCategories = if (type == TransactionType.INCOME) {
            DefaultData.incomeCategories.map { it.name }
        } else {
            DefaultData.expenseCategories.map { it.name }
        }

        ExposedDropdownMenuBox(
            expanded = categoryDropdownExpanded,
            onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoría") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = categoryDropdownExpanded,
                onDismissRequest = { categoryDropdownExpanded = false }
            ) {
                availableCategories.forEach { catName ->
                    DropdownMenuItem(
                        text = { Text(catName) },
                        onClick = {
                            category = catName
                            categoryDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Miembro de la Familia
        ExposedDropdownMenuBox(
            expanded = memberDropdownExpanded,
            onExpandedChange = { memberDropdownExpanded = !memberDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = familyMember,
                onValueChange = {},
                readOnly = true,
                label = { Text("Asignado a (Miembro Familiar)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = memberDropdownExpanded,
                onDismissRequest = { memberDropdownExpanded = false }
            ) {
                DefaultData.familyMembers.forEach { memberName ->
                    DropdownMenuItem(
                        text = { Text(memberName) },
                        onClick = {
                            familyMember = memberName
                            memberDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Selector de Fecha
        OutlinedTextField(
            value = displayDate,
            onValueChange = {},
            readOnly = true,
            label = { Text("Fecha") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Cambiar fecha",
                    modifier = Modifier.clickable { datePickerDialog.show() }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { datePickerDialog.show() },
            shape = RoundedCornerShape(12.dp)
        )

        // Resaltador de color
        Column {
            Text(
                text = "Color de Resaltado (Opcional):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DefaultData.highlightColors.forEach { opt ->
                    val isSelected = selectedHighlightHex == opt.colorHex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (opt.colorHex == 0L) Color(0xFFF1F5F9) else Color(opt.colorHex))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                                shape = CircleShape
                            )
                            .clickable { selectedHighlightHex = opt.colorHex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Seleccionado",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Notas adicionales
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notas o descripción adicional") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )

        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    val amountVal = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amountVal > 0) {
                        val entity = (initialTransaction ?: TransactionEntity(
                            title = title,
                            amount = amountVal,
                            type = type,
                            category = category,
                            dateMillis = dateMillis
                        )).copy(
                            title = title.trim(),
                            amount = amountVal,
                            type = type,
                            category = category,
                            familyMember = familyMember,
                            paymentMethod = paymentMethod,
                            notes = notes.trim(),
                            isAntExpense = isAntExpense,
                            isTentative = isTentative,
                            tentativeDecision = if (isTentative) (initialTransaction?.tentativeDecision?.ifBlank { "PENDING" } ?: "PENDING") else "",
                            highlightColorHex = selectedHighlightHex,
                            dateMillis = dateMillis
                        )
                        onSave(entity)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTentative) TentativePurple else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (initialTransaction == null) "Guardar" else "Actualizar")
            }
        }
    }
}
