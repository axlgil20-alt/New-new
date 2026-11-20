package com.example

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.model.PaymentFrequency
import com.example.data.model.PaymentStatus
import com.example.data.repository.BudgetRepository
import com.example.ui.components.CommitmentPriorityGroup
import com.example.ui.components.PaymentCommitmentStackLogic
import com.example.ui.screens.PaymentsScreen
import com.example.ui.viewmodel.BudgetUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaymentCardStackAndEdgeCasesTest {

    private val dayMs = 24L * 60L * 60L * 1000L

    private fun fixedNowMillis(): Long {
        return Calendar.getInstance(TimeZone.getDefault()).apply {
            set(2026, Calendar.NOVEMBER, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun sampleCommitment(
        id: Long,
        title: String,
        beneficiary: String = "EEGSA",
        amount: Double = 250.0,
        dueDateMillis: Long,
        isPaid: Boolean = false,
        status: String = if (isPaid) PaymentStatus.PAID else PaymentStatus.PENDING,
        paidDateMillis: Long? = null
    ) = PaymentCommitmentEntity(
        id = id,
        title = title,
        beneficiaryName = beneficiary,
        description = "Servicio mensual",
        amount = amount,
        dueDateMillis = dueDateMillis,
        isPaid = isPaid,
        status = status,
        paidDateMillis = paidDateMillis
    )

    @Test
    fun emptyAndSingleCommitment_preserveCleanBoundariesWithoutArtificialDuplicates() {
        val now = fixedNowMillis()

        // 0 compromisos
        val emptySorted = PaymentCommitmentStackLogic.sortCommitmentsForStack(emptyList(), now)
        assertTrue(emptySorted.isEmpty())
        assertNull(PaymentCommitmentStackLogic.resolveSelectedCommitmentId(emptySorted, 99L))
        assertEquals(0, PaymentCommitmentStackLogic.countUpcomingOrOverdue(emptySorted, now))

        // 1 único compromiso
        val single = sampleCommitment(id = 10L, title = "Agua Municipal", dueDateMillis = now + 2 * dayMs)
        val singleSorted = PaymentCommitmentStackLogic.sortCommitmentsForStack(listOf(single, single), now)
        assertEquals(1, singleSorted.size)
        assertEquals(10L, PaymentCommitmentStackLogic.resolveSelectedCommitmentId(singleSorted, null))
        assertEquals(1, PaymentCommitmentStackLogic.countUpcomingOrOverdue(singleSorted, now))
    }

    @Test
    fun prioritySorting_placesOverdueOldestFirst_thenUpcomingNearest_thenLaterPending_thenPaid() {
        val now = fixedNowMillis()
        val overdueOldest = sampleCommitment(id = 1L, title = "Luz Septiembre", dueDateMillis = now - 10 * dayMs)
        val overdueNewer = sampleCommitment(id = 2L, title = "Agua Octubre", dueDateMillis = now - 2 * dayMs)
        val dueToday = sampleCommitment(id = 3L, title = "Internet Hoy", dueDateMillis = now)
        val upcomingSoon = sampleCommitment(id = 4L, title = "Teléfono +3d", dueDateMillis = now + 3 * dayMs)
        val upcomingFarther = sampleCommitment(id = 5L, title = "Colegiatura +6d", dueDateMillis = now + 6 * dayMs)
        val laterPending = sampleCommitment(id = 6L, title = "Seguro +20d", dueDateMillis = now + 20 * dayMs)
        val paidItem = sampleCommitment(
            id = 7L,
            title = "Renta Pagada",
            dueDateMillis = now - 15 * dayMs,
            isPaid = true,
            status = PaymentStatus.PAID,
            paidDateMillis = now - dayMs
        )

        val shuffled = listOf(paidItem, laterPending, upcomingFarther, overdueNewer, dueToday, overdueOldest, upcomingSoon)
        val sorted = PaymentCommitmentStackLogic.sortCommitmentsForStack(shuffled, now)

        assertEquals(
            listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
            sorted.map { it.id }
        )
        assertEquals(CommitmentPriorityGroup.OVERDUE, PaymentCommitmentStackLogic.classifyPriorityGroup(overdueOldest, now))
        assertEquals(CommitmentPriorityGroup.DUE_TODAY_OR_UPCOMING, PaymentCommitmentStackLogic.classifyPriorityGroup(dueToday, now))
        assertEquals(CommitmentPriorityGroup.LATER_PENDING, PaymentCommitmentStackLogic.classifyPriorityGroup(laterPending, now))
        assertEquals(CommitmentPriorityGroup.PAID, PaymentCommitmentStackLogic.classifyPriorityGroup(paidItem, now))
        assertEquals(5, PaymentCommitmentStackLogic.countUpcomingOrOverdue(sorted, now))
    }

    @Test
    fun filterChangeAndPaidTransition_safelyResetsSelectionAndAvailability() {
        val now = fixedNowMillis()
        val c1 = sampleCommitment(id = 11L, title = "Tigo Hogar", dueDateMillis = now - 3 * dayMs)
        val c2 = sampleCommitment(id = 12L, title = "Claro Móvil", dueDateMillis = now + 2 * dayMs)

        // Usuario tenía seleccionado c1 (vencido); cambia filtro a "Próximos 7 días" (solo queda c2)
        val filteredUpcoming = listOf(c2)
        val reselected = PaymentCommitmentStackLogic.resolveSelectedCommitmentId(filteredUpcoming, currentSelectedId = 11L)
        assertEquals(12L, reselected)

        // Filtro o búsqueda sin resultados
        assertNull(PaymentCommitmentStackLogic.resolveSelectedCommitmentId(emptyList(), currentSelectedId = 12L))

        // Cambio de estado tras confirmar pago de c1
        val c1Paid = c1.copy(isPaid = true, status = PaymentStatus.PAID, paidDateMillis = now)
        assertFalse(PaymentCommitmentStackLogic.canPayCommitment(c1Paid))
        assertTrue(PaymentCommitmentStackLogic.canPayCommitment(c2))
        val reorderedAfterPay = PaymentCommitmentStackLogic.sortCommitmentsForStack(listOf(c1Paid, c2), now)
        assertEquals(listOf(12L, 11L), reorderedAfterPay.map { it.id })
    }

    @Test
    fun swipeThresholdLogic_snapsBackBeforeThreshold_andRevealsPastThresholdWithoutAutoAction() {
        val trayWidthPx = 240f
        // Umbral = 32% de 240 = -76.8px
        // Antes del umbral (-40px): debe regresar suavemente a cerrado (false)
        assertFalse(
            PaymentCommitmentStackLogic.computeSwipeEndState(
                isCurrentlyOpen = false,
                dragOffsetPx = -40f,
                trayWidthPx = trayWidthPx
            )
        )
        // Supera el umbral (-95px): debe revelar acciones rápidas (true)
        assertTrue(
            PaymentCommitmentStackLogic.computeSwipeEndState(
                isCurrentlyOpen = false,
                dragOffsetPx = -95f,
                trayWidthPx = trayWidthPx
            )
        )
        // Con acciones abiertas, arrastrar de regreso hacia la derecha (-100px > -(240-76.8) = -163.2px): cierra (false)
        assertFalse(
            PaymentCommitmentStackLogic.computeSwipeEndState(
                isCurrentlyOpen = true,
                dragOffsetPx = -100f,
                trayWidthPx = trayWidthPx
            )
        )
        // Con acciones abiertas y arrastre mínimo (-210px): permanece abierto (true)
        assertTrue(
            PaymentCommitmentStackLogic.computeSwipeEndState(
                isCurrentlyOpen = true,
                dragOffsetPx = -210f,
                trayWidthPx = trayWidthPx
            )
        )
    }

    @Test
    fun confirmPaymentFlow_preventsDuplicateTransactionsOnRepeatedTrigger() = runBlocking {
        val fakeTxDao = PaymentCommitmentIntegrationTest.FakeTestTransactionDao()
        val fakePaymentDao = PaymentCommitmentIntegrationTest.FakeTestPaymentCommitmentDao()
        val repository = BudgetRepository(
            transactionDao = fakeTxDao,
            budgetDao = PaymentCommitmentIntegrationTest.FakeTestBudgetDao(),
            savingsGoalDao = PaymentCommitmentIntegrationTest.FakeTestSavingsGoalDao(),
            recurringExpenseDao = PaymentCommitmentIntegrationTest.FakeTestRecurringExpenseDao(),
            smartCartDao = PaymentCommitmentIntegrationTest.FakeTestSmartCartDao(),
            paymentCommitmentDao = fakePaymentDao,
            beneficiaryDao = PaymentCommitmentIntegrationTest.FakeTestBeneficiaryDao(),
            database = null
        )

        val now = fixedNowMillis()
        val id = repository.createPaymentCommitment(
            PaymentCommitmentEntity(
                title = "Colegiatura",
                beneficiaryName = "Colegio Xela",
                amount = 750.0,
                dueDateMillis = now,
                frequency = PaymentFrequency.ONCE
            )
        )

        assertTrue(repository.markPaymentAsPaid(id))
        val txCountAfterFirst = fakeTxDao.transactions.size
        // Segundo intento duplicado debe ignorarse
        assertFalse(repository.markPaymentAsPaid(id))
        assertEquals(txCountAfterFirst, fakeTxDao.transactions.size)
        val saved = fakePaymentDao.getCommitmentById(id)!!
        assertTrue(saved.isPaid)
        assertFalse(PaymentCommitmentStackLogic.canPayCommitment(saved))
    }

    @Test
    fun paymentsScreen_composesCleanlyAcrossZeroOneAndMultipleMixedCommitmentsInRobolectric() {
        val now = fixedNowMillis()
        val mixedCommitments = listOf(
            sampleCommitment(id = 1L, title = "EEGSA Vencida", beneficiary = "EEGSA", dueDateMillis = now - 4 * dayMs),
            sampleCommitment(id = 2L, title = "Internet Próximo", beneficiary = "Claro", dueDateMillis = now + 2 * dayMs),
            sampleCommitment(id = 3L, title = "Agua Futura", beneficiary = "Empagua", dueDateMillis = now + 15 * dayMs)
        )

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()

        listOf(
            emptyList(),
            listOf(mixedCommitments.first()),
            mixedCommitments
        ).forEach { scenarioList ->
            val composeView = ComposeView(activity).apply {
                setContent {
                    PaymentsScreen(
                        uiState = BudgetUiState(
                            paymentCommitments = scenarioList,
                            totalPendingPayments = scenarioList.filter { !it.isPaid }.sumOf { it.amount },
                            overduePaymentsCount = scenarioList.count { it.dueDateMillis < now && !it.isPaid },
                            upcomingPaymentsCount = scenarioList.count { it.dueDateMillis >= now && !it.isPaid }
                        ),
                        onAddPayment = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
                        onMarkAsPaid = {},
                        onUpdatePayment = {},
                        onDeletePayment = {},
                        onAddBeneficiary = { _, _, _, _, _ -> },
                        onUpdateBeneficiary = {},
                        onDeleteBeneficiary = {}
                    )
                }
            }
            activity.setContentView(composeView)
            shadowOf(Looper.getMainLooper()).idle()
            assertNotNull(composeView)
            assertTrue(composeView.childCount > 0)
        }

        controller.pause().stop().destroy()
    }
}
