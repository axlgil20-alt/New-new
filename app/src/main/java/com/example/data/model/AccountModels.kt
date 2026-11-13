package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/** Tipos de cuenta financiera soportados en Mis Cuentas (offline-first, sin credenciales). */
object AccountType {
    const val CASH = "CASH"
    const val BANK_CHECKING = "BANK_CHECKING"
    const val BANK_SAVINGS = "BANK_SAVINGS"
    const val COOPERATIVE = "COOPERATIVE"
    const val CREDIT_CARD = "CREDIT_CARD"

    fun isCreditCard(type: String): Boolean = type == CREDIT_CARD
    fun isLiquidAsset(type: String): Boolean = type != CREDIT_CARD
}

/**
 * Política configurable de reserva de crédito para compras en cuotas / Visacuotas.
 * No todas las instituciones financieras operan igual:
 * - [RESERVE_FULL_PENDING_PRINCIPAL]: El total del capital pendiente bloquea cupo desde la compra
 *   (comportamiento estándar en VisaNet/bancos guatemaltecos para Visacuotas ordinarias).
 * - [RESERVE_BILLED_INSTALLMENTS_ONLY]: Solo consumen límite ordinario las cuotas ya facturadas/cargadas
 *   al estado de cuenta (común en extrafinanciamientos con línea paralela o cooperativas).
 */
object CreditReservePolicy {
    const val RESERVE_FULL_PENDING_PRINCIPAL = "RESERVE_FULL_PENDING_PRINCIPAL"
    const val RESERVE_BILLED_INSTALLMENTS_ONLY = "RESERVE_BILLED_INSTALLMENTS_ONLY"
}

/** Clasificación contable de cada movimiento en el libro mayor único (`transactions`). */
object TransactionEntryKind {
    /** Ingreso o gasto real ordinario (afecta presupuestos, límites y quincenas). */
    const val STANDARD = "STANDARD"
    /** Salida por transferencia entre cuentas propias o pago emitido hacia tarjeta (no afecta presupuesto). */
    const val TRANSFER_OUT = "TRANSFER_OUT"
    /** Entrada por transferencia entre cuentas propias (no afecta presupuestos ni cuenta como ingreso). */
    const val TRANSFER_IN = "TRANSFER_IN"
    /** Abono recibido en tarjeta de crédito desde cuenta propia (reduce deuda sin duplicar gastos). */
    const val CARD_PAYMENT = "CARD_PAYMENT"
    /** Cargo individual de una cuota/Visacuota facturada al periodo (afecta presupuesto una sola vez). */
    const val INSTALLMENT_CHARGE = "INSTALLMENT_CHARGE"
    /** Ajuste de arqueo o conciliación de saldo (actualiza saldo de cuenta sin inflar flujo del mes). */
    const val BALANCE_ADJUSTMENT = "BALANCE_ADJUSTMENT"

    fun affectsBudget(entryKind: String): Boolean =
        entryKind.isBlank() || entryKind == STANDARD || entryKind == INSTALLMENT_CHARGE

    fun isTransferOrSettlement(entryKind: String): Boolean =
        entryKind == TRANSFER_OUT || entryKind == TRANSFER_IN || entryKind == CARD_PAYMENT
}

object InstallmentPlanStatus {
    const val ACTIVE = "ACTIVE"
    const val COMPLETED = "COMPLETED"
    const val CANCELLED = "CANCELLED"
}

object InstallmentChargeStatus {
    const val SCHEDULED = "SCHEDULED"
    const val POSTED = "POSTED"
    const val PAID_OFF_EARLY = "PAID_OFF_EARLY"
    const val CANCELLED = "CANCELLED"
}

object CommitmentKind {
    const val BILL = "BILL"
    const val CREDIT_CARD_STATEMENT = "CREDIT_CARD_STATEMENT"
    const val LOAN_INSTALLMENT = "LOAN_INSTALLMENT"
}

