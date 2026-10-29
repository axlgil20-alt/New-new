package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TransactionEntity
import com.example.ui.components.CategoryBreakdownChart
import com.example.ui.components.DashboardVisualChartsCard
import com.example.ui.components.FinancialSummaryCard
import com.example.ui.components.MonthlyExecutiveSummaryCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.QuetziMascotView
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.TentativePurple
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.BudgetUiState

/**
 * InicioScreen - Pantalla Principal de Quetzi con Identidad Guatemalteca
 *
 * Características:
 * 1. Fondo de Tikal y Volcanes de Guatemala: Ilustración panorámica en la cabecera con opacidad elegante
 *    y degradado inferior que ambienta el entorno chapín sin comprometer la legibilidad.
 * 2. Quetzi con Vuelo Libre y Profundo (3D): Despegue desde los volcanes, barrido curvilíneo y posado.
 * 3. Saludo Real de Caricatura: Movimiento activo de manos que sella con pulgar arriba (👍).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    uiState: BudgetUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleAllTime: () -> Unit,
    onToggleIncludeTentative: () -> Unit,
    onToggleDiscreetMode: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onAddAntExpenseClick: () -> Unit,
    onOpenAdvisorClick: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onHighlightTransaction: (TransactionEntity, Long) -> Unit,
    onToggleVerifyTransaction: (TransactionEntity) -> Unit,
    onDecideTentative: (TransactionEntity, String) -> Unit,
    onToggleDarkTheme: () -> Unit = {},
    onOpenSmartCartClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. ILUSTRACIÓN DE FONDO CON VERIFICACIÓN FALLBACK SEGURA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            com.example.ui.components.SafeTikalVolcanoBackground(
                modifier = Modifier.fillMaxSize(),
                alpha = 0.22f
            )

            // Degradado de fusión suave
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar Oficial con Bandera, Marca Quetzi, Controles y Botón "+ Gasto"
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldGreen,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "🇬🇹", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Quetzi",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (uiState.isDarkTheme) Color.White else EmeraldGreen
                            )
                            Text(
                                text = "Tus finanzas sin clavos",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Ojito de Camuflaje: Oculta / Muestra los montos en Quetzales
                    IconButton(
                        onClick = onToggleDiscreetMode,
                        modifier = Modifier.testTag("discreet_mode_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isDiscreetMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (uiState.isDiscreetMode) "Desactivar Modo Discreto" else "Activar Modo Discreto",
                            tint = if (uiState.isDiscreetMode) Color(0xFFF59E0B) else (if (uiState.isDarkTheme) Color.White else EmeraldGreen),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Botón Modo Nocturno
                    IconButton(
                        onClick = onToggleDarkTheme,
                        modifier = Modifier.testTag("dark_mode_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambiar Tema",
                            tint = if (uiState.isDarkTheme) Color(0xFFFDE047) else (if (uiState.isDarkTheme) Color.White else EmeraldGreen),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Botón "+ Gasto" Oficial destacado como en la barra superior de referencia
                    Button(
                        onClick = onAddTransactionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("topbar_add_expense_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gasto",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Selector de Mes
                item {
                    MonthSelectorBar(
                        currentMonthLabel = uiState.currentMonthLabel,
                        isAllTime = uiState.isAllTime,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onToggleAllTime = onToggleAllTime
                    )
                }

                // 2. Mascota Quetzi con Consejos Financieros Dinámicos
                item {
                    QuetziMascotView(
                        isDiscreetMode = uiState.isDiscreetMode,
                        celebrationTrigger = uiState.celebrationTrigger,
                        uiState = uiState
                    )
                }

                // 3. Resumen Mensual Ejecutivo (Salud Financiera & Comparación Ingresos vs Gastos)
                item {
                    MonthlyExecutiveSummaryCard(
                        currentMonthLabel = uiState.currentMonthLabel,
                        totalIncome = uiState.totalIncome,
                        totalExpense = uiState.totalExpense,
                        netBalance = uiState.netBalance,
                        savingsRate = uiState.savingsRate,
                        healthScore = uiState.financialHealthScore,
                        currencySymbol = uiState.currencySymbol,
                        isDiscreetMode = uiState.isDiscreetMode
                    )
                }

                // 4. Tarjeta Resumen Financiero General
                item {
                    FinancialSummaryCard(
                        totalIncome = uiState.totalIncome,
                        totalExpense = uiState.totalExpense,
                        netBalance = uiState.netBalance,
                        confirmedNetBalance = uiState.confirmedNetBalance,
                        simulatedNetBalance = uiState.simulatedNetBalance,
                        tentativeExpensesPending = uiState.tentativeExpensesPending,
                        discardedTentativeSavings = uiState.discardedTentativeSavings,
                        savingsRate = uiState.savingsRate,
                        currencySymbol = uiState.currencySymbol,
                        includeTentativeInBalance = uiState.includeTentativeInBalance,
                        onToggleIncludeTentative = onToggleIncludeTentative,
                        isDiscreetMode = uiState.isDiscreetMode
                    )
                }

                // 5. Alertas de Presupuesto Excedido o Déficit en Simulación
                if (uiState.upcomingDeficitWarnings.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uiState.upcomingDeficitWarnings.forEach { w ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${w.title}: ${uiState.currencySymbol}${"%,.2f".format(w.amount)} • ${w.dueDateMessage}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ExpenseRed,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Gráficos Visuales de Distribución de Gastos
                item {
                    DashboardVisualChartsCard(
                        transactions = uiState.filteredTransactions,
                        currencySymbol = uiState.currencySymbol,
                        isDiscreetMode = uiState.isDiscreetMode
                    )
                }

                // 7. Acciones Rápidas (Registrar Gasto Hormiga y Abrir Asesor IA)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAddAntExpenseClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ Gasto Hormiga",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onOpenAdvisorClick,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Asesor IA",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 7b. Tarjeta Destacada: Smart Cart Quetzi (Lista de Súper & Cotizador Chapín)
                item {
                    val cartCount = uiState.smartCartItems.size
                    val totalCartEst = uiState.smartCartItems.sumOf { it.quantity * it.estimatedUnitPrice }
                    val checkedCount = uiState.smartCartItems.count { it.isChecked }
                    val storeName = uiState.selectedCartStore

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, EmeraldGreen.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { onOpenSmartCartClick() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = EmeraldGreen,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Smart Cart Quetzi",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = storeName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                if (cartCount == 0) {
                                    Text(
                                        text = "Planifica tu súper y ahorra antes de ir a tienda",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "$cartCount artículos • $checkedCount comprados • ${FormatUtils.formatMoney(uiState.currencySymbol, totalCartEst, uiState.isDiscreetMode)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Abrir Smart Cart",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 8. Encabezado de Movimientos Recientes
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Movimientos del Mes (${uiState.filteredTransactions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (uiState.filteredTransactions.isNotEmpty()) {
                            Text(
                                text = "Toca para opciones",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Lista de transacciones recientes
                if (uiState.filteredTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🇬🇹 No hay transacciones registradas este mes.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onAddTransactionClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Agregar Registro")
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = uiState.filteredTransactions.take(8),
                        key = { it.id }
                    ) { tx ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                            TransactionItemCard(
                                transaction = tx,
                                currencySymbol = uiState.currencySymbol,
                                isDiscreetMode = uiState.isDiscreetMode,
                                onEdit = { onEditTransaction(tx) },
                                onDelete = { onDeleteTransaction(tx) },
                                onHighlight = { colorHex -> onHighlightTransaction(tx, colorHex) },
                                onToggleVerify = { onToggleVerifyTransaction(tx) },
                                onDecideTentative = { decision -> onDecideTentative(tx, decision) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}
