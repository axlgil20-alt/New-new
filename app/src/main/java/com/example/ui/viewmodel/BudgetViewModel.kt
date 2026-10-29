package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CategoryExpenseStat(
    val category: String,
    val totalAmount: Double,
    val percentageOfTotal: Double
)

data class BudgetUiState(
    val isLoading: Boolean = false,
    val isAllTime: Boolean = false,
    val isDiscreetMode: Boolean = false, // Modo Discreto (Privacidad)
    val isDarkTheme: Boolean = false, // Modo Nocturno (Dark Mode)
    val currentYear: Int = 2026,
    val currentMonthIndex: Int = 9, // Octubre por defecto
    val currentMonthLabel: String = "Octubre 2026",
    val currencySymbol: String = "Q ",
    val searchQuery: String = "",
    val filterType: String? = null,
    val filterCategory: String? = null,
    val filterMember: String? = null,
    val onlyHighlighted: Boolean = false,
    val selectedThemeIndex: Int = 0,
    val includeTentativeInBalance: Boolean = true,
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val confirmedNetBalance: Double = 0.0,
    val simulatedNetBalance: Double = 0.0,
    val tentativeExpensesPending: Double = 0.0,
    val tentativeIncomesPending: Double = 0.0,
    val discardedTentativeSavings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val categoryStats: List<CategoryExpenseStat> = emptyList(),
    val budgetLimits: List<BudgetLimitEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val recurringExpenses: List<RecurringExpenseEntity> = emptyList(),
    val smartCartItems: List<SmartCartItemEntity> = emptyList(),
    val smartPantryItems: List<SmartCartItemEntity> = emptyList(),
    val smartCartBudget: Double = 0.0,
    val selectedCartStore: String = "Walmart",
    val upcomingDeficitWarnings: List<RecurringExpenseWarning> = emptyList(),
    val financialHealthScore: Int = 100,
    val celebrationTrigger: Long = 0L,
    val userMessage: String? = null,
    val totalDbTransactionsCount: Int = 0,
    val allTimeIncome: Double = 0.0,
    val allTimeExpense: Double = 0.0
)

private data class FilterBundle(
    val type: String?,
    val category: String?,
    val member: String?,
    val query: String,
    val currency: String,
    val onlyHighlighted: Boolean,
    val themeIndex: Int,
    val includeTentative: Boolean,
    val isDiscreet: Boolean,
    val isDarkTheme: Boolean,
    val celebrationTrigger: Long,
    val cartBudget: Double,
    val selectedCartStore: String
)

