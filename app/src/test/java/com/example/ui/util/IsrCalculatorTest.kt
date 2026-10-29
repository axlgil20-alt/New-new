package com.example.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IsrCalculatorTest {

    @Test
    fun testLowSalary_isExemptFromIsr() {
        // Q3,500 mensuales -> Anual Q42,000. La deducción legal única de Q48,000 cubre la totalidad.
        val result = IsrCalculator.calculateIsr(monthlySalary = 3500.0)
        assertEquals(0.0, result.taxableIncome, 0.01)
        assertEquals(0.0, result.rawAnnualTax, 0.01)
        assertEquals(0.0, result.monthlyRetention, 0.01)
        assertTrue(result.bracketDescription.contains("Exento"))
    }

    @Test
    fun testStandardSalary_bracketOneCalculation() {
        // Q10,000 mensuales -> Q120,000 anual
        // IGSS = 120,000 * 0.0483 = 5,796
        // Deducción personal = 48,000
        // Total deducciones = 53,796
        // Renta imponible = 120,000 - 53,796 = 66,204
        // ISR 5% = 66,204 * 0.05 = 3,310.20
        // Retención mensual = 3,310.20 / 12 = 275.85
        val result = IsrCalculator.calculateIsr(monthlySalary = 10000.0)
        assertEquals(120000.0, result.annualGrossSalary, 0.01)
        assertEquals(5796.0, result.annualIgssDeduction, 0.01)
        assertEquals(53796.0, result.totalDeductions, 0.01)
        assertEquals(66204.0, result.taxableIncome, 0.01)
        assertEquals(3310.20, result.rawAnnualTax, 0.01)
        assertEquals(275.85, result.monthlyRetention, 0.01)
        assertTrue(result.bracketDescription.contains("5%"))
    }

    @Test
    fun testSalaryWithIvaCredit() {
        // Q10,000 mensuales con crédito fiscal de Q1,000
        val result = IsrCalculator.calculateIsr(
            monthlySalary = 10000.0,
            estimatedIvaCredit = 1000.0
        )
        assertEquals(3310.20, result.rawAnnualTax, 0.01)
        assertEquals(1000.0, result.ivaCreditApplied, 0.01)
        assertEquals(2310.20, result.netAnnualTax, 0.01)
        assertEquals(2310.20 / 12.0, result.monthlyRetention, 0.01)
    }

    @Test
    fun testExcessiveIvaCredit_cappedAtTax() {
        // Si el crédito de IVA supera el impuesto, no puede generar saldo a favor en contra de la SAT
        val result = IsrCalculator.calculateIsr(
            monthlySalary = 10000.0,
            estimatedIvaCredit = 20000.0 // Se limita a MAX_IVA_CREDIT (12k) y luego al total del impuesto
        )
        assertEquals(3310.20, result.rawAnnualTax, 0.01)
        assertEquals(3310.20, result.ivaCreditApplied, 0.01)
        assertEquals(0.0, result.netAnnualTax, 0.01)
        assertEquals(0.0, result.monthlyRetention, 0.01)
    }

    @Test
    fun testHighSalary_bracketTwoCalculation() {
        // Salario muy alto donde la renta imponible supera Q300,000
        // Q40,000/mes -> Q480,000 anual
        // IGSS = 480,000 * 0.0483 = 23,184
        // Deducción personal = 48,000
        // Total deducciones = 71,184
        // Renta Imponible = 480,000 - 71,184 = 408,816
        // Excedente = 408,816 - 300,000 = 108,816
        // Impuesto = 15,000 + (108,816 * 0.07) = 15,000 + 7,617.12 = 22,617.12
        val result = IsrCalculator.calculateIsr(monthlySalary = 40000.0)
        assertEquals(408816.0, result.taxableIncome, 0.01)
        assertEquals(22617.12, result.rawAnnualTax, 0.01)
        assertEquals(22617.12 / 12.0, result.monthlyRetention, 0.01)
        assertTrue(result.bracketDescription.contains("7%"))
    }

    @Test
    fun testNegativeOrEmptyValues_handledSafely() {
        val result = IsrCalculator.calculateIsr(
            monthlySalary = -5000.0,
            otherMonthlyTaxableIncome = -100.0,
            estimatedIvaCredit = -200.0
        )
        assertEquals(0.0, result.annualGrossSalary, 0.01)
        assertEquals(0.0, result.taxableIncome, 0.01)
        assertEquals(0.0, result.netAnnualTax, 0.01)
        assertEquals(0.0, result.monthlyRetention, 0.01)
    }
}
