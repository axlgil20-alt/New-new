package com.example.ui.viewmodel

import com.example.data.model.BudgetLimitEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.SmartCartItemEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.util.FinancialAdvisorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para la Fase 2:
 * 1. Motor del Asesor Financiero IA (margen de ahorro, presupuesto restante vs saldo, detección de datos vacíos).
 * 2. Distinción entre margen de ahorro y metas de ahorro.
 * 3. Operaciones de datos y Smart Cart (eliminación individual, masiva, persistencia).
 */
class AdvisorAndDataOperationsTest {

    @Test
    fun testSavingsMarginCalculation_positive() {
        val uiState = BudgetUiState(
            totalIncome = 10000.0,
            totalExpense = 6000.0,
            netBalance = 4000.0,
            currencySymbol = "Q "
        )

        val margin = FinancialAdvisorEngine.calculateSavingsMargin(uiState)
        assertEquals(4000.0, margin.marginAmount, 0.01)
        assertEquals(40.0, margin.marginRate, 0.01)
        assertTrue(margin.hasIncome)
        assertFalse(margin.isDeficit)

        val response = FinancialAdvisorEngine.generateAdvisorResponse("¿Cuál es mi margen de ahorro?", uiState)
        assertTrue("Debe contener el monto en Quetzales", response.contains("Q 4,000.00") || response.contains("4,000"))
        assertTrue("Debe contener la tasa porcentual del 40%", response.contains("40.0%"))
        assertFalse("No debe confundir con metas de ahorro", response.contains("meta(s) de ahorro configuradas"))
    }

    @Test
    fun testSavingsMarginCalculation_deficit() {
        val uiState = BudgetUiState(
            totalIncome = 5000.0,
            totalExpense = 7000.0,
            netBalance = -2000.0,
            currencySymbol = "Q "
        )

        val margin = FinancialAdvisorEngine.calculateSavingsMargin(uiState)
        assertEquals(-2000.0, margin.marginAmount, 0.01)
        assertTrue(margin.isDeficit)

        val response = FinancialAdvisorEngine.generateAdvisorResponse("¿Cuánto puedo ahorrar este mes?", uiState)
        assertTrue("Debe alertar sobre déficit", response.contains("déficit") || response.contains("Déficit"))
        assertTrue("Debe mostrar el monto del déficit", response.contains("2,000.00"))
    }

    @Test
    fun testSavingsMarginCalculation_zeroIncomeWithExpenses() {
        val uiState = BudgetUiState(
            totalIncome = 0.0,
            totalExpense = 1500.0,
            netBalance = -1500.0,
            currencySymbol = "Q "
        )

        val margin = FinancialAdvisorEngine.calculateSavingsMargin(uiState)
        assertFalse("No tiene ingresos registrados", margin.hasIncome)

        val response = FinancialAdvisorEngine.generateAdvisorResponse("¿Cuál es mi margen de ahorro?", uiState)
        assertTrue("Debe indicar que no hay ingresos registrados", response.contains("no tienes ingresos registrados") || response.contains("no tienes ningún ingreso"))
        assertFalse("No debe inventar 0.0% como porcentaje válido", response.contains("(0.0% de tus ingresos)"))
    }

    @Test
    fun testSavingsMarginCalculation_emptyCurrentMonthWithHistoryInDb() {
        val uiState = BudgetUiState(
            totalIncome = 0.0,
            totalExpense = 0.0,
            netBalance = 0.0,
            totalDbTransactionsCount = 45,
            allTimeIncome = 25000.0,
            allTimeExpense = 18000.0,
            currentMonthLabel = "Octubre 2026",
            currencySymbol = "Q "
        )

        val response = FinancialAdvisorEngine.generateAdvisorResponse("¿Cuál es mi margen de ahorro?", uiState)
        assertTrue("Debe detectar transacciones en el historial acumulado", response.contains("45 transacciones"))
        assertTrue("Debe sugerir ver Todo el Historial", response.contains("Todo el Historial"))
        assertFalse("No debe decir que el margen es 0% como si estuviera vacío", response.contains("margen de ahorro actual es de Q 0.00"))
    }

    @Test
    fun testSavingsMarginCalculation_completelyEmptyDb() {
        val uiState = BudgetUiState(
            totalIncome = 0.0,
            totalExpense = 0.0,
            netBalance = 0.0,
            totalDbTransactionsCount = 0,
            currencySymbol = "Q "
        )

        val response = FinancialAdvisorEngine.generateAdvisorResponse("¿Cuál es mi margen de ahorro?", uiState)
        assertTrue("Debe indicar que aún no hay transacciones", response.contains("Aún no tienes transacciones registradas"))
    }

