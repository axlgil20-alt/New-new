package com.example.ui.util

import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Modalidades admitidas por la configuración de ingresos de KETZI. */
enum class IncomeScheduleMode {
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    CUSTOM_DATES,
    IRREGULAR
}

data class IncomeScheduleConfig(
    val mode: IncomeScheduleMode = IncomeScheduleMode.BIWEEKLY,
    /** Días oficiales de pago para CUSTOM_DATES, por ejemplo 7 y 22. */
    val paymentDays: List<Int> = listOf(15, 31),
    /** Fechas oficiales para IRREGULAR, en milisegundos, ordenadas o no. */
    val irregularPaymentDatesMillis: List<Long> = emptyList(),
    /** Identificador versionado; cambiarlo no muta asignaciones históricas ya guardadas. */
    val version: String = "default-biweekly-v2",
    val adjustWeekendPayments: Boolean = false,
    val holidayDates: Set<String> = emptySet()
)

data class FinancialPeriod(
    val key: String,
    val year: Int,
    val monthIndex: Int,
    val sequence: Int,
    val startMillis: Long,
    val endMillis: Long,
    val officialPaymentDateMillis: Long? = null,
    val receivedDateMillis: Long? = null
)

object IncomePeriodEngine {
    fun defaultConfig() = IncomeScheduleConfig()

    fun resolveExpensePeriod(timeMillis: Long, config: IncomeScheduleConfig = defaultConfig()): FinancialPeriod =
        resolveDate(timeMillis, config, paymentDate = null)

    /** Resuelve un ingreso por su fecha oficial de pago, no por la fecha de registro local. */
    fun resolveIncomePeriod(
        officialPaymentMillis: Long,
        receivedMillis: Long = officialPaymentMillis,
        config: IncomeScheduleConfig = defaultConfig()
    ): FinancialPeriod {
        // La fecha real se conserva para auditoría; la ventana financiera nace de la fecha oficial.
        val adjustedOfficial = adjustPaymentDate(officialPaymentMillis, config)
        return resolveDate(officialPaymentMillis, config, paymentDate = officialPaymentMillis)
            .copy(receivedDateMillis = if (receivedMillis != officialPaymentMillis) receivedMillis else adjustedOfficial)
    }

    fun periodRange(
        year: Int,
        monthIndex: Int,
        sequence: Int,
        config: IncomeScheduleConfig = defaultConfig()
    ): Pair<Long, Long> = when (config.mode) {
        IncomeScheduleMode.BIWEEKLY -> {
            val first = normalizedPaymentDays(config).firstOrNull() ?: 15
            if (sequence == 1) dayRange(year, monthIndex, 1, (first - 1).coerceAtLeast(1))
            else dayRange(year, monthIndex, first.coerceAtMost(daysInMonth(year, monthIndex)), daysInMonth(year, monthIndex))
        }
        IncomeScheduleMode.CUSTOM_DATES -> {
            val days = normalizedPaymentDays(config)
            val first = days.firstOrNull() ?: 7
            val second = days.getOrElse(1) { 22 }
            if (sequence == 1) dayRange(year, monthIndex, first, (second - 1).coerceAtLeast(first))
            else {
                val nextMonth = if (monthIndex == 11) 0 else monthIndex + 1
                val nextYear = if (monthIndex == 11) year + 1 else year
                dayRangeCrossMonth(year, monthIndex, second, nextYear, nextMonth, first - 1)
            }
        }
        IncomeScheduleMode.MONTHLY -> dayRange(year, monthIndex, 1, daysInMonth(year, monthIndex))
        IncomeScheduleMode.WEEKLY -> {
            val first = Calendar.getInstance().apply { clear(); set(year, monthIndex, 1) }
            val firstDay = first.get(Calendar.DAY_OF_WEEK)
            val daysToSunday = Calendar.SUNDAY - firstDay
            val endDay = (1 + daysToSunday).coerceAtLeast(1)
            dayRange(year, monthIndex, 1, endDay.coerceAtMost(daysInMonth(year, monthIndex)))
        }
        IncomeScheduleMode.IRREGULAR -> dayRange(year, monthIndex, 1, daysInMonth(year, monthIndex))
    }

    private fun resolveDate(timeMillis: Long, config: IncomeScheduleConfig, paymentDate: Long?): FinancialPeriod {
        val source = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val year = source.get(Calendar.YEAR)
        val month = source.get(Calendar.MONTH)
        val day = source.get(Calendar.DAY_OF_MONTH)
        return when (config.mode) {
            IncomeScheduleMode.BIWEEKLY -> {
                // Regla chapina: 15 inicia el segundo período; el último día financia el siguiente Q1.
                if (paymentDate != null && day == daysInMonth(year, month)) nextPeriod(year, month, 1, config, paymentDate)
                else if (day >= 15) makePeriod(year, month, 2, config, paymentDate)
                else makePeriod(year, month, 1, config, paymentDate)
            }
            IncomeScheduleMode.CUSTOM_DATES -> resolveCustom(year, month, day, config, paymentDate)
            IncomeScheduleMode.MONTHLY -> makePeriod(year, month, 1, config, paymentDate)
            IncomeScheduleMode.WEEKLY -> {
                val mondayBased = (source.get(Calendar.DAY_OF_WEEK) + 5) % 7
                val start = (day - mondayBased).coerceAtLeast(1)
                makeWeeklyPeriod(year, month, start, config, paymentDate)
            }
            IncomeScheduleMode.IRREGULAR -> resolveIrregular(timeMillis, config, paymentDate)
        }
    }

