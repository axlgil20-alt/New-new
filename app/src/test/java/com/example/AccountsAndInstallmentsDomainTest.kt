package com.example

import com.example.data.model.AccountType
import com.example.data.model.CreditReservePolicy
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.InstallmentChargeStatus
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.InstallmentPlanStatus
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionEntryKind
import com.example.data.model.TransactionType
import com.example.data.model.affectsBudgetTotals
import com.example.data.model.effectiveAmountMinor
import com.example.data.model.isGastoReal
import com.example.data.model.isIngresoReal
import com.example.data.model.isMonetarySynchronized
import com.example.data.model.withSyncedLegacyAmountFromMinor
import com.example.data.model.withSyncedMinorFromLegacyAmount
import com.example.data.service.AccountBalanceCalculator
import com.example.data.service.CreditCardCycleCalculator
import com.example.data.service.InstallmentScheduleEngine
import com.example.ui.util.BudgetLimitCalculator
import com.example.ui.util.QuincenaFilterHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar
import java.util.TimeZone

class AccountsAndInstallmentsDomainTest {

    private val tz: TimeZone = TimeZone.getTimeZone("America/Guatemala")

    private fun millis(year: Int, monthIndex: Int, day: Int, hour: Int = 12): Long {
        return Calendar.getInstance(tz).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }

    @Test
    fun moneyMinorUnits_eliminatesFloatingPointDrift_andDistributesRemaindersExactly() {
        // 0.10 + 0.20 exacto en centavos
        val sum = MoneyMinorUnits.sumMinorToBigDecimal(listOf(10L, 20L))
        assertEquals(0, BigDecimal("0.30").compareTo(sum))

        // Q100.00 en 3 cuotas -> Q33.33, Q33.33, Q33.34 (suma exacta = 10000L)
        val three = MoneyMinorUnits.distributeEvenly(10_000L, 3)
        assertEquals(listOf(3333L, 3333L, 3334L), three)
        assertEquals(10_000L, three.sum())

        // Caso extremo: Q0.04 en 6 cuotas -> nunca cuotas negativas ni pérdida de centavos
        val tiny = MoneyMinorUnits.distributeEvenly(4L, 6)
        assertEquals(listOf(0L, 0L, 1L, 1L, 1L, 1L), tiny)
        assertTrue(tiny.all { it >= 0L })
        assertEquals(4L, tiny.sum())
    }

