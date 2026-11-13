package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BeneficiaryEntity
import com.example.data.model.BudgetLimitEntity
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.InstallmentScheduleItemEntity
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.model.RecurringExpenseEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.ShoppingTemplateEntity
import com.example.data.model.SmartCartItemEntity
import com.example.data.model.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        BudgetLimitEntity::class,
        SavingsGoalEntity::class,
        RecurringExpenseEntity::class,
        SmartCartItemEntity::class,
        PaymentCommitmentEntity::class,
        BeneficiaryEntity::class,
        ShoppingTemplateEntity::class,
        FinancialAccountEntity::class,
        InstallmentPlanEntity::class,
        InstallmentScheduleItemEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun smartCartDao(): SmartCartDao
    abstract fun paymentCommitmentDao(): PaymentCommitmentDao
    abstract fun beneficiaryDao(): BeneficiaryDao
    abstract fun shoppingTemplateDao(): ShoppingTemplateDao
    abstract fun financialAccountDao(): FinancialAccountDao
    abstract fun installmentPlanDao(): InstallmentPlanDao
    abstract fun installmentScheduleDao(): InstallmentScheduleDao
    abstract fun accountLedgerDao(): AccountLedgerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN externalId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN importHash TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isSuspectedDuplicate INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
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
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
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
        }

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Nuevas tablas del módulo Mis Cuentas (montos exactos INTEGER en centavos)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `accountType` TEXT NOT NULL,
                        `institutionName` TEXT NOT NULL,
                        `lastFourDigits` TEXT NOT NULL,
                        `currencyCode` TEXT NOT NULL,
                        `openingBalanceMinor` INTEGER NOT NULL,
                        `creditLimitMinor` INTEGER NOT NULL,
                        `cutoffDay` INTEGER,
                        `paymentDueDay` INTEGER,
                        `installmentReservePolicy` TEXT NOT NULL,
                        `autoCreatePaymentReminder` INTEGER NOT NULL,
                        `includeInTotalLiquidity` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `colorHex` INTEGER NOT NULL,
                        `iconKey` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_accounts_type` ON `accounts` (`accountType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_accounts_archived_sort` ON `accounts` (`isArchived`, `sortOrder`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `installment_plans` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `merchantName` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `principalTotalMinor` INTEGER NOT NULL,
                        `totalPlannedMinor` INTEGER NOT NULL,
                        `installmentsCount` INTEGER NOT NULL,
                        `annualInterestRateBps` INTEGER NOT NULL,
                        `installmentAmountMinor` INTEGER NOT NULL,
                        `creditReservePolicy` TEXT NOT NULL,
                        `originTransactionId` INTEGER,
                        `purchaseDateMillis` INTEGER NOT NULL,
                        `firstChargeDateMillis` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `familyMember` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_installment_plans_accountId` ON `installment_plans` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_installment_plans_status` ON `installment_plans` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_installment_plans_originTx` ON `installment_plans` (`originTransactionId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `installment_schedule_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `planId` INTEGER NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `installmentNumber` INTEGER NOT NULL,
                        `idempotencyKey` TEXT NOT NULL,
                        `dueDateMillis` INTEGER NOT NULL,
                        `statementCycleKey` TEXT NOT NULL,
                        `principalMinor` INTEGER NOT NULL,
                        `interestMinor` INTEGER NOT NULL,
                        `totalChargeMinor` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `linkedTransactionId` INTEGER,
                        `postedDateMillis` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_schedule_plan_number_unique` ON `installment_schedule_items` (`planId`, `installmentNumber`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_schedule_idempotency_unique` ON `installment_schedule_items` (`idempotencyKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_schedule_account_status` ON `installment_schedule_items` (`accountId`, `status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_schedule_linkedTx` ON `installment_schedule_items` (`linkedTransactionId`)")

                // 2. Extensión aditiva y no destructiva de `transactions`
                db.execSQL("ALTER TABLE transactions ADD COLUMN amountMinor INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE transactions SET amountMinor = CAST(ROUND(amount * 100.0) AS INTEGER)")
                db.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN entryKind TEXT NOT NULL DEFAULT 'STANDARD'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN transferGroupId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN counterpartAccountId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN installmentPlanId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN installmentNumber INTEGER DEFAULT NULL")

                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_transactions_accountId` ON `transactions` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_transactions_entryKind` ON `transactions` (`entryKind`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_transactions_transferGroupId` ON `transactions` (`transferGroupId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_transactions_installmentPlanId` ON `transactions` (`installmentPlanId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_transactions_installment_unique` ON `transactions` (`installmentPlanId`, `installmentNumber`)")

                // 3. Extensión aditiva y no destructiva de `payment_commitments`
                db.execSQL("ALTER TABLE payment_commitments ADD COLUMN amountMinor INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE payment_commitments SET amountMinor = CAST(ROUND(amount * 100.0) AS INTEGER)")
                db.execSQL("ALTER TABLE payment_commitments ADD COLUMN creditCardAccountId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE payment_commitments ADD COLUMN paymentSourceAccountId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE payment_commitments ADD COLUMN commitmentKind TEXT NOT NULL DEFAULT 'BILL'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_commitments_creditCardAccountId` ON `payment_commitments` (`creditCardAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_commitments_paymentSourceAccountId` ON `payment_commitments` (`paymentSourceAccountId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_app_database"
                )
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
