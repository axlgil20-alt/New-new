package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.IncomeGreen
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CuchubalParticipant(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val turnNumber: Int,
    val phone: String = "",
    val hasPaidCurrentRound: Boolean = false,
    val hasCollectedPot: Boolean = false,
    val notes: String = ""
)

/**
 * Módulo Oficial: El Cuchubal Chapín (CuchubalModule.kt)
 *
 * Características:
 * - Gestión completa de grupos de ahorro tradicional guatemalteco (cuchubales).
 * - Registro de participantes con número de turno asignado, monto y frecuencia.
 * - Indicadores visuales de quién ya pagó en la ronda y quién ya cobró el pozo.
 * - Envío de recordatorios por WhatsApp a participantes.
 * - Botón para ingresar el cobro del pozo directamente al presupuesto como Ingreso.
 * - Persistencia local automática en el teléfono.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuchubalModule(
    currencySymbol: String = "Q ",
    onDismiss: () -> Unit,
    onSaveIncomeToBudget: (title: String, amountGtq: Double, note: String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("quetzi_cuchubal_prefs", Context.MODE_PRIVATE) }

    var groupName by remember { mutableStateOf("Cuchubal Familiar Navideño") }
    var amountPerTurn by remember { mutableStateOf(200.0) }
    var frequency by remember { mutableStateOf("QUINCENAL") } // "QUINCENAL" o "MENSUAL"
    var currentRound by remember { mutableIntStateOf(1) }

    val participants = remember { mutableStateListOf<CuchubalParticipant>() }

    var showAddParticipantDialog by remember { mutableStateOf(false) }
    var showEditConfigDialog by remember { mutableStateOf(false) }

    // Cargar datos guardados
    fun loadSavedCuchubal() {
        val savedJson = prefs.getString("current_cuchubal_json", null)
        if (savedJson != null) {
            try {
                val obj = JSONObject(savedJson)
                groupName = obj.optString("groupName", "Cuchubal Familiar Navideño")
                amountPerTurn = obj.optDouble("amountPerTurn", 200.0)
                frequency = obj.optString("frequency", "QUINCENAL")
                currentRound = obj.optInt("currentRound", 1)

                val arr = obj.optJSONArray("participants") ?: JSONArray()
                participants.clear()
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    participants.add(
                        CuchubalParticipant(
                            id = p.optString("id", UUID.randomUUID().toString()),
                            name = p.optString("name", "Participante"),
                            turnNumber = p.optInt("turnNumber", i + 1),
                            phone = p.optString("phone", ""),
                            hasPaidCurrentRound = p.optBoolean("hasPaidCurrentRound", false),
                            hasCollectedPot = p.optBoolean("hasCollectedPot", false),
                            notes = p.optString("notes", "")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Inicializar con participantes sugeridos
            participants.clear()
            participants.addAll(
                listOf(
                    CuchubalParticipant(name = "1. Mamá / Doña Lucre", turnNumber = 1, hasPaidCurrentRound = true, hasCollectedPot = true),
                    CuchubalParticipant(name = "2. Yo (Organizador)", turnNumber = 2, hasPaidCurrentRound = true, hasCollectedPot = false),
                    CuchubalParticipant(name = "3. Carlos (Hermano)", turnNumber = 3, hasPaidCurrentRound = false, hasCollectedPot = false),
                    CuchubalParticipant(name = "4. Tía Marta", turnNumber = 4, hasPaidCurrentRound = false, hasCollectedPot = false),
                    CuchubalParticipant(name = "5. Primo Juanjo", turnNumber = 5, hasPaidCurrentRound = false, hasCollectedPot = false)
                )
            )
        }
    }

    // Guardar datos en SharedPreferences
    fun persistCuchubal() {
        try {
            val obj = JSONObject().apply {
                put("groupName", groupName)
                put("amountPerTurn", amountPerTurn)
                put("frequency", frequency)
                put("currentRound", currentRound)
                val arr = JSONArray()
                participants.forEach { p ->
                    arr.put(
                        JSONObject().apply {
                            put("id", p.id)
                            put("name", p.name)
                            put("turnNumber", p.turnNumber)
                            put("phone", p.phone)
                            put("hasPaidCurrentRound", p.hasPaidCurrentRound)
                            put("hasCollectedPot", p.hasCollectedPot)
                            put("notes", p.notes)
                        }
                    )
                }
                put("participants", arr)
            }
            prefs.edit().putString("current_cuchubal_json", obj.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        loadSavedCuchubal()
    }

    // Cálculos del Pozo Total
    val totalPot = amountPerTurn * participants.size
    val paidCount = participants.count { it.hasPaidCurrentRound }
    val collectedCount = participants.count { it.hasCollectedPot }
    val totalCollectedSoFar = paidCount * amountPerTurn

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cuchubal_module_dialog"),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Cabecera Oficial Chapina
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
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "El Cuchubal Chapín",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "$groupName • $frequency",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD1FAE5)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showEditConfigDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White)
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tarjeta Principal del Pozo y Estado de la Ronda
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "BOLSA DEL TURNO (EL POZO)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "$currencySymbol${"%,.2f".format(totalPot)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Black,
                                            color = IncomeGreen
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFEF3C7),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                                    ) {
                                        Text(
                                            text = "Ronda #$currentRound",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Cuota por turno: $currencySymbol${"%,.2f".format(amountPerTurn)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Pagados: $paidCount de ${participants.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (paidCount == participants.size) IncomeGreen else Color(0xFFD97706),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Acciones Rápidas del Cuchubal
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddParticipantDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Añadir", fontSize = 12.sp)
                            }

                            // Reiniciar pagos para la nueva quincena
                            OutlinedButton(
                                onClick = {
                                    val updated = participants.map { it.copy(hasPaidCurrentRound = false) }
                                    participants.clear()
                                    participants.addAll(updated)
                                    currentRound++
                                    persistCuchubal()
                                    Toast.makeText(context, "¡Ronda #$currentRound iniciada! Pagos reseteados a pendiente.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nueva Ronda", fontSize = 12.sp)
                            }
                        }
                    }

                    // Título de Participantes
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Participantes y Turnos (${participants.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Toca para marcar pago",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }

                    // Lista de Participantes
                    items(participants.sortedBy { it.turnNumber }, key = { it.id }) { p ->
                        ParticipantCardItem(
                            participant = p,
                            currencySymbol = currencySymbol,
                            amountPerTurn = amountPerTurn,
                            groupName = groupName,
                            currentRound = currentRound,
                            onTogglePaid = {
                                val idx = participants.indexOfFirst { it.id == p.id }
                                if (idx != -1) {
                                    participants[idx] = p.copy(hasPaidCurrentRound = !p.hasPaidCurrentRound)
                                    persistCuchubal()
                                }
                            },
                            onToggleCollected = {
                                val idx = participants.indexOfFirst { it.id == p.id }
                                if (idx != -1) {
                                    participants[idx] = p.copy(hasCollectedPot = !p.hasCollectedPot)
                                    persistCuchubal()
                                }
                            },
                            onDelete = {
                                participants.removeAll { it.id == p.id }
                                persistCuchubal()
                            }
                        )
                    }
                }

                // Barra Inferior: Cobrar el Pozo al Presupuesto de Quetzi
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Button(
                            onClick = {
                                val title = "Cobro de Cuchubal: $groupName (Ronda $currentRound)"
                                val note = "Pozo de $currencySymbol${"%,.2f".format(totalPot)} recibido de ${participants.size} participantes."
                                onSaveIncomeToBudget(title, totalPot, note)
                                onDismiss()
                            },
                            enabled = totalPot > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("collect_cuchubal_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cobrar Pozo ($currencySymbol${"%,.2f".format(totalPot)}) a mi Presupuesto",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo para Agregar Participante
    if (showAddParticipantDialog) {
        var newName by remember { mutableStateOf("") }
        var newTurn by remember { mutableIntStateOf(participants.size + 1) }
        var newPhone by remember { mutableStateOf("") }

        BasicAlertDialog(onDismissRequest = { showAddParticipantDialog = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Añadir al Cuchubal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre del Participante") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newTurn.toString(),
                        onValueChange = { newTurn = it.toIntOrNull() ?: 1 },
                        label = { Text("Número de Turno asignado") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Teléfono / WhatsApp (Opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showAddParticipantDialog = false }) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    participants.add(
                                        CuchubalParticipant(
                                            name = newName.trim(),
                                            turnNumber = newTurn,
                                            phone = newPhone.trim()
                                        )
                                    )
                                    persistCuchubal()
                                    showAddParticipantDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }

    // Diálogo para Configurar Cuchubal
    if (showEditConfigDialog) {
        var editName by remember { mutableStateOf(groupName) }
        var editAmount by remember { mutableStateOf(amountPerTurn.toString()) }
        var editFreq by remember { mutableStateOf(frequency) }

        BasicAlertDialog(onDismissRequest = { showEditConfigDialog = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Configurar Cuchubal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nombre del Grupo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("Monto de Cuota por Turno (Q)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = editFreq == "QUINCENAL",
                            onClick = { editFreq = "QUINCENAL" },
                            label = { Text("Quincenal (Día 15 y 30)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = editFreq == "MENSUAL",
                            onClick = { editFreq = "MENSUAL" },
                            label = { Text("Mensual") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showEditConfigDialog = false }) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                groupName = editName.trim()
                                amountPerTurn = editAmount.toDoubleOrNull() ?: 200.0
                                frequency = editFreq
                                persistCuchubal()
                                showEditConfigDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Text("Guardar Cambios")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantCardItem(
    participant: CuchubalParticipant,
    currencySymbol: String,
    amountPerTurn: Double,
    groupName: String,
    currentRound: Int,
    onTogglePaid: () -> Unit,
    onToggleCollected: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (participant.hasPaidCurrentRound) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Turn Number Badge
                    Surface(
                        shape = CircleShape,
                        color = if (participant.hasCollectedPot) Color(0xFFFDE68A) else Color(0xFFE2E8F0),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#${participant.turnNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (participant.hasCollectedPot) Color(0xFFB45309) else Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = participant.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Turno #${participant.turnNumber} • Cuota: $currencySymbol${"%,.2f".format(amountPerTurn)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botón Recordatorio WhatsApp
                    IconButton(
                        onClick = {
                            val msg = "¡Qué onda ${participant.name}! Te recuerdo tu cuota de $currencySymbol${"%,.2f".format(amountPerTurn)} para la ronda #$currentRound del cuchubal '$groupName'. ¡Saludos y gracias por estar al día! 🇬🇹"
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(msg)}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp no disponible, compartiendo mensaje...", Toast.LENGTH_SHORT).show()
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(share, "Recordatorio de Cuchubal"))
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Recordatorio",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Indicadores Visuales de Estado: Pagó / Cobró el Pozo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Indicador de Pago de esta ronda
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (participant.hasPaidCurrentRound) Color(0xFFD1FAE5) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (participant.hasPaidCurrentRound) Color(0xFF6EE7B7) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTogglePaid() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (participant.hasPaidCurrentRound) Icons.Default.CheckCircle else Icons.Default.PendingActions,
                            contentDescription = null,
                            tint = if (participant.hasPaidCurrentRound) IncomeGreen else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (participant.hasPaidCurrentRound) "Ya Pagó 👍" else "Pendiente",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (participant.hasPaidCurrentRound) Color(0xFF065F46) else Color.Gray
                        )
                    }
                }

                // Indicador de Turno Cobrado
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (participant.hasCollectedPot) Color(0xFFFEF3C7) else Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (participant.hasCollectedPot) Color(0xFFFDE68A) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleCollected() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (participant.hasCollectedPot) Icons.Default.Star else Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = if (participant.hasCollectedPot) Color(0xFFD97706) else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (participant.hasCollectedPot) "¡Cobró el Pozo! 🎉" else "Turno pendiente",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (participant.hasCollectedPot) Color(0xFF92400E) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}
