package com.example

import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionEntryKind
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.isGastoReal
import com.example.data.model.isIngresoSupuesto
import com.example.data.model.status
import com.example.ui.util.QuincenaFilterHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class Registros30ClassificationAndDuplicatesTest {
    private fun date(day: Int): Long =
        QuincenaFilterHelper.atStartOfDay(LocalDate.of(2026, 3, day))

    private fun tx(
        id: Long,
        type: String = TransactionType.EXPENSE,
        amount: Double = 25.0,
        day: Int = 10,
        tentative: Boolean = false,
        decision: String = "",
        accountId: Long? = null,
        transferGroupId: String? = null
    ) = TransactionEntity(
        id = id,
        title = "Café",
        amount = amount,
        type = type,
        category = "Prueba",
        dateMillis = date(day),
        isTentative = tentative,
        tentativeDecision = decision,
        accountId = accountId,
        transferGroupId = transferGroupId
    )

    @Test
    fun discardedNoDependeDeLaBanderaTentativeYNoEsGastoReal() {
        val discarded = tx(1, tentative = false, decision = "DISCARDED")
        assertTrue(discarded.status == TransactionStatus.DISCARDED)
        assertFalse(discarded.isGastoReal)
    }

    @Test
    fun ingresoPendienteNoSeClasificaComoReal() {
        val pendingIncome = tx(
            id = 2,
            type = TransactionType.INCOME,
            tentative = true,
            decision = "PENDING"
        )
        assertTrue(pendingIncome.isIngresoSupuesto)
        assertTrue(pendingIncome.status == TransactionStatus.SUPPOSED_INCOME)
    }

    @Test
    fun duplicadosMantienenCriteriosYExcluyenTransferenciasYCuentasDistintas() {
        val duplicateA = tx(1, accountId = 1)
        val duplicateB = tx(2, accountId = 1)
        val differentDay = tx(3, day = 11)
        val differentAccount = tx(4, accountId = 2)
        val transferA = tx(5, amount = 30.0, transferGroupId = "g1")
        val transferB = tx(6, amount = 30.0, transferGroupId = "g1")

        val ids = QuincenaFilterHelper.findDuplicateTransactionIds(
            listOf(duplicateA, duplicateB, differentDay, differentAccount, transferA, transferB)
        )

        assertTrue(1L in ids)
        assertTrue(2L in ids)
        assertFalse(3L in ids)
        assertFalse(4L in ids)
        assertFalse(5L in ids)
        assertFalse(6L in ids)
    }

    @Test
    fun movimientosSospechososMarcadosSeConservanAunqueNoTenganPareja() {
        val marked = tx(7).copy(isSuspectedDuplicate = true)
        val ids = QuincenaFilterHelper.findDuplicateTransactionIds(listOf(marked))
        assertTrue(7L in ids)
    }
}