    private fun resolveIrregular(timeMillis: Long, config: IncomeScheduleConfig, paymentDate: Long?): FinancialPeriod {
        val dates = config.irregularPaymentDatesMillis.sorted()
        if (dates.isEmpty()) {
            val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
            return makePeriod(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1, config, paymentDate)
        }
        val index = dates.indexOfLast { it <= timeMillis }.coerceAtLeast(0)
        val start = dates[index]
        val end = dates.getOrNull(index + 1)?.minus(1L) ?: Long.MAX_VALUE
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        return FinancialPeriod(
            key = "${config.version}:IRREGULAR:$index",
            year = startCal.get(Calendar.YEAR),
            monthIndex = startCal.get(Calendar.MONTH),
            sequence = index + 1,
            startMillis = start,
            endMillis = end,
            officialPaymentDateMillis = paymentDate
        )
    }

    private fun adjustPaymentDate(timeMillis: Long, config: IncomeScheduleConfig): Long {
        if (!config.adjustWeekendPayments && config.holidayDates.isEmpty()) return timeMillis
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
            cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ||
            format.format(Date(cal.timeInMillis)) in config.holidayDates
        ) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return cal.timeInMillis
    }

    private fun resolveCustom(year: Int, month: Int, day: Int, config: IncomeScheduleConfig, paymentDate: Long?): FinancialPeriod {
        val days = normalizedPaymentDays(config)
        val first = days.firstOrNull() ?: 15
        val second = days.getOrElse(1) { 31 }
        return if (day >= second) makePeriod(year, month, 2, config, paymentDate)
        else if (day >= first) makePeriod(year, month, 1, config, paymentDate)
        else previousMonthPeriod(year, month, 2, config, paymentDate)
    }

    private fun makePeriod(year: Int, month: Int, sequence: Int, config: IncomeScheduleConfig, paymentDate: Long?): FinancialPeriod {
        val range = periodRange(year, month, sequence, config)
        return FinancialPeriod("${config.version}:$year-$month-$sequence", year, month, sequence, range.first, range.second, paymentDate)
    }

    private fun makeWeeklyPeriod(year: Int, month: Int, startDay: Int, config: IncomeScheduleConfig, paymentDate: Long?): FinancialPeriod {
        val endDay = (startDay + 6).coerceAtMost(daysInMonth(year, month))
        val range = dayRange(year, month, startDay, endDay)
        return FinancialPeriod("${config.version}:$year-$month-W$startDay", year, month, startDay, range.first, range.second, paymentDate)
    }

    private fun nextPeriod(year: Int, month: Int, sequence: Int, config: IncomeScheduleConfig, paymentDate: Long?): FinancialPeriod {
        val nextMonth = if (month == 11) 0 else month + 1
        val nextYear = if (month == 11) year + 1 else year
        return makePeriod(nextYear, nextMonth, sequence, config, paymentDate)
    }

    private fun previousMonthPeriod(year: Int, month: Int, sequence: Int, config: IncomeScheduleConfig, paymentDate: Long?): FinancialPeriod {
        val previousMonth = if (month == 0) 11 else month - 1
        val previousYear = if (month == 0) year - 1 else year
        return makePeriod(previousYear, previousMonth, sequence, config, paymentDate)
    }

    private fun normalizedPaymentDays(config: IncomeScheduleConfig): List<Int> =
        config.paymentDays.filter { it in 1..31 }.distinct().sorted().take(2).ifEmpty { listOf(15, 31) }

    private fun daysInMonth(year: Int, month: Int): Int = Calendar.getInstance().apply {
        clear(); set(year, month, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    private fun dayRange(year: Int, month: Int, startDay: Int, endDay: Int): Pair<Long, Long> {
        val start = Calendar.getInstance().apply { clear(); set(year, month, startDay.coerceAtLeast(1)); }.timeInMillis
        val end = Calendar.getInstance().apply {
            clear(); set(year, month, endDay.coerceAtLeast(startDay));
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        return start to end
    }

    private fun dayRangeCrossMonth(
        startYear: Int,
        startMonth: Int,
        startDay: Int,
        endYear: Int,
        endMonth: Int,
        endDay: Int
    ): Pair<Long, Long> {
        val start = Calendar.getInstance().apply { clear(); set(startYear, startMonth, startDay) }.timeInMillis
        val end = Calendar.getInstance().apply {
            clear(); set(endYear, endMonth, endDay.coerceAtLeast(1));
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        return start to end
    }
}
