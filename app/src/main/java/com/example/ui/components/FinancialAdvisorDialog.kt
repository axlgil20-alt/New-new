package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.BasicAlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.FinancialAdvisorEngine
import com.example.ui.viewmodel.BudgetUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AdvisorMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Asesor Financiero IA Oficial de Quetzi (FinancialAdvisorDialog.kt)
 *
 * Características:
 * - Chat conversacional inteligente con personalidad chapina amigable.
 * - Buscador superior con ícono de lupa (🔍) para filtrar preguntas y buscar dudas en tiempo real.
 * - Conexión directa a la base de datos de Room a través de BudgetUiState:
 *   Conoce el saldo real, ingresos, gastos, gastos hormiga, límites excedidos y metas de ahorro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAdvisorDialog(
    uiState: BudgetUiState,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    // Calcular datos financieros reales de la base de datos
    val antSum = remember(uiState.filteredTransactions) {
        uiState.filteredTransactions.filter { it.isAntExpense }.sumOf { it.amount }
    }
    val antPct = if (uiState.totalIncome > 0) (antSum / uiState.totalIncome) * 100 else 0.0
    val overruns = remember(uiState.budgetLimits, uiState.categoryStats) {
        uiState.budgetLimits.filter { limit ->
            val spent = uiState.categoryStats.find { it.category == limit.category }?.totalAmount ?: 0.0
            spent > limit.monthlyLimit
        }.map { it.category }
    }

    val welcomeText = remember(uiState.totalIncome, uiState.totalExpense, uiState.totalDbTransactionsCount, uiState.currentMonthLabel) {
        if (uiState.totalIncome == 0.0 && uiState.totalExpense == 0.0 && uiState.totalDbTransactionsCount > 0) {
            "¡Qué onda chapín! 🇬🇹 Soy Quetzi, tu Asesor Financiero con IA.\n\n" +
                    "En ${uiState.currentMonthLabel} no tienes movimientos registrados aún, pero detecté ${uiState.totalDbTransactionsCount} transacciones en tu historial general.\n\n" +
                    "¿En qué te puedo apoyar hoy? Puedes preguntarme por tu margen de ahorro, presupuesto restante o saldo disponible."
        } else if (uiState.totalIncome == 0.0 && uiState.totalExpense == 0.0) {
            "¡Qué onda chapín! 🇬🇹 Soy Quetzi, tu Asesor Financiero con IA.\n\n" +
                    "Aún no tienes movimientos registrados. Registra tus ingresos y gastos con el botón (+) o importa tu archivo Excel para comenzar.\n\n" +
                    "¿Tienes dudas sobre cómo presupuestar o usar las herramientas? ¡Escríbeme o pulsa las sugerencias de abajo!"
        } else {
            "¡Qué onda chapín! 🇬🇹 Soy Quetzi, tu Asesor Financiero con IA.\n\n" +
                    "Ya revisé tus finanzas de ${uiState.currentMonthLabel}:\n" +
                    "• Saldo Disponible: ${uiState.currencySymbol}${"%,.2f".format(uiState.netBalance)}\n" +
                    "• Ingresos: ${uiState.currencySymbol}${"%,.2f".format(uiState.totalIncome)}\n" +
                    "• Gastos: ${uiState.currencySymbol}${"%,.2f".format(uiState.totalExpense)}\n" +
                    "• Salud Financiera: ${uiState.financialHealthScore}% (${if (uiState.financialHealthScore >= 70) "¡Chilero!" else "¡A socarse el cincho!"})\n\n" +
                    "¿En qué te puedo apoyar hoy? Escribe tu duda o usa la lupa 🔍 para buscar."
        }
    }

    // Historial de mensajes conversacionales
    val messages = remember {
        mutableStateListOf(
            AdvisorMessage(
                id = "welcome",
                isUser = false,
                text = welcomeText
            )
        )
    }

    // Sugerencias rápidas de preguntas
    val quickQuestions = FinancialAdvisorEngine.QUICK_QUESTIONS

    // Filtrar sugerencias según el buscador de la lupa
    val filteredSuggestions = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            quickQuestions
        } else {
            quickQuestions.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Función inteligente de respuesta basada en los datos de la base de datos
    fun generateAnswer(userQuery: String) {
        val responseText = FinancialAdvisorEngine.generateAdvisorResponse(userQuery, uiState)

        coroutineScope.launch {
            isTyping = true
            delay(500)
            isTyping = false
            messages.add(AdvisorMessage(id = System.currentTimeMillis().toString(), isUser = false, text = responseText))
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun submitUserMessage(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        keyboardController?.hide()
        focusManager.clearFocus()
        messages.add(AdvisorMessage(id = System.currentTimeMillis().toString(), isUser = true, text = clean))
        inputText = ""
        searchQuery = ""
        coroutineScope.launch {
            delay(80)
            listState.animateScrollToItem(messages.size - 1)
        }
        generateAnswer(clean)
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .testTag("financial_advisor_dialog"),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Cabecera con Marca Quetzi IA
                Surface(
                    color = EmeraldGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.quetzi_mascot),
                                contentDescription = "Quetzi Avatar",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Quetzi Asesor IA",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE047),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Conectado a tu base de datos • En vivo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD1FAE5)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_advisor_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White
                            )
                        }
                    }
                }

                // 2. Buscador Superior con Ícono de Lupa (🔍)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar duda o tema (ej. saldo, tortillas, ahorro)...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar tema",
                                tint = EmeraldGreen
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = EmeraldGreen,
                            unfocusedIndicatorColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("advisor_search_bar")
                    )

                    // Carrusel horizontal de Sugerencias / Preguntas frecuentes
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredSuggestions.forEach { question ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE6F4EA),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB7E1CD)),
                                modifier = Modifier.clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    submitUserMessage(question.substringAfter(" "))
                                }
                            ) {
                                Text(
                                    text = question,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F5132),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Lista de Mensajes del Chat Conversacional
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubbleItem(msg = msg)
                    }

                    if (isTyping) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Quetzi está analizando tus registros...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }

                // 4. Barra Inferior de Entrada de Texto
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Escribe tu pregunta o duda financiera...", fontSize = 13.sp) },
                            singleLine = false,
                            maxLines = 3,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("advisor_input_field"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                submitUserMessage(inputText)
                            },
                            enabled = inputText.isNotBlank(),
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank()) EmeraldGreen else Color(0xFFE2E8F0))
                                .testTag("advisor_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = if (inputText.isNotBlank()) Color.White else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(msg: AdvisorMessage) {
    val isUser = msg.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Image(
                painter = painterResource(id = R.drawable.quetzi_mascot),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .align(Alignment.Top),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) EmeraldGreen else Color(0xFFF1F5F9),
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!isUser) {
                    Text(
                        text = "Quetzi Asesor",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else Color(0xFF1E293B),
                    lineHeight = 20.sp
                )
            }
        }
    }
}
