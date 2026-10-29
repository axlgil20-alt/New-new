package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.ui.components.AdvisorMessage
import com.example.ui.components.HeaderVolcanoBackground
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.FinancialAdvisorEngine
import com.example.ui.viewmodel.BudgetUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * FinancialAdvisorScreen - Pantalla Dedicada para el Asesor Financiero IA (Pestaña Asesor)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAdvisorScreen(
    uiState: BudgetUiState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

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

    val messages = remember {
        mutableStateListOf(
            AdvisorMessage(
                id = "welcome",
                isUser = false,
                text = welcomeText
            )
        )
    }

    val quickQuestions = FinancialAdvisorEngine.QUICK_QUESTIONS

    val filteredSuggestions = remember(searchQuery) {
        if (searchQuery.isBlank()) quickQuestions
        else quickQuestions.filter { it.contains(searchQuery, ignoreCase = true) }
    }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HeaderVolcanoBackground(height = 180.dp, alpha = 0.20f)

        Column(modifier = Modifier.fillMaxSize()) {
            // Cabecera Oficial del Asesor IA
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.quetzi_mascot),
                            contentDescription = "Quetzi Avatar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Asesor Quetzi IA",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Salud Financiera: ${uiState.financialHealthScore}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = IncomeGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            // Buscador de Consultas (🔍)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar duda o tema...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = EmeraldGreen)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = EmeraldGreen,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            // Chips con preguntas frecuentes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredSuggestions.forEach { question ->
                    FilterChip(
                        selected = false,
                        onClick = { submitUserMessage(question.substringAfter(" ")) },
                        label = { Text(question, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            }

            // Lista de Mensajes del Chat
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(msg)
                }

                if (isTyping) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = "Quetzi está analizando tus datos... ✨",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(76.dp))
                }
            }

            // Barra inferior de Entrada de Texto (con ocultación garantizada de teclado)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 76.dp)
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
                        placeholder = { Text("Pregúntale a Quetzi...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("advisor_input_field"),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = EmeraldGreen,
                            unfocusedIndicatorColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            submitUserMessage(inputText)
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                            .testTag("advisor_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: AdvisorMessage) {
    val isUser = msg.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Image(
                painter = painterResource(id = R.drawable.quetzi_mascot),
                contentDescription = "Quetzi",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
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
            color = if (isUser) EmeraldGreen else MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = msg.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