    @Test
    fun internalTransfers_shiftAccountBalances_withZeroImpactOnBudgetQuincenasAndLimits() {
        val bankAccount = FinancialAccountEntity(
            id = 1L,
            name = "Monetaria BI",
            accountType = AccountType.BANK_CHECKING,
            openingBalanceMinor = 500_000L // Q5,000.00
        )
        val cashAccount = FinancialAccountEntity(
            id = 2L,
            name = "Efectivo Casa",
            accountType = AccountType.CASH,
            openingBalanceMinor = 50_000L // Q500.00
        )

        val dNov02 = millis(2026, Calendar.NOVEMBER, 2)
        val dNov05 = millis(2026, Calendar.NOVEMBER, 5)

        val salary = TransactionEntity(
            id = 1L,
            title = "Salario Q1",
            amount = 4000.00,
            amountMinor = 400_000L,
            type = TransactionType.INCOME,
            category = "Salario / Sueldo",
            dateMillis = dNov02,
            accountId = 1L,
            entryKind = TransactionEntryKind.STANDARD
        )
        val grocery = TransactionEntity(
            id = 2L,
            title = "Mercado",
            amount = 300.00,
            amountMinor = 30_000L,
            type = TransactionType.EXPENSE,
            category = "Alimentación & Súper",
            dateMillis = dNov05,
            accountId = 2L,
            entryKind = TransactionEntryKind.STANDARD
        )

        // Transferencia propia de Banco BI -> Efectivo por Q1,000.00 vinculada con transferGroupId
        val transferGroup = "tx-grp-100"
        val transferOut = TransactionEntity(
            id = 3L,
            title = "Retiro a Efectivo Casa",
            amount = 1000.00,
            amountMinor = 100_000L,
            type = TransactionType.EXPENSE,
            category = "Transferencia entre cuentas",
            dateMillis = dNov05,
            accountId = 1L,
            counterpartAccountId = 2L,
            transferGroupId = transferGroup,
            entryKind = TransactionEntryKind.TRANSFER_OUT
        )
        val transferIn = TransactionEntity(
            id = 4L,
            title = "Ingreso desde Monetaria BI",
            amount = 1000.00,
            amountMinor = 100_000L,
            type = TransactionType.INCOME,
            category = "Transferencia entre cuentas",
            dateMillis = dNov05,
            accountId = 2L,
            counterpartAccountId = 1L,
            transferGroupId = transferGroup,
            entryKind = TransactionEntryKind.TRANSFER_IN
        )

        val allTxs = listOf(salary, grocery, transferOut, transferIn)

        // 1. Neutralidad presupuestaria
        assertFalse(transferOut.affectsBudgetTotals)
        assertFalse(transferOut.isGastoReal)
        assertFalse(transferIn.affectsBudgetTotals)
        assertFalse(transferIn.isIngresoReal)

        val qData = QuincenaFilterHelper.filterByQuincena(allTxs, 2026, Calendar.NOVEMBER)
        assertEquals(4000.00, qData.q1Income, 0.001)
        assertEquals(300.00, qData.q1Expense, 0.001)
        assertEquals(3700.00, qData.q1Net, 0.001)

        val foodSpend = BudgetLimitCalculator.monthlySpend(allTxs, "Alimentación & Súper", 2026, Calendar.NOVEMBER)
        assertEquals(0, BigDecimal("300.00").compareTo(foodSpend))

        // No debe marcar los dos lados de la transferencia como duplicados
        assertTrue(QuincenaFilterHelper.findDuplicateTransactionIds(allTxs).isEmpty())

        // 2. Saldos exactos por cuenta y liquidez total conservada
        val portfolio = AccountBalanceCalculator.calculatePortfolioSummary(
            accounts = listOf(bankAccount, cashAccount),
            transactions = allTxs
        )
        // BI: 5,000 + 4,000 - 1,000 = 8,000 (800,000 centavos)
        assertEquals(800_000L, portfolio.snapshotsByAccountId[1L]!!.currentBalanceMinor)
        // Efectivo: 500 + 1,000 - 300 = 1,200 (120,000 centavos)
        assertEquals(120_000L, portfolio.snapshotsByAccountId[2L]!!.currentBalanceMinor)
        // Liquidez total: 8,000 + 1,200 = 9,200 (igual que sin la transferencia)
        assertEquals(920_000L, portfolio.totalLiquidAssetsMinor)
        assertEquals(920_000L, portfolio.netWorthMinor)
    }