@Entity(
    tableName = "accounts",
    indices = [
        Index(name = "idx_accounts_type", value = ["accountType"]),
        Index(name = "idx_accounts_archived_sort", value = ["isArchived", "sortOrder"])
    ]
)
data class FinancialAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val accountType: String = AccountType.CASH,
    val institutionName: String = "",
    val lastFourDigits: String = "",
    val currencyCode: String = "GTQ",
    /**
     * Saldo inicial en centavos (`Long`).
     * En cuentas de activo representa el efectivo disponible inicial.
     * En `CREDIT_CARD` representa deuda inicial preexistente al registrar la tarjeta (`>= 0`).
     */
    val openingBalanceMinor: Long = 0L,
    /** Límite de crédito aprobado en centavos (`Long`), aplicable a `CREDIT_CARD`. */
    val creditLimitMinor: Long = 0L,
    /** Día de corte mensual (`1..31`), aplicable a `CREDIT_CARD`. */
    val cutoffDay: Int? = null,
    /** Día límite de pago mensual (`1..31`), aplicable a `CREDIT_CARD`. */
    val paymentDueDay: Int? = null,
    /** Política configurable de reserva de cupo para compras en cuotas / Visacuotas. */
    val installmentReservePolicy: String = CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL,
    val autoCreatePaymentReminder: Boolean = true,
    val includeInTotalLiquidity: Boolean = true,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val colorHex: Long = 0xFF0F5132L,
    val iconKey: String = "account_balance_wallet",
    val sortOrder: Int = 0,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val openingBalanceBigDecimal: BigDecimal
        get() = MoneyMinorUnits.toBigDecimal(openingBalanceMinor)

    val creditLimitBigDecimal: BigDecimal
        get() = MoneyMinorUnits.toBigDecimal(creditLimitMinor)
}

@Entity(
    tableName = "installment_plans",
    indices = [
        Index(name = "idx_installment_plans_accountId", value = ["accountId"]),
        Index(name = "idx_installment_plans_status", value = ["status"]),
        Index(name = "idx_installment_plans_originTx", value = ["originTransactionId"])
    ]
)
data class InstallmentPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val accountId: Long,
    val title: String,
    val merchantName: String = "",
    val category: String = "Otros Gastos",
    /** Capital original financiado en centavos (`Long`). */
    val principalTotalMinor: Long,
    /** Suma exacta de capital + intereses planificados en centavos (`Long`). */
    val totalPlannedMinor: Long,
    val installmentsCount: Int,
    /** Tasa anual en basis points (`0` = Visacuotas sin interés; `2400` = 24.00%). */
    val annualInterestRateBps: Int = 0,
    /** Cuota base de referencia en centavos (`Long`). */
    val installmentAmountMinor: Long,
    /** Política efectiva de reserva de cupo al momento de crear/configurar el plan. */
    val creditReservePolicy: String = CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL,
    val originTransactionId: Long? = null,
    val purchaseDateMillis: Long,
    val firstChargeDateMillis: Long,
    val status: String = InstallmentPlanStatus.ACTIVE,
    val familyMember: String = "Todos",
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val principalTotalBigDecimal: BigDecimal
        get() = MoneyMinorUnits.toBigDecimal(principalTotalMinor)

    val totalPlannedBigDecimal: BigDecimal
        get() = MoneyMinorUnits.toBigDecimal(totalPlannedMinor)

    val installmentAmountBigDecimal: BigDecimal
        get() = MoneyMinorUnits.toBigDecimal(installmentAmountMinor)
}

/**
 * Representación individual e idempotente de cada cuota perteneciente a un [InstallmentPlanEntity].
 * Los índices únicos `(planId, installmentNumber)` y `(idempotencyKey)` impiden duplicar cuotas
 * incluso ante reintentos o ejecuciones repetidas del ciclo de facturación.
 */
@Entity(
    tableName = "installment_schedule_items",
    indices = [
        Index(name = "idx_schedule_plan_number_unique", value = ["planId", "installmentNumber"], unique = true),
        Index(name = "idx_schedule_idempotency_unique", value = ["idempotencyKey"], unique = true),
        Index(name = "idx_schedule_account_status", value = ["accountId", "status"]),
        Index(name = "idx_schedule_linkedTx", value = ["linkedTransactionId"])
    ]
)
data class InstallmentScheduleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val planId: Long,
    val accountId: Long,
    /** Número ordinal 1-indexed de la cuota (`1..installmentsCount`). */
    val installmentNumber: Int,
    /** Clave única idempotente por plan y número de cuota (ej. `"plan_12_cuota_3"`). */
    val idempotencyKey: String,
    val dueDateMillis: Long,
    val statementCycleKey: String,
    /** Porción de amortización de capital en centavos (`Long`). */
    val principalMinor: Long,
    /** Porción de intereses del periodo en centavos (`Long`). */
    val interestMinor: Long = 0L,
    /** Cargo total exacto de la cuota (`principalMinor + interestMinor`). */
    val totalChargeMinor: Long,
    val status: String = InstallmentChargeStatus.SCHEDULED,
    val linkedTransactionId: Long? = null,
    val postedDateMillis: Long? = null
) {
    val principalBigDecimal: BigDecimal
        get() = MoneyMinorUnits.toBigDecimal(principalMinor)

    val interestBigDecimal: BigDecimal
        get() = MoneyMinorUnits.toBigDecimal(interestMinor)

    val totalChargeBigDecimal: BigDecimal
        get() = MoneyMinorUnits.toBigDecimal(totalChargeMinor)
}
