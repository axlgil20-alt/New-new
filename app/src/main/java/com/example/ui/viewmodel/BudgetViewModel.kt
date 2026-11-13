package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.BudgetRepository
import com.example.ui.util.BudgetLimitCalculator
import com.example.ui.util.FormatUtils
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
    val allTimeExpense: Double = 0.0,
    // Filtros Avanzados y Centro de Control Quicken Simplifi
    val filterPeriod: String = FilterPeriod.MONTH,
    val filterSpecificDateMillis: Long? = null,
    val filterDateRangeStart: Long? = null,
    val filterDateRangeEnd: Long? = null,
    val filterQuincena: Int = 0,
    val filterSubCategory: String? = null,
    val filterBeneficiary: String? = null,
    val filterStatus: String? = null,
    val filterSource: String? = null,
    val filterDuplicatesMode: String = DuplicateFilterMode.ALL,
    val activeFiltersCount: Int = 0,
    val suspectedDuplicatesCount: Int = 0,
    // Métricas Financieras Estrictas (Separación Real vs Proyectado)
    val filteredRealIncome: Double = 0.0,
    val filteredRealExpense: Double = 0.0,
    val filteredRealNetBalance: Double = 0.0,
    val filteredProjectedIncome: Double = 0.0,
    val filteredProjectedExpense: Double = 0.0,
    val filteredProjectedNetBalance: Double = 0.0,
    val filteredCategorySubtotals: List<CategoryExpenseStat> = emptyList(),
    // Overview Quicken Simplifi con Salario Roll-Over y Gastos Supuestos
    val totalAmountToSpend: Double = 0.0,
    val quincenaRolloverAmount: Double = 0.0,
    val filteredConfirmedExpense: Double = 0.0,
    val filteredSupuestoExpense: Double = 0.0,
    val filteredTotalBothExpenses: Double = 0.0,
    val filteredRealRemainingBalance: Double = 0.0,
    val filteredProjectedRemainingBalance: Double = 0.0,
    // Módulo Pagos
    val paymentCommitments: List<PaymentCommitmentEntity> = emptyList(),
    val paymentBeneficiaries: List<BeneficiaryEntity> = emptyList(),
    val totalPendingPayments: Double = 0.0,
    val upcomingPaymentsCount: Int = 0,
    val overduePaymentsCount: Int = 0,
    val monthPaidPaymentsTotal: Double = 0.0,
    val nextUpcomingPayment: PaymentCommitmentEntity? = null,
    // Plantillas Reutilizables de Compras
    val shoppingTemplates: List<ShoppingTemplateEntity> = emptyList()
)