    @Test
    fun creditCardPayments_partialAndFull_neverDoubleCountExpenses_andTrackDebtAndAvailableCredit() {
        val checking = FinancialAccountEntity(
            id = 1L,
            name = "Monetaria G&T",
            accountType = AccountType.BANK_CHECKING,
            openingBalanceMinor = 500_000L // Q5,000.00
        )
        val visaCard = FinancialAccountEntity(
            id = 2L,
            name = "Visa G&T",
            accountType = AccountType.CREDIT_CARD,
            openingBalanceMinor = 20_000L, // Q200.00 saldo previo
            creditLimitMinor = 1_000_000L, // Q10,000.00 límite
            cutoffDay = 15,
            paymentDueDay = 5
        )

        val dNov03 = millis(2026, Calendar.NOVEMBER, 3)
        val dNov10 = millis(2026, Calendar.NOVEMBER, 10)
        val dNov12 = millis(2026, Calendar.NOVEMBER, 12)

        val purchase = TransactionEntity(
            id = 10L,
            title = "Supermercado Paiz",
            amount = 100.0 * 12,
            amountMinor = 120_000L, // Q1,200.00
            type = TransactionType.EXPENSE,
            category = "Alimentación & Súper",
            dateMillis = dNov03,
            accountId = 2L,
            entryKind = TransactionEntryKind.STANDARD
        )

        // Abono parcial Q500.00 desde Monetaria G&T -> Visa G&T
        val partialOut = TransactionEntity(
            id = 11L,
            title = "Abono Visa G&T",
            amount = 500.00,
            amountMinor = 50_000L,
            type = TransactionType.EXPENSE,
            category = "Pago de Tarjeta",
            dateMillis = dNov10,
            accountId = 1L,
            counterpartAccountId = 2L,
            transferGroupId = "card-pay-1",
            entryKind = TransactionEntryKind.TRANSFER_OUT
        )
        val partialCardCredit = TransactionEntity(
            id = 12L,
            title = "Pago recibido desde Monetaria G&T",
            amount = 500.00,
            amountMinor = 50_000L,
            type = TransactionType.INCOME,
            category = "Pago de Tarjeta",
            dateMillis = dNov10,
            accountId = 2L,
            counterpartAccountId = 1L,
            transferGroupId = "card-pay-1",
            entryKind = TransactionEntryKind.CARD_PAYMENT
        )

        val txsPartial = listOf(purchase, partialOut, partialCardCredit)
        val snapAfterPartial = AccountBalanceCalculator.calculateAccountSnapshot(visaCard, txsPartial)

        // Deuda rotativa: 200 previo + 1,200 compra - 500 abono = Q900.00 (90,000 centavos)
        assertEquals(90_000L, snapAfterPartial.revolvingDebtMinor)
        assertEquals(90_000L, snapAfterPartial.totalDebtMinor)
        // Disponible: 10,000 - 900 = Q9,100.00
        assertEquals(910_000L, snapAfterPartial.availableCreditMinor)
        assertEquals(0, BigDecimal("9.00").compareTo(snapAfterPartial.utilizationPercent))

        // Validación de pago parcial vs total
        val checkingSnap = AccountBalanceCalculator.calculateAccountSnapshot(checking, txsPartial)
        assertEquals(450_000L, checkingSnap.currentBalanceMinor) // Q4,500.00

        val partialValidation = AccountBalanceCalculator.validateCreditCardPayment(
            sourceAccount = checking,
            sourceSnapshot = checkingSnap,
            targetCard = visaCard,
            targetSnapshot = snapAfterPartial,
            paymentAmountMinor = 40_000L // otro abono de Q400
        )
        assertTrue(partialValidation.isValid)
        assertTrue(partialValidation.isPartialPayment)
        assertFalse(partialValidation.isOverpayment)
        assertEquals(50_000L, partialValidation.resultingRevolvingDebtMinor)

        // Presupuesto / Quincenas / Límites: solo contó los Q1,200 de la compra, cero doble conteo por Q500
        val qData = QuincenaFilterHelper.filterByQuincena(txsPartial, 2026, Calendar.NOVEMBER)
        assertEquals(0.0, qData.q1Income, 0.001)
        assertEquals(1200.0, qData.q1Expense, 0.001)
        val limitFood = BudgetLimitCalculator.monthlySpend(txsPartial, "Alimentación & Súper", 2026, Calendar.NOVEMBER)
        assertEquals(0, BigDecimal("1200.00").compareTo(limitFood))

        // Sobrepago de Q1,000 sobre deuda restante de Q900 -> saldo a favor Q100, disponible no excede límite
        val overPayCredit = TransactionEntity(
            id = 13L,
            title = "Pago completo + excedente",
            amount = 1000.00,
            amountMinor = 100_000L,
            type = TransactionType.INCOME,
            category = "Pago de Tarjeta",
            dateMillis = dNov12,
            accountId = 2L,
            counterpartAccountId = 1L,
            transferGroupId = "card-pay-2",
            entryKind = TransactionEntryKind.CARD_PAYMENT
        )
        val snapOverpaid = AccountBalanceCalculator.calculateAccountSnapshot(visaCard, txsPartial + overPayCredit)
        assertEquals(-10_000L, snapOverpaid.totalDebtMinor)
        assertEquals(10_000L, snapOverpaid.creditSurplusMinor)
        assertEquals(1_000_000L, snapOverpaid.availableCreditMinor)
    }

