package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AboutQuetziDialog
import com.example.ui.components.NotificationsSettingsDialog
import com.example.ui.components.PrivacySecurityDialog
import com.example.ui.screens.FinancialAdvisorScreen
import com.example.ui.screens.PaymentsScreen
import com.example.ui.screens.SmartCartScreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseRed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.model.TransactionEntity
import com.example.ui.components.AddAntExpenseDialog
import com.example.ui.components.AddEditRecurringExpenseDialog
import com.example.ui.components.AddEditTransactionDialog
import com.example.ui.components.BudgetLimitDialog
import com.example.ui.components.ChapinQuickFab
import com.example.ui.components.FinancialAdvisorDialog
import com.example.ui.components.HouseholdFinancialAdvisorDialog
import com.example.ui.components.SavingsGoalDialog
import com.example.ui.components.SmartSupermarketModule
import com.example.ui.components.TermsAndPrivacyDialog
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GoalsAndToolsScreen
import com.example.ui.screens.InicioScreen
import com.example.ui.screens.PlannedVsRealScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.PresupuestoTheme
import com.example.ui.util.TermsAcceptanceManager
import com.example.ui.viewmodel.BudgetViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: BudgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()

            var selectedTab by remember { mutableIntStateOf(0) }
            var showAddTransactionDialog by remember { mutableStateOf(false) }
            var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
            var showAddAntExpenseDialog by remember { mutableStateOf(false) }
            var showBudgetLimitDialog by remember { mutableStateOf(false) }
            var budgetLimitBeingEdited by remember { mutableStateOf<com.example.data.model.BudgetLimitEntity?>(null) }
            var showRecurringDialog by remember { mutableStateOf(false) }
            var showSavingsGoalDialog by remember { mutableStateOf(false) }
            var showAdvisorDialog by remember { mutableStateOf(false) }
            var showSmartCartDialog by remember { mutableStateOf(false) }
            var showSplashScreen by remember { mutableStateOf(true) }

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            var showNotificationsDialog by remember { mutableStateOf(false) }
            var showPrivacySecurityDialog by remember { mutableStateOf(false) }
            var showTermsDialog by remember { mutableStateOf(false) }
            var showAboutDialog by remember { mutableStateOf(false) }

            BackHandler(enabled = drawerState.isOpen) {
                coroutineScope.launch { drawerState.close() }
            }

            BackHandler(enabled = !drawerState.isOpen && selectedTab != 0) {
                selectedTab = 0
            }

            val prefs = remember { getSharedPreferences(TermsAcceptanceManager.PREFS_NAME, MODE_PRIVATE) }
            var requiresTermsAcceptance by remember {
                mutableStateOf(TermsAcceptanceManager.requiresAcceptance(prefs))
            }

            // Observar mensajes para mostrar Feedback Toast
            LaunchedEffect(uiState.userMessage) {
                uiState.userMessage?.let { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }

            PresupuestoTheme(
                themeIndex = uiState.selectedThemeIndex,
                isDarkTheme = uiState.isDarkTheme
            ) {
                if (showSplashScreen) {
                    SplashScreen(
                        onContinue = { showSplashScreen = false }
                    )
                } else if (requiresTermsAcceptance) {
                    BackHandler(enabled = true) {
                        // Impide omitir los términos mediante el botón Atrás del sistema
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        TermsAndPrivacyDialog(
                            isMandatory = true,
                            termsVersion = TermsAcceptanceManager.CURRENT_TERMS_VERSION,
                            onAccept = {
                                TermsAcceptanceManager.recordAcceptance(prefs)
                                requiresTermsAcceptance = false
                            },
                            onDeclineAndExit = {
                                finishAffinity()
                            }
                        )
                    }
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = true,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = if (uiState.isDarkTheme) Color(0xFF131D1B) else Color(0xFFF7FAF9),
                                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(320.dp)
                            ) {
                                // Header del Menú Hamburguesa con Identidad Quetzi
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFF022C22),
                                                    Color(0xFF064E3B)
                                                )
                                            )
                                        )
                                        .padding(horizontal = 20.dp, vertical = 24.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = EmeraldGreen,
                                            border = BorderStroke(1.5.dp, Color(0xFF34D399)),
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("🇬🇹", fontSize = 24.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "QUETZI",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = "Tus finanzas sin clavos",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFA7F3D0),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // GRUPO 1: HERRAMIENTAS
                                Text(
                                    text = "HERRAMIENTAS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldGreen,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Límites y Metas", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.PieChart, contentDescription = null, tint = EmeraldGreen) },
                                    selected = selectedTab == 6,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        selectedTab = 6
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Asesor Financiero IA", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                    selected = selectedTab == 3,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        selectedTab = 3
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                // GRUPO 2: CONFIGURACIÓN
                                Text(
                                    text = "CONFIGURACIÓN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldGreen,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Ajustes", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                    selected = selectedTab == 4,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        selectedTab = 4
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Notificaciones", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                    selected = false,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showNotificationsDialog = true
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                // GRUPO 3: INFORMACIÓN
                                Text(
                                    text = "INFORMACIÓN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldGreen,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Privacidad y seguridad", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981)) },
                                    selected = false,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showPrivacySecurityDialog = true
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Términos y condiciones", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF64748B)) },
                                    selected = false,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showTermsDialog = true
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Acerca de QUETZI", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldGreen) },
                                    selected = false,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showAboutDialog = true
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Footer
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 16.dp)
                                ) {
                                    Text(
                                        text = "QUETZI v1.0",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "MarvGr Studios® · Offline-First",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    ) {
                    Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    // FAB Chapín: Conexión real con guardado directo en Room Database y registro completo
                    floatingActionButton = {
                        ChapinQuickFab(
                            onSaveExpense = { title, amt, cat, notes ->
                                viewModel.addChapinQuickExpense(title, amt, cat, notes)
                            },
                            onAddFullTransaction = {
                                editingTransaction = null
                                showAddTransactionDialog = true
                            }
                        )
                    },
                    // Barra de Navegación Inferior Simétrica: EXACTAMENTE 5 OPCIONES
                    // REGISTROS | QUINCENAS | HOY (CENTRO DESTACADO) | PAGOS | SMART CART
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            // 1. REGISTROS
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Registros") },
                                label = { Text("Registros", fontSize = 10.sp, maxLines = 1, softWrap = false) }
                            )

                            // 2. QUINCENAS
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Quincenas") },
                                label = { Text("Quincenas", fontSize = 10.sp, maxLines = 1, softWrap = false) }
                            )

                            // 3. HOY (Centrado y Destacado con relieve visual)
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selectedTab == 0) EmeraldGreen else EmeraldGreen.copy(alpha = 0.15f),
                                        shadowElevation = if (selectedTab == 0) 6.dp else 0.dp,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Home,
                                                contentDescription = "HOY",
                                                tint = if (selectedTab == 0) Color.White else EmeraldGreen,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = "HOY",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (selectedTab == 0) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )

                            // 4. PAGOS (Pestaña principal independiente)
                            NavigationBarItem(
                                selected = selectedTab == 5,
                                onClick = { selectedTab = 5 },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (uiState.overduePaymentsCount > 0) {
                                                Badge(containerColor = ExpenseRed) {
                                                    Text("${uiState.overduePaymentsCount}", fontSize = 9.sp)
                                                }
                                            } else if (uiState.upcomingPaymentsCount > 0) {
                                                Badge(containerColor = Color(0xFF2563EB)) {
                                                    Text("${uiState.upcomingPaymentsCount}", fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = "Pagos")
                                    }
                                },
                                label = {
                                    Text(
                                        text = "Pagos",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (selectedTab == 5) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == 5) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )

                            // 5. SMART CART (Pestaña principal independiente: lista, despensa y plantillas)
                            val pendingCartCount = uiState.smartCartItems.count { !it.isChecked }
                            NavigationBarItem(
                                selected = selectedTab == 7,
                                onClick = { selectedTab = 7 },
                                icon = {
                                    if (pendingCartCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = EmeraldGreen) {
                                                    Text("$pendingCartCount", fontSize = 9.sp, color = Color.White)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = "Smart Cart")
                                        }
                                    } else {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = "Smart Cart")
                                    }
                                },
                                label = {
                                    Text(
                                        text = "Smart Cart",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (selectedTab == 7) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == 7) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (selectedTab) {
                            0 -> InicioScreen(
                                uiState = uiState,
                                onPreviousMonth = { viewModel.previousMonth() },
                                onNextMonth = { viewModel.nextMonth() },
                                onToggleAllTime = { viewModel.toggleAllTime() },
                                onToggleIncludeTentative = { viewModel.toggleIncludeTentative() },
                                onToggleDiscreetMode = { viewModel.toggleDiscreetMode() },
                                onToggleDarkTheme = { viewModel.toggleDarkTheme() },
                                onAddTransactionClick = {
                                    editingTransaction = null
                                    showAddTransactionDialog = true
                                },
                                onAddAntExpenseClick = { showAddAntExpenseDialog = true },
                                onOpenAdvisorClick = { selectedTab = 3 },
                                onEditTransaction = { tx ->
                                    editingTransaction = tx
                                    showAddTransactionDialog = true
                                },
                                onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                                onHighlightTransaction = { tx, hex -> viewModel.highlightTransaction(tx, hex) },
                                onToggleVerifyTransaction = { tx -> viewModel.toggleVerifyTransaction(tx) },
                                onDecideTentative = { tx, decision -> viewModel.decideTentativeTransaction(tx, decision) },
                                onOpenSmartCartClick = { selectedTab = 7 },
                                onOpenPaymentsClick = { selectedTab = 5 },
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                            )

                            1 -> TransactionsScreen(
                                uiState = uiState,
                                onPreviousMonth = { viewModel.previousMonth() },
                                onNextMonth = { viewModel.nextMonth() },
                                onToggleAllTime = { viewModel.toggleAllTime() },
                                onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                                onFilterTypeChange = { t -> viewModel.setFilterType(t) },
                                onFilterCategoryChange = { c -> viewModel.setCategoryFilter(c) },
                                onFilterMemberChange = { m -> viewModel.setMemberFilter(m) },
                                onToggleOnlyHighlighted = { h -> viewModel.setOnlyHighlighted(h) },
                                onAddTransactionClick = {
                                    editingTransaction = null
                                    showAddTransactionDialog = true
                                },
                                onEditTransaction = { tx ->
                                    editingTransaction = tx
                                    showAddTransactionDialog = true
                                },
                                onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                                onHighlightTransaction = { tx, hex -> viewModel.highlightTransaction(tx, hex) },
                                onToggleVerifyTransaction = { tx -> viewModel.toggleVerifyTransaction(tx) },
                                onDecideTentative = { tx, decision -> viewModel.decideTentativeTransaction(tx, decision) },
                                onExportCsv = { viewModel.getCsvExport() },
                                onImportCsv = { text -> viewModel.importCsv(text) },
                                onFilterPeriodChange = { p -> viewModel.setFilterPeriod(p) },
                                onFilterSpecificDateChange = { d -> viewModel.setFilterSpecificDate(d) },
                                onFilterDateRangeChange = { s, e -> viewModel.setFilterDateRange(s, e) },
                                onFilterQuincenaChange = { q -> viewModel.setFilterQuincena(q) },
                                onFilterSubCategoryChange = { sc -> viewModel.setFilterSubCategory(sc) },
                                onFilterBeneficiaryChange = { b -> viewModel.setFilterBeneficiary(b) },
                                onFilterStatusChange = { st -> viewModel.setFilterStatus(st) },
                                onFilterSourceChange = { src -> viewModel.setFilterSource(src) },
                                onFilterDuplicatesModeChange = { m -> viewModel.setFilterDuplicatesMode(m) },
                                onClearAllFilters = { viewModel.clearAllFilters() },
                                onDeleteTransactions = { ids -> viewModel.deleteTransactions(ids) },
                                onMarkTransactionsAsLegitimate = { ids -> viewModel.markTransactionsAsLegitimate(ids) }
                            )

                            2 -> {
                                BackHandler { selectedTab = 4 }
                                PlannedVsRealScreen(
                                    uiState = uiState,
                                    onPreviousMonth = { viewModel.previousMonth() },
                                    onNextMonth = { viewModel.nextMonth() },
                                    onEditTransaction = { tx ->
                                        editingTransaction = tx
                                        showAddTransactionDialog = true
                                    },
                                    onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                                    onHighlightTransaction = { tx, hex -> viewModel.highlightTransaction(tx, hex) },
                                    onToggleVerifyTransaction = { tx -> viewModel.toggleVerifyTransaction(tx) },
                                    onDecideTentative = { tx, decision -> viewModel.decideTentativeTransaction(tx, decision) }
                                )
                            }

                            3 -> {
                                BackHandler { selectedTab = 4 }
                                FinancialAdvisorScreen(
                                    uiState = uiState
                                )
                            }

                            4 -> GoalsAndToolsScreen(
                                uiState = uiState,
                                onOpenAdvisor = { selectedTab = 3 },
                                onSetTheme = { idx -> viewModel.setAppTheme(idx) },
                                onSetCurrency = { sym -> viewModel.setCurrency(sym) },
                                onImportCsv = { text -> viewModel.importCsv(text) },
                                onExportCsv = { viewModel.getCsvExport() },
                                onClearAllData = { viewModel.clearAllData() },
                                onToggleDarkTheme = { viewModel.toggleDarkTheme() },
                                onOpenQuincenas = { selectedTab = 2 },
                                onOpenLimits = { selectedTab = 6 },
                                onOpenSmartCart = { selectedTab = 7 },
                                onSaveIncomeToBudget = { title, amountGtq, note ->
                                    viewModel.addTransaction(
                                        com.example.data.model.TransactionEntity(
                                            title = title,
                                            amount = amountGtq,
                                            type = com.example.data.model.TransactionType.INCOME,
                                            category = "Ingreso Extra / Remesa",
                                            dateMillis = System.currentTimeMillis(),
                                            notes = note
                                        )
                                    )
                                },
                                onSaveExpenseToBudget = { title, amountGtq, category, note ->
                                    viewModel.addTransaction(
                                        com.example.data.model.TransactionEntity(
                                            title = title,
                                            amount = amountGtq,
                                            type = com.example.data.model.TransactionType.EXPENSE,
                                            category = category,
                                            dateMillis = System.currentTimeMillis(),
                                            notes = note
                                        )
                                    )
                                }
                            )

                            5 -> {
                                BackHandler { selectedTab = 0 }
                                PaymentsScreen(
                                    uiState = uiState,
                                    onAddPayment = { title, ben, desc, amt, due, cat, freq, isVar, rem, notes ->
                                        viewModel.addPaymentCommitment(title, ben, desc, amt, due, cat, freq, isVar, rem, notes)
                                    },
                                    onMarkAsPaid = { id -> viewModel.markPaymentAsPaid(id) },
                                    onUpdatePayment = { p -> viewModel.updatePaymentCommitment(p) },
                                    onDeletePayment = { p -> viewModel.deletePaymentCommitment(p) },
                                    onAddBeneficiary = { name, cat, desc, cont, notes ->
                                        viewModel.addBeneficiary(name, cat, desc, cont, notes)
                                    },
                                    onUpdateBeneficiary = { ben -> viewModel.updateBeneficiary(ben) },
                                    onDeleteBeneficiary = { ben -> viewModel.deleteBeneficiary(ben) }
                                )
                            }

                            6 -> {
                                BackHandler { selectedTab = 4 }
                                BudgetsScreen(
                                    uiState = uiState,
                                    onPreviousMonth = { viewModel.previousMonth() },
                                    onNextMonth = { viewModel.nextMonth() },
                                    onToggleAllTime = { viewModel.toggleAllTime() },
                                    onAddBudgetLimitClick = {
                                        budgetLimitBeingEdited = null
                                        showBudgetLimitDialog = true
                                    },
                                    onEditBudgetLimit = { limit ->
                                        budgetLimitBeingEdited = limit
                                        showBudgetLimitDialog = true
                                    },
                                    onDeleteBudgetLimit = { cat -> viewModel.deleteBudgetLimit(cat) },
                                    onAddRecurringExpenseClick = { showRecurringDialog = true },
                                    onDeleteRecurringExpense = { rec -> viewModel.deleteRecurringExpense(rec) },
                                    onToggleRecurringExpense = { rec -> viewModel.toggleRecurringExpenseActive(rec) },
                                    onAddSavingsGoalClick = { showSavingsGoalDialog = true },
                                    onDeleteSavingsGoal = { goal -> viewModel.deleteSavingsGoal(goal) },
                                    onContributeSavingsGoal = { goal, amt -> viewModel.contributeToGoal(goal, amt) }
                                )
                            }

                            7 -> {
                                BackHandler { selectedTab = 0 }
                                SmartCartScreen(
                                    uiState = uiState,
                                    onAddItem = { name, qty, unit, brand, price, store, notes, url ->
                                        viewModel.addSmartCartItem(
                                            name = name,
                                            quantity = qty,
                                            unit = unit,
                                            brand = brand,
                                            price = price,
                                            store = store,
                                            notes = notes,
                                            url = url,
                                            priceSource = "MANUAL"
                                        )
                                    },
                                    onUpdateItem = { item -> viewModel.updateSmartCartItem(item) },
                                    onDeleteItem = { item -> viewModel.deleteSmartCartItem(item) },
                                    onToggleChecked = { item -> viewModel.toggleSmartCartItemChecked(item) },
                                    onSetBudget = { budget -> viewModel.setSmartCartBudget(budget) },
                                    onCheckoutExpense = { title, amount, store, purchasedNames, updatePantry ->
                                        viewModel.checkoutCartAsExpense(title, amount, store, purchasedNames, updatePantry)
                                    },
                                    onAddPantryItem = { name, qty, unit, status, price, store, notes, brand ->
                                        viewModel.addPantryItem(name, qty, unit, status, price, store, notes, brand)
                                    },
                                    onUpdatePantryItem = { item -> viewModel.updatePantryItem(item) },
                                    onTogglePantryStatus = { item -> viewModel.togglePantryStatus(item) },
                                    onDeletePantryItem = { item -> viewModel.deletePantryItem(item) },
                                    onTogglePantryInCart = { item -> viewModel.togglePantryInCart(item) },
                                    onIncorporateNeedToBuy = { viewModel.incorporateNeedToBuy() },
                                    onClearChecked = { viewModel.clearCheckedCartItems() },
                                    onClearAllCart = { viewModel.clearAllCartItems() },
                                    onSaveTemplate = { name, desc -> viewModel.saveCartAsTemplate(name, desc) },
                                    onApplyTemplate = { id -> viewModel.applyTemplateToCart(id) },
                                    onDuplicateTemplate = { id, name -> viewModel.duplicateTemplate(id, name) },
                                    onDeleteTemplate = { template -> viewModel.deleteTemplate(template) },
                                    onAddScannedItems = { items, dest, avoidDup ->
                                        viewModel.addScannedItems(items, dest, avoidDup)
                                    }
                                )
                            }
                        }
                    }

                    // Diálogos modales
                    if (showAddTransactionDialog) {
                        AddEditTransactionDialog(
                            transaction = editingTransaction,
                            currencySymbol = uiState.currencySymbol,
                            onDismiss = {
                                showAddTransactionDialog = false
                                editingTransaction = null
                            },
                            onSave = { tx ->
                                if (editingTransaction == null) {
                                    viewModel.addTransaction(tx)
                                } else {
                                    viewModel.updateTransaction(tx)
                                }
                            }
                        )
                    }

                    if (showAddAntExpenseDialog) {
                        AddAntExpenseDialog(
                            currencySymbol = uiState.currencySymbol,
                            onDismiss = { showAddAntExpenseDialog = false },
                            onSave = { tx -> viewModel.addAntExpense(tx) }
                        )
                    }

                    if (showBudgetLimitDialog) {
                        BudgetLimitDialog(
                            initialLimit = budgetLimitBeingEdited,
                            currencySymbol = "Q ",
                            onDismiss = {
                                showBudgetLimitDialog = false
                                budgetLimitBeingEdited = null
                            },
                            onSave = { cat, limit, reason, tip ->
                                viewModel.setBudgetLimit(cat, limit, reason, tip)
                            }
                        )
                    }

                    if (showRecurringDialog) {
                        AddEditRecurringExpenseDialog(
                            currencySymbol = uiState.currencySymbol,
                            onDismiss = { showRecurringDialog = false },
                            onSave = { title, amt, cat, freq, member, pay, notes ->
                                viewModel.addRecurringExpense(title, amt, cat, freq, member, pay, notes)
                            }
                        )
                    }

                    if (showSavingsGoalDialog) {
                        SavingsGoalDialog(
                            currencySymbol = uiState.currencySymbol,
                            onDismiss = { showSavingsGoalDialog = false },
                            onSave = { name, target, initAmt, notes ->
                                viewModel.addSavingsGoal(name, target, initAmt, notes)
                            }
                        )
                    }

                    if (showAdvisorDialog) {
                        FinancialAdvisorDialog(
                            uiState = uiState,
                            onDismiss = { showAdvisorDialog = false }
                        )
                    }

                    if (showSmartCartDialog) {
                        SmartSupermarketModule(
                            cartItems = uiState.smartCartItems,
                            pantryItems = uiState.smartPantryItems,
                            budgetAmount = uiState.smartCartBudget,
                            selectedStoreName = uiState.selectedCartStore,
                            historicalTransactions = uiState.filteredTransactions,
                            currencySymbol = uiState.currencySymbol,
                            isDiscreetMode = uiState.isDiscreetMode,
                            onDismiss = { showSmartCartDialog = false },
                            onAddItem = { name, qty, unit, brand, price, store, notes, url, source ->
                                viewModel.addSmartCartItem(
                                    name = name,
                                    quantity = qty,
                                    unit = unit,
                                    brand = brand,
                                    price = price,
                                    store = store,
                                    notes = notes,
                                    url = url,
                                    priceSource = source
                                )
                            },
                            onUpdateItem = { item -> viewModel.updateSmartCartItem(item) },
                            onDeleteItem = { item -> viewModel.deleteSmartCartItem(item) },
                            onToggleChecked = { item -> viewModel.toggleSmartCartItemChecked(item) },
                            onSetBudget = { budget -> viewModel.setSmartCartBudget(budget) },
                            onSelectStore = { store -> viewModel.setSelectedCartStore(store) },
                            onClearChecked = { viewModel.clearCheckedCartItems() },
                            onCheckoutExpense = { title, amount, store, purchasedNames ->
                                viewModel.checkoutCartAsExpense(title, amount, store, purchasedNames)
                            },
                            onAddPantryItem = { name, qty, unit, status, price, store, notes ->
                                viewModel.addPantryItem(name, qty, unit, status, price, store, notes)
                            },
                            onUpdatePantryItem = { item -> viewModel.updatePantryItem(item) },
                            onTogglePantryStatus = { item -> viewModel.togglePantryStatus(item) },
                            onDeletePantryItem = { item -> viewModel.deletePantryItem(item) },
                            onGenerateReplenishment = { viewModel.generateReplenishmentList() },
                            onAddScannedItems = { items, destination, avoidDuplicates ->
                                viewModel.addScannedItems(items, destination, avoidDuplicates)
                            }
                        )
                    }

                    // Modales del Menú Lateral
                    if (showNotificationsDialog) {
                        NotificationsSettingsDialog(onDismiss = { showNotificationsDialog = false })
                    }

                    if (showPrivacySecurityDialog) {
                        PrivacySecurityDialog(onDismiss = { showPrivacySecurityDialog = false })
                    }

                    if (showTermsDialog) {
                        TermsAndPrivacyDialog(
                            onAccept = { showTermsDialog = false },
                            onDismiss = { showTermsDialog = false },
                            isMandatory = false,
                            termsVersion = TermsAcceptanceManager.CURRENT_TERMS_VERSION,
                            acceptanceRecord = TermsAcceptanceManager.getAcceptanceRecord(prefs)
                        )
                    }

                    if (showAboutDialog) {
                        AboutQuetziDialog(onDismiss = { showAboutDialog = false })
                    }
                }
                }
                }
            }
        }
    }
}
