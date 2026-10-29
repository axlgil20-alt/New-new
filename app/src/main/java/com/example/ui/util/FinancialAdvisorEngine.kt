package com.example.ui.util

import com.example.ui.viewmodel.BudgetUiState
import kotlin.math.abs

/**
 * FinancialAdvisorEngine
 *
 * Motor inteligente de asesoría financiera local de QUETZI.
 * Procesa consultas del usuario en lenguaje natural y genera diagnósticos precisos
 * distinguiendo estrictamente entre:
 *  - Margen de Ahorro (Monto en Q y Tasa %)
 *  - Saldo Disponible (Dinero líquido: Ingresos - Gastos)
 *  - Presupuesto Restante (Límites mensuales asignados - Gastos ejecutados)
 *  - Gastos Hormiga y Categorías
 *  - Metas de Ahorro y Simulación de Supuestos
 */
object FinancialAdvisorEngine {

    data class SavingsMarginDetails(
        val marginAmount: Double,
        val marginRate: Double,
        val income: Double,
        val expense: Double,
        val hasIncome: Boolean,
        val hasTransactionsInPeriod: Boolean,
        val hasTransactionsInDb: Boolean,
        val isDeficit: Boolean
    )

    data class BudgetRemainingDetails(
        val totalLimit: Double,
        val totalSpentInLimits: Double,
        val remainingBudget: Double,
        val overruns: List<String>,
        val hasLimits: Boolean
    )

    val QUICK_QUESTIONS = listOf(
        "📈 ¿Cuál es mi margen de ahorro?",
        "📊 ¿Cuál es mi presupuesto restante?",
        "💰 ¿Cuál es mi saldo disponible?",
        "💵 ¿Cuáles son mis ingresos y gastos?",
        "🫓 ¿Cuánto he gastado en hormigas?",
        "🎯 ¿Cómo van mis metas de ahorro?",
        "🟣 ¿Me alcanza para comprar un supuesto?",
        "🛒 ¿Cómo va mi lista de Smart Cart?",
        "👁️ ¿Cómo activo el Modo Discreto?",
        "🇬🇹 ¿Cómo uso el FAB Chapín (+)?"
    )

    /**
     * Calcula los detalles exactos del Margen de Ahorro para el período activo.
     */
    fun calculateSavingsMargin(uiState: BudgetUiState): SavingsMarginDetails {
        val income = uiState.totalIncome
        val expense = uiState.totalExpense
        val margin = income - expense
        val hasIncome = income > 0.0
        val hasTransactionsInPeriod = uiState.filteredTransactions.isNotEmpty() || income > 0.0 || expense > 0.0
        val hasTransactionsInDb = uiState.totalDbTransactionsCount > 0 || hasTransactionsInPeriod

        val rate = if (hasIncome) {
            ((margin / income) * 100.0).coerceIn(-100.0, 100.0)
        } else {
            0.0
        }

        return SavingsMarginDetails(
            marginAmount = margin,
            marginRate = rate,
            income = income,
            expense = expense,
            hasIncome = hasIncome,
            hasTransactionsInPeriod = hasTransactionsInPeriod,
            hasTransactionsInDb = hasTransactionsInDb,
            isDeficit = margin < 0.0
        )
    }

    /**
     * Calcula los detalles del Presupuesto Restante (límites mensuales vs gasto en categorías presupuestadas).
     */
    fun calculateBudgetRemaining(uiState: BudgetUiState): BudgetRemainingDetails {
        val limits = uiState.budgetLimits
        if (limits.isEmpty()) {
            return BudgetRemainingDetails(
                totalLimit = 0.0,
                totalSpentInLimits = 0.0,
                remainingBudget = 0.0,
                overruns = emptyList(),
                hasLimits = false
            )
        }

        val totalLimit = limits.sumOf { it.monthlyLimit }
        val overruns = mutableListOf<String>()
        var spentInLimits = 0.0

        for (limit in limits) {
            val spent = uiState.categoryStats.find { it.category == limit.category }?.totalAmount ?: 0.0
            spentInLimits += spent
            if (spent > limit.monthlyLimit) {
                overruns.add(limit.category)
            }
        }

        return BudgetRemainingDetails(
            totalLimit = totalLimit,
            totalSpentInLimits = spentInLimits,
            remainingBudget = totalLimit - spentInLimits,
            overruns = overruns,
            hasLimits = true
        )
    }