    @Test
    fun visacuotas_configurableCreditReserve_andIdempotentSchedulePosting_preventDoubleCounting() {
        val cardFullReserve = FinancialAccountEntity(
            id = 10L,
            name = "Visa Promerica",
            accountType = AccountType.CREDIT_CARD,
            creditLimitMinor = 1_000_000L, // Q10,000.00
            cutoffDay = 15,
            paymentDueDay = 5,
            installmentReservePolicy = CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL
        )
        val cardBilledOnly = cardFullReserve.copy(
            id = 11L,
            name = "Extra Cooperativa",
            installmentReservePolicy = CreditReservePolicy.RESERVE_BILLED_INSTALLMENTS_ONLY
        )

        val dPurchase = millis(2026, Calendar.NOVEMBER, 2)
        val dFirstCharge = millis(2026, Calendar.NOVEMBER, 15)

        val planFull = InstallmentPlanEntity(
            id = 50L,
            accountId = 10L,
            title = "Refrigeradora MAX",
            category = "Vivienda & Servicios",
            principalTotalMinor = 600_000L, // Q6,000.00
            totalPlannedMinor = 600_000L,
            installmentsCount = 12,
            annualInterestRateBps = 0,
            installmentAmountMinor = 50_000L, // Q500.00
            creditReservePolicy = CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL,
            purchaseDateMillis = dPurchase,
            firstChargeDateMillis = dFirstCharge,
            status = InstallmentPlanStatus.ACTIVE
        )
        val planBilledOnly = planFull.copy(
            id = 51L,
            accountId = 11L,
            creditReservePolicy = CreditReservePolicy.RESERVE_BILLED_INSTALLMENTS_ONLY
        )

        val scheduleFull = InstallmentScheduleEngine.generateSchedule(
            planId = 50L,
            accountId = 10L,
            principalTotalMinor = 600_000L,
            installmentsCount = 12,
            annualInterestRateBps = 0,
            firstChargeDateMillis = dFirstCharge,
            cutoffDay = 15,
            timeZone = tz
        )

        // Identidad única de cada una de las 12 cuotas
        assertEquals(12, scheduleFull.items.size)
        assertEquals(12, scheduleFull.items.map { it.idempotencyKey }.toSet().size)
        assertEquals("plan_50_cuota_1", scheduleFull.items[0].idempotencyKey)
        assertEquals("plan_50_cuota_12", scheduleFull.items[11].idempotencyKey)
        assertEquals(600_000L, scheduleFull.items.sumOf { it.totalChargeMinor })

        // Día 1 (antes de facturar cuota 1):
        // Política FULL -> reserva completa Q6,000 -> disponible Q4,000
        val snapBeforeChargeFull = AccountBalanceCalculator.calculateAccountSnapshot(
            account = cardFullReserve,
            transactions = emptyList(),
            installmentPlans = listOf(planFull),
            scheduleItems = scheduleFull.items
        )
        assertEquals(0L, snapBeforeChargeFull.revolvingDebtMinor)
        assertEquals(600_000L, snapBeforeChargeFull.reservedInstallmentDebtMinor)
        assertEquals(600_000L, snapBeforeChargeFull.totalDebtMinor)
        assertEquals(400_000L, snapBeforeChargeFull.availableCreditMinor)

        // Política BILLED_ONLY -> no reserva cuotas futuras -> disponible Q10,000
        val scheduleBilled = InstallmentScheduleEngine.generateSchedule(
            planId = 51L,
            accountId = 11L,
            principalTotalMinor = 600_000L,
            installmentsCount = 12,
            annualInterestRateBps = 0,
            firstChargeDateMillis = dFirstCharge,
            cutoffDay = 15,
            timeZone = tz
        )
        val snapBeforeChargeBilled = AccountBalanceCalculator.calculateAccountSnapshot(
            account = cardBilledOnly,
            transactions = emptyList(),
            installmentPlans = listOf(planBilledOnly),
            scheduleItems = scheduleBilled.items
        )
        assertEquals(0L, snapBeforeChargeBilled.reservedInstallmentDebtMinor)
        assertEquals(1_000_000L, snapBeforeChargeBilled.availableCreditMinor)

        // Se factura Cuota 1 (Q500.00): pasa de SCHEDULED a POSTED + asiento INSTALLMENT_CHARGE
        val item1Posted = scheduleFull.items[0].copy(
            status = InstallmentChargeStatus.POSTED,
            linkedTransactionId = 901L,
            postedDateMillis = dFirstCharge
        )
        val updatedItems = listOf(item1Posted) + scheduleFull.items.drop(1)
        val txCuota1 = InstallmentScheduleEngine.buildInstallmentChargeTransaction(
            plan = planFull,
            item = item1Posted,
            accountName = cardFullReserve.name
        ).copy(id = 901L)

        // Invariante crítica: deuda rotativa Q500 + reserva pendiente Q5,500 = Q6,000 exactos (sin duplicar)
        val snapAfterCuota1 = AccountBalanceCalculator.calculateAccountSnapshot(
            account = cardFullReserve,
            transactions = listOf(txCuota1),
            installmentPlans = listOf(planFull),
            scheduleItems = updatedItems
        )
        assertEquals(50_000L, snapAfterCuota1.revolvingDebtMinor)
        assertEquals(550_000L, snapAfterCuota1.reservedInstallmentDebtMinor)
        assertEquals(600_000L, snapAfterCuota1.totalDebtMinor)
        assertEquals(400_000L, snapAfterCuota1.availableCreditMinor)

        // Al pagar la tarjeta Q500.00, deuda baja a Q5,500 y se liberan Q500 de cupo sin duplicar gasto
        val cardPay = TransactionEntity(
            id = 902L,
            title = "Pago cuota 1",
            amount = 500.00,
            amountMinor = 50_000L,
            type = TransactionType.INCOME,
            category = "Pago de Tarjeta",
            dateMillis = dFirstCharge,
            accountId = 10L,
            transferGroupId = "pay-cuota-1",
            entryKind = TransactionEntryKind.CARD_PAYMENT
        )
        val txsAfterPayment = listOf(txCuota1, cardPay)
        val snapAfterPayment = AccountBalanceCalculator.calculateAccountSnapshot(
            account = cardFullReserve,
            transactions = txsAfterPayment,
            installmentPlans = listOf(planFull),
            scheduleItems = updatedItems
        )
        assertEquals(0L, snapAfterPayment.revolvingDebtMinor)
        assertEquals(550_000L, snapAfterPayment.reservedInstallmentDebtMinor)
        assertEquals(550_000L, snapAfterPayment.totalDebtMinor)
        assertEquals(450_000L, snapAfterPayment.availableCreditMinor)

        // En presupuesto del mes únicamente contó la cuota 1 (Q500.00)
        val budgetSpend = BudgetLimitCalculator.monthlySpend(txsAfterPayment, "Vivienda & Servicios", 2026, Calendar.NOVEMBER)
        assertEquals(0, BigDecimal("500.00").compareTo(budgetSpend))
    }