private data class BasicFilterBundle(
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
    val selectedCartStore: String,
    val period: String,
    val specificDateMillis: Long?,
    val dateRangeStart: Long?,
    val dateRangeEnd: Long?,
    val quincena: Int,
    val subCategory: String?,
    val beneficiary: String?,
    val status: String?,
    val source: String?,
    val duplicatesMode: String
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

    // Filtros Avanzados y Control Financiero Quicken Simplifi
    private val filterPeriodFlow = MutableStateFlow(FilterPeriod.MONTH)
    private val filterSpecificDateFlow = MutableStateFlow<Long?>(null)
    private val filterDateRangeStartFlow = MutableStateFlow<Long?>(null)
    private val filterDateRangeEndFlow = MutableStateFlow<Long?>(null)
    private val filterQuincenaFlow = MutableStateFlow(0)
    private val filterSubCategoryFlow = MutableStateFlow<String?>(null)
    private val filterBeneficiaryFlow = MutableStateFlow<String?>(null)
    private val filterStatusFlow = MutableStateFlow<String?>(null)
    private val filterSourceFlow = MutableStateFlow<String?>(null)
    private val filterDuplicatesModeFlow = MutableStateFlow(DuplicateFilterMode.ALL)

    private data class AdvancedFilters(
        val period: String,
        val specificDateMillis: Long?,
        val dateRangeStart: Long?,
        val dateRangeEnd: Long?,
        val quincena: Int,
        val subCategory: String?,
        val beneficiary: String?,
        val status: String?,
        val source: String?,
        val duplicatesMode: String
    )

    private val advancedFiltersFlow: Flow<AdvancedFilters> = combine(
        filterPeriodFlow,
        filterSpecificDateFlow,
        filterDateRangeStartFlow,
        filterDateRangeEndFlow,
        filterQuincenaFlow,
        filterSubCategoryFlow,
        filterBeneficiaryFlow,
        filterStatusFlow,
        filterSourceFlow,
        filterDuplicatesModeFlow
    ) { args: Array<Any?> ->
        AdvancedFilters(
            period = args[0] as String,
            specificDateMillis = args[1] as Long?,
            dateRangeStart = args[2] as Long?,
            dateRangeEnd = args[3] as Long?,
            quincena = args[4] as Int,
            subCategory = args[5] as String?,
            beneficiary = args[6] as String?,
            status = args[7] as String?,
            source = args[8] as String?,
            duplicatesMode = args[9] as String
        )
    }

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BudgetRepository(
            transactionDao = db.transactionDao(),
            budgetDao = db.budgetDao(),
            savingsGoalDao = db.savingsGoalDao(),
            recurringExpenseDao = db.recurringExpenseDao(),
            smartCartDao = db.smartCartDao(),
            paymentCommitmentDao = db.paymentCommitmentDao(),
            beneficiaryDao = db.beneficiaryDao(),
            shoppingTemplateDao = db.shoppingTemplateDao(),
            database = db,
            financialAccountDao = db.financialAccountDao(),
            installmentPlanDao = db.installmentPlanDao(),
            installmentScheduleDao = db.installmentScheduleDao(),
            accountLedgerDao = db.accountLedgerDao()
        )
        viewModelScope.launch {
            repository.deduplicateItemsIfAny()
            repository.ensureDefaultBeneficiaries()
            repository.ensureDefaultShoppingTemplates()
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

        val (budgetStart, budgetEnd) = com.example.ui.util.QuincenaFilterHelper.getBudgetPeriodRange(
            year = ym.first,
            monthIndex = ym.second,
            quincena = 0
        )

        val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        val label = if (allTime) "Todo el Historial" else sdf.format(cal.time).replaceFirstChar { it.uppercase() }

        DateRange(
            startMillis = if (allTime) 0L else budgetStart,
            endMillis = if (allTime) Long.MAX_VALUE else budgetEnd,
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
        BasicFilterBundle(
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

    private val filterBundleFlow: Flow<FilterBundle> = combine(
        filterStateFlow,
        advancedFiltersFlow
    ) { basic, adv ->
        FilterBundle(
            type = basic.type,
            category = basic.category,
            member = basic.member,
            query = basic.query,
            currency = basic.currency,
            onlyHighlighted = basic.onlyHighlighted,
            themeIndex = basic.themeIndex,
            includeTentative = basic.includeTentative,
            isDiscreet = basic.isDiscreet,
            isDarkTheme = basic.isDarkTheme,
            celebrationTrigger = basic.celebrationTrigger,
            cartBudget = basic.cartBudget,
            selectedCartStore = basic.selectedCartStore,
            period = adv.period,
            specificDateMillis = adv.specificDateMillis,
            dateRangeStart = adv.dateRangeStart,
            dateRangeEnd = adv.dateRangeEnd,
            quincena = adv.quincena,
            subCategory = adv.subCategory,
            beneficiary = adv.beneficiary,
            status = adv.status,
            source = adv.source,
            duplicatesMode = adv.duplicatesMode
        )
    }

    private data class PlansData(
        val budgets: List<BudgetLimitEntity>,
        val goals: List<SavingsGoalEntity>,
        val recurring: List<RecurringExpenseEntity>,
        val cartItems: List<SmartCartItemEntity>,
        val payments: List<PaymentCommitmentEntity>,
        val beneficiaries: List<BeneficiaryEntity>,
        val templates: List<ShoppingTemplateEntity>
    )

    private val plansDataFlow = combine(
        repository.allBudgets,
        repository.allGoals,
        repository.allRecurringExpenses,
        repository.allCartItems,
        repository.allPaymentCommitments,
        repository.allBeneficiaries,
        repository.allShoppingTemplates
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        PlansData(
            budgets = args[0] as List<BudgetLimitEntity>,
            goals = args[1] as List<SavingsGoalEntity>,
            recurring = args[2] as List<RecurringExpenseEntity>,
            cartItems = args[3] as List<SmartCartItemEntity>,
            payments = args[4] as List<PaymentCommitmentEntity>,
            beneficiaries = args[5] as List<BeneficiaryEntity>,
            templates = args[6] as List<ShoppingTemplateEntity>
        )
    }

    val uiState: StateFlow<BudgetUiState> = combine(
        repository.allTransactions,
        plansDataFlow,
        dateFilterFlow,
        filterBundleFlow,
        userMessageFlow
    ) { txList, plans, dateRange, filters, message ->
        val budgets = plans.budgets
        val goals = plans.goals
        val recurring = plans.recurring
        val cartItems = plans.cartItems

        val totalDbCount = txList.size
        val allTimeInc = txList
            .filter { it.isIngresoReal }
            .fold(java.math.BigDecimal.ZERO) { acc, tx -> acc.add(tx.effectiveAmountBigDecimal) }
            .toDouble()
        val allTimeExp = txList
            .filter { it.type == TransactionType.EXPENSE && it.affectsBudgetTotals }
            .fold(java.math.BigDecimal.ZERO) { acc, tx -> acc.add(tx.effectiveAmountBigDecimal) }
            .toDouble()

        // Identificación confiable de duplicados en el conjunto de datos
        val duplicateIds = com.example.ui.util.QuincenaFilterHelper.findDuplicateTransactionIds(txList)

        // 1. Filtrar por rango de fechas del periodo seleccionado o periodo avanzado
        val periodTx = txList.filter { tx ->
            val matchesDate = when {
                filters.specificDateMillis != null -> {
                    com.example.ui.util.QuincenaFilterHelper.isSameCalendarDay(tx.dateMillis, filters.specificDateMillis)
                }
                filters.dateRangeStart != null && filters.dateRangeEnd != null -> {
                    tx.dateMillis in filters.dateRangeStart..filters.dateRangeEnd
                }
                filters.period == FilterPeriod.ALL || dateRange.startMillis == 0L -> {
                    true
                }
                filters.period == FilterPeriod.DAY -> {
                    com.example.ui.util.QuincenaFilterHelper.isSameCalendarDay(tx.dateMillis, System.currentTimeMillis())
                }
                filters.period == FilterPeriod.WEEK -> {
                    val (wStart, wEnd) = com.example.ui.util.QuincenaFilterHelper.getWeekRange(System.currentTimeMillis())
                    tx.dateMillis in wStart..wEnd
                }
                filters.period == FilterPeriod.YEAR -> {
                    val (yStart, yEnd) = com.example.ui.util.QuincenaFilterHelper.getYearRange(dateRange.year)
                    tx.dateMillis in yStart..yEnd
                }
                else -> {
                    com.example.ui.util.QuincenaFilterHelper.isDateInBudgetMonth(tx.dateMillis, dateRange.year, dateRange.monthIndex, tx.type)
                }
            }
            val matchesQuincena = if (filters.quincena > 0) {
                com.example.ui.util.QuincenaFilterHelper.isDateInQuincena(tx.dateMillis, filters.quincena, tx.type)
            } else true

            matchesDate && matchesQuincena
        }

        // 2. Calcular montos confirmados, supuestos y descartados del periodo base con BigDecimal exacto
        var confirmedIncomeBd = java.math.BigDecimal.ZERO
        var confirmedExpenseBd = java.math.BigDecimal.ZERO
        var tentativeExpensePendingBd = java.math.BigDecimal.ZERO
        var tentativeIncomePendingBd = java.math.BigDecimal.ZERO
        var discardedTentativeSavingsBd = java.math.BigDecimal.ZERO
        val catMapBd = mutableMapOf<String, java.math.BigDecimal>()

        for (tx in periodTx) {
            if (!tx.affectsBudgetTotals) continue
            val isTentative = tx.isTentative
            val decision = tx.tentativeDecision
            val txAmtBd = tx.effectiveAmountBigDecimal

            when {
                isTentative && decision == "DISCARDED" -> {
                    discardedTentativeSavingsBd = discardedTentativeSavingsBd.add(txAmtBd)
                }
                isTentative && (decision.isBlank() || decision == "PENDING") -> {
                    if (tx.type == TransactionType.EXPENSE) {
                        tentativeExpensePendingBd = tentativeExpensePendingBd.add(txAmtBd)
                    } else {
                        tentativeIncomePendingBd = tentativeIncomePendingBd.add(txAmtBd)
                    }
                }
                else -> {
                    if (tx.isIngresoReal) {
                        confirmedIncomeBd = confirmedIncomeBd.add(txAmtBd)
                    } else if (tx.type == TransactionType.EXPENSE) {
                        confirmedExpenseBd = confirmedExpenseBd.add(txAmtBd)
                        catMapBd[tx.category] = (catMapBd[tx.category] ?: java.math.BigDecimal.ZERO).add(txAmtBd)
                    }
                }
            }
        }

        val confirmedIncome = confirmedIncomeBd.toDouble()
        val confirmedExpense = confirmedExpenseBd.toDouble()
        val tentativeExpensePending = tentativeExpensePendingBd.toDouble()
        val tentativeIncomePending = tentativeIncomePendingBd.toDouble()
        val discardedTentativeSavings = discardedTentativeSavingsBd.toDouble()

        val confirmedNet = confirmedIncomeBd.subtract(confirmedExpenseBd).toDouble()
        val simulatedNet = confirmedIncomeBd
            .subtract(confirmedExpenseBd)
            .subtract(tentativeExpensePendingBd)
            .add(tentativeIncomePendingBd)
            .add(discardedTentativeSavingsBd)
            .toDouble()

        val activeTotalExpenseBd = if (filters.includeTentative) {
            confirmedExpenseBd.add(tentativeExpensePendingBd)
        } else {
            confirmedExpenseBd
        }

        val activeTotalIncomeBd = if (filters.includeTentative) {
            confirmedIncomeBd.add(tentativeIncomePendingBd).add(discardedTentativeSavingsBd)
        } else {
            confirmedIncomeBd
        }

        val activeTotalExpense = activeTotalExpenseBd.toDouble()
        val activeTotalIncome = activeTotalIncomeBd.toDouble()
        val activeNetBalance = activeTotalIncomeBd.subtract(activeTotalExpenseBd).toDouble()

        val savingsRate = if (activeTotalIncome > 0) {
            ((activeNetBalance / activeTotalIncome) * 100).coerceIn(-100.0, 100.0)
        } else {
            0.0
        }

        val totalCategorizedBd = catMapBd.values.fold(java.math.BigDecimal.ZERO) { acc, v -> acc.add(v) }
        val totalCategorized = totalCategorizedBd.toDouble()
        val catStats: List<CategoryExpenseStat> = catMapBd.map { (cat, amountBd) ->
            val amt = amountBd.toDouble()
            CategoryExpenseStat(
                category = cat,
                totalAmount = amt,
                percentageOfTotal = if (totalCategorized > 0) (amt / totalCategorized) * 100 else 0.0
            )
        }.sortedByDescending { stat: CategoryExpenseStat -> stat.totalAmount }

        // 3. Filtrado completo combinable de los 11 criterios
        val searchDateFormats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.ROOT),
            SimpleDateFormat("d MMMM yyyy", Locale("es", "GT"))
        )
        val filtered = periodTx.filter { tx ->
            val matchesType = when (filters.type) {
                null -> true
                "TENTATIVE", "SUPUESTO" -> tx.isGastoSupuesto
                "REAL_EXPENSE" -> tx.isGastoReal
                TransactionType.EXPENSE -> tx.type == TransactionType.EXPENSE
                TransactionType.INCOME -> tx.type == TransactionType.INCOME
                "SAVING" -> tx.category.contains("Ahorro", ignoreCase = true) || tx.notes.contains("Ahorro", ignoreCase = true)
                "TRANSFER" -> com.example.data.model.TransactionEntryKind.isTransferOrSettlement(tx.entryKind) ||
                    tx.category.contains("Transferencia", ignoreCase = true) ||
                    tx.title.contains("Transferencia", ignoreCase = true)
                else -> tx.type == filters.type
            }
            val matchesCategory = filters.category == null || tx.category.equals(filters.category, ignoreCase = true)
            val matchesMember = filters.member == null || tx.familyMember == filters.member || filters.member == "Todos"
            val matchesSubCat = filters.subCategory == null || tx.familyMember.equals(filters.subCategory, ignoreCase = true) || tx.notes.contains(filters.subCategory, ignoreCase = true)
            val matchesBeneficiary = filters.beneficiary.isNullOrBlank() || tx.title.contains(filters.beneficiary.trim(), ignoreCase = true)
            val matchesStatus = filters.status == null || tx.status == filters.status
            val matchesSource = filters.source == null || tx.source == filters.source
            val matchesDuplicates = when (filters.duplicatesMode) {
                DuplicateFilterMode.ALL -> true
                DuplicateFilterMode.ONLY_DUPLICATES -> duplicateIds.contains(tx.id) || tx.isSuspectedDuplicate
                DuplicateFilterMode.EXCLUDE_DUPLICATES -> !duplicateIds.contains(tx.id) && !tx.isSuspectedDuplicate
                else -> true
            }
            val matchesHighlighted = !filters.onlyHighlighted || tx.highlightColorHex != 0L
            val matchesQuery = if (filters.query.isBlank()) {
                true
            } else {
                val q = filters.query.trim().lowercase(Locale.ROOT)
                val formattedAmountLocal = String.format(Locale.getDefault(), "%.2f", tx.amount)
                val formattedAmountRoot = String.format(Locale.ROOT, "%.2f", tx.amount)
                val currency = filters.currency.trim().lowercase(Locale.ROOT)
                val matchesAmount = listOf(
                    tx.amount.toString(),
                    formattedAmountLocal,
                    formattedAmountRoot,
                    "$currency$formattedAmountLocal",
                    "$currency $formattedAmountLocal"
                ).any { it.lowercase(Locale.ROOT).contains(q) }
                val matchesDate = searchDateFormats.any { formatter ->
                    formatter.format(java.util.Date(tx.dateMillis)).lowercase(Locale.ROOT).contains(q)
                }
                tx.title.lowercase(Locale.ROOT).contains(q) ||
                tx.category.lowercase(Locale.ROOT).contains(q) ||
                tx.notes.lowercase(Locale.ROOT).contains(q) ||
                tx.familyMember.lowercase(Locale.ROOT).contains(q) ||
                matchesAmount ||
                matchesDate
            }
            matchesType && matchesCategory && matchesMember && matchesSubCat && matchesBeneficiary &&
                    matchesStatus && matchesSource && matchesDuplicates && matchesHighlighted && matchesQuery
        }

        val rolloverAmt = if (dateRange.startMillis != 0L) {
            com.example.ui.util.QuincenaFilterHelper.calculateQuincenaRollover(
                transactions = txList,
                targetYear = dateRange.year,
                targetMonthIndex = dateRange.monthIndex,
                targetQuincena = filters.quincena
            )
        } else 0.0

        // Métricas financieras calculadas estrictamente sobre el conjunto filtrado usando BigDecimal exacto
        val filteredRealIncBd = filtered.filter { it.isIngresoReal }
            .fold(java.math.BigDecimal.ZERO) { acc, tx -> acc.add(tx.effectiveAmountBigDecimal) }
        val filteredConfirmedExpBd = filtered.filter { it.isGastoReal }
            .fold(java.math.BigDecimal.ZERO) { acc, tx -> acc.add(tx.effectiveAmountBigDecimal) }
        val filteredSupuestoExpBd = filtered.filter { it.isGastoSupuesto }
            .fold(java.math.BigDecimal.ZERO) { acc, tx -> acc.add(tx.effectiveAmountBigDecimal) }
        val filteredTotalBothExpBd = filteredConfirmedExpBd.add(filteredSupuestoExpBd)
        val rolloverBd = java.math.BigDecimal.valueOf(rolloverAmt)

        val totalAmountToSpendBd = filteredRealIncBd.add(rolloverBd)
        val filteredRealRemainingBd = totalAmountToSpendBd.subtract(filteredConfirmedExpBd)
        val filteredProjectedRemainingBd = totalAmountToSpendBd.subtract(filteredTotalBothExpBd)

        val filteredRealInc = filteredRealIncBd.toDouble()
        val filteredConfirmedExp = filteredConfirmedExpBd.toDouble()
        val filteredSupuestoExp = filteredSupuestoExpBd.toDouble()
        val filteredTotalBothExp = filteredTotalBothExpBd.toDouble()
        val totalAmountToSpendVal = totalAmountToSpendBd.toDouble()
        val filteredRealRemaining = filteredRealRemainingBd.toDouble()
        val filteredProjectedRemaining = filteredProjectedRemainingBd.toDouble()
        val filteredRealNet = filteredRealRemaining
        val filteredProjNet = filteredProjectedRemaining

        val filteredCatMapBd = mutableMapOf<String, java.math.BigDecimal>()
        for (tx in filtered) {
            if (tx.type == TransactionType.EXPENSE && tx.affectsBudgetTotals && tx.status != TransactionStatus.DISCARDED) {
                filteredCatMapBd[tx.category] = (filteredCatMapBd[tx.category] ?: java.math.BigDecimal.ZERO)
                    .add(tx.effectiveAmountBigDecimal)
            }
        }
        val totFilteredExp = filteredCatMapBd.values
            .fold(java.math.BigDecimal.ZERO) { acc, v -> acc.add(v) }
            .toDouble()
        val filteredCatStats = filteredCatMapBd.map { (cat, amtBd) ->
            val amt = amtBd.toDouble()
            CategoryExpenseStat(
                category = cat,
                totalAmount = amt,
                percentageOfTotal = if (totFilteredExp > 0.0) (amt / totFilteredExp) * 100.0 else 0.0
            )
        }.sortedByDescending { it.totalAmount }

        var activeCount = 0
        if (filters.query.isNotBlank()) activeCount++
        if (filters.type != null) activeCount++
        if (filters.category != null) activeCount++
        if (filters.member != null && filters.member != "Todos") activeCount++
        if (filters.onlyHighlighted) activeCount++
        if (filters.period != FilterPeriod.MONTH) activeCount++
        if (filters.specificDateMillis != null) activeCount++
        if (filters.dateRangeStart != null || filters.dateRangeEnd != null) activeCount++
        if (filters.quincena != 0) activeCount++
        if (filters.subCategory != null) activeCount++
        if (!filters.beneficiary.isNullOrBlank()) activeCount++
        if (filters.status != null) activeCount++
        if (filters.source != null) activeCount++
        if (filters.duplicatesMode != DuplicateFilterMode.ALL) activeCount++

        val warnings = mutableListOf<RecurringExpenseWarning>()
        for (limit in budgets) {
            val usage = BudgetLimitCalculator.usage(
                spentAmount = BudgetLimitCalculator.monthlySpend(
                    transactions = txList,
                    category = limit.category,
                    year = dateRange.year,
                    monthIndex = dateRange.monthIndex
                ),
                limitAmount = limit.monthlyLimit
            )
            if (usage.isOverLimit) {
                warnings.add(
                    RecurringExpenseWarning(
                        title = "Límite excedido en ${limit.category}",
                        amount = usage.exceededBy.toDouble(),
                        dueDateMessage = "Has gastado ${FormatUtils.formatCurrency(usage.spent.toDouble(), BudgetLimitCalculator.QUETZAL_SYMBOL, filters.isDiscreet)} de tu límite de ${FormatUtils.formatCurrency(usage.limit.toDouble(), BudgetLimitCalculator.QUETZAL_SYMBOL, filters.isDiscreet)}. Excedido por ${FormatUtils.formatCurrency(usage.exceededBy.toDouble(), BudgetLimitCalculator.QUETZAL_SYMBOL, filters.isDiscreet)}.",
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

        // Métricas de Pagos y Compromisos
        val payments = plans.payments
        val beneficiaries = plans.beneficiaries

        val nowTime = System.currentTimeMillis()
        val totalPendingPaymentsVal = payments
            .filter { !it.isPaid && it.status != PaymentStatus.PAID }
            .fold(java.math.BigDecimal.ZERO) { acc, p -> acc.add(MoneyMinorUnits.toBigDecimal(p.effectiveAmountMinor)) }
            .toDouble()
        val upcomingPaymentsCountVal = payments.count { it.isUpcoming(nowTime, 7) }
        val overduePaymentsCountVal = payments.count { it.isOverdue(nowTime) }

        val (monthStartMillis, monthEndMillis) = run {
            val cal = Calendar.getInstance().apply {
                clear()
                set(Calendar.YEAR, dateRange.year)
                set(Calendar.MONTH, dateRange.monthIndex)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, maxDay)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            Pair(start, end)
        }
        val monthPaidPaymentsTotalVal = payments
            .filter { it.isPaid && (it.paidDateMillis ?: 0L) in monthStartMillis..monthEndMillis }
            .fold(java.math.BigDecimal.ZERO) { acc, p -> acc.add(MoneyMinorUnits.toBigDecimal(p.effectiveAmountMinor)) }
            .toDouble()

        val nextUpcomingPaymentVal = payments.filter { !it.isPaid && it.status != PaymentStatus.PAID }
            .minByOrNull { it.dueDateMillis }

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
            userMessage = message,
            // Métricas y filtros avanzados Quicken Simplifi
            filterPeriod = filters.period,
            filterSpecificDateMillis = filters.specificDateMillis,
            filterDateRangeStart = filters.dateRangeStart,
            filterDateRangeEnd = filters.dateRangeEnd,
            filterQuincena = filters.quincena,
            filterSubCategory = filters.subCategory,
            filterBeneficiary = filters.beneficiary,
            filterStatus = filters.status,
            filterSource = filters.source,
            filterDuplicatesMode = filters.duplicatesMode,
            activeFiltersCount = activeCount,
            suspectedDuplicatesCount = duplicateIds.size,
            filteredRealIncome = filteredRealInc,
            filteredRealExpense = filteredConfirmedExp,
            filteredRealNetBalance = filteredRealNet,
            filteredProjectedIncome = 0.0,
            filteredProjectedExpense = filteredSupuestoExp,
            filteredProjectedNetBalance = filteredProjNet,
            filteredCategorySubtotals = filteredCatStats,
            totalAmountToSpend = totalAmountToSpendVal,
            quincenaRolloverAmount = rolloverAmt,
            filteredConfirmedExpense = filteredConfirmedExp,
            filteredSupuestoExpense = filteredSupuestoExp,
            filteredTotalBothExpenses = filteredTotalBothExp,
            filteredRealRemainingBalance = filteredRealRemaining,
            filteredProjectedRemainingBalance = filteredProjectedRemaining,
            paymentCommitments = payments,
            paymentBeneficiaries = beneficiaries,
            totalPendingPayments = totalPendingPaymentsVal,
            upcomingPaymentsCount = upcomingPaymentsCountVal,
            overduePaymentsCount = overduePaymentsCountVal,
            monthPaidPaymentsTotal = monthPaidPaymentsTotalVal,
            nextUpcomingPayment = nextUpcomingPaymentVal,
            shoppingTemplates = plans.templates
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

    fun setFilterPeriod(period: String) {
        filterPeriodFlow.value = period
        if (period != FilterPeriod.MONTH) {
            filterSpecificDateFlow.value = null
            filterDateRangeStartFlow.value = null
            filterDateRangeEndFlow.value = null
        }
    }

    fun setFilterSpecificDate(millis: Long?) {
        filterSpecificDateFlow.value = millis
        if (millis != null) {
            filterDateRangeStartFlow.value = null
            filterDateRangeEndFlow.value = null
        }
    }

    fun setFilterDateRange(startMillis: Long?, endMillis: Long?) {
        filterDateRangeStartFlow.value = startMillis
        filterDateRangeEndFlow.value = endMillis
        if (startMillis != null || endMillis != null) {
            filterSpecificDateFlow.value = null
        }
    }

    fun setFilterQuincena(quincena: Int) {
        filterQuincenaFlow.value = quincena
    }

    fun setFilterSubCategory(subCategory: String?) {
        filterSubCategoryFlow.value = subCategory
    }

    fun setFilterBeneficiary(beneficiary: String?) {
        filterBeneficiaryFlow.value = beneficiary
    }

    fun setFilterStatus(status: String?) {
        filterStatusFlow.value = status
    }

    fun setFilterSource(source: String?) {
        filterSourceFlow.value = source
    }

    fun setFilterDuplicatesMode(mode: String) {
        filterDuplicatesModeFlow.value = mode
    }

    fun clearAllFilters() {
        searchQueryFlow.value = ""
        filterTypeFlow.value = null
        filterCategoryFlow.value = null
        filterMemberFlow.value = null
        onlyHighlightedFlow.value = false
        filterPeriodFlow.value = FilterPeriod.MONTH
        filterSpecificDateFlow.value = null
        filterDateRangeStartFlow.value = null
        filterDateRangeEndFlow.value = null
        filterQuincenaFlow.value = 0
        filterSubCategoryFlow.value = null
        filterBeneficiaryFlow.value = null
        filterStatusFlow.value = null
        filterSourceFlow.value = null
        filterDuplicatesModeFlow.value = DuplicateFilterMode.ALL
        isAllTimeFlow.value = false
    }

    fun deleteTransactions(ids: List<Long>) {
        viewModelScope.launch {
            repository.deleteTransactionsByIds(ids)
            showToast("${ids.size} registro(s) eliminado(s) correctamente.")
        }
    }

    fun markTransactionsAsLegitimate(ids: List<Long>) {
        viewModelScope.launch {
            repository.markTransactionsAsLegitimate(ids)
            showToast("${ids.size} registro(s) marcado(s) como legítimo(s).")
        }
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
                    val cleanNotes = tx.notes.replace("[PENDIENTE]", "")
                        .replace("Pendiente", "", ignoreCase = true)
                        .replace("[SUPUESTO]", "")
                        .replace("Supuesto", "", ignoreCase = true)
                        .trim()
                    val updated = tx.copy(
                        isTentative = false,
                        tentativeDecision = "CONFIRMED",
                        isVerified = true,
                        notes = if (cleanNotes.isNotBlank()) "$cleanNotes (Confirmado)" else "Confirmado"
                    )
                    repository.updateTransaction(updated)
                    showToast("Gasto confirmado y registrado en tus gastos reales.")
                }
                "DISCARD" -> {
                    val updated = tx.copy(
                        isTentative = true,
                        tentativeDecision = "DISCARDED",
                        notes = (tx.notes + " (Descartado: Ahorro)").trim()
                    )
                    repository.updateTransaction(updated)
                    showToast("¡Gasto supuesto no realizado! Ahorro preservado.")
                }
            }
        }
    }

    fun toggleGastoSupuesto(tx: TransactionEntity) {
        viewModelScope.launch {
            val willBeSupuesto = !tx.isGastoSupuesto
            val updated = if (willBeSupuesto) {
                tx.copy(
                    isTentative = true,
                    tentativeDecision = "PENDING"
                )
            } else {
                val cleanNotes = tx.notes.replace("[PENDIENTE]", "")
                    .replace("Pendiente", "", ignoreCase = true)
                    .replace("[SUPUESTO]", "")
                    .replace("Supuesto", "", ignoreCase = true)
                    .trim()
                tx.copy(
                    isTentative = false,
                    tentativeDecision = "CONFIRMED",
                    notes = cleanNotes
                )
            }
            repository.updateTransaction(updated)
            showToast(if (willBeSupuesto) "Marcado como gasto supuesto / pendiente." else "Marcado como gasto real / confirmado.")
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
            val amountMinor = MoneyMinorUnits.fromDouble(amount.coerceAtLeast(0.0))
            val displayAmount = MoneyMinorUnits.toDisplayDouble(amountMinor)
            val tx = TransactionEntity(
                title = title,
                amount = displayAmount,
                type = TransactionType.EXPENSE,
                category = category,
                dateMillis = System.currentTimeMillis(),
                isAntExpense = true,
                notes = notes,
                amountMinor = amountMinor
            )
            repository.insertTransaction(tx)
            celebrationTriggerFlow.value = System.currentTimeMillis()
            showToast("¡Listo! Se guardó Q${"%.2f".format(displayAmount)} en $title.")
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

    private val isImportCsvInProgress = java.util.concurrent.atomic.AtomicBoolean(false)

    fun importCsv(csvContent: String, onComplete: ((ImportSummary) -> Unit)? = null) {
        if (!isImportCsvInProgress.compareAndSet(false, true)) {
            return
        }
        viewModelScope.launch {
            try {
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
            } finally {
                kotlinx.coroutines.delay(600L)
                isImportCsvInProgress.set(false)
            }
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
        priceSource: String = "MANUAL"
    ) {
        viewModelScope.launch {
            repository.addOrUpdateCartItem(
                name = name,
                quantity = quantity,
                unit = unit,
                brand = brand,
                price = price,
                store = store,
                notes = notes,
                url = url,
                priceSource = if (priceSource.startsWith("PANTRY_")) "MANUAL" else priceSource
            )
            showToast("Producto agregado a la lista del súper.")
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
        notes: String = "",
        brand: String = ""
    ) {
        viewModelScope.launch {
            repository.addOrUpdatePantryItem(
                name = name,
                quantity = quantity,
                unit = unit,
                status = status,
                price = price,
                store = store,
                notes = notes,
                brand = brand
            )
            showToast("Artículo guardado en Mi Despensa.")
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

    fun checkoutCartAsExpense(
        title: String,
        amount: Double,
        storeName: String,
        purchasedNames: List<String>,
        updatePantryOnCheckout: Boolean = true
    ) {
        if (!isCheckoutInProgress.compareAndSet(false, true)) {
            return
        }
        viewModelScope.launch {
            try {
                val success = repository.confirmPurchaseAndUpdatePantry(
                    title = title,
                    amount = amount,
                    storeName = storeName,
                    purchasedItemNames = purchasedNames,
                    updatePantryOnCheckout = updatePantryOnCheckout
                )
                if (success) {
                    showToast("Compra registrada de Q${"%,.2f".format(amount)} en Alimentación & Súper.")
                }
            } finally {
                kotlinx.coroutines.delay(800L)
                isCheckoutInProgress.set(false)
            }
        }
    }

    fun saveCartAsTemplate(name: String, description: String = "") {
        viewModelScope.launch {
            val id = repository.saveCartAsTemplate(name, description)
            if (id > 0) {
                showToast("Plantilla «$name» guardada exitosamente.")
            } else {
                showToast("No hay productos en la lista activa para guardar.")
            }
        }
    }

    fun applyTemplateToCart(templateId: Long) {
        viewModelScope.launch {
            val count = repository.applyTemplateToCart(templateId)
            if (count > 0) {
                showToast("¡Listo! $count productos agregados a tu lista del súper.")
            } else {
                showToast("La plantilla seleccionada no contiene productos.")
            }
        }
    }

    fun duplicateTemplate(templateId: Long, newName: String = "") {
        viewModelScope.launch {
            repository.duplicateTemplate(templateId, newName)
            showToast("Plantilla duplicada.")
        }
    }

    fun deleteTemplate(template: ShoppingTemplateEntity) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
            showToast("Plantilla «${template.name}» eliminada.")
        }
    }

    fun updateTemplate(template: ShoppingTemplateEntity) {
        viewModelScope.launch {
            repository.updateTemplate(template)
            showToast("Plantilla actualizada.")
        }
    }

    // =========================================================================
    // MÓDULO PAGOS: MÉTODOS DE COMPROMISOS FAMILIARES & BENEFICIARIOS
    // =========================================================================

    fun addPaymentCommitment(
        title: String,
        beneficiaryName: String,
        description: String = "",
        amount: Double,
        dueDateMillis: Long,
        category: String = "Vivienda & Servicios",
        frequency: String = PaymentFrequency.ONCE,
        isVariableAmount: Boolean = false,
        reminderDaysBefore: Int = 1,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val amountMinor = MoneyMinorUnits.fromDouble(amount.coerceAtLeast(0.0))
            val commitment = PaymentCommitmentEntity(
                title = title.trim(),
                beneficiaryName = beneficiaryName.trim(),
                description = description.trim(),
                amount = MoneyMinorUnits.toDisplayDouble(amountMinor),
                dueDateMillis = dueDateMillis,
                category = category,
                frequency = frequency,
                isVariableAmount = isVariableAmount,
                reminderDaysBefore = reminderDaysBefore,
                notes = notes.trim(),
                amountMinor = amountMinor
            )
            val savedId = repository.createPaymentCommitment(commitment)
            if (reminderDaysBefore >= 0) {
                com.example.data.service.PaymentReminderManager.scheduleReminder(
                    getApplication(),
                    commitment.copy(id = savedId)
                )
            }
            showToast("Compromiso de pago guardado y proyectado.")
        }
    }

    fun markPaymentAsPaid(commitmentId: Long) {
        viewModelScope.launch {
            val success = repository.markPaymentAsPaid(commitmentId)
            if (success) {
                com.example.data.service.PaymentReminderManager.cancelReminder(getApplication(), commitmentId)
                showToast("¡Pago confirmado! Registrado en tus gastos reales.")
            } else {
                showToast("El pago ya se encontraba confirmado.")
            }
        }
    }

    fun updatePaymentCommitment(commitment: PaymentCommitmentEntity) {
        viewModelScope.launch {
            repository.updatePaymentCommitment(commitment)
            if (commitment.reminderDaysBefore >= 0 && !commitment.isPaid) {
                com.example.data.service.PaymentReminderManager.scheduleReminder(getApplication(), commitment)
            } else {
                com.example.data.service.PaymentReminderManager.cancelReminder(getApplication(), commitment.id)
            }
            showToast("Compromiso de pago actualizado.")
        }
    }

    fun deletePaymentCommitment(commitment: PaymentCommitmentEntity) {
        viewModelScope.launch {
            com.example.data.service.PaymentReminderManager.cancelReminder(getApplication(), commitment.id)
            repository.deletePaymentCommitment(commitment)
            showToast("Compromiso de pago eliminado.")
        }
    }

    fun addBeneficiary(
        name: String,
        category: String = "Vivienda & Servicios",
        defaultDescription: String = "",
        phoneOrContact: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.addBeneficiary(
                BeneficiaryEntity(
                    name = name.trim(),
                    category = category,
                    defaultDescription = defaultDescription.trim(),
                    phoneOrContact = phoneOrContact.trim(),
                    notes = notes.trim()
                )
            )
            showToast("Beneficiario «$name» guardado.")
        }
    }

    fun updateBeneficiary(beneficiary: BeneficiaryEntity) {
        viewModelScope.launch {
            repository.updateBeneficiary(beneficiary)
            showToast("Beneficiario actualizado.")
        }
    }

    fun deleteBeneficiary(beneficiary: BeneficiaryEntity) {
        viewModelScope.launch {
            repository.deleteBeneficiary(beneficiary)
            showToast("Beneficiario eliminado.")
        }
    }
}
