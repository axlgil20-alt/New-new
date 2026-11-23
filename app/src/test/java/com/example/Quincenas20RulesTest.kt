package com.example

import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionEntryKind
import com.example.data.model.TransactionType
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.model.PaymentStatus
import com.example.data.model.isCancelled
import com.example.data.model.isIngresoReal
import com.example.data.model.isIngresoSupuesto
import com.example.ui.util.BudgetPeriod
import com.example.ui.util.QuincenaFilterHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.Instant

class Quincenas20RulesTest {
    private fun date(year: Int, month: Int, day: Int): Long =
        QuincenaFilterHelper.atStartOfDay(LocalDate.of(year, month, day))

    private fun tx(id: Long, amount: Double, type: String, year: Int, month: Int, day: Int, kind: String = TransactionEntryKind.STANDARD, tentative: Boolean = false) =
        TransactionEntity(id = id, title = "Movimiento $id", amount = amount, type = type, category = "Prueba", dateMillis = date(year, month, day), entryKind = kind, isTentative = tentative)

    @Test fun ultimoDiaIniciaCicloDelMesSiguienteParaCualquierTipo() {
        assertEquals(BudgetPeriod(2026, 1, 1), QuincenaFilterHelper.getBudgetPeriod(date(2026, 1, 31), TransactionType.EXPENSE))
        assertEquals(BudgetPeriod(2026, 1, 1), QuincenaFilterHelper.getBudgetPeriod(date(2026, 1, 31), TransactionType.INCOME))
    }

    @Test fun dia15IniciaSegundoCicloYNoSeMuevePorTipo() {
        val expected = BudgetPeriod(2026, 1, 2)
        assertEquals(expected, QuincenaFilterHelper.getBudgetPeriod(date(2026, 2, 15), TransactionType.EXPENSE))
        assertEquals(expected, QuincenaFilterHelper.getBudgetPeriod(date(2026, 2, 15), TransactionType.INCOME))
    }

    @Test fun limites28_29_30_31YContinuidadNoTienenHuecos() {
        listOf(2025 to 2, 2024 to 2, 2026 to 4, 2026 to 1).forEach { (year, month) ->
            val ym = YearMonth.of(year, month)
            val q1 = QuincenaFilterHelper.getBudgetPeriodRange(year, month - 1, 1)
            val q2 = QuincenaFilterHelper.getBudgetPeriodRange(year, month - 1, 2)
            assertEquals(ym.atDay(14), QuincenaFilterHelper.localDate(q1.second))
            assertEquals(ym.atDay(15), QuincenaFilterHelper.localDate(q2.first))
            assertEquals(ym.atEndOfMonth().minusDays(1), QuincenaFilterHelper.localDate(q2.second))
            assertEquals(ym.minusMonths(1).atEndOfMonth(), QuincenaFilterHelper.localDate(q1.first))
        }
    }

    @Test fun ambosCiclosDeFebreroSePuedenConsultarSinSolaparse() {
        val q1 = QuincenaFilterHelper.getBudgetPeriodRange(2026, 1, 1)
        val q2 = QuincenaFilterHelper.getBudgetPeriodRange(2026, 1, 2)
        assertEquals(LocalDate.of(2026, 1, 31), QuincenaFilterHelper.localDate(q1.first))
        assertEquals(LocalDate.of(2026, 2, 14), QuincenaFilterHelper.localDate(q1.second))
        assertEquals(LocalDate.of(2026, 2, 15), QuincenaFilterHelper.localDate(q2.first))
        assertEquals(LocalDate.of(2026, 2, 27), QuincenaFilterHelper.localDate(q2.second))
    }

    @Test fun rolloverNoSeCuentaComoIngresoNuevoYTransferenciasSeExcluyen() {
        val txs = listOf(
            tx(1, 100.0, TransactionType.INCOME, 2026, 1, 15),
            tx(2, 25.0, TransactionType.EXPENSE, 2026, 1, 16),
            tx(3, 50.0, TransactionType.INCOME, 2026, 1, 31, TransactionEntryKind.TRANSFER_IN),
            tx(4, 10.0, TransactionType.EXPENSE, 2026, 2, 1, tentative = true)
        )
        val febQ1 = QuincenaFilterHelper.filterByQuincena(txs, 2026, 1)
        assertEquals(0.0, febQ1.totalIncome, 0.001)
        assertEquals(75.0, QuincenaFilterHelper.calculateQuincenaRollover(txs, 2026, 1, 1), 0.001)
        assertEquals(10.0, febQ1.totalPendingExpense, 0.001)
        assertTrue(febQ1.monthTransactions.none { it.entryKind == TransactionEntryKind.TRANSFER_IN })
    }

    @Test fun fechaLocalGuatemalaSeUsaParaAsignacion() {
        val local = LocalDate.of(2026, 3, 15)
        val millis = QuincenaFilterHelper.atStartOfDay(local)
        assertEquals(local, QuincenaFilterHelper.localDate(millis))
        assertEquals(2, QuincenaFilterHelper.getBudgetPeriod(millis).quincena)
    }

    @Test fun timestampsUtcRespetanElDiaLocalDeGuatemala() {
        val antesDeMedianocheLocal = Instant.parse("2026-02-15T05:30:00Z").toEpochMilli()
        val despuesDeMedianocheLocal = Instant.parse("2026-02-15T06:30:00Z").toEpochMilli()
        assertEquals(LocalDate.of(2026, 2, 14), QuincenaFilterHelper.localDate(antesDeMedianocheLocal))
        assertEquals(1, QuincenaFilterHelper.getBudgetPeriod(antesDeMedianocheLocal).quincena)
        assertEquals(LocalDate.of(2026, 2, 15), QuincenaFilterHelper.localDate(despuesDeMedianocheLocal))
        assertEquals(2, QuincenaFilterHelper.getBudgetPeriod(despuesDeMedianocheLocal).quincena)

        val ultimoDiaLocal = Instant.parse("2026-03-01T05:30:00Z").toEpochMilli()
        assertEquals(LocalDate.of(2026, 2, 28), QuincenaFilterHelper.localDate(ultimoDiaLocal))
        assertEquals(BudgetPeriod(2026, 2, 1), QuincenaFilterHelper.getBudgetPeriod(ultimoDiaLocal))
    }

    @Test fun ingresosTentativosNoSeConfundenConIngresosConfirmados() {
        val tentativeIncome = tx(10, 500.0, TransactionType.INCOME, 2026, 2, 10, tentative = true)
        assertTrue(tentativeIncome.isIngresoSupuesto)
        assertTrue(!tentativeIncome.isIngresoReal)
        assertEquals(0.0, QuincenaFilterHelper.filterByQuincena(listOf(tentativeIncome), 2026, 1).totalIncome, 0.001)
    }
    @Test fun estadosDeCompromisoDistinguenPagadoCanceladoYPendiente() {
        val pending = PaymentCommitmentEntity(title = "Pendiente", beneficiaryName = "Prueba", amount = 10.0, dueDateMillis = date(2026, 2, 10))
        val paid = pending.copy(title = "Pagado", isPaid = true, status = PaymentStatus.PAID)
        val cancelled = pending.copy(title = "Cancelado", status = PaymentStatus.CANCELLED)
        assertTrue(!pending.isPaid && !pending.isCancelled)
        assertTrue(paid.isPaid && !paid.isCancelled)
        assertTrue(!cancelled.isPaid && cancelled.isCancelled)
    }
}