    @Test
    fun interestBearingInstallments_absorbResidualCentsInFinalInstallment_closingPrincipalAtZero() {
        val dFirst = millis(2026, Calendar.JANUARY, 31)
        val schedule = InstallmentScheduleEngine.generateSchedule(
            planId = 77L,
            accountId = 10L,
            principalTotalMinor = 1_000_000L, // Q10,000.00
            installmentsCount = 12,
            annualInterestRateBps = 2400, // 24.00% anual -> 2.00% mensual
            firstChargeDateMillis = dFirst,
            cutoffDay = 31,
            timeZone = tz
        )

        assertEquals(12, schedule.items.size)
        val sumPrincipal = schedule.items.sumOf { it.principalMinor }
        val sumInterest = schedule.items.sumOf { it.interestMinor }
        val sumTotal = schedule.items.sumOf { it.totalChargeMinor }

        assertEquals("La amortización total debe cerrar exactamente en Q10,000.00", 1_000_000L, sumPrincipal)
        assertEquals(sumPrincipal + sumInterest, sumTotal)
        assertEquals(schedule.totalInterestMinor, sumInterest)
        assertEquals(schedule.totalPlannedMinor, sumTotal)
        assertTrue(schedule.items.all { it.totalChargeMinor == it.principalMinor + it.interestMinor })

        // Clamping seguro de día 31 en febrero (no salta a marzo!)
        val febCal = Calendar.getInstance(tz).apply { timeInMillis = schedule.items[1].dueDateMillis }
        assertEquals(Calendar.FEBRUARY, febCal.get(Calendar.MONTH))
        assertEquals(28, febCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun creditCardCycleCalculator_handlesLeapYearCutoffClamping_andPostCutoffPayments() {
        val card = FinancialAccountEntity(
            id = 20L,
            name = "Visa Feb",
            accountType = AccountType.CREDIT_CARD,
            creditLimitMinor = 500_000L,
            cutoffDay = 30,
            paymentDueDay = 15
        )
        // En año bisiesto 2024, corte día 30 en febrero se ajusta al 29 de febrero
        val cutoffFebLeap = CreditCardCycleCalculator.resolveDayEndMillis(2024, Calendar.FEBRUARY, 30, tz)
        val calLeap = Calendar.getInstance(tz).apply { timeInMillis = cutoffFebLeap }
        assertEquals(Calendar.FEBRUARY, calLeap.get(Calendar.MONTH))
        assertEquals(29, calLeap.get(Calendar.DAY_OF_MONTH))

        // En noviembre 2026 con corte 15 y pago día 5:
        // Consumos <= Nov 15 entran al corte Nov 15 (pago Dic 5); consumos > Nov 15 entran al ciclo abierto Dic 15
        val cardNov = card.copy(cutoffDay = 15, paymentDueDay = 5)
        val nov10Purchase = TransactionEntity(
            id = 1L,
            title = "Compra ciclo cerrado",
            amount = 1200.00,
            amountMinor = 120_000L,
            type = TransactionType.EXPENSE,
            category = "Comida",
            dateMillis = millis(2026, Calendar.NOVEMBER, 10),
            accountId = 20L,
            entryKind = TransactionEntryKind.STANDARD
        )
        val nov20Purchase = TransactionEntity(
            id = 2L,
            title = "Compra ciclo abierto",
            amount = 300.00,
            amountMinor = 30_000L,
            type = TransactionType.EXPENSE,
            category = "Comida",
            dateMillis = millis(2026, Calendar.NOVEMBER, 20),
            accountId = 20L,
            entryKind = TransactionEntryKind.STANDARD
        )
        val nov25PartialPayment = TransactionEntity(
            id = 3L,
            title = "Abono al corte de Nov 15",
            amount = 500.00,
            amountMinor = 50_000L,
            type = TransactionType.INCOME,
            category = "Pago de Tarjeta",
            dateMillis = millis(2026, Calendar.NOVEMBER, 25),
            accountId = 20L,
            entryKind = TransactionEntryKind.CARD_PAYMENT
        )

        val refNov26 = millis(2026, Calendar.NOVEMBER, 26, hour = 18)
        val statement = CreditCardCycleCalculator.calculateStatementSummary(
            card = cardNov,
            transactions = listOf(nov10Purchase, nov20Purchase, nov25PartialPayment),
            referenceDateMillis = refNov26,
            timeZone = tz
        )

        assertEquals("2026-11", statement.cycleWindow.lastStatementCycleKey)
        assertEquals("2026-12", statement.cycleWindow.currentStatementCycleKey)
        // Saldo del corte Nov 15 (Q1,200) menos abono parcial Nov 25 (Q500) = Q700 exigible
        assertEquals(70_000L, statement.closedStatementRemainingDueMinor)
        assertEquals(50_000L, statement.paymentsAfterLastCutoffMinor)
        // Consumos nuevos del ciclo actual (Q300) aún no exigibles en el corte cerrado
        assertEquals(30_000L, statement.currentCycleNewChargesMinor)
        assertTrue(statement.hasStatementBalanceDue)
    }

    @Test
    fun legacyUnassignedTransactions_remainTrackedSeparately_withoutBreakingExistingCalculations() {
        val legacyIncome = TransactionEntity(
            id = 1L,
            title = "Sueldo sin cuenta",
            amount = 5000.00,
            type = TransactionType.INCOME,
            category = "Salario / Sueldo",
            dateMillis = millis(2026, Calendar.NOVEMBER, 1)
        )
        val legacyExpense = TransactionEntity(
            id = 2L,
            title = "Gasto sin cuenta",
            amount = 1250.50,
            type = TransactionType.EXPENSE,
            category = "Alimentación & Súper",
            dateMillis = millis(2026, Calendar.NOVEMBER, 4)
        )
        val cashAccount = FinancialAccountEntity(
            id = 1L,
            name = "Efectivo",
            accountType = AccountType.CASH,
            openingBalanceMinor = 20_000L // Q200.00
        )

        val portfolio = AccountBalanceCalculator.calculatePortfolioSummary(
            accounts = listOf(cashAccount),
            transactions = listOf(legacyIncome, legacyExpense)
        )

        assertEquals(20_000L, portfolio.totalLiquidAssetsMinor)
        assertEquals(2, portfolio.unassignedTransactionsCount)
        assertEquals(374_950L, portfolio.unassignedNetImpactMinor) // 5000.00 - 1250.50 = 3749.50
        assertTrue(legacyIncome.isIngresoReal)
        assertTrue(legacyExpense.isGastoReal)
    }

    @Test
    fun monetaryDesynchronization_failsLoudly_andExplicitSyncRestoresParity() {
        val mismatchedTx = TransactionEntity(
            id = 99L,
            title = "Edición desincronizada",
            amount = 250.00,
            amountMinor = 10_000L, // Q100.00 heredado vs Q250.00 nuevo
            type = TransactionType.EXPENSE,
            category = "Otros Gastos",
            dateMillis = millis(2026, Calendar.NOVEMBER, 10)
        )
        assertFalse(mismatchedTx.isMonetarySynchronized)
        var txFailedLoudly = false
        try {
            mismatchedTx.effectiveAmountMinor
        } catch (_: IllegalStateException) {
            txFailedLoudly = true
        }
        assertTrue("TransactionEntity desincronizada no debe ocultar ni elegir en silencio un monto", txFailedLoudly)

        val syncedFromLegacy = mismatchedTx.withSyncedMinorFromLegacyAmount()
        assertTrue(syncedFromLegacy.isMonetarySynchronized)
        assertEquals(25_000L, syncedFromLegacy.effectiveAmountMinor)

        val syncedFromMinor = mismatchedTx.withSyncedLegacyAmountFromMinor()
        assertTrue(syncedFromMinor.isMonetarySynchronized)
        assertEquals(10_000L, syncedFromMinor.effectiveAmountMinor)
        assertEquals(100.00, syncedFromMinor.amount, 0.0001)

        val mismatchedCommitment = PaymentCommitmentEntity(
            id = 55L,
            title = "Pago desincronizado",
            beneficiaryName = "EEGSA",
            amount = 350.00,
            amountMinor = 20_000L,
            dueDateMillis = millis(2026, Calendar.NOVEMBER, 15)
        )
        assertFalse(mismatchedCommitment.isMonetarySynchronized)
        var commitmentFailedLoudly = false
        try {
            mismatchedCommitment.effectiveAmountMinor
        } catch (_: IllegalStateException) {
            commitmentFailedLoudly = true
        }
        assertTrue("PaymentCommitmentEntity desincronizado debe fallar explícitamente", commitmentFailedLoudly)

        val syncedCommitment = mismatchedCommitment.withSyncedMinorFromLegacyAmount()
        assertTrue(syncedCommitment.isMonetarySynchronized)
        assertEquals(35_000L, syncedCommitment.effectiveAmountMinor)
    }
}
