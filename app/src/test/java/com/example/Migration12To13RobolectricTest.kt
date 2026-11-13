package com.example

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.AccountType
import com.example.data.model.CommitmentKind
import com.example.data.model.CreditReservePolicy
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.InstallmentChargeStatus
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.InstallmentScheduleItemEntity
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.model.PaymentFrequency
import com.example.data.model.PaymentStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionEntryKind
import com.example.data.model.TransactionType
import com.example.data.model.affectsBudgetTotals
import com.example.data.model.effectiveAmountMinor
import com.example.data.model.isGastoReal
import com.example.data.model.isGastoSupuesto
import com.example.data.model.isIngresoReal
import com.example.data.service.InstallmentScheduleEngine
import com.example.ui.util.BudgetLimitCalculator
import com.example.ui.util.QuincenaFilterHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.math.BigDecimal
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration12To13RobolectricTest {

    private lateinit var context: Context
    private lateinit var dbName: String
    private var migratedDb: AppDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbName = "quetzi_migration_12_13_${System.nanoTime()}.db"
    }

    @After
    fun tearDown() {
        migratedDb?.close()
        context.deleteDatabase(dbName)
        File(context.getDatabasePath(dbName).absolutePath).delete()
    }

    private fun dateMillis(year: Int, monthIndex: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }

    private fun createVersion12Schema(db: SupportSQLiteDatabase) {
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
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budget_limits` (
                `category` TEXT NOT NULL,
                `monthlyLimit` REAL NOT NULL,
                `reason` TEXT NOT NULL,
                `tipForControl` TEXT NOT NULL,
                PRIMARY KEY(`category`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `savings_goals` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `targetAmount` REAL NOT NULL,
                `currentAmount` REAL NOT NULL,
                `targetDateMillis` INTEGER,
                `notes` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recurring_expenses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `category` TEXT NOT NULL,
                `frequency` TEXT NOT NULL,
                `familyMember` TEXT NOT NULL,
                `paymentMethod` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `smart_cart_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `quantity` REAL NOT NULL,
                `unit` TEXT NOT NULL,
                `brand` TEXT NOT NULL,
                `estimatedUnitPrice` REAL NOT NULL,
                `isChecked` INTEGER NOT NULL,
                `storeName` TEXT NOT NULL,
                `priceSource` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `productUrl` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `payment_commitments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `beneficiaryName` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `dueDateMillis` INTEGER NOT NULL,
                `category` TEXT NOT NULL,
                `frequency` TEXT NOT NULL,
                `isVariableAmount` INTEGER NOT NULL,
                `reminderDaysBefore` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `isPaid` INTEGER NOT NULL,
                `paidDateMillis` INTEGER,
                `linkedTransactionId` INTEGER,
                `createdAtMillis` INTEGER NOT NULL,
                `notes` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `payment_beneficiaries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `defaultDescription` TEXT NOT NULL,
                `phoneOrContact` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shopping_templates` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `itemsJson` TEXT NOT NULL,
                `itemCount` INTEGER NOT NULL,
                `estimatedTotal` REAL NOT NULL,
                `createdAtMillis` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    @Test
    fun migrate12To13_preservesAllLegacyData_populatesMinorUnits_andKeepsTotalsIdentical() = runBlocking {
        val dOct31 = dateMillis(2026, Calendar.OCTOBER, 31)
        val dNov05 = dateMillis(2026, Calendar.NOVEMBER, 5)
        val dNov10 = dateMillis(2026, Calendar.NOVEMBER, 10)
        val dNov15 = dateMillis(2026, Calendar.NOVEMBER, 15)
        val dNov18 = dateMillis(2026, Calendar.NOVEMBER, 18)
        val dNov22 = dateMillis(2026, Calendar.NOVEMBER, 22)

        // 1. Construir base de datos real SQLite en versión 12 y poblarla con datos representativos
        val factory = FrameworkSQLiteOpenHelperFactory()
        val v12Helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(12) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersion12Schema(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        v12Helper.writableDatabase.use { db ->
            // Transacciones heredadas v12 (ingresos, gastos reales, hormiga, importado, pendiente, descartado)
            db.execSQL(
                "INSERT INTO transactions VALUES (1, 'Salario Fin de Octubre', 6000.00, 'INCOME', 'Salario / Sueldo', ?, 'Depósito', 'Personal', 'Nómina Q1', 0, 0, NULL, 0, 1, 0, 'CONFIRMED', NULL, NULL, 0)",
                arrayOf(dOct31)
            )
            db.execSQL(
                "INSERT INTO transactions VALUES (2, 'Súper La Torre', 450.75, 'EXPENSE', 'Alimentación & Súper', ?, 'Tarjeta', 'Hogar', 'Despensa quincenal', 0, 0, 420.00, 123456, 1, 0, 'CONFIRMED', 'EXT-100', 'HASH-100', 0)",
                arrayOf(dNov05)
            )
            db.execSQL(
                "INSERT INTO transactions VALUES (3, 'Café y Pan', 18.50, 'EXPENSE', 'Antojitos & Gastos Hormiga', ?, 'Efectivo', 'Personal', 'Merienda', 1, 1, NULL, 0, 0, 0, '', NULL, NULL, 0)",
                arrayOf(dNov10)
            )
            db.execSQL(
                "INSERT INTO transactions VALUES (4, 'Salario Día 15', 5500.00, 'INCOME', 'Salario / Sueldo', ?, 'Depósito', 'Personal', 'Nómina Q2', 0, 0, NULL, 0, 1, 0, 'CONFIRMED', NULL, NULL, 0)",
                arrayOf(dNov15)
            )
            db.execSQL(
                "INSERT INTO transactions VALUES (5, 'Recibo EEGSA', 350.25, 'EXPENSE', 'Vivienda & Servicios', ?, 'Efectivo', 'Hogar', '[PENDIENTE] Contador 9988', 0, 0, NULL, 0, 0, 1, 'PENDING', NULL, NULL, 0)",
                arrayOf(dNov18)
            )
            db.execSQL(
                "INSERT INTO transactions VALUES (6, 'Zapatos Impulso', 299.99, 'EXPENSE', 'Ropa & Calzado', ?, 'Efectivo', 'Personal', 'No comprado', 0, 0, NULL, 0, 0, 1, 'DISCARDED', NULL, NULL, 0)",
                arrayOf(dNov22)
            )

            // Compromisos de pago y beneficiarios (relación histórica con tx 2 y tx 5)
            db.execSQL(
                "INSERT INTO payment_commitments VALUES (10, 'Luz Eléctrica', 'EEGSA', 'Contador 9988', 350.25, ?, 'Vivienda & Servicios', ?, 1, 2, ?, 0, NULL, 5, ?, 'Recibo mensual')",
                arrayOf<Any>(dNov18, PaymentFrequency.MONTHLY, PaymentStatus.PENDING, dNov05)
            )
            db.execSQL(
                "INSERT INTO payment_commitments VALUES (11, 'Compra Súper', 'La Torre', 'Despensa', 450.75, ?, 'Alimentación & Súper', ?, 0, 1, ?, 1, ?, 2, ?, 'Pagado en caja')",
                arrayOf<Any>(dNov05, PaymentFrequency.ONCE, PaymentStatus.PAID, dNov05, dOct31)
            )
            db.execSQL(
                "INSERT INTO payment_beneficiaries VALUES (1, 'EEGSA', 'Vivienda & Servicios', 'Energía eléctrica', '2277-7000', 'Servicio residencial', ?)",
                arrayOf(dOct31)
            )

            // Límites presupuestarios
            db.execSQL("INSERT INTO budget_limits VALUES ('Alimentación & Súper', 2500.00, 'Control familiar', 'Usar Smart Cart')")
            db.execSQL("INSERT INTO budget_limits VALUES ('Vivienda & Servicios', 1800.00, 'Servicios básicos', 'Revisar consumo')")

            // Smart Cart y Plantillas
            db.execSQL(
                "INSERT INTO smart_cart_items VALUES (1, 'Frijoles Ducal', 2.0, 'Bolsa', 'Ducal', 14.50, 1, 'La Torre', 'PANTRY_AMARILLO+CART', 'En lista y despensa', '', ?)",
                arrayOf(dNov05)
            )
            db.execSQL(
                "INSERT INTO smart_cart_items VALUES (2, 'Leche Dos Pinos', 3.0, 'Litro', 'Dos Pinos', 16.25, 0, 'Walmart', 'MANUAL', 'Compra semanal', '', ?)",
                arrayOf(dNov10)
            )
            db.execSQL(
                "INSERT INTO shopping_templates VALUES (1, 'Canasta Básica Quincenal', 'Esenciales', '[{\"name\":\"Frijoles Ducal\"}]', 1, 29.00, ?)",
                arrayOf(dOct31)
            )

            // Metas y gastos recurrentes
            db.execSQL(
                "INSERT INTO savings_goals VALUES (1, 'Fondo Emergencia', 10000.00, 2500.00, ?, 'Meta familiar')",
                arrayOf(dNov22)
            )
            db.execSQL(
                "INSERT INTO recurring_expenses VALUES (1, 'Internet Claro', 299.00, 'Telefonía & Internet', 'MONTHLY', 'Hogar', 'Efectivo', 'Fibra', 1)"
            )
        }
        v12Helper.close()

        // Línea base pre-migración equivalente a v12 para HOY, Registros y Quincenas
        val legacySnapshotTxs = listOf(
            TransactionEntity(1L, "Salario Fin de Octubre", 6000.00, TransactionType.INCOME, "Salario / Sueldo", dOct31, "Depósito", "Personal", "Nómina Q1", false, false, null, 0L, true, false, "CONFIRMED", null, null, false),
            TransactionEntity(2L, "Súper La Torre", 450.75, TransactionType.EXPENSE, "Alimentación & Súper", dNov05, "Tarjeta", "Hogar", "Despensa quincenal", false, false, 420.00, 123456L, true, false, "CONFIRMED", "EXT-100", "HASH-100", false),
            TransactionEntity(3L, "Café y Pan", 18.50, TransactionType.EXPENSE, "Antojitos & Gastos Hormiga", dNov10, "Efectivo", "Personal", "Merienda", true, true, null, 0L, false, false, "", null, null, false),
            TransactionEntity(4L, "Salario Día 15", 5500.00, TransactionType.INCOME, "Salario / Sueldo", dNov15, "Depósito", "Personal", "Nómina Q2", false, false, null, 0L, true, false, "CONFIRMED", null, null, false),
            TransactionEntity(5L, "Recibo EEGSA", 350.25, TransactionType.EXPENSE, "Vivienda & Servicios", dNov18, "Efectivo", "Hogar", "[PENDIENTE] Contador 9988", false, false, null, 0L, false, true, "PENDING", null, null, false),
            TransactionEntity(6L, "Zapatos Impulso", 299.99, TransactionType.EXPENSE, "Ropa & Calzado", dNov22, "Efectivo", "Personal", "No comprado", false, false, null, 0L, false, true, "DISCARDED", null, null, false)
        )
        val expectedQuincena = QuincenaFilterHelper.filterByQuincena(legacySnapshotTxs, 2026, Calendar.NOVEMBER)
        val expectedRolloverQ2 = QuincenaFilterHelper.calculateQuincenaRollover(legacySnapshotTxs, 2026, Calendar.NOVEMBER, 2)
        val expectedLimitSpendFood = BudgetLimitCalculator.monthlySpend(legacySnapshotTxs, "Alimentación & Súper", 2026, Calendar.NOVEMBER)

        // 2. Ejecutar migración real Room v12 -> v13 (incluye validación de esquema de Room)
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13
            )
            .allowMainThreadQueries()
            .build()
        migratedDb = db

        // 3. Verificar integridad fila por fila en todas las tablas heredadas
        val migratedTxs = db.transactionDao().getAllTransactionsList().sortedBy { it.id }
        assertEquals(6, migratedTxs.size)

        val expectedMinorAmounts = listOf(600000L, 45075L, 1850L, 550000L, 35025L, 29999L)
        for (i in migratedTxs.indices) {
            val actual = migratedTxs[i]
            val legacy = legacySnapshotTxs[i]
            assertEquals(legacy.id, actual.id)
            assertEquals(legacy.title, actual.title)
            assertEquals(legacy.amount, actual.amount, 0.0001)
            assertEquals(expectedMinorAmounts[i], actual.amountMinor)
            assertEquals(expectedMinorAmounts[i], actual.effectiveAmountMinor)
            assertEquals(legacy.type, actual.type)
            assertEquals(legacy.category, actual.category)
            assertEquals(legacy.dateMillis, actual.dateMillis)
            assertEquals(legacy.paymentMethod, actual.paymentMethod)
            assertEquals(legacy.familyMember, actual.familyMember)
            assertEquals(legacy.notes, actual.notes)
            assertEquals(legacy.isAntExpense, actual.isAntExpense)
            assertEquals(legacy.isUnplanned, actual.isUnplanned)
            assertEquals(legacy.originalPlannedAmount, actual.originalPlannedAmount)
            assertEquals(legacy.highlightColorHex, actual.highlightColorHex)
            assertEquals(legacy.isVerified, actual.isVerified)
            assertEquals(legacy.isTentative, actual.isTentative)
            assertEquals(legacy.tentativeDecision, actual.tentativeDecision)
            assertEquals(legacy.externalId, actual.externalId)
            assertEquals(legacy.importHash, actual.importHash)
            assertEquals(legacy.isSuspectedDuplicate, actual.isSuspectedDuplicate)

            // Candado de compatibilidad v13
            assertNull("Movimiento heredado debe conservar accountId = null", actual.accountId)
            assertEquals("Movimiento heredado debe tener entryKind = STANDARD", TransactionEntryKind.STANDARD, actual.entryKind)
            assertNull(actual.transferGroupId)
            assertNull(actual.counterpartAccountId)
            assertNull(actual.installmentPlanId)
            assertNull(actual.installmentNumber)
            assertTrue(actual.affectsBudgetTotals)
        }

        // Compromisos y relación histórica intacta
        val commitments = db.paymentCommitmentDao().getAllCommitmentsList().sortedBy { it.id }
        assertEquals(2, commitments.size)
        assertEquals(10L, commitments[0].id)
        assertEquals(35025L, commitments[0].amountMinor)
        assertEquals(5L, commitments[0].linkedTransactionId)
        assertEquals(PaymentStatus.PENDING, commitments[0].status)
        assertFalse(commitments[0].isPaid)
        assertNull(commitments[0].creditCardAccountId)
        assertNull(commitments[0].paymentSourceAccountId)
        assertEquals("BILL", commitments[0].commitmentKind)

        assertEquals(11L, commitments[1].id)
        assertEquals(45075L, commitments[1].amountMinor)
        assertEquals(2L, commitments[1].linkedTransactionId)
        assertEquals(PaymentStatus.PAID, commitments[1].status)
        assertTrue(commitments[1].isPaid)
        assertNull(commitments[1].creditCardAccountId)
        assertNull(commitments[1].paymentSourceAccountId)

        // Beneficiarios, límites, Smart Cart, plantillas, metas y recurrentes
        val beneficiaries = db.beneficiaryDao().getAllBeneficiariesList()
        assertEquals(1, beneficiaries.size)
        assertEquals("EEGSA", beneficiaries[0].name)

        val limits = db.budgetDao().getAllBudgets().first().sortedBy { it.category }
        assertEquals(2, limits.size)
        assertEquals(2500.00, limits[0].monthlyLimit, 0.001)
        assertEquals(1800.00, limits[1].monthlyLimit, 0.001)

        val cartItems = db.smartCartDao().getAllItemsList().sortedBy { it.id }
        assertEquals(2, cartItems.size)
        assertEquals("Frijoles Ducal", cartItems[0].name)
        assertEquals("PANTRY_AMARILLO+CART", cartItems[0].priceSource)
        assertTrue(cartItems[0].isChecked)
        assertEquals("Leche Dos Pinos", cartItems[1].name)

        val templates = db.shoppingTemplateDao().getAllTemplatesList()
        assertEquals(1, templates.size)
        assertEquals("Canasta Básica Quincenal", templates[0].name)

        val goals = db.savingsGoalDao().getAllGoals().first()
        assertEquals(1, goals.size)
        assertEquals(2500.00, goals[0].currentAmount, 0.001)

        val recurring = db.recurringExpenseDao().getAll().first()
        assertEquals(1, recurring.size)
        assertEquals(299.00, recurring[0].amount, 0.001)

        // 4. Verificar que HOY, Registros, Quincenas y Límites conservan resultados exactos post-migración
        assertEquals(legacySnapshotTxs.count { it.isIngresoReal }, migratedTxs.count { it.isIngresoReal })
        assertEquals(legacySnapshotTxs.count { it.isGastoReal }, migratedTxs.count { it.isGastoReal })
        assertEquals(legacySnapshotTxs.count { it.isGastoSupuesto }, migratedTxs.count { it.isGastoSupuesto })
        assertEquals(1, migratedTxs.count { it.isGastoSupuesto && it.tentativeDecision != "DISCARDED" })

        val hoyConfirmedIncome = migratedTxs.filter { it.isIngresoReal }.sumOf { it.effectiveAmountMinor }
        val hoyConfirmedExpense = migratedTxs.filter { it.isGastoReal }.sumOf { it.effectiveAmountMinor }
        assertEquals(1150000L, hoyConfirmedIncome)
        assertEquals(46925L, hoyConfirmedExpense) // 450.75 + 18.50

        val actualQuincena = QuincenaFilterHelper.filterByQuincena(migratedTxs, 2026, Calendar.NOVEMBER)
        assertEquals(expectedQuincena.q1Income, actualQuincena.q1Income, 0.001)
        assertEquals(expectedQuincena.q1Expense, actualQuincena.q1Expense, 0.001)
        assertEquals(expectedQuincena.q2Income, actualQuincena.q2Income, 0.001)
        assertEquals(expectedQuincena.q2Expense, actualQuincena.q2Expense, 0.001)
        assertEquals(expectedQuincena.q2PendingExpense, actualQuincena.q2PendingExpense, 0.001)
        assertEquals(expectedQuincena.totalNet, actualQuincena.totalNet, 0.001)

        val actualRolloverQ2 = QuincenaFilterHelper.calculateQuincenaRollover(migratedTxs, 2026, Calendar.NOVEMBER, 2)
        assertEquals(expectedRolloverQ2, actualRolloverQ2, 0.001)

        val actualLimitSpendFood = BudgetLimitCalculator.monthlySpend(migratedTxs, "Alimentación & Súper", 2026, Calendar.NOVEMBER)
        assertEquals(0, expectedLimitSpendFood.compareTo(actualLimitSpendFood))
        assertEquals(0, BigDecimal("450.75").compareTo(actualLimitSpendFood))

        // 5. Verificar que las tablas nuevas v13 operan y que el índice único de cuotas impide duplicados en SQLite
        val cardId = db.financialAccountDao().insertAccount(
            FinancialAccountEntity(
                name = "Visa BAC",
                accountType = AccountType.CREDIT_CARD,
                creditLimitMinor = 1000000L,
                cutoffDay = 15,
                paymentDueDay = 5,
                installmentReservePolicy = CreditReservePolicy.RESERVE_FULL_PENDING_PRINCIPAL
            )
        )
        val planId = db.installmentPlanDao().insertPlan(
            InstallmentPlanEntity(
                accountId = cardId,
                title = "Laptop",
                principalTotalMinor = 300000L,
                totalPlannedMinor = 300000L,
                installmentsCount = 3,
                installmentAmountMinor = 100000L,
                purchaseDateMillis = dNov05,
                firstChargeDateMillis = dNov15
            )
        )
        val schedule = InstallmentScheduleEngine.generateSchedule(
            planId = planId,
            accountId = cardId,
            principalTotalMinor = 300000L,
            installmentsCount = 3,
            annualInterestRateBps = 0,
            firstChargeDateMillis = dNov15,
            cutoffDay = 15
        )
        val firstInsert = db.installmentScheduleDao().insertItemsIdempotent(schedule.items)
        val secondInsert = db.installmentScheduleDao().insertItemsIdempotent(schedule.items)
        assertEquals(3, firstInsert.count { it > 0L })
        assertTrue("Reintento idempotente no debe insertar filas nuevas", secondInsert.all { it == -1L })

        val cuota1Tx = InstallmentScheduleEngine.buildInstallmentChargeTransaction(
            plan = db.installmentPlanDao().getPlanById(planId)!!,
            item = schedule.items.first().copy(status = InstallmentChargeStatus.POSTED),
            accountName = "Visa BAC"
        )
        val insertedCuotaTxId = db.transactionDao().insertTransaction(cuota1Tx)
        assertTrue(insertedCuotaTxId > 0L)

        // Intentar insertar manualmente en SQLite una segunda transacción para (planId, installmentNumber=1) debe fallar por UNIQUE
        var duplicateBlockedBySqlite = false
        try {
            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO transactions (title, amount, type, category, dateMillis, paymentMethod, familyMember, notes, isAntExpense, isUnplanned, highlightColorHex, isVerified, isTentative, tentativeDecision, isSuspectedDuplicate, amountMinor, accountId, entryKind, installmentPlanId, installmentNumber) VALUES ('Dup', 1000.0, 'EXPENSE', 'Otros Gastos', ?, 'Visa', 'Todos', '', 0, 0, 0, 1, 0, 'CONFIRMED', 0, 100000, ?, 'INSTALLMENT_CHARGE', ?, 1)",
                arrayOf(dNov15, cardId, planId)
            )
        } catch (_: SQLiteConstraintException) {
            duplicateBlockedBySqlite = true
        }
        assertTrue("El índice UNIQUE(installmentPlanId, installmentNumber) debe impedir cargos duplicados", duplicateBlockedBySqlite)

        // 6. Verificar almacenamiento y recuperación independiente de creditCardAccountId y paymentSourceAccountId
        val checkingId = db.financialAccountDao().insertAccount(
            FinancialAccountEntity(
                name = "Monetaria BI",
                accountType = AccountType.BANK_CHECKING,
                openingBalanceMinor = 800_000L
            )
        )
        val cCardOnlyId = db.paymentCommitmentDao().insertCommitment(
            PaymentCommitmentEntity(
                title = "Corte Visa BAC (sin cuenta fuente aún)",
                beneficiaryName = "BAC Credomatic",
                amount = 1250.00,
                dueDateMillis = dNov22,
                creditCardAccountId = cardId,
                paymentSourceAccountId = null,
                commitmentKind = CommitmentKind.CREDIT_CARD_STATEMENT
            )
        )
        val cSourceOnlyId = db.paymentCommitmentDao().insertCommitment(
            PaymentCommitmentEntity(
                title = "Colegiatura domiciliada",
                beneficiaryName = "Colegio",
                amount = 950.00,
                dueDateMillis = dNov22,
                creditCardAccountId = null,
                paymentSourceAccountId = checkingId,
                commitmentKind = CommitmentKind.BILL
            )
        )
        val cBothId = db.paymentCommitmentDao().insertCommitment(
            PaymentCommitmentEntity(
                title = "Pago mensual Visa BAC desde Monetaria BI",
                beneficiaryName = "BAC Credomatic",
                amount = 1250.00,
                dueDateMillis = dNov22,
                creditCardAccountId = cardId,
                paymentSourceAccountId = checkingId,
                commitmentKind = CommitmentKind.CREDIT_CARD_STATEMENT
            )
        )

        val retrievedCardOnly = db.paymentCommitmentDao().getCommitmentById(cCardOnlyId)!!
        assertEquals(cardId, retrievedCardOnly.creditCardAccountId)
        assertNull(retrievedCardOnly.paymentSourceAccountId)
        assertEquals(125_000L, retrievedCardOnly.amountMinor)
        assertEquals(CommitmentKind.CREDIT_CARD_STATEMENT, retrievedCardOnly.commitmentKind)

        val retrievedSourceOnly = db.paymentCommitmentDao().getCommitmentById(cSourceOnlyId)!!
        assertNull(retrievedSourceOnly.creditCardAccountId)
        assertEquals(checkingId, retrievedSourceOnly.paymentSourceAccountId)
        assertEquals(95_000L, retrievedSourceOnly.amountMinor)

        val retrievedBoth = db.paymentCommitmentDao().getCommitmentById(cBothId)!!
        assertEquals(cardId, retrievedBoth.creditCardAccountId)
        assertEquals(checkingId, retrievedBoth.paymentSourceAccountId)
        assertTrue(retrievedBoth.creditCardAccountId != retrievedBoth.paymentSourceAccountId)
    }
}
