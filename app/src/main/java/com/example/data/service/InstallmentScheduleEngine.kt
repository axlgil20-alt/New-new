package com.example.data.service

import com.example.data.model.CreditReservePolicy
import com.example.data.model.InstallmentChargeStatus
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.InstallmentPlanStatus
import com.example.data.model.InstallmentScheduleItemEntity
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionEntryKind
import com.example.data.model.TransactionType
import com.example.data.model.effectiveAmountMinor
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class GeneratedInstallmentSchedule(
    val items: List<InstallmentScheduleItemEntity>,
    val referenceInstallmentAmountMinor: Long,
    val totalPlannedMinor: Long,
    val totalInterestMinor: Long
)

/**
 * Motor matemático puro para planes en cuotas y Visacuotas.
 *
 * Reglas explícitas de precisión y redondeo (`BigDecimal`, escala 2, `RoundingMode.HALF_UP`):
 * 1. **Planes sin interés (`annualInterestRateBps == 0`, ej. Visacuotas 0%):**
 *    - Se distribuye `principalTotalMinor` entre `installmentsCount` con [MoneyMinorUnits.distributeEvenly].
 *    - Los centavos residuales (`principalTotalMinor % N`) se asignan `+1` centavo a las últimas `rem` cuotas.
 *    - Invariante: cada cuota es `>= 0`, diferencia máxima de `Q0.01`, suma exacta = `principalTotalMinor`.
 * 2. **Planes con interés (`annualInterestRateBps > 0`):**
 *    - Tasa mensual `i = (bps / 10_000) / 12` calculada con precisión 16 (`HALF_UP`).
 *    - Cuota nivelada francesa `C = P * i*(1+i)^N / ((1+i)^N - 1)` redondeada a 2 decimales (`HALF_UP`).
 *    - Para cada periodo `1..N-1`: `interes_k = round2(saldo * i)` y `capital_k = min(saldo, C - interes_k)`.
 *    - **Última cuota (`N`) absorbente de residuo:** `capital_N = saldo_pendiente` exacto,
 *      garantizando que la suma de amortizaciones de capital sea exactamente `principalTotalMinor` y saldo final `0.00`.
 * 3. **Identidad única e idempotente:**
 *    - Cada cuota recibe `idempotencyKey = "plan_${planId}_cuota_${installmentNumber}"`, además de la
 *      clave compuesta `(planId, installmentNumber)`.
 */
object InstallmentScheduleEngine {

    fun buildIdempotencyKey(planId: Long, installmentNumber: Int): String =
        "plan_${planId}_cuota_${installmentNumber}"