    @Test
    fun testDistinguishBalanceVsBudgetRemaining() {
        val uiState = BudgetUiState(
            totalIncome = 8000.0,
            totalExpense = 3000.0,
            netBalance = 5000.0,
            budgetLimits = listOf(
                BudgetLimitEntity(category = "Alimentación", monthlyLimit = 2500.0),
                BudgetLimitEntity(category = "Transporte", monthlyLimit = 1000.0)
            ),
            categoryStats = listOf(
                CategoryExpenseStat(category = "Alimentación", totalAmount = 1800.0, percentageOfTotal = 60.0),
                CategoryExpenseStat(category = "Transporte", totalAmount = 1200.0, percentageOfTotal = 40.0)
            ),
            currencySymbol = "Q "
        )

        // Consulta de Saldo Disponible
        val balanceResponse = FinancialAdvisorEngine.generateAdvisorResponse("¿Cuál es mi saldo real?", uiState)
        assertTrue("Debe reportar el saldo líquido de Q 5,000", balanceResponse.contains("5,000.00"))
        assertFalse("No debe confundir con el presupuesto restante", balanceResponse.contains("Presupuesto Total Asignado"))

        // Consulta de Presupuesto Restante
        val budgetResponse = FinancialAdvisorEngine.generateAdvisorResponse("¿Cuánto me queda de presupuesto?", uiState)
        assertTrue("Debe calcular el presupuesto total de Q 3,500", budgetResponse.contains("3,500.00"))
        assertTrue("Debe calcular el gasto en límites de Q 3,000", budgetResponse.contains("3,000.00"))
        assertTrue("Debe calcular el presupuesto restante de Q 500", budgetResponse.contains("500.00"))
        assertTrue("Debe alertar sobre Transporte sobrepasado", budgetResponse.contains("Transporte"))
    }

    @Test
    fun testDistinguishSavingsMarginVsSavingsGoals() {
        val uiState = BudgetUiState(
            totalIncome = 12000.0,
            totalExpense = 8000.0,
            netBalance = 4000.0,
            savingsGoals = listOf(
                SavingsGoalEntity(id = 1L, name = "Fondo de Emergencia", targetAmount = 15000.0, currentAmount = 5000.0)
            ),
            currencySymbol = "Q "
        )

        // Margen de ahorro NO debe mostrar la lista de metas de ahorro
        val marginResp = FinancialAdvisorEngine.generateAdvisorResponse("margen de ahorro", uiState)
        assertTrue(marginResp.contains("4,000.00"))
        assertFalse(marginResp.contains("Fondo de Emergencia"))

        // Metas de ahorro SI debe mostrar las metas
        val goalsResp = FinancialAdvisorEngine.generateAdvisorResponse("¿Cómo van mis metas de ahorro?", uiState)
        assertTrue(goalsResp.contains("Fondo de Emergencia"))
        assertTrue(goalsResp.contains("5,000.00 de Q 15,000.00"))
    }

    @Test
    fun testPantryStatusReplenishmentLogic() {
        val pantry = listOf(
            SmartCartItemEntity(id = 1L, name = "Frijoles", quantity = 2.0, priceSource = "PANTRY_VERDE", isChecked = false),
            SmartCartItemEntity(id = 2L, name = "Huevos", quantity = 1.0, priceSource = "PANTRY_AMARILLO", isChecked = false),
            SmartCartItemEntity(id = 3L, name = "Café", quantity = 1.0, priceSource = "PANTRY_ROJO", isChecked = false)
        )

        val toReplenish = pantry.filter { it.priceSource == "PANTRY_AMARILLO" || it.priceSource == "PANTRY_ROJO" }
        assertEquals(2, toReplenish.size)
        assertTrue(toReplenish.any { it.name == "Huevos" })
        assertTrue(toReplenish.any { it.name == "Café" })
    }

    @Test
    fun testSingleAndBulkDeletionFiltering() {
        val items = mutableListOf(
            SmartCartItemEntity(id = 1L, name = "Arroz", isChecked = true, priceSource = "General"),
            SmartCartItemEntity(id = 2L, name = "Leche", isChecked = false, priceSource = "PANTRY_VERDE"),
            SmartCartItemEntity(id = 3L, name = "Pan", isChecked = true, priceSource = "PANTRY_AMARILLO")
        )

        // Eliminación individual por ID
        val itemToDelete = items.find { it.id == 1L }!!
        items.remove(itemToDelete)
        assertEquals(2, items.size)
        assertFalse(items.any { it.id == 1L })

        // Limpieza de comprados (desmarcar elementos de despensa)
        val remainingCheckedPantry = items.filter { it.isChecked && it.priceSource.startsWith("PANTRY_") }
        assertEquals(1, remainingCheckedPantry.size)
        assertEquals("Pan", remainingCheckedPantry[0].name)
    }
}
