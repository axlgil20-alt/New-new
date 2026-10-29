package com.example.ui.util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Resultado del cálculo legal del Aguinaldo según la legislación de Guatemala.
 */
data class AguinaldoResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val baseSalary: Double = 0.0,
    val computedDays: Long = 0L,
    val totalPeriodDays: Int = 365,
    val isFullYear: Boolean = false,
    val totalAguinaldo: Double = 0.0,
    val firstPaymentDecember: Double = 0.0, // 50% en primera quincena de diciembre
    val secondPaymentJanuary: Double = 0.0, // 50% en segunda quincena de enero
    val periodStartFormatted: String = "",
    val periodEndFormatted: String = "",
    val calculationSummary: String = "",
    val legalNote: String = "Estimación informativa calculada según la Ley Reguladora de la Prestación del Aguinaldo (Decreto 76-78). No sustituye la liquidación contable definitiva emitida por el patrono."
)

/**
 * Calculador de Aguinaldo para Guatemala
 *
 * Fundamento Legal:
 * Decreto 76-78 del Congreso de la República de Guatemala.
 * - Período de cómputo anual: del 1 de diciembre del año anterior al 30 de noviembre del año en curso.
 * - Sueldo base: Salario ordinario mensual promedio devengado.
 * - Proporcionalidad: Si el tiempo laborado es menor a un año continuo dentro del período legal,
 *   se calcula proporcionalmente a los días trabajados: (Salario * Días) / 365 (o 366 si es bisiesto).
 * - Forma de pago: 50% en la primera quincena de diciembre y 50% en la segunda quincena de enero.
 */
object AguinaldoCalculator {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-GT"))

    /**
     * Determina si un año es bisiesto.
     */
    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    /**
     * Retorna los días totales del período de aguinaldo para el año dado.
     * El período corre del 1 de diciembre de (year - 1) al 30 de noviembre de (year).
     * Si febrero de (year) tiene 29 días, el período tiene 366 días; de lo contrario 365 días.
     */
    fun getPeriodTotalDays(periodTargetYear: Int): Int {
        return if (isLeapYear(periodTargetYear)) 366 else 365
    }