    /**
     * Genera la respuesta del Asesor Quetzi IA basada en la consulta y los datos de Room.
     */
    fun generateAdvisorResponse(userQuery: String, uiState: BudgetUiState): String {
        val q = userQuery.trim().lowercase()
        val currency = uiState.currencySymbol
        val period = uiState.currentMonthLabel

        // 1. Margen de Ahorro (distinguir de metas de ahorro)
        val isMarginQuery = q.contains("margen") ||
                q.contains("tasa de ahorro") ||
                (q.contains("cuanto") && (q.contains("ahorro") || q.contains("ahorrar"))) ||
                (q.contains("puedo") && q.contains("ahorr")) ||
                q.contains("superavit")

        if (isMarginQuery) {
            val marginDetails = calculateSavingsMargin(uiState)
            return when {
                marginDetails.hasIncome -> {
                    val amountStr = "$currency${"%,.2f".format(marginDetails.marginAmount)}"
                    val rateStr = "${"%.1f".format(marginDetails.marginRate)}%"
                    if (marginDetails.marginAmount > 0.0) {
                        "Tu margen de ahorro en $period es de $amountStr ($rateStr de tus ingresos).\n\n" +
                                "• Ingresos registrados: $currency${"%,.2f".format(marginDetails.income)}\n" +
                                "• Gastos registrados: $currency${"%,.2f".format(marginDetails.expense)}\n" +
                                "• Superávit disponible: $amountStr\n\n" +
                                if (marginDetails.marginRate >= 20.0) {
                                    "¡Excelente disciplina chapina! Superas la regla del 20% recomendada para ahorro e inversión."
                                } else {
                                    "Estás ahorrando, pero por debajo de la meta del 20%. Te sugiero optimizar compras o revisar tus gastos hormiga."
                                }
                    } else if (marginDetails.marginAmount == 0.0) {
                        "En $period tus ingresos ($currency${"%,.2f".format(marginDetails.income)}) igualan exactamente a tus gastos.\n\n" +
                                "Tu margen de ahorro actual es de ${currency}0.00 (0.0%). Estás en equilibrio, pero sin colchón para imprevistos."
                    } else {
                        "⚠️ En $period no cuentas con margen de ahorro positivo. Tienes un déficit de $currency${"%,.2f".format(abs(marginDetails.marginAmount))}.\n\n" +
                                "• Ingresos registrados: $currency${"%,.2f".format(marginDetails.income)}\n" +
                                "• Gastos registrados: $currency${"%,.2f".format(marginDetails.expense)}\n\n" +
                                "Tus gastos superan tus ingresos. Revisa compras no indispensables o registra ingresos pendientes para revertir el saldo negativo."
                    }
                }

                marginDetails.expense > 0.0 -> {
                    "En $period registras gastos por $currency${"%,.2f".format(marginDetails.expense)}, pero no tienes ingresos registrados.\n\n" +
                            "Por ello no es posible calcular un margen de ahorro positivo. Registra tus ingresos con el botón (+) para determinar tu margen real."
                }

                marginDetails.hasTransactionsInDb && uiState.totalDbTransactionsCount > 0 -> {
                    "En el período actual ($period) no hay movimientos registrados, pero tienes ${uiState.totalDbTransactionsCount} transacciones en tu historial general ($currency${"%,.2f".format(uiState.allTimeIncome)} en ingresos y $currency${"%,.2f".format(uiState.allTimeExpense)} en gastos).\n\n" +
                            "Para ver el margen de ahorro de ese historial, cambia de mes en el selector superior o selecciona 'Todo el Historial'."
                }

                else -> {
                    "Aún no tienes transacciones registradas en la aplicación para calcular tu margen de ahorro.\n\n" +
                            "Comienza registrando tus ingresos y gastos con el botón flotante (+) o importa tu archivo Excel desde Ajustes."
                }
            }
        }

        // 2. Presupuesto Restante vs Saldo Disponible (distinción estricta)
        val isBudgetRemainingQuery = q.contains("presupuesto restante") ||
                q.contains("cuanto me queda de presupuesto") ||
                (q.contains("presupuesto") && (q.contains("queda") || q.contains("resta") || q.contains("disponible"))) ||
                q.contains("limite") || q.contains("limites") || q.contains("tope")

        if (isBudgetRemainingQuery) {
            val budgetDetails = calculateBudgetRemaining(uiState)
            return if (!budgetDetails.hasLimits) {
                "No tienes límites de presupuesto configurados actualmente en la pestaña 'Límites'.\n\n" +
                        "Diferencia clave en tus finanzas:\n" +
                        "• Saldo Disponible: $currency${"%,.2f".format(uiState.netBalance)} (dinero real en tu bolsa tras gastos).\n" +
                        "• Presupuesto Restante: ${currency}0.00 (aún no has definido topes de gasto por categoría).\n\n" +
                        "Ve a 'Límites' y pulsa '+' para fijar presupuestos mensuales en Alimentación, Transporte, etc."
            } else {
                val totalLimitStr = "$currency${"%,.2f".format(budgetDetails.totalLimit)}"
                val spentStr = "$currency${"%,.2f".format(budgetDetails.totalSpentInLimits)}"
                val remStr = "$currency${"%,.2f".format(budgetDetails.remainingBudget)}"
                val overrunsMsg = if (budgetDetails.overruns.isNotEmpty()) {
                    "⚠️ Has sobrepasado el tope en: ${budgetDetails.overruns.joinToString(", ")}."
                } else {
                    "¡Todas tus categorías presupuestadas están dentro de su límite!"
                }

                "Resumen de Presupuesto Mensual ($period):\n\n" +
                        "• Presupuesto Total Asignado: $totalLimitStr\n" +
                        "• Gastado en categorías con presupuesto: $spentStr\n" +
                        "• Presupuesto Restante: $remStr\n" +
                        "• Saldo Líquido Disponible: $currency${"%,.2f".format(uiState.netBalance)}\n\n" +
                        overrunsMsg
            }
        }

        // 3. Saldo Disponible Real
        val isBalanceQuery = q.contains("saldo") || q.contains("balance") ||
                q.contains("cuanto tengo") || q.contains("dinero disponible") ||
                q.contains("efectivo") || q.contains("saldo real")

        if (isBalanceQuery) {
            val balanceStr = "$currency${"%,.2f".format(uiState.netBalance)}"
            val incomeStr = "$currency${"%,.2f".format(uiState.totalIncome)}"
            val expenseStr = "$currency${"%,.2f".format(uiState.totalExpense)}"

            val tentativeMsg = if (uiState.tentativeExpensesPending > 0.0) {
                "\n\n🟣 Compras supuestas pendientes: $currency${"%,.2f".format(uiState.tentativeExpensesPending)}.\n" +
                        "Saldo proyectado si las realizas: $currency${"%,.2f".format(uiState.simulatedNetBalance)}."
            } else ""

            return if (uiState.totalIncome == 0.0 && uiState.totalExpense == 0.0) {
                if (uiState.totalDbTransactionsCount > 0) {
                    "En $period no tienes movimientos registrados. Tu saldo disponible para este mes es de ${currency}0.00.\n\n" +
                            "Tienes ${uiState.totalDbTransactionsCount} transacciones en tu historial general. Selecciona 'Todo el Historial' para ver el balance acumulado."
                } else {
                    "Tu saldo disponible es de ${currency}0.00 porque aún no has registrado movimientos.\n\n" +
                            "Registra tu primer ingreso con el botón verde (+) para comenzar a gestionar tu presupuesto."
                }
            } else {
                "Tu saldo disponible en $period es de $balanceStr.\n\n" +
                        "• Ingresos registrados: $incomeStr\n" +
                        "• Gastos registrados: $expenseStr$tentativeMsg\n\n" +
                        if (uiState.netBalance >= 0.0) {
                            "¡Tienes saldo a favor! Recuerda reservar una parte para tu fondo de emergencia."
                        } else {
                            "⚠️ Estás en números rojos por $currency${"%,.2f".format(abs(uiState.netBalance))}. Frena compras no prioritarias."
                        }
            }
        }

        // 4. Ingresos
        val isIncomeQuery = q.contains("ingreso") || q.contains("ingresos") ||
                q.contains("ganancia") || q.contains("sueldo") || q.contains("salario") ||
                q.contains("cuanto gane")

        if (isIncomeQuery) {
            return if (uiState.totalIncome > 0.0) {
                "Tus ingresos registrados en $period suman $currency${"%,.2f".format(uiState.totalIncome)}.\n\n" +
                        "• Gastos descontados: $currency${"%,.2f".format(uiState.totalExpense)}\n" +
                        "• Remanente disponible: $currency${"%,.2f".format(uiState.netBalance)}\n\n" +
                        "💡 Consejo Chapín: Si recibes un ingreso extra (remesa, bono o cuchubal), destina al menos el 20% a tu ahorro."
            } else {
                "No tienes ingresos registrados en $period.\n\n" +
                        "Para registrar tu sueldo, ventas o remesas, toca el botón (+) y selecciona 'Ingreso'."
            }
        }

        // 5. Gastos Generales y Categorías
        val isExpenseQuery = q.contains("gasto") || q.contains("gastos") ||
                q.contains("en que gaste") || q.contains("egreso") || q.contains("salidas de dinero")

        if (isExpenseQuery && !q.contains("hormiga")) {
            return if (uiState.totalExpense > 0.0) {
                val topCategories = uiState.categoryStats.take(3)
                val breakdown = if (topCategories.isNotEmpty()) {
                    "\n\nTus principales rubros de gasto son:\n" + topCategories.joinToString("\n") { stat ->
                        "• ${stat.category}: $currency${"%,.2f".format(stat.totalAmount)} (${"%.1f".format(stat.percentageOfTotal)}%)"
                    }
                } else ""

                "Tus gastos registrados en $period suman $currency${"%,.2f".format(uiState.totalExpense)}.$breakdown\n\n" +
                        "• Saldo neto restante: $currency${"%,.2f".format(uiState.netBalance)}"
            } else {
                "No tienes gastos registrados en $period. ¡Tus ingresos están intactos!"
            }
        }

        // 6. Gastos Hormiga
        val isAntQuery = q.contains("hormiga") || q.contains("tortilla") ||
                q.contains("tuc") || q.contains("recarga") || q.contains("chucheria") ||
                q.contains("antojito") || q.contains("cafecito")

        if (isAntQuery) {
            val antSum = uiState.filteredTransactions.filter { it.isAntExpense }.sumOf { it.amount }
            val antPct = if (uiState.totalIncome > 0) (antSum / uiState.totalIncome) * 100 else 0.0
            return if (antSum > 0.0) {
                "¡Ojo al piojo con los gastos hormiga! 🐜\n\n" +
                        "Llevas acumulados $currency${"%,.2f".format(antSum)} en compras pequeñas, lo que representa el ${"%.1f".format(antPct)}% de tus ingresos.\n\n" +
                        "💡 Tip de Quetzi: Si reduces a la mitad esos cafecitos o compras de paso, tendrías $currency${"%,.2f".format(antSum / 2)} extras para tu alcancía este mes."
            } else {
                if (uiState.filteredTransactions.isNotEmpty()) {
                    "¡Felicidades! 🎉 No tienes gastos hormiga registrados en este ciclo. ¡Gran disciplina financiera!"
                } else {
                    "No hay gastos hormiga registrados porque no tienes transacciones en este período."
                }
            }
        }

        // 7. Metas de Ahorro (Alcancías)
        val isGoalQuery = q.contains("meta") || q.contains("metas") ||
                q.contains("alcancia") || q.contains("objetivo de ahorro")

        if (isGoalQuery) {
            val goals = uiState.savingsGoals
            return if (goals.isNotEmpty()) {
                val totalSaved = goals.sumOf { it.currentAmount }
                val totalTarget = goals.sumOf { it.targetAmount }
                val pct = if (totalTarget > 0) (totalSaved / totalTarget) * 100.0 else 0.0
                val goalList = goals.take(3).joinToString("\n") { g ->
                    "• ${g.name}: $currency${"%,.2f".format(g.currentAmount)} de $currency${"%,.2f".format(g.targetAmount)}"
                }

                "Tienes ${goals.size} meta(s) de ahorro configuradas:\n\n" +
                        "$goalList\n\n" +
                        "• Total Ahorrado: $currency${"%,.2f".format(totalSaved)} de $currency${"%,.2f".format(totalTarget)} (${"%.1f".format(pct)}% completado).\n\n" +
                        "💡 Consejo Chapín: Usa los botones de abono rápido (+Q50, +Q100, +Q200) en la pestaña 'Límites' para aportar antes de gastar."
            } else {
                "No tienes metas de ahorro configuradas aún.\n\n" +
                        "Puedes definir metas para un Fondo de Emergencia, Enganche o Estudios en la pestaña 'Límites' -> 'Metas de Ahorro'."
            }
        }

        // 8. Compras Supuestas / Planificación
        val isTentativeQuery = q.contains("supuesto") || q.contains("simul") ||
                q.contains("comprar") || q.contains("alcanza")

        if (isTentativeQuery) {
            val pending = uiState.tentativeExpensesPending
            val simBalance = uiState.netBalance - pending
            return if (pending > 0.0) {
                "Tienes compras supuestas pendientes por $currency${"%,.2f".format(pending)}.\n\n" +
                        if (simBalance >= 0.0) {
                            "Si realizas estas compras, te quedará un saldo disponible de $currency${"%,.2f".format(simBalance)}. ¡Te alcanza bien!"
                        } else {
                            "⚠️ Si compras eso, quedarás en déficit por $currency${"%,.2f".format(abs(simBalance))}. Mi recomendación: presiona 'Descartar' en la transacción para enviar ese dinero a tu ahorro."
                        }
            } else {
                "No tienes compras supuestas pendientes en este momento.\n\n" +
                        "Tu saldo disponible es de $currency${"%,.2f".format(uiState.netBalance)}. Cualquier gasto que realices se descontará de este monto."
            }
        }

        // 9. Smart Cart / Despensa
        val isCartQuery = q.contains("smart cart") || q.contains("supermercado") ||
                q.contains("despensa") || q.contains("lista de compras") || q.contains("super")

        if (isCartQuery) {
            val cartCount = uiState.smartCartItems.size
            val pantryItems = uiState.smartPantryItems
            val yellowOrRed = pantryItems.count { it.priceSource.contains("AMARILLO") || it.priceSource.contains("ROJO") }
            val budget = uiState.smartCartBudget

            return "Estado de tu Smart Cart Quetzi:\n\n" +
                    "• Artículos en tu lista: $cartCount\n" +
                    "• Productos por reponer (despensa amarilla/roja): $yellowOrRed\n" +
                    "• Presupuesto asignado al súper: $currency${"%,.2f".format(budget)}\n\n" +
                    "Puedes abrir Smart Cart desde el acceso rápido para cotizar en supermercados de Guatemala, usar el escáner OCR o registrar tu compra."
        }

        // 10. Consejos Financieros y Mejora
        val isAdviceQuery = q.contains("consejo") || q.contains("mejorar") ||
                q.contains("recomendacion") || q.contains("optimizar") || q.contains("ayuda")

        if (isAdviceQuery) {
            val marginDetails = calculateSavingsMargin(uiState)
            val antSum = uiState.filteredTransactions.filter { it.isAntExpense }.sumOf { it.amount }
            val tips = mutableListOf<String>()

            if (marginDetails.isDeficit) {
                tips.add("⚠️ Estás en déficit ($currency${"%,.2f".format(abs(marginDetails.marginAmount))}). Suspende compras no indispensables.")
            }
            if (antSum > 50.0) {
                tips.add("🐜 Llevas $currency${"%,.2f".format(antSum)} en gastos hormiga. Reducirlos generará ahorro inmediato.")
            }
            if (uiState.savingsGoals.isEmpty()) {
                tips.add("🎯 Configura al menos una meta de ahorro (ej. Fondo de Emergencia) en la pestaña 'Límites'.")
            }
            if (marginDetails.hasIncome && marginDetails.marginRate < 20.0 && !marginDetails.isDeficit) {
                tips.add("📈 Tu tasa de ahorro es del ${"%.1f".format(marginDetails.marginRate)}%. Apunta al 20% apartando tu ahorro al recibir la quincena.")
            }

            if (tips.isEmpty()) {
                tips.add("💡 Mantén la regla 50/30/20: 50% necesidades básicas, 30% gustos y 20% ahorro e inversión.")
                tips.add("🛡️ Activa el Modo Discreto (ícono del ojo) al consultar tus finanzas en la calle o transporte.")
            }

            return "Diagnóstico Financiero de Quetzi ($period):\n\n" +
                    tips.joinToString("\n\n")
        }

        // 11. Herramientas del Sistema (Modo Discreto, Excel, FAB)
        if (q.contains("discreto") || q.contains("ojo") || q.contains("privacidad") || q.contains("ocultar")) {
            return "El Modo Discreto (👁️) oculta al instante todos los montos de dinero mostrándolos como 'Q ****.**'.\n\n" +
                    "Para activarlo o desactivarlo, presiona el ícono del Ojito en la esquina superior derecha del Inicio. ¡Es ideal si vas en camioneta o en la calle!"
        }

        if (q.contains("excel") || q.contains("csv") || q.contains("exportar") || q.contains("importar")) {
            return "Puedes importar y exportar tus finanzas desde Ajustes -> 'Importar / Exportar Excel & CSV'.\n\n" +
                    "Admite archivos Excel (.xlsx) y texto (.csv) compatibles con hojas de cálculo."
        }

        if (q.contains("fab") || q.contains("boton") || q.contains("+") || q.contains("mas")) {
            return "El botón flotante verde (+) en Inicio te permite registrar rápidamente transacciones y gastos hormiga típicos como tortillas, pasaje o recargas con un solo toque."
        }

        // 12. Caso General / Consulta no clasificada
        return if (uiState.filteredTransactions.isEmpty() && uiState.totalDbTransactionsCount == 0) {
            "No tengo registros financieros suficientes para responder a tu consulta \"$userQuery\".\n\n" +
                    "Por favor registra tus primeros ingresos o gastos con el botón (+) o importa tu archivo Excel para que pueda analizar tus datos."
        } else {
            val marginDetails = calculateSavingsMargin(uiState)
            val marginStr = if (marginDetails.hasIncome) "${"%.1f".format(marginDetails.marginRate)}%" else "Sin ingresos registrados"
            "Sobre tu consulta \"$userQuery\", aquí tienes el estado de tus finanzas en $period:\n\n" +
                    "• Saldo Disponible: $currency${"%,.2f".format(uiState.netBalance)}\n" +
                    "• Margen de Ahorro: $marginStr\n" +
                    "• Salud Financiera: ${uiState.financialHealthScore}%\n\n" +
                    "Puedes hacerme preguntas directas como:\n" +
                    "• \"¿Cuál es mi margen de ahorro?\"\n" +
                    "• \"¿Cuál es mi presupuesto restante?\"\n" +
                    "• \"¿Cuáles son mis ingresos y gastos?\"\n" +
                    "• \"¿Cuánto he gastado en hormigas?\""
        }
    }
}
