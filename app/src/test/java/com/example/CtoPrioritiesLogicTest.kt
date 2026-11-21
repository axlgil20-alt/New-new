package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.ChapinFabLayoutLogic
import com.example.ui.viewmodel.BudgetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CtoPrioritiesLogicTest {

    @Before
    fun setUp() {
        AppDatabase.closeAndResetInstanceForTesting()
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("budget_app_database")
        context.getSharedPreferences("quetzi_user_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        AppDatabase.closeAndResetInstanceForTesting()
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("budget_app_database")
        context.getSharedPreferences("quetzi_user_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun awaitCondition(timeoutMs: Long = 8_000L, predicate: () -> Boolean) {
        runBlocking {
            withTimeout(timeoutMs) {
                while (!predicate()) {
                    shadowOf(Looper.getMainLooper()).idle()
                    delay(10L)
                }
            }
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun chapinFabLayoutLogic_clampsAndRoundTripsBoundsSafely() {
        val (clampedX, clampedY) = ChapinFabLayoutLogic.clampFractions(-0.4f, 1.8f)
        assertEquals(0f, clampedX, 0.001f)
        assertEquals(1f, clampedY, 0.001f)

        val width = 1080f
        val height = 1920f
        val fab = 168f
        val margin = 42f
        val topSafe = 192f
        val bottomSafe = 36f

        val (minPxX, minPxY) = ChapinFabLayoutLogic.computeClampedOffsetPx(
            xFraction = -1f,
            yFraction = -1f,
            containerWidthPx = width,
            containerHeightPx = height,
            fabSizePx = fab,
            marginPx = margin,
            topSafePx = topSafe,
            bottomSafePx = bottomSafe
        )
        assertEquals(margin, minPxX, 0.1f)
        assertEquals(margin + topSafe, minPxY, 0.1f)

        val (maxPxX, maxPxY) = ChapinFabLayoutLogic.computeClampedOffsetPx(
            xFraction = 2f,
            yFraction = 2f,
            containerWidthPx = width,
            containerHeightPx = height,
            fabSizePx = fab,
            marginPx = margin,
            topSafePx = topSafe,
            bottomSafePx = bottomSafe
        )
        assertEquals(width - fab - margin, maxPxX, 0.1f)
        assertEquals(height - fab - margin - bottomSafe, maxPxY, 0.1f)

        val (midPxX, midPxY) = ChapinFabLayoutLogic.computeClampedOffsetPx(
            xFraction = 0.25f,
            yFraction = 0.60f,
            containerWidthPx = width,
            containerHeightPx = height,
            fabSizePx = fab,
            marginPx = margin,
            topSafePx = topSafe,
            bottomSafePx = bottomSafe
        )
        val (rtX, rtY) = ChapinFabLayoutLogic.offsetPxToFractions(
            offsetXPx = midPxX,
            offsetYPx = midPxY,
            containerWidthPx = width,
            containerHeightPx = height,
            fabSizePx = fab,
            marginPx = margin,
            topSafePx = topSafe,
            bottomSafePx = bottomSafe
        )
        assertEquals(0.25f, rtX, 0.002f)
        assertEquals(0.60f, rtY, 0.002f)

        assertFalse(ChapinFabLayoutLogic.shouldOpenMenuUpward(0.20f))
        assertTrue(ChapinFabLayoutLogic.shouldOpenMenuUpward(0.75f))
        assertFalse(ChapinFabLayoutLogic.shouldAlignMenuEnd(0.20f))
        assertTrue(ChapinFabLayoutLogic.shouldAlignMenuEnd(0.80f))
    }

    @Test
    fun primaryTabPagerLogic_mapsExactFiveBottomBarTabsAndIgnoresSecondary() {
        assertEquals(listOf(1, 2, 0, 5, 7), PrimaryTabPagerLogic.primaryTabsOrder)
        assertEquals(0, PrimaryTabPagerLogic.pageIndexForTab(1))
        assertEquals(1, PrimaryTabPagerLogic.pageIndexForTab(2))
        assertEquals(2, PrimaryTabPagerLogic.pageIndexForTab(0))
        assertEquals(3, PrimaryTabPagerLogic.pageIndexForTab(5))
        assertEquals(4, PrimaryTabPagerLogic.pageIndexForTab(7))

        assertNull(PrimaryTabPagerLogic.pageIndexForTab(3))
        assertNull(PrimaryTabPagerLogic.pageIndexForTab(4))
        assertNull(PrimaryTabPagerLogic.pageIndexForTab(6))
        assertNull(PrimaryTabPagerLogic.pageIndexForTab(8))
        assertFalse(PrimaryTabPagerLogic.isPrimaryTab(8))

        assertEquals(0, PrimaryTabPagerLogic.tabForPageIndex(99))
    }

    @Test(timeout = 15_000L)
    fun budgetViewModel_persistsFabPositionAndCompactMode_andRestoresDeletedTransaction() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = BudgetViewModel(app)
        val collector = CoroutineScope(Dispatchers.Main).launch { vm.uiState.collect {} }
        try {
            awaitCondition { !vm.uiState.value.isLoading }

            vm.updateChapinFabPosition(0.18f, 0.42f)
            vm.setCompactTransactionsMode(true)
            awaitCondition {
                kotlin.math.abs(vm.uiState.value.fabOffsetXFraction - 0.18f) < 0.01f &&
                    vm.uiState.value.isCompactTransactionsMode
            }

            // Reinstantiating ViewModel reads persisted local SharedPreferences
            val reloadedVm = BudgetViewModel(app)
            val reloadedCollector = CoroutineScope(Dispatchers.Main).launch { reloadedVm.uiState.collect {} }
            try {
                awaitCondition { !reloadedVm.uiState.value.isLoading }
                assertEquals(0.18f, reloadedVm.uiState.value.fabOffsetXFraction, 0.005f)
                assertEquals(0.42f, reloadedVm.uiState.value.fabOffsetYFraction, 0.005f)
                assertTrue(reloadedVm.uiState.value.isCompactTransactionsMode)

                reloadedVm.resetChapinFabPosition()
                awaitCondition { reloadedVm.uiState.value.fabOffsetXFraction == 1f }
                assertEquals(1f, reloadedVm.uiState.value.fabOffsetYFraction, 0.001f)
            } finally {
                reloadedCollector.cancel()
            }

            // Reversible delete & restore
            val sample = TransactionEntity(
                title = "Café Antigua Reversible",
                amount = 45.0,
                type = TransactionType.EXPENSE,
                category = "Antojitos & Gastos Hormiga",
                dateMillis = System.currentTimeMillis()
            )
            vm.addTransaction(sample)
            awaitCondition { vm.uiState.value.allTransactions.any { it.title == "Café Antigua Reversible" } }

            val inserted = vm.uiState.value.allTransactions.first { it.title == "Café Antigua Reversible" }
            vm.deleteTransaction(inserted)
            awaitCondition { vm.uiState.value.allTransactions.none { it.title == "Café Antigua Reversible" } }

            vm.restoreTransaction(inserted)
            awaitCondition { vm.uiState.value.allTransactions.any { it.title == "Café Antigua Reversible" && it.amount == 45.0 } }
        } finally {
            collector.cancel()
        }
    }
}
