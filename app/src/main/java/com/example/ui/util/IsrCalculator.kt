package com.example.ui.util

/**
 * Resultado detallado del cálculo de ISR para Guatemala (SAT - Rentas del Trabajo)
 */
data class IsrResult(
    val monthlyGrossSalary: Double,
    val annualGrossSalary: Double,
    val annualIgssDeduction: Double,
    val annualPersonalDeduction: Double,
    val totalDeductions: Double,
    val taxableIncome: Double,
    val rawAnnualTax: Double,
    val ivaCreditApplied: Double,
    val netAnnualTax: Double,
    val monthlyRetention: Double,
    val effectiveRatePercentage: Double,
    val bracketDescription: String
)

/**
 * Calculadora Oficial de ISR para Asalariados en Guatemala
 * Conforme al Decreto 10-2012 (Ley de Actualización Tributaria - Artículos 72 y 73)
 */
object IsrCalculator {

    const val PERSONAL_ANNUAL_DEDUCTION = 48000.0 // Q48,000 deducción fija legal
    const val IGSS_LABORAL_PERCENTAGE = 0.0483 // 4.83% cuota IGSS trabajador
    const val BRACKET_1_LIMIT = 300000.0 // Límite del tramo 5%
    const val BRACKET_1_RATE = 0.05 // 5%
    const val BRACKET_2_BASE_TAX = 15000.0 // Q15,000 cuota fija
    const val BRACKET_2_RATE = 0.07 // 7% sobre excedente de Q300k
    const val MAX_IVA_CREDIT = 12000.0 // Crédito fiscal máximo por planilla IVA

    /**
     * Calcula la proyección anual de ISR y la retención mensual estimada.
     *
     * @param monthlySalary Salario mensual ordinario en Quetzales.
     * @param otherMonthlyTaxableIncome Otras bonificaciones afectas mensuales.
     * @param estimatedIvaCredit Planilla de IVA estimada (máximo Q12,000).
     */
    fun calculateIsr(
        monthlySalary: Double,
        otherMonthlyTaxableIncome: Double = 0.0,
        estimatedIvaCredit: Double = 0.0
    ): IsrResult {
        // Validación estricta contra valores negativos o NaN
        val validSalary = if (monthlySalary.isNaN() || monthlySalary < 0.0) 0.0 else monthlySalary
        val validOtherIncome = if (otherMonthlyTaxableIncome.isNaN() || otherMonthlyTaxableIncome < 0.0) 0.0 else otherMonthlyTaxableIncome
        val validIvaCredit = if (estimatedIvaCredit.isNaN() || estimatedIvaCredit < 0.0) 0.0 else minOf(estimatedIvaCredit, MAX_IVA_CREDIT)

        val totalMonthlyIncome = validSalary + validOtherIncome
        val annualGrossSalary = totalMonthlyIncome * 12.0

        // Deducción IGSS Laboral (4.83% sobre sueldos ordinarios afectos)
        val annualIgssDeduction = annualGrossSalary * IGSS_LABORAL_PERCENTAGE

        // Total deducciones legales (Deducción única de Q48,000 + IGSS)
        val totalDeductions = PERSONAL_ANNUAL_DEDUCTION + annualIgssDeduction

        // Renta Imponible (base afecta)
        val taxableIncome = maxOf(0.0, annualGrossSalary - totalDeductions)

        // Aplicación de escala progresiva de la SAT (Art. 73)
        val rawAnnualTax: Double
        val bracketDesc: String

        if (taxableIncome <= 0.0) {
            rawAnnualTax = 0.0
            bracketDesc = "Exento de ISR (Deducciones cubren los ingresos)"
        } else if (taxableIncome <= BRACKET_1_LIMIT) {
            rawAnnualTax = taxableIncome * BRACKET_1_RATE
            bracketDesc = "Tramo 1: 5% sobre Renta Imponible (Hasta Q300,000)"
        } else {
            val excess = taxableIncome - BRACKET_1_LIMIT
            rawAnnualTax = BRACKET_2_BASE_TAX + (excess * BRACKET_2_RATE)
            bracketDesc = "Tramo 2: Q15,000 fijo + 7% sobre excedente de Q300,000"
        }

        // Aplicación del crédito por planilla de IVA (no puede generar saldo a favor de SAT)
        val ivaCreditApplied = minOf(validIvaCredit, rawAnnualTax)
        val netAnnualTax = maxOf(0.0, rawAnnualTax - ivaCreditApplied)

        // Retención mensual estimada
        val monthlyRetention = netAnnualTax / 12.0

        // Tasa efectiva
        val effectiveRate = if (annualGrossSalary > 0.0) (netAnnualTax / annualGrossSalary) * 100.0 else 0.0

        return IsrResult(
            monthlyGrossSalary = totalMonthlyIncome,
            annualGrossSalary = annualGrossSalary,
            annualIgssDeduction = annualIgssDeduction,
            annualPersonalDeduction = PERSONAL_ANNUAL_DEDUCTION,
            totalDeductions = totalDeductions,
            taxableIncome = taxableIncome,
            rawAnnualTax = rawAnnualTax,
            ivaCreditApplied = ivaCreditApplied,
            netAnnualTax = netAnnualTax,
            monthlyRetention = monthlyRetention,
            effectiveRatePercentage = effectiveRate,
            bracketDescription = bracketDesc
        )
    }
}