private data class DateRange(
    val startMillis: Long,
    val endMillis: Long,
    val label: String,
    val year: Int,
    val monthIndex: Int
)

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BudgetRepository
    private val prefs = application.getSharedPreferences("quetzi_user_preferences", Context.MODE_PRIVATE)

    private val calendarNow = Calendar.getInstance()
    private val selectedYearMonth = MutableStateFlow(Pair(calendarNow.get(Calendar.YEAR), calendarNow.get(Calendar.MONTH)))
    private val isAllTimeFlow = MutableStateFlow(false)
    private val isDiscreetModeFlow = MutableStateFlow(prefs.getBoolean("is_discreet_mode", false))
    private val isDarkThemeFlow = MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
    private val celebrationTriggerFlow = MutableStateFlow(0L)
    private val filterTypeFlow = MutableStateFlow<String?>(null)
    private val filterCategoryFlow = MutableStateFlow<String?>(null)
    private val filterMemberFlow = MutableStateFlow<String?>(null)
    private val searchQueryFlow = MutableStateFlow("")
    private val onlyHighlightedFlow = MutableStateFlow(false)
    private val currencySymbolFlow = MutableStateFlow(prefs.getString("currency_symbol", "Q ") ?: "Q ")
    private val selectedThemeIndexFlow = MutableStateFlow(prefs.getInt("selected_theme_index", 0))
    private val includeTentativeFlow = MutableStateFlow(true)
    private val userMessageFlow = MutableStateFlow<String?>(null)

    private val smartCartBudgetFlow = MutableStateFlow(prefs.getFloat("cart_budget_val", 0f).toDouble())
    private val selectedCartStoreFlow = MutableStateFlow(prefs.getString("cart_store_name", "Walmart") ?: "Walmart")

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BudgetRepository(
            transactionDao = db.transactionDao(),
            budgetDao = db.budgetDao(),
            savingsGoalDao = db.savingsGoalDao(),
            recurringExpenseDao = db.recurringExpenseDao(),
            smartCartDao = db.smartCartDao(),
            database = db
        )
        viewModelScope.launch {
            repository.deduplicateItemsIfAny()
        }
    }

    private val dateFilterFlow: Flow<DateRange> = combine(
        selectedYearMonth,
        isAllTimeFlow
    ) { ym, allTime ->
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, ym.first)
        cal.set(Calendar.MONTH, ym.second)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        val label = if (allTime) "Todo el Historial" else sdf.format(cal.time).replaceFirstChar { it.uppercase() }

        DateRange(
            startMillis = if (allTime) 0L else start,
            endMillis = if (allTime) Long.MAX_VALUE else end,
            label = label,
            year = ym.first,
            monthIndex = ym.second
        )
    }

    private val filterStateFlow = combine(
        filterTypeFlow,
        filterCategoryFlow,
        filterMemberFlow,
        searchQueryFlow,
        currencySymbolFlow,
        onlyHighlightedFlow,
        selectedThemeIndexFlow,
        includeTentativeFlow,
        isDiscreetModeFlow,
        isDarkThemeFlow,
        celebrationTriggerFlow,
        smartCartBudgetFlow,
        selectedCartStoreFlow
    ) { args: Array<Any?> ->
        FilterBundle(
            type = args[0] as String?,
            category = args[1] as String?,
            member = args[2] as String?,
            query = args[3] as String,
            currency = args[4] as String,
            onlyHighlighted = args[5] as Boolean,
            themeIndex = args[6] as Int,
            includeTentative = args[7] as Boolean,
            isDiscreet = args[8] as Boolean,
            isDarkTheme = args[9] as Boolean,
            celebrationTrigger = args[10] as Long,
            cartBudget = args[11] as Double,
            selectedCartStore = args[12] as String
        )
    }

    private data class PlansData(
        val budgets: List<BudgetLimitEntity>,
        val goals: List<SavingsGoalEntity>,
        val recurring: List<RecurringExpenseEntity>,
        val cartItems: List<SmartCartItemEntity>
    )

    private val plansDataFlow = combine(
        repository.allBudgets,
        repository.allGoals,
        repository.allRecurringExpenses,
        repository.allCartItems
    ) { budgets, goals, recurring, cartItems ->
        PlansData(budgets, goals, recurring, cartItems)
    }

    val uiState: StateFlow<BudgetUiState> = combine(
        repository.allTransactions,
        plansDataFlow,
        dateFilterFlow,
        filterStateFlow,
        userMessageFlow
    ) { txList, plans, dateRange, filters, message ->
        val budgets = plans.budgets
        val goals = plans.goals
        val recurring = plans.recurring
        val cartItems = plans.cartItems

        val totalDbCount = txList.size
        val allTimeInc = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val allTimeExp = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // 1. Filtrar por rango de fechas del periodo seleccionado
        val periodTx = txList.filter {
            it.dateMillis in dateRange.startMillis..dateRange.endMillis
        }

        // 2. Calcular montos confirmados, supuestos y descartados
        var confirmedIncome = 0.0
        var confirmedExpense = 0.0
        var tentativeExpensePending = 0.0
        var tentativeIncomePending = 0.0
        var discardedTentativeSavings = 0.0
        val catMap = mutableMapOf<String, Double>()

        for (tx in periodTx) {
            val isTentative = tx.isTentative
            val decision = tx.tentativeDecision

            when {
                isTentative && decision == "DISCARDED" -> {
                    discardedTentativeSavings += tx.amount
                }
                isTentative && (decision.isBlank() || decision == "PENDING") -> {
                    if (tx.type == TransactionType.EXPENSE) {
                        tentativeExpensePending += tx.amount
                    } else {
                        tentativeIncomePending += tx.amount
                    }
                }
                else -> {
                    if (tx.type == TransactionType.INCOME) {
                        confirmedIncome += tx.amount
                    } else {
                        confirmedExpense += tx.amount
                        catMap[tx.category] = (catMap[tx.category] ?: 0.0) + tx.amount
                    }
                }
            }
        }

        val confirmedNet = confirmedIncome - confirmedExpense
        val simulatedNet = confirmedNet - tentativeExpensePending + tentativeIncomePending + discardedTentativeSavings

        val activeTotalExpense = if (filters.includeTentative) {
            confirmedExpense + tentativeExpensePending
        } else {
            confirmedExpense
        }

        val activeTotalIncome = if (filters.includeTentative) {
            confirmedIncome + tentativeIncomePending + discardedTentativeSavings
        } else {
            confirmedIncome
        }

        val activeNetBalance = activeTotalIncome - activeTotalExpense

        val savingsRate = if (activeTotalIncome > 0) {
            ((activeNetBalance / activeTotalIncome) * 100).coerceIn(-100.0, 100.0)
        } else {
            0.0
        }

        val totalCategorized = catMap.values.sum()
        val catStats: List<CategoryExpenseStat> = catMap.map { (cat, amount) ->
            CategoryExpenseStat(
                category = cat,
                totalAmount = amount,
                percentageOfTotal = if (totalCategorized > 0) (amount / totalCategorized) * 100 else 0.0
            )
        }.sortedByDescending { stat: CategoryExpenseStat -> stat.totalAmount }

        val filtered = periodTx.filter { tx ->
            val matchesType = when (filters.type) {
                null -> true
                "TENTATIVE" -> tx.isTentative && (tx.tentativeDecision.isBlank() || tx.tentativeDecision == "PENDING")
                else -> tx.type == filters.type
            }
            val matchesCategory = filters.category == null || tx.category == filters.category
            val matchesMember = filters.member == null || tx.familyMember == filters.member || filters.member == "Todos"
            val matchesHighlighted = !filters.onlyHighlighted || tx.highlightColorHex != 0L
            val matchesQuery = if (filters.query.isBlank()) {
                true
            } else {
                val q = filters.query.trim().lowercase()
                tx.title.lowercase().contains(q) ||
                tx.category.lowercase().contains(q) ||
                tx.notes.lowercase().contains(q) ||
                tx.amount.toString().contains(q) ||
                "%.2f".format(tx.amount).contains(q) ||
                "q${"%.2f".format(tx.amount)}".contains(q)
            }
            matchesType && matchesCategory && matchesMember && matchesHighlighted && matchesQuery
        }

        val warnings = mutableListOf<RecurringExpenseWarning>()
        for (limit in budgets) {
            val spent = catMap[limit.category] ?: 0.0
            if (spent > limit.monthlyLimit) {
                warnings.add(
                    RecurringExpenseWarning(
                        title = "Límite excedido en ${limit.category}",
                        amount = spent - limit.monthlyLimit,
                        dueDateMessage = "Has gastado ${filters.currency}${"%.2f".format(spent)} de tu límite de ${filters.currency}${"%.2f".format(limit.monthlyLimit)}.",
                        severity = "HIGH"
                    )
                )
            }
        }

        if (tentativeExpensePending > 0 && simulatedNet < 0) {
            warnings.add(
                RecurringExpenseWarning(
                    title = "Alerta: Saldo Negativo Proyectado",
                    amount = kotlin.math.abs(simulatedNet),
                    dueDateMessage = "Si realizas todas tus compras supuestas (${filters.currency}${"%.2f".format(tentativeExpensePending)}), tu saldo será negativo.",
                    severity = "HIGH"
                )
            )
        }

        val healthScore = when {
            activeTotalIncome > 0 -> {
                val expenseRatio = (activeTotalExpense / activeTotalIncome)
                val score = ((1.0 - expenseRatio.coerceAtMost(1.0)) * 100.0).toInt()
                score.coerceIn(0, 100)
            }
            activeTotalExpense > 0 -> 20
            else -> 100
        }

            BudgetUiState(
                isLoading = false,
                isAllTime = dateRange.startMillis == 0L,
                isDiscreetMode = filters.isDiscreet,
                isDarkTheme = filters.isDarkTheme,
                currentYear = dateRange.year,
                currentMonthIndex = dateRange.monthIndex,
                currentMonthLabel = dateRange.label,
                currencySymbol = filters.currency,
                searchQuery = filters.query,
                filterType = filters.type,
                filterCategory = filters.category,
                filterMember = filters.member,
                onlyHighlighted = filters.onlyHighlighted,
                selectedThemeIndex = filters.themeIndex,
                includeTentativeInBalance = filters.includeTentative,
                filteredTransactions = filtered,
                allTransactions = txList,
                totalIncome = activeTotalIncome,
                totalExpense = activeTotalExpense,
                netBalance = activeNetBalance,
                confirmedNetBalance = confirmedNet,
                simulatedNetBalance = simulatedNet,
                tentativeExpensesPending = tentativeExpensePending,
                tentativeIncomesPending = tentativeIncomePending,
                discardedTentativeSavings = discardedTentativeSavings,
                savingsRate = savingsRate,
                categoryStats = catStats,
                budgetLimits = budgets,
                savingsGoals = goals,
                recurringExpenses = recurring,
                smartCartItems = cartItems.filter { it.isInShoppingList() },
                smartPantryItems = cartItems.filter { it.isPantryProduct() },
                smartCartBudget = filters.cartBudget,
            selectedCartStore = filters.selectedCartStore,
            upcomingDeficitWarnings = warnings,
            financialHealthScore = healthScore,
            totalDbTransactionsCount = totalDbCount,
            allTimeIncome = allTimeInc,
            allTimeExpense = allTimeExp,
            celebrationTrigger = filters.celebrationTrigger,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState(isLoading = true)
    )

    fun showToast(msg: String) {
        userMessageFlow.value = msg
    }

    fun clearMessage() {
        userMessageFlow.value = null
    }

    fun toggleDiscreetMode() {
        val newDiscreet = !isDiscreetModeFlow.value
        isDiscreetModeFlow.value = newDiscreet
        prefs.edit().putBoolean("is_discreet_mode", newDiscreet).apply()
        val msg = if (newDiscreet) "Modo Discreto Activado: Montos ocultos" else "Modo Discreto Desactivado"
        showToast(msg)
    }

    fun toggleDarkTheme() {
        val newDark = !isDarkThemeFlow.value
        isDarkThemeFlow.value = newDark
        prefs.edit().putBoolean("is_dark_theme", newDark).apply()
        val msg = if (newDark) "Modo Nocturno activado 🌙" else "Modo Claro activado ☀️"
        showToast(msg)
    }

    fun nextMonth() {
        val curr = selectedYearMonth.value
        val nextMonth = if (curr.second == 11) 0 else curr.second + 1
        val nextYear = if (curr.second == 11) curr.first + 1 else curr.first
        selectedYearMonth.value = Pair(nextYear, nextMonth)
        isAllTimeFlow.value = false
    }

    fun previousMonth() {
        val curr = selectedYearMonth.value
        val prevMonth = if (curr.second == 0) 11 else curr.second - 1
        val prevYear = if (curr.second == 0) curr.first - 1 else curr.first
        selectedYearMonth.value = Pair(prevYear, prevMonth)
        isAllTimeFlow.value = false
    }

    fun toggleAllTime() {
        isAllTimeFlow.value = !isAllTimeFlow.value
    }

    fun setSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun setFilterType(type: String?) {
        filterTypeFlow.value = type
    }

    fun setCategoryFilter(category: String?) {
        filterCategoryFlow.value = category
    }

    fun setMemberFilter(member: String?) {
        filterMemberFlow.value = member
    }

    fun setOnlyHighlighted(enabled: Boolean) {
        onlyHighlightedFlow.value = enabled
    }

    fun setCurrency(symbol: String) {
        currencySymbolFlow.value = symbol
        prefs.edit().putString("currency_symbol", symbol).apply()
    }

    fun setAppTheme(index: Int) {
        selectedThemeIndexFlow.value = index
        prefs.edit().putInt("selected_theme_index", index).apply()
    }

    fun toggleIncludeTentative() {
        includeTentativeFlow.value = !includeTentativeFlow.value
        val msg = if (includeTentativeFlow.value) {
            "Simulación activada: Compras supuestas incluidas."
        } else {
            "Solo compras confirmadas en el saldo."
        }
        showToast(msg)
    }

    fun setIncludeTentative(enabled: Boolean) {
        includeTentativeFlow.value = enabled
    }

    fun decideTentativeTransaction(tx: TransactionEntity, decision: String) {
        viewModelScope.launch {
            when (decision.uppercase()) {
                "CONFIRM" -> {
                    val updated = tx.copy(
                        isTentative = false,
                        tentativeDecision = "CONFIRMED",
                        notes = (tx.notes + " (Confirmada)").trim()
                    )
                    repository.updateTransaction(updated)
                    showToast("Compra confirmada y registrada en tus gastos.")
                }
                "DISCARD" -> {
                    val updated = tx.copy(
                        isTentative = true,
                        tentativeDecision = "DISCARDED",
                        notes = (tx.notes + " (No comprada: Ahorro positivo)").trim()
                    )
                    repository.updateTransaction(updated)
                    showToast("¡Decidiste no comprar! Q${"%.2f".format(tx.amount)} sumado a tu ahorro.")
                }
            }
        }
    }

    fun triggerCelebration() {
        celebrationTriggerFlow.value = System.currentTimeMillis()
    }

    fun addTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(tx)
            celebrationTriggerFlow.value = System.currentTimeMillis()
            showToast("Transacción guardada exitosamente.")
        }
    }

    // ACCESO RÁPIDO CHAPÍN: Guarda gastos reales inmediatos desde el FAB
    fun addChapinQuickExpense(title: String, amount: Double, category: String, notes: String = "Gasto rápido Chapín") {
        viewModelScope.launch {
            val tx = TransactionEntity(
                title = title,
                amount = amount,
                type = TransactionType.EXPENSE,
                category = category,
                dateMillis = System.currentTimeMillis(),
                isAntExpense = true,
                notes = notes
            )
            repository.insertTransaction(tx)
            celebrationTriggerFlow.value = System.currentTimeMillis()
            showToast("¡Listo! Se guardó Q${"%.2f".format(amount)} en $title.")
        }
    }

    fun updateTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(tx)
            showToast("Transacción actualizada.")
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            showToast("Transacción eliminada.")
        }
    }

    fun addAntExpense(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(tx.copy(isAntExpense = true))
            celebrationTriggerFlow.value = System.currentTimeMillis()
            showToast("Gasto hormiga registrado.")
        }
    }

    fun highlightTransaction(tx: TransactionEntity, colorHex: Long) {
        viewModelScope.launch {
            repository.updateTransaction(tx.copy(highlightColorHex = colorHex))
        }
    }

    fun toggleVerifyTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            val updated = tx.copy(isVerified = !tx.isVerified)
            repository.updateTransaction(updated)
            val msg = if (updated.isVerified) "Verificado ✓" else "Marcado como pendiente"
            showToast(msg)
        }
    }

    fun setBudgetLimit(category: String, limit: Double, reason: String = "", tip: String = "") {
        viewModelScope.launch {
            repository.setBudget(BudgetLimitEntity(category, limit, reason, tip))
            showToast("Límite guardado para $category.")
        }
    }

    fun deleteBudgetLimit(category: String) {
        viewModelScope.launch {
            repository.deleteBudget(category)
            showToast("Límite eliminado.")
        }
    }

    fun addSavingsGoal(name: String, target: Double, initial: Double = 0.0, notes: String = "") {
        viewModelScope.launch {
            repository.insertGoal(SavingsGoalEntity(name = name, targetAmount = target, currentAmount = initial, notes = notes))
            showToast("Meta de ahorro creada.")
        }
    }

    fun updateSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.updateGoal(goal)
            showToast("Meta actualizada.")
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            showToast("Meta eliminada.")
        }
    }

    fun contributeToGoal(goal: SavingsGoalEntity, amount: Double) {
        viewModelScope.launch {
            repository.contributeToGoal(goal, amount)
            showToast("Aporte de Q${"%.2f".format(amount)} registrado a tu meta.")
        }
    }

    fun addRecurringExpense(
        title: String,
        amount: Double,
        category: String,
        frequency: String,
        familyMember: String = "Personal",
        paymentMethod: String = "Efectivo",
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.insertRecurringExpense(
                RecurringExpenseEntity(
                    title = title,
                    amount = amount,
                    category = category,
                    frequency = frequency,
                    familyMember = familyMember,
                    paymentMethod = paymentMethod,
                    notes = notes
                )
            )
            showToast("Gasto recurrente programado.")
        }
    }

    fun updateRecurringExpense(expense: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.updateRecurringExpense(expense)
            showToast("Gasto recurrente actualizado.")
        }
    }

    fun deleteRecurringExpense(expense: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(expense)
            showToast("Gasto recurrente eliminado.")
        }
    }

    fun toggleRecurringExpenseActive(expense: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.updateRecurringExpense(expense.copy(isActive = !expense.isActive))
        }
    }

    fun importCsv(csvContent: String, onComplete: ((ImportSummary) -> Unit)? = null) {
        viewModelScope.launch {
            val summary = repository.importFromCsv(csvContent)
            val msg = buildString {
                if (summary.totalProcessed == 0) {
                    append("No se encontraron transacciones válidas para importar.")
                } else {
                    append("Importación finalizada: ")
                    val parts = mutableListOf<String>()
                    if (summary.newCount > 0) parts.add("${summary.newCount} nuevas")
                    if (summary.duplicateCount > 0) parts.add("${summary.duplicateCount} duplicados omitidos")
                    if (summary.suspectedDuplicateCount > 0) parts.add("${summary.suspectedDuplicateCount} para revisión")
                    if (summary.errorCount > 0) parts.add("${summary.errorCount} errores")
                    append(parts.joinToString(", "))
                }
            }
            showToast(msg)
            onComplete?.invoke(summary)
        }
    }

    suspend fun getCsvExport(): String {
        return repository.exportToCsv(uiState.value.filteredTransactions)
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            showToast("Base de datos limpia y lista.")
        }
    }

    // =========================================================================
    // SMART CART (SÚPER INTELIGENTE) QUETZI - PERSISTENCIA ROOM & FINANZAS
    // =========================================================================

    fun setSmartCartBudget(amount: Double) {
        val clean = amount.coerceAtLeast(0.0)
        prefs.edit().putFloat("cart_budget_val", clean.toFloat()).apply()
        smartCartBudgetFlow.value = clean
    }

    fun setSelectedCartStore(store: String) {
        prefs.edit().putString("cart_store_name", store).apply()
        selectedCartStoreFlow.value = store
    }

    fun addSmartCartItem(
        name: String,
        quantity: Double = 1.0,
        unit: String = "Unidad",
        brand: String = "",
        price: Double = 0.0,
        store: String = "General",
        notes: String = "",
        url: String = "",
        priceSource: String = "PANTRY_AMARILLO"
    ) {
        viewModelScope.launch {
            repository.addOrUpdateUnifiedItem(
                name = name,
                quantity = quantity,
                unit = unit,
                brand = brand,
                estimatedUnitPrice = price,
                storeName = store,
                status = if (priceSource.startsWith("PANTRY_")) priceSource else "PANTRY_AMARILLO",
                isChecked = false,
                notes = notes,
                productUrl = url
            )
            showToast("Producto guardado en Smart Cart.")
        }
    }

    fun updateSmartCartItem(item: SmartCartItemEntity) {
        viewModelScope.launch {
            repository.updateCartItem(item)
        }
    }

    fun deleteSmartCartItem(item: SmartCartItemEntity) {
        viewModelScope.launch {
            if (item.isPantryProduct()) {
                repository.removeCartItemFromCart(item)
                showToast("Artículo retirado de la compra (conservado en despensa).")
            } else {
                repository.deleteCartItem(item)
                showToast("Producto eliminado.")
            }
        }
    }

    fun togglePantryInCart(item: SmartCartItemEntity) {
        viewModelScope.launch {
            repository.togglePantryInCart(item)
            if (!item.isInShoppingList()) {
                showToast("«${item.name}» agregado a la lista de compras.")
            } else {
                showToast("«${item.name}» retirado de la compra.")
            }
        }
    }

    fun toggleSmartCartItemChecked(item: SmartCartItemEntity) {
        viewModelScope.launch {
            repository.toggleCartItemChecked(item.id)
        }
    }

    fun clearCheckedCartItems() {
        viewModelScope.launch {
            repository.clearCheckedCartItems()
            showToast("Lista de compras actualizada (despensa preservada).")
        }
    }

    fun clearAllCartItems() {
        viewModelScope.launch {
            repository.clearCartItems()
            showToast("Lista de compras vaciada.")
        }
    }

    // =========================================================================
    // DESPENSA REUTILIZABLE (PANTRY) QUETZI
    // =========================================================================

    fun addPantryItem(
        name: String,
        quantity: Double = 1.0,
        unit: String = "Unidad",
        status: String = "PANTRY_VERDE",
        price: Double = 0.0,
        store: String = "General",
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.addOrUpdateUnifiedItem(
                name = name,
                quantity = quantity,
                unit = unit,
                status = status,
                estimatedUnitPrice = price,
                storeName = store,
                notes = notes
            )
            showToast("Artículo guardado en tu despensa.")
        }
    }

    fun updatePantryItem(item: SmartCartItemEntity) {
        viewModelScope.launch {
            repository.updateCartItem(item)
            showToast("Despensa actualizada.")
        }
    }

    fun togglePantryStatus(item: SmartCartItemEntity) {
        val nextStatus = when (item.priceSource) {
            "PANTRY_VERDE" -> "PANTRY_AMARILLO"
            "PANTRY_AMARILLO" -> "PANTRY_ROJO"
            "PANTRY_ROJO" -> "PANTRY_VERDE"
            else -> if (item.priceSource.contains("ROJO")) "PANTRY_VERDE" else if (item.priceSource.contains("AMARILLO") || item.priceSource == "REPOSICION") "PANTRY_ROJO" else "PANTRY_AMARILLO"
        }
        viewModelScope.launch {
            repository.updatePantryStatus(item.id, nextStatus)
        }
    }

    fun deletePantryItem(item: SmartCartItemEntity) {
        viewModelScope.launch {
            repository.deleteCartItem(item)
            showToast("Artículo retirado de la despensa.")
        }
    }

    fun incorporateNeedToBuy() {
        viewModelScope.launch {
            val count = repository.incorporateNeedToBuyIntoCart()
            if (count > 0) {
                showToast("¡Listo! Se incorporaron $count productos a tu compra.")
            } else {
                showToast("Todos los productos por reponer ya están en la compra.")
            }
        }
    }

    fun uncheckAllCartItems() {
        viewModelScope.launch {
            repository.uncheckAllCartItems()
            showToast("Todos los productos desmarcados.")
        }
    }

    fun generateReplenishmentList() {
        incorporateNeedToBuy()
    }

    fun addScannedItems(
        items: List<com.example.ui.util.ScannedGroceryItem>,
        destination: String, // "CART" (Por comprar) o "PANTRY" (Suficiente)
        avoidDuplicates: Boolean
    ) {
        viewModelScope.launch {
            val selected = items.filter { it.isSelected && it.name.isNotBlank() }
            if (selected.isEmpty()) return@launch

            val defaultStatus = if (destination == "PANTRY") "PANTRY_VERDE" else "PANTRY_AMARILLO"
            val defaultChecked = destination == "CART"

            for (scanned in selected) {
                repository.addOrUpdateUnifiedItem(
                    name = scanned.name.trim(),
                    quantity = scanned.quantity,
                    unit = scanned.unit,
                    estimatedUnitPrice = scanned.price,
                    status = defaultStatus,
                    isChecked = defaultChecked,
                    notes = "Escaneado con OCR"
                )
            }
            showToast("Se agregaron ${selected.size} productos a Smart Cart.")
        }
    }

    private val isCheckoutInProgress = java.util.concurrent.atomic.AtomicBoolean(false)

    fun checkoutCartAsExpense(title: String, amount: Double, storeName: String, purchasedNames: List<String>) {
        if (!isCheckoutInProgress.compareAndSet(false, true)) {
            return
        }
        viewModelScope.launch {
            try {
                val success = repository.confirmPurchaseAndUpdatePantry(
                    title = title,
                    amount = amount,
                    storeName = storeName,
                    purchasedItemNames = purchasedNames
                )
                if (success) {
                    showToast("Compra registrada de Q${"%,.2f".format(amount)} y despensa actualizada.")
                }
            } finally {
                kotlinx.coroutines.delay(800L)
                isCheckoutInProgress.set(false)
            }
        }
    }
}
