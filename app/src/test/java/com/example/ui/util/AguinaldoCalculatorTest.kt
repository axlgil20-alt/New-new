package com.example.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AguinaldoCalculatorTest {

    // 1. Año completo en año regular (365 días): el aguinaldo debe ser exactamente el 100% del sueldo ordinario
    @Test
    fun testFullYearRegular_returnsFullSalary() {
        val salary = 5000.0
        val targetYear = 2023 // 2023 no fue bisiesto, período 01/12/2022 al 30/11/2023 = 365 días
        val result = AguinaldoCalculator.calculate(
            salaryStr = "5000.00",
            startDay = 1,
            startMonth = 12,
            startYear = 2022,
            endDay = 30,
            endMonth = 11,
            endYear = 2023
        )

        assertTrue(result.isValid)
        assertTrue(result.isFullYear)
        assertEquals(365L, result.computedDays)
        assertEquals(365, result.totalPeriodDays)
        assertEquals(5000.0, result.totalAguinaldo, 0.01)
        assertEquals(2500.0, result.firstPaymentDecember, 0.01)
        assertEquals(2500.0, result.secondPaymentJanuary, 0.01)
    }

    // 2. Año bisiesto (2024 tuvo 29 de febrero -> 366 días)
    @Test
    fun testLeapYear_periodHas366Days() {
        assertTrue(AguinaldoCalculator.isLeapYear(2024))
        assertFalse(AguinaldoCalculator.isLeapYear(2023))
        assertFalse(AguinaldoCalculator.isLeapYear(2025))

        val result = AguinaldoCalculator.calculate(
            salaryStr = "4250.00",
            startDay = 1,
            startMonth = 12,
            startYear = 2023,
            endDay = 30,
            endMonth = 11,
            endYear = 2024
        )

        assertTrue(result.isValid)
        assertTrue(result.isFullYear)
        assertEquals(366L, result.computedDays)
        assertEquals(366, result.totalPeriodDays)
        assertEquals(4250.0, result.totalAguinaldo, 0.01)
    }

    // 3. Período proporcional (ej. entró a trabajar el 1 de junio y corte el 30 de noviembre en 2023)
    // Junio(30) + Julio(31) + Agosto(31) + Septiembre(30) + Octubre(31) + Noviembre(30) = 183 días
    @Test
    fun testProportionalPeriod_sixMonths() {
        val salary = 6000.0
        val result = AguinaldoCalculator.calculate(
            salaryStr = "6000",
            startDay = 1,
            startMonth = 6,
            startYear = 2023,
            endDay = 30,
            endMonth = 11,
            endYear = 2023
        )

        assertTrue(result.isValid)
        assertFalse(result.isFullYear)
        assertEquals(183L, result.computedDays)
        val expected = (6000.0 * 183) / 365.0
        assertEquals(expected, result.totalAguinaldo, 0.01)
        assertEquals(expected / 2.0, result.firstPaymentDecember, 0.01)
    }

    // 4. Período corto (ej. 15 días trabajados del 16 al 30 de noviembre de 2023)
    @Test
    fun testShortPeriod_fifteenDays() {
        val salary = 3650.0
        val result = AguinaldoCalculator.calculate(
            salaryStr = "3650",
            startDay = 16,
            startMonth = 11,
            startYear = 2023,
            endDay = 30,
            endMonth = 11,
            endYear = 2023
        )

        assertTrue(result.isValid)
        assertEquals(15L, result.computedDays)
        val expected = (3650.0 * 15) / 365.0
        assertEquals(150.0, expected, 0.01)
        assertEquals(150.0, result.totalAguinaldo, 0.01)
    }

    // 5. Límites de fechas: Si inició hace años (ej. 2018), solo se computa el período anual oficial (365 días)
    @Test
    fun testStartDatePriorToPeriod_cappedAtPeriodStart() {
        val result = AguinaldoCalculator.calculate(
            salaryStr = "4000",
            startDay = 15,
            startMonth = 3,
            startYear = 2018,
            endDay = 30,
            endMonth = 11,
            endYear = 2023
        )

        assertTrue(result.isValid)
        assertTrue(result.isFullYear)
        assertEquals(365L, result.computedDays)
        assertEquals(4000.0, result.totalAguinaldo, 0.01)
    }

    // 6. Salario cero -> devuelve aguinaldo 0.00 sin error
    @Test
    fun testZeroSalary_returnsZeroAmount() {
        val result = AguinaldoCalculator.calculate(
            salaryStr = "0",
            startDay = 1,
            startMonth = 12,
            startYear = 2022,
            endDay = 30,
            endMonth = 11,
            endYear = 2023
        )

        assertTrue(result.isValid)
        assertEquals(0.0, result.totalAguinaldo, 0.001)
    }

    // 7. Entradas inválidas: Salario vacío
    @Test
    fun testEmptySalary_returnsValidationError() {
        val result = AguinaldoCalculator.calculate(
            salaryStr = "   ",
            startDay = 1,
            startMonth = 12,
            startYear = 2022,
            endDay = 30,
            endMonth = 11,
            endYear = 2023
        )

        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    // 8. Entradas inválidas: Salario negativo
    @Test
    fun testNegativeSalary_returnsValidationError() {
        val result = AguinaldoCalculator.calculate(
            salaryStr = "-3500",
            startDay = 1,
            startMonth = 12,
            startYear = 2022,
            endDay = 30,
            endMonth = 11,
            endYear = 2023
        )

        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    // 9. Entradas inválidas: Texto no numérico
    @Test
    fun testNonNumericSalary_returnsValidationError() {
        val result = AguinaldoCalculator.calculate(
            salaryStr = "abcde",
            startDay = 1,
            startMonth = 12,
            startYear = 2022,
            endDay = 30,
            endMonth = 11,
            endYear = 2023
        )

        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    // 10. Fechas invertidas (fecha inicio posterior a fecha de corte)
    @Test
    fun testInvertedDates_returnsValidationError() {
        val result = AguinaldoCalculator.calculate(
            salaryStr = "4000",
            startDay = 15,
            startMonth = 10,
            startYear = 2023,
            endDay = 10,
            endMonth = 5,
            endYear = 2023
        )

        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    // 11. Fecha fuera de calendario (ej. día 31 en febrero)
    @Test
    fun testInvalidCalendarDate_returnsValidationError() {
        val result = AguinaldoCalculator.calculate(
            salaryStr = "4000",
            startDay = 31,
            startMonth = 2,
            startYear = 2023,
            endDay = 30,
            endMonth = 11,
            endYear = 2023
        )

        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }
}
