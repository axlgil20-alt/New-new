package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BeneficiaryEntity
import com.example.data.model.BudgetLimitEntity
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
        ShoppingTemplateEntity::class
    ],
    version = 12,
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_app_database"
                )
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