    fun generateSchedule(
        planId: Long,
        accountId: Long,
        principalTotalMinor: Long,
        installmentsCount: Int,
        annualInterestRateBps: Int,
        firstChargeDateMillis: Long,
        cutoffDay: Int? = null,
        timeZone: TimeZone = TimeZone.getDefault()
    ): GeneratedInstallmentSchedule {
        require(installmentsCount > 0) { "La cantidad de cuotas debe ser mayor a 0." }
        require(principalTotalMinor > 0L) { "El monto financiado debe ser mayor a Q0.00." }
        require(annualInterestRateBps >= 0) { "La tasa de interés no puede ser negativa." }

        val items = ArrayList<InstallmentScheduleItemEntity>(installmentsCount)

        if (annualInterestRateBps == 0) {
            val shares = MoneyMinorUnits.distributeEvenly(principalTotalMinor, installmentsCount)
            for (idx in shares.indices) {
                val number = idx + 1
                val dueMillis = addMonthsPreservingDay(firstChargeDateMillis, idx, timeZone)
                val cycleKey = resolveCycleKey(dueMillis, cutoffDay, timeZone)
                val shareMinor = shares[idx]
                items.add(
                    InstallmentScheduleItemEntity(
                        planId = planId,
                        accountId = accountId,
                        installmentNumber = number,
                        idempotencyKey = buildIdempotencyKey(planId, number),
                        dueDateMillis = dueMillis,
                        statementCycleKey = cycleKey,
                        principalMinor = shareMinor,
                        interestMinor = 0L,
                        totalChargeMinor = shareMinor,
                        status = InstallmentChargeStatus.SCHEDULED
                    )
                )
            }
            return GeneratedInstallmentSchedule(
                items = items,
                referenceInstallmentAmountMinor = shares.first(),
                totalPlannedMinor = principalTotalMinor,
                totalInterestMinor = 0L
            )
        }

        // Sistema francés con BigDecimal y ajuste exacto de residuo en la última cuota
        val principalBd = MoneyMinorUnits.toBigDecimal(principalTotalMinor)
        val monthlyRate = MoneyMinorUnits.annualBpsToMonthlyRate(annualInterestRateBps)
        val onePlusRatePowN = BigDecimal.ONE.add(monthlyRate)
            .pow(installmentsCount, MoneyMinorUnits.DOMAIN_MATH_CONTEXT)
        val numerator = principalBd.multiply(monthlyRate, MoneyMinorUnits.DOMAIN_MATH_CONTEXT)
            .multiply(onePlusRatePowN, MoneyMinorUnits.DOMAIN_MATH_CONTEXT)
        val denominator = onePlusRatePowN.subtract(BigDecimal.ONE, MoneyMinorUnits.DOMAIN_MATH_CONTEXT)

        val fixedPaymentMinor = if (denominator.signum() > 0) {
            MoneyMinorUnits.fromBigDecimal(
                numerator.divide(denominator, MoneyMinorUnits.CURRENCY_SCALE, MoneyMinorUnits.ROUNDING_MODE)
            )
        } else {
            MoneyMinorUnits.distributeEvenly(principalTotalMinor, installmentsCount).first()
        }

        var remainingPrincipalMinor = principalTotalMinor
        var totalInterestMinor = 0L
        var totalPlannedMinor = 0L

        for (number in 1..installmentsCount) {
            val remainingBd = MoneyMinorUnits.toBigDecimal(remainingPrincipalMinor)
            val interestBd = remainingBd.multiply(monthlyRate, MoneyMinorUnits.DOMAIN_MATH_CONTEXT)
                .setScale(MoneyMinorUnits.CURRENCY_SCALE, MoneyMinorUnits.ROUNDING_MODE)
            val interestMinor = MoneyMinorUnits.fromBigDecimal(interestBd)

            val principalPortionMinor = if (number == installmentsCount) {
                // La última cuota absorbe todo el residuo exacto para cerrar saldo en 0.00
                remainingPrincipalMinor
            } else {
                val tentativePrincipal = (fixedPaymentMinor - interestMinor).coerceAtLeast(0L)
                tentativePrincipal.coerceAtMost(remainingPrincipalMinor)
            }

            val totalChargeMinor = principalPortionMinor + interestMinor
            remainingPrincipalMinor -= principalPortionMinor
            totalInterestMinor += interestMinor
            totalPlannedMinor += totalChargeMinor

            val dueMillis = addMonthsPreservingDay(firstChargeDateMillis, number - 1, timeZone)
            val cycleKey = resolveCycleKey(dueMillis, cutoffDay, timeZone)

            items.add(
                InstallmentScheduleItemEntity(
                    planId = planId,
                    accountId = accountId,
                    installmentNumber = number,
                    idempotencyKey = buildIdempotencyKey(planId, number),
                    dueDateMillis = dueMillis,
                    statementCycleKey = cycleKey,
                    principalMinor = principalPortionMinor,
                    interestMinor = interestMinor,
                    totalChargeMinor = totalChargeMinor,
                    status = InstallmentChargeStatus.SCHEDULED
                )
            )
        }

        return GeneratedInstallmentSchedule(
            items = items,
            referenceInstallmentAmountMinor = fixedPaymentMinor,
            totalPlannedMinor = totalPlannedMinor,
            totalInterestMinor = totalInterestMinor
        )
    }