    /**
     * Calcula el aguinaldo dados el salario y las fechas de inicio y fin dentro del período.
     *
     * @param salaryStr Texto del salario ingresado por el usuario.
     * @param startDay Día de inicio de labores (1..31).
     * @param startMonth Mes de inicio de labores (1..12).
     * @param startYear Año de inicio de labores.
     * @param endDay Día de corte (1..31).
     * @param endMonth Mes de corte (1..12).
     * @param endYear Año de corte (año objetivo del aguinaldo).
     */
    fun calculate(
        salaryStr: String,
        startDay: Int,
        startMonth: Int,
        startYear: Int,
        endDay: Int,
        endMonth: Int,
        endYear: Int
    ): AguinaldoResult {
        // 1. Validación de salario
        val cleanSalaryStr = salaryStr.trim().replace(",", "").replace("Q", "").replace("q", "").trim()
        if (cleanSalaryStr.isEmpty()) {
            return AguinaldoResult(
                isValid = false,
                errorMessage = "Por favor ingresa el salario mensual ordinario."
            )
        }

        val salary = cleanSalaryStr.toDoubleOrNull()
        if (salary == null) {
            return AguinaldoResult(
                isValid = false,
                errorMessage = "El salario ingresado no es un número válido."
            )
        }

        if (salary < 0.0) {
            return AguinaldoResult(
                isValid = false,
                errorMessage = "El salario no puede ser un valor negativo."
            )
        }

        if (salary == 0.0) {
            return AguinaldoResult(
                isValid = true,
                baseSalary = 0.0,
                computedDays = 0L,
                totalPeriodDays = getPeriodTotalDays(endYear),
                totalAguinaldo = 0.0,
                calculationSummary = "Salario en cero: Aguinaldo correspondiente es Q 0.00."
            )
        }

        // 2. Validación y construcción de fechas con Calendar
        val userStartCal = Calendar.getInstance().apply {
            isLenient = false
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val userEndCal = Calendar.getInstance().apply {
            isLenient = false
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        try {
            userStartCal.set(startYear, startMonth - 1, startDay)
            userStartCal.time // Dispara validación si la fecha no existe (ej. 31 de febrero)
        } catch (_: Exception) {
            return AguinaldoResult(
                isValid = false,
                errorMessage = "La fecha de inicio ingresada no es válida en el calendario."
            )
        }

        try {
            userEndCal.set(endYear, endMonth - 1, endDay)
            userEndCal.time
        } catch (_: Exception) {
            return AguinaldoResult(
                isValid = false,
                errorMessage = "La fecha de corte ingresada no es válida en el calendario."
            )
        }

        if (userStartCal.after(userEndCal)) {
            return AguinaldoResult(
                isValid = false,
                errorMessage = "La fecha de inicio no puede ser posterior a la fecha de corte."
            )
        }

        // 3. Período oficial del decreto: del 1 de diciembre de (endYear - 1) al 30 de noviembre de (endYear)
        val officialPeriodStart = Calendar.getInstance().apply {
            set(endYear - 1, Calendar.DECEMBER, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val officialPeriodEnd = Calendar.getInstance().apply {
            set(endYear, Calendar.NOVEMBER, 30, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val totalDaysInPeriod = getPeriodTotalDays(endYear)

        // Si la fecha de inicio del usuario es posterior al cierre del período
        if (userStartCal.after(officialPeriodEnd)) {
            return AguinaldoResult(
                isValid = false,
                errorMessage = "La fecha de inicio ingresada es posterior al cierre del período de aguinaldo ($endYear)."
            )
        }

        // Computar inicio y fin efectivos dentro del período legal
        val effectiveStart = if (userStartCal.before(officialPeriodStart)) officialPeriodStart else userStartCal
        val effectiveEnd = if (userEndCal.after(officialPeriodEnd)) officialPeriodEnd else userEndCal

        if (effectiveStart.after(effectiveEnd)) {
            return AguinaldoResult(
                isValid = false,
                errorMessage = "El rango de fechas no se traslapa con el período legal de aguinaldo."
            )
        }

        // Cálculo de días exactos (inclusive ambos extremos: fecha fin - fecha inicio + 1)
        val diffMillis = effectiveEnd.timeInMillis - effectiveStart.timeInMillis
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)) + 1

        val computedDays = diffDays.coerceIn(0L, totalDaysInPeriod.toLong())
        val isFullYear = computedDays >= totalDaysInPeriod

        // Cálculo del monto
        val totalAguinaldo = if (isFullYear) {
            salary
        } else {
            (salary * computedDays) / totalDaysInPeriod.toDouble()
        }

        val firstPayment = totalAguinaldo * 0.50
        val secondPayment = totalAguinaldo * 0.50

        val startFormatted = dateFormat.format(effectiveStart.time)
        val endFormatted = dateFormat.format(effectiveEnd.time)

        val summary = if (isFullYear) {
            "Año completo laborado ($computedDays días de $totalDaysInPeriod). Corresponde el 100% de un sueldo ordinario mensual."
        } else {
            "Período proporcional: $computedDays días laborados de $totalDaysInPeriod. Se calcula: (Q ${"%,.2f".format(salary)} × $computedDays) ÷ $totalDaysInPeriod días."
        }

        return AguinaldoResult(
            isValid = true,
            baseSalary = salary,
            computedDays = computedDays,
            totalPeriodDays = totalDaysInPeriod,
            isFullYear = isFullYear,
            totalAguinaldo = totalAguinaldo,
            firstPaymentDecember = firstPayment,
            secondPaymentJanuary = secondPayment,
            periodStartFormatted = startFormatted,
            periodEndFormatted = endFormatted,
            calculationSummary = summary
        )
    }

    /**
     * Sobrecarga de conveniencia para cálculo directo de año completo.
     */
    fun calculateFullYear(salary: Double, targetYear: Int): AguinaldoResult {
        return calculate(
            salaryStr = salary.toString(),
            startDay = 1,
            startMonth = 12,
            startYear = targetYear - 1,
            endDay = 30,
            endMonth = 11,
            endYear = targetYear
        )
    }
}
