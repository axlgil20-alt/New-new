package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.PaymentFrequency
import com.example.data.model.PaymentStatus
import com.example.ui.viewmodel.BudgetViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityStartupRobolectricTest {

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("budget_app_database")
    }

    @Test(timeout = 15_000L)
    fun mainActivity_cleanLaunch_completesWithoutCrash() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS)
        val activity = controller.get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)
        controller.pause().stop().destroy()
    }

    @Test(timeout = 15_000L)
    fun upgradedV12Database_withHalfCentAmountsAndStage1Edits_doesNotCrashBudgetViewModel() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("budget_app_database")

        // Crear BD real v12 con datos reales que provocan discrepancia en CAST(ROUND(amount * 100.0))
        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("budget_app_database")
                .callback(object : SupportSQLiteOpenHelper.Callback(12) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createV12Schema(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        val now = System.currentTimeMillis()
        helper.writableDatabase.use { db ->
            // 1.005 causa diferencia entre SQLite ROUND(1.005*100.0)=100 y BigDecimal.HALF_UP=101
            db.execSQL(
                "INSERT INTO transactions VALUES (1, 'Diferencia redondeo SQLite', 1.005, 'EXPENSE', 'Otros Gastos', ?, 'Efectivo', 'Personal', '', 0, 0, NULL, 0, 1, 0, 'CONFIRMED', NULL, NULL, 0)",
                arrayOf(now)
            )
            db.execSQL(
                "INSERT INTO transactions VALUES (2, 'Ingreso nómina', 5000.0, 'INCOME', 'Salario / Sueldo', ?, 'Depósito', 'Personal', '', 0, 0, NULL, 0, 1, 0, 'CONFIRMED', NULL, NULL, 0)",
                arrayOf(now)
            )
            db.execSQL(
                "INSERT INTO payment_commitments VALUES (1, 'Pago Agua', 'Empagua', 'Mensual', 1.005, ?, 'Vivienda & Servicios', ?, 0, 1, ?, 0, NULL, 1, ?, '')",
                arrayOf<Any>(now, PaymentFrequency.ONCE, PaymentStatus.PENDING, now)
            )
        }
        helper.close()

        // Simular además un registro v13 editado en Etapa 1 donde amount cambió pero amountMinor quedó previo
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, "budget_app_database")
            .addMigrations(AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13)
            .allowMainThreadQueries()
            .build()
        migrated.openHelper.writableDatabase.execSQL(
            "INSERT INTO transactions (id, title, amount, type, category, dateMillis, paymentMethod, familyMember, notes, isAntExpense, isUnplanned, highlightColorHex, isVerified, isTentative, tentativeDecision, isSuspectedDuplicate, amountMinor, accountId, entryKind) VALUES (3, 'Editado previo Etapa 2', 250.0, 'EXPENSE', 'Otros Gastos', ?, 'Efectivo', 'Personal', '', 0, 0, 0, 1, 0, 'CONFIRMED', 0, 10000, NULL, 'STANDARD')",
            arrayOf(now)
        )
        migrated.close()

        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = BudgetViewModel(app)
        val emitted = withTimeout(5_000L) {
            vm.uiState.first { !it.isLoading }
        }
        assertTrue(emitted.totalDbTransactionsCount >= 3)
    }

    private fun createV12Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `type` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `dateMillis` INTEGER NOT NULL,
                `paymentMethod` TEXT NOT NULL,
                `familyMember` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `isAntExpense` INTEGER NOT NULL,
                `isUnplanned` INTEGER NOT NULL,
                `originalPlannedAmount` REAL,
                `highlightColorHex` INTEGER NOT NULL,
                `isVerified` INTEGER NOT NULL,
                `isTentative` INTEGER NOT NULL,
                `tentativeDecision` TEXT NOT NULL,
                `externalId` TEXT,
                `importHash` TEXT,
                `isSuspectedDuplicate` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE TABLE IF NOT EXISTS `budget_limits` (`category` TEXT NOT NULL, `monthlyLimit` REAL NOT NULL, `reason` TEXT NOT NULL, `tipForControl` TEXT NOT NULL, PRIMARY KEY(`category`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `savings_goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `currentAmount` REAL NOT NULL, `targetDateMillis` INTEGER, `notes` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `recurring_expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `category` TEXT NOT NULL, `frequency` TEXT NOT NULL, `familyMember` TEXT NOT NULL, `paymentMethod` TEXT NOT NULL, `notes` TEXT NOT NULL, `isActive` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `smart_cart_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `quantity` REAL NOT NULL, `unit` TEXT NOT NULL, `brand` TEXT NOT NULL, `estimatedUnitPrice` REAL NOT NULL, `isChecked` INTEGER NOT NULL, `storeName` TEXT NOT NULL, `priceSource` TEXT NOT NULL, `notes` TEXT NOT NULL, `productUrl` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `payment_commitments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `beneficiaryName` TEXT NOT NULL, `description` TEXT NOT NULL, `amount` REAL NOT NULL, `dueDateMillis` INTEGER NOT NULL, `category` TEXT NOT NULL, `frequency` TEXT NOT NULL, `isVariableAmount` INTEGER NOT NULL, `reminderDaysBefore` INTEGER NOT NULL, `status` TEXT NOT NULL, `isPaid` INTEGER NOT NULL, `paidDateMillis` INTEGER, `linkedTransactionId` INTEGER, `createdAtMillis` INTEGER NOT NULL, `notes` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `payment_beneficiaries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `defaultDescription` TEXT NOT NULL, `phoneOrContact` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `shopping_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `itemsJson` TEXT NOT NULL, `itemCount` INTEGER NOT NULL, `estimatedTotal` REAL NOT NULL, `createdAtMillis` INTEGER NOT NULL)")
    }
}
