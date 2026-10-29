package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DefaultData
import com.example.ui.components.AguinaldoCalculatorDialog
import com.example.ui.components.CuchubalModule
import com.example.ui.components.ExcelCsvImporter
import com.example.ui.components.FinancialAdvisorDialog
import com.example.ui.components.HeaderVolcanoBackground
import com.example.ui.components.IsrCalculatorDialog
import com.example.ui.components.RemittanceCalculator
import com.example.ui.components.SmartSupermarketModule
import com.example.ui.components.TermsAndPrivacyDialog
import com.example.ui.components.ThemePickerDialog
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.BudgetUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsAndToolsScreen(
    uiState: BudgetUiState,
    onOpenAdvisor: () -> Unit,
    onSetTheme: (Int) -> Unit,
    onSetCurrency: (String) -> Unit,
    onImportCsv: (String) -> Unit,
    onExportCsv: suspend () -> String,
    onClearAllData: () -> Unit,
    onSaveIncomeToBudget: (title: String, amountGtq: Double, note: String) -> Unit = { _, _, _ -> },
    onSaveExpenseToBudget: (title: String, amountGtq: Double, category: String, note: String) -> Unit = { _, _, _, _ -> },
    onToggleDarkTheme: () -> Unit = {},
    onOpenQuincenas: () -> Unit = {},
    onOpenSmartCart: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showCsvDialog by remember { mutableStateOf(false) }
    var showAdvisorDialog by remember { mutableStateOf(false) }
    var showCuchubalDialog by remember { mutableStateOf(false) }
    var showRemittanceDialog by remember { mutableStateOf(false) }
    var showAguinaldoDialog by remember { mutableStateOf(false) }
    var showIsrDialog by remember { mutableStateOf(false) }
    var showFeedbackModal by remember { mutableStateOf(false) }
    var showLegalModal by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showAguinaldoDialog) {
        AguinaldoCalculatorDialog(
            currencySymbol = uiState.currencySymbol,
            isDiscreetMode = uiState.isDiscreetMode,
            onDismiss = { showAguinaldoDialog = false },
            onSaveAsIncome = onSaveIncomeToBudget
        )
    }

    if (showCuchubalDialog) {
        CuchubalModule(
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showCuchubalDialog = false },
            onSaveIncomeToBudget = onSaveIncomeToBudget
        )
    }

    if (showRemittanceDialog) {
        RemittanceCalculator(
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showRemittanceDialog = false },
            onSaveAsIncome = onSaveIncomeToBudget
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentThemeIndex = uiState.selectedThemeIndex,
            onDismiss = { showThemeDialog = false },
            onSelectTheme = { index -> onSetTheme(index) }
        )
    }

    if (showCsvDialog) {
        ExcelCsvImporter(
            initialExportMode = false,
            onDismiss = { showCsvDialog = false },
            onImportText = { text: String -> onImportCsv(text) },
            onExportRequest = onExportCsv
        )
    }

    if (showIsrDialog) {
        IsrCalculatorDialog(
            currencySymbol = uiState.currencySymbol,
            isDiscreetMode = uiState.isDiscreetMode,
            onDismiss = { showIsrDialog = false },
            onSaveAsExpense = { title, amt, note ->
                onSaveExpenseToBudget(title, amt, "Impuestos & SAT", note)
            }
        )
    }

    if (showAdvisorDialog) {
        FinancialAdvisorDialog(
            uiState = uiState,
            onDismiss = { showAdvisorDialog = false }
        )
    }

    // Modal de Feedback
    if (showFeedbackModal) {
        BasicAlertDialog(onDismissRequest = { showFeedbackModal = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF065F46).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shadowElevation = 12.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Buzón de Comentarios",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7)
                        )
                        IconButton(onClick = { showFeedbackModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.LightGray)
                        }
                    }

                    Text(
                        text = "Tu mensaje se enviará directamente a marvg9559@gmail.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = feedbackMessage,
                        onValueChange = { feedbackMessage = it },
                        placeholder = { Text("Escribe tus comentarios o sugerencias aquí...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF021B16),
                            unfocusedContainerColor = Color(0xFF021B16),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = EmeraldGreen,
                            unfocusedIndicatorColor = Color(0xFF065F46).copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:marvg9559@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Feedback App Quetzi")
                                putExtra(Intent.EXTRA_TEXT, feedbackMessage)
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, "Enviar Feedback con..."))
                            } catch (_: Exception) {
                                Toast.makeText(context, "No se encontró cliente de correo en el dispositivo", Toast.LENGTH_SHORT).show()
                            }
                            showFeedbackModal = false
                            feedbackMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar Correo", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Modal de Términos y Privacidad (Fase 2)
    if (showLegalModal) {
        TermsAndPrivacyDialog(
            onAccept = { showLegalModal = false },
            onDismiss = { showLegalModal = false }
        )
    }

    // Modal de confirmación para limpiar base de datos
    if (showClearConfirmDialog) {
        BasicAlertDialog(onDismissRequest = { showClearConfirmDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "¿Limpiar toda la base de datos?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Esta acción eliminará todas las transacciones, metas y límites registrados. ¿Deseas continuar?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onClearAllData()
                                showClearConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("Limpiar Todo", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HeaderVolcanoBackground(height = 180.dp, alpha = 0.20f)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Banner Esmeralda Oscuro
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF065F46).copy(alpha = 0.45f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF022C22),
                                        Color(0xFF0F172A),
                                        Color(0xFF064E3B)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(
                                text = "Herramientas y Ajustes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Personalización, gestión chapina y soporte de Quetzi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            // Modo Nocturno (Dark Mode) en contenedor degradado esmeralda
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF065F46).copy(alpha = 0.35f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF022C22),
                                        Color(0xFF0F172A),
                                        Color(0xFF022C22)
                                    )
                                )
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (uiState.isDarkTheme) Color(0xFF6366F1).copy(alpha = 0.20f)
                                        else Color(0xFFF59E0B).copy(alpha = 0.20f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = "Modo Nocturno",
                                    tint = if (uiState.isDarkTheme) Color(0xFF818CF8) else Color(0xFFFBBF24),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Modo Nocturno (Dark Mode)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (uiState.isDarkTheme) "Tema oscuro activo para proteger la vista" else "Tema claro activo para máxima nitidez",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isDarkTheme,
                            onCheckedChange = { onToggleDarkTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }
                }
            }

            // 1. Asesor Financiero Familiar (IA)
            item {
                ToolActionCard(
                    title = "Asesor Financiero Familiar (IA)",
                    subtitle = "Chat conversacional inteligente con diagnóstico de salud financiera",
                    icon = Icons.Default.AutoAwesome,
                    iconColor = Color(0xFF10B981),
                    onClick = { showAdvisorDialog = true }
                )
            }

            // 2. Control de Quincenas (Planificado vs Real)
            item {
                ToolActionCard(
                    title = "Control de Quincenas (Planificado vs Real)",
                    subtitle = "Comparativa quincenal de presupuestos asignados contra gastos ejecutados",
                    icon = Icons.Default.CalendarMonth,
                    iconColor = Color(0xFF0284C7),
                    onClick = onOpenQuincenas
                )
            }

            // 3. El Cuchubal Chapín
            item {
                ToolActionCard(
                    title = "El Cuchubal Chapín (Ahorro en Grupo)",
                    subtitle = "Grupos de ahorro tradicional, asignación de turnos y cobro del pozo",
                    icon = Icons.Default.Group,
                    iconColor = Color(0xFF16A34A),
                    onClick = { showCuchubalDialog = true }
                )
            }

            // 4. Calculadora de Remesas USD ↔ GTQ
            item {
                ToolActionCard(
                    title = "Calculadora de Remesas USD ↔ GTQ",
                    subtitle = "Conversión rápida de dólares a quetzales con guardado directo como ingreso",
                    icon = Icons.Default.CurrencyExchange,
                    iconColor = Color(0xFFD97706),
                    onClick = { showRemittanceDialog = true }
                )
            }

            // 5. Smart Cart Quetzi (Supermercados)
            item {
                ToolActionCard(
                    title = "Smart Cart Quetzi (Supermercados)",
                    subtitle = "Planificador y cotizador con cadenas (Walmart, Paiz, La Torre...), autocompletado y checklist en tienda",
                    icon = Icons.Default.ShoppingCart,
                    iconColor = Color(0xFF10B981),
                    onClick = onOpenSmartCart
                )
            }

            // 6. Calculadora de Aguinaldo (Decreto 76-78)
            item {
                ToolActionCard(
                    title = "Calculadora de Aguinaldo (Decreto 76-78)",
                    subtitle = "Cálculo oficial de aguinaldo completo o proporcional con desglose de cuotas (Dic/Ene)",
                    icon = Icons.Default.Calculate,
                    iconColor = Color(0xFFFDE047),
                    onClick = { showAguinaldoDialog = true }
                )
            }

            // 7. Calculadora de ISR (Tablas SAT D. 10-2012) (Fase 3)
            item {
                ToolActionCard(
                    title = "Calculadora de ISR (Tablas SAT D. 10-2012)",
                    subtitle = "Cálculo en quetzales con tramos del 5% y 7%, deducción Q48,000, IGSS y crédito IVA",
                    icon = Icons.Default.AccountBalance,
                    iconColor = Color(0xFF38BDF8),
                    onClick = { showIsrDialog = true }
                )
            }

            // 5. Importar / Exportar Excel & CSV
            item {
                ToolActionCard(
                    title = "Importar / Exportar Excel & CSV",
                    subtitle = "Abre archivos .csv del teléfono, pega celdas copiadas o exporta reportes",
                    icon = Icons.Default.FileUpload,
                    iconColor = Color(0xFF0D6EFD),
                    onClick = { showCsvDialog = true }
                )
            }

            // 6. Tema Visual
            item {
                val currentTheme = DefaultData.appThemes.getOrNull(uiState.selectedThemeIndex)?.name ?: "Esmeralda Chapín"
                ToolActionCard(
                    title = "Apariencia y Colores ($currentTheme)",
                    subtitle = "Cambia entre Esmeralda Chapín, Azul Clásico, Dorado Ejecutivo y Grafito",
                    icon = Icons.Default.Palette,
                    iconColor = Color(0xFF8B5CF6),
                    onClick = { showThemeDialog = true }
                )
            }

            // 7. Selector de Moneda en contenedor degradado esmeralda
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF065F46).copy(alpha = 0.35f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF022C22),
                                        Color(0xFF0F172A),
                                        Color(0xFF022C22)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Símbolo de Moneda",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Q ", "$ ", "€ ", "S/ ", "L ").forEach { sym ->
                                val isSel = uiState.currencySymbol == sym
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) EmeraldGreen else Color(0xFF1E293B),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSel) Color(0xFF34D399) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSetCurrency(sym) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sym.trim(),
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. BUZÓN DE FEEDBACK (Fase 2)
            item {
                ToolActionCard(
                    title = "Enviar Comentarios",
                    subtitle = "Sugerencias y soporte directo a marvg9559@gmail.com",
                    icon = Icons.Default.Mail,
                    iconColor = Color(0xFF38BDF8),
                    onClick = { showFeedbackModal = true }
                )
            }

            // 9. TÉRMINOS Y PRIVACIDAD (Fase 2)
            item {
                ToolActionCard(
                    title = "Términos y Privacidad",
                    subtitle = "Preparación legal para integración bancaria y offline-first",
                    icon = Icons.Default.Shield,
                    iconColor = Color(0xFF34D399),
                    onClick = { showLegalModal = true }
                )
            }

            // 10. Borrar Todos los Datos (Limpieza / Reset)
            item {
                ToolActionCard(
                    title = "Limpiar Base de Datos (Clean Slate)",
                    subtitle = "Elimina todas las transacciones, metas y presupuestos",
                    icon = Icons.Default.DeleteSweep,
                    iconColor = Color(0xFFEF4444),
                    onClick = { showClearConfirmDialog = true }
                )
            }

            // Firma y Versión de la Aplicación
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Quetzi v1.0",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7)
                        )
                        Text(
                            text = "Tu asistente financiero y control de presupuesto familiar en Quetzales. De chapines para chapines, sin hacerte bolas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MarvGr Studios® - Todos los derechos reservados",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun ToolActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF065F46).copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF022C22),
                            Color(0xFF0F172A),
                            Color(0xFF022C22)
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