    /**
     * Calcula el capital pendiente reservado contra el límite de crédito según política institucional.
     *
     * - [CreditReservePolicy.RESERVE_BILLED_INSTALLMENTS_ONLY]: devuelve `0L` (no reserva cuotas futuras).
     * - [CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL]: suma exclusivamente el capital de cuotas
     *   en estado [InstallmentChargeStatus.SCHEDULED]. Si no se proporcionan ítems de calendario,
     *   descuenta del capital total lo ya cargado en `transactions` para ese plan.
     */
    fun calculateReservedPrincipalMinor(
        plan: InstallmentPlanEntity,
        scheduleItems: List<InstallmentScheduleItemEntity>,
        postedTransactions: List<TransactionEntity>,
        policyOverride: String? = null
    ): Long {
        if (plan.status != InstallmentPlanStatus.ACTIVE) return 0L
        val effectivePolicy = policyOverride?.takeIf { it.isNotBlank() } ?: plan.creditReservePolicy
        if (effectivePolicy == CreditReservePolicy.RESERVE_BILLED_INSTALLMENTS_ONLY) {
            return 0L
        }

        if (scheduleItems.isNotEmpty()) {
            val planItems = scheduleItems.filter { it.planId == plan.id && it.status == InstallmentChargeStatus.SCHEDULED }
            val sumBd = MoneyMinorUnits.sumMinorToBigDecimal(planItems.map { it.principalMinor })
            return MoneyMinorUnits.fromBigDecimal(sumBd).coerceAtLeast(0L)
        }

        // Respaldo determinístico si solo se dispone del plan y transacciones ya cargadas
        val postedPrincipalMinor = postedTransactions
            .asSequence()
            .filter {
                it.installmentPlanId == plan.id &&
                    it.entryKind == TransactionEntryKind.INSTALLMENT_CHARGE &&
                    it.tentativeDecision != "DISCARDED"
            }
            .sumOf { it.effectiveAmountMinor }
        return (plan.principalTotalMinor - postedPrincipalMinor).coerceAtLeast(0L)
    }

    /**
     * Construye el asiento único e idempotente para registrar la cuota #k en `transactions`.
     * Lleva `installmentPlanId`, `installmentNumber` y `externalId = item.idempotencyKey`.
     */
    fun buildInstallmentChargeTransaction(
        plan: InstallmentPlanEntity,
        item: InstallmentScheduleItemEntity,
        accountName: String
    ): TransactionEntity {
        val paymentMethodLabel = accountName.ifBlank { "Tarjeta de Crédito" }
        return TransactionEntity(
            title = "${plan.title} (Cuota ${item.installmentNumber}/${plan.installmentsCount})",
            amount = MoneyMinorUnits.toDisplayDouble(item.totalChargeMinor),
            type = TransactionType.EXPENSE,
            category = plan.category,
            dateMillis = item.dueDateMillis,
            paymentMethod = paymentMethodLabel,
            familyMember = plan.familyMember,
            notes = buildString {
                append("Cuota ${item.installmentNumber} de ${plan.installmentsCount}")
                if (item.interestMinor > 0L) {
                    append(" · Capital Q${MoneyMinorUnits.toBigDecimal(item.principalMinor)}")
                    append(" + Interés Q${MoneyMinorUnits.toBigDecimal(item.interestMinor)}")
                } else {
                    append(" · Sin intereses")
                }
            },
            isVerified = true,
            isTentative = false,
            tentativeDecision = "CONFIRMED",
            externalId = item.idempotencyKey,
            amountMinor = item.totalChargeMinor,
            accountId = plan.accountId,
            entryKind = TransactionEntryKind.INSTALLMENT_CHARGE,
            installmentPlanId = plan.id,
            installmentNumber = item.installmentNumber
        )
    }

    private fun addMonthsPreservingDay(
        baseMillis: Long,
        monthsToAdd: Int,
        timeZone: TimeZone
    ): Long {
        val baseCal = Calendar.getInstance(timeZone).apply { timeInMillis = baseMillis }
        val targetDay = baseCal.get(Calendar.DAY_OF_MONTH)
        val hour = baseCal.get(Calendar.HOUR_OF_DAY)
        val minute = baseCal.get(Calendar.MINUTE)
        val second = baseCal.get(Calendar.SECOND)

        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            set(Calendar.YEAR, baseCal.get(Calendar.YEAR))
            set(Calendar.MONTH, baseCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthsToAdd)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, targetDay.coerceIn(1, maxDay))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun resolveCycleKey(
        dueMillis: Long,
        cutoffDay: Int?,
        timeZone: TimeZone
    ): String {
        return if (cutoffDay != null) {
            CreditCardCycleCalculator.classifyCycleKey(dueMillis, cutoffDay, timeZone)
        } else {
            val cal = Calendar.getInstance(timeZone).apply { timeInMillis = dueMillis }
            String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }
    }
}
