package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.InstallmentScheduleItemEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialAccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    fun getActiveAccounts(): Flow<List<FinancialAccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, sortOrder ASC, id ASC")
    fun getAllAccounts(): Flow<List<FinancialAccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, sortOrder ASC, id ASC")
    suspend fun getAllAccountsList(): List<FinancialAccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: Long): FinancialAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: FinancialAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<FinancialAccountEntity>)

    @Update
    suspend fun updateAccount(account: FinancialAccountEntity)

    @Query("UPDATE accounts SET isArchived = 1, isDefault = 0 WHERE id = :accountId")
    suspend fun archiveAccount(accountId: Long)

    @Query("UPDATE accounts SET isArchived = 0 WHERE id = :accountId")
    suspend fun unarchiveAccount(accountId: Long)

    @Query("UPDATE accounts SET isDefault = 0 WHERE isDefault = 1 AND id != :exceptAccountId")
    suspend fun clearDefaultExcept(exceptAccountId: Long)

    @Delete
    suspend fun deleteAccount(account: FinancialAccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun clearAll()
}

@Dao
interface InstallmentPlanDao {
    @Query("SELECT * FROM installment_plans ORDER BY purchaseDateMillis DESC, id DESC")
    fun getAllPlans(): Flow<List<InstallmentPlanEntity>>

    @Query("SELECT * FROM installment_plans WHERE accountId = :accountId ORDER BY purchaseDateMillis DESC, id DESC")
    fun getPlansByAccount(accountId: Long): Flow<List<InstallmentPlanEntity>>

    @Query("SELECT * FROM installment_plans ORDER BY purchaseDateMillis DESC, id DESC")
    suspend fun getAllPlansList(): List<InstallmentPlanEntity>

    @Query("SELECT * FROM installment_plans WHERE id = :planId LIMIT 1")
    suspend fun getPlanById(planId: Long): InstallmentPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: InstallmentPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: InstallmentPlanEntity)

    @Delete
    suspend fun deletePlan(plan: InstallmentPlanEntity)

    @Query("DELETE FROM installment_plans")
    suspend fun clearAll()
}

@Dao
interface InstallmentScheduleDao {
    @Query("SELECT * FROM installment_schedule_items ORDER BY dueDateMillis ASC, installmentNumber ASC")
    fun getAllScheduleItems(): Flow<List<InstallmentScheduleItemEntity>>

    @Query("SELECT * FROM installment_schedule_items ORDER BY dueDateMillis ASC, installmentNumber ASC")
    suspend fun getAllScheduleItemsList(): List<InstallmentScheduleItemEntity>

    @Query("SELECT * FROM installment_schedule_items WHERE planId = :planId ORDER BY installmentNumber ASC")
    fun getItemsForPlan(planId: Long): Flow<List<InstallmentScheduleItemEntity>>

    @Query("SELECT * FROM installment_schedule_items WHERE planId = :planId ORDER BY installmentNumber ASC")
    suspend fun getItemsForPlanList(planId: Long): List<InstallmentScheduleItemEntity>

    @Query("SELECT * FROM installment_schedule_items WHERE planId = :planId AND installmentNumber = :installmentNumber LIMIT 1")
    suspend fun getItemByPlanAndNumber(planId: Long, installmentNumber: Int): InstallmentScheduleItemEntity?

    @Query("SELECT * FROM installment_schedule_items WHERE idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun getByIdempotencyKey(idempotencyKey: String): InstallmentScheduleItemEntity?

    @Query("SELECT * FROM installment_schedule_items WHERE accountId = :accountId AND status = :status ORDER BY dueDateMillis ASC")
    suspend fun getByAccountAndStatus(accountId: Long, status: String): List<InstallmentScheduleItemEntity>

    @Query("SELECT * FROM installment_schedule_items WHERE status = 'SCHEDULED' AND dueDateMillis <= :upToMillis ORDER BY dueDateMillis ASC, planId ASC, installmentNumber ASC")
    suspend fun getDueScheduledItems(upToMillis: Long): List<InstallmentScheduleItemEntity>

    @Query("SELECT * FROM installment_schedule_items WHERE accountId = :accountId AND status = 'SCHEDULED' AND dueDateMillis <= :upToMillis ORDER BY dueDateMillis ASC, planId ASC, installmentNumber ASC")
    suspend fun getDueScheduledItemsForAccount(accountId: Long, upToMillis: Long): List<InstallmentScheduleItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemsIdempotent(items: List<InstallmentScheduleItemEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemIdempotent(item: InstallmentScheduleItemEntity): Long

    @Update
    suspend fun updateItem(item: InstallmentScheduleItemEntity)

    @Query("DELETE FROM installment_schedule_items WHERE planId = :planId")
    suspend fun deleteByPlanId(planId: Long)

    @Query("DELETE FROM installment_schedule_items")
    suspend fun clearAll()
}

/**
 * Consultas especializadas sobre `transactions` para Mis Cuentas sin alterar la interfaz
 * heredada de [TransactionDao] ni romper los fakes existentes en pruebas unitarias.
 */
@Dao
interface AccountLedgerDao {
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY dateMillis DESC, id DESC")
    suspend fun getTransactionsForAccountList(accountId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :txId LIMIT 1")
    suspend fun getTransactionById(txId: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE externalId = :externalId LIMIT 1")
    suspend fun getTransactionByExternalId(externalId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transferGroupId = :transferGroupId ORDER BY id ASC")
    suspend fun getTransactionsByTransferGroup(transferGroupId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE installmentPlanId = :planId AND installmentNumber = :installmentNumber LIMIT 1")
    suspend fun getTransactionForInstallment(planId: Long, installmentNumber: Int): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE accountId IS NULL ORDER BY dateMillis DESC, id DESC")
    fun getUnassignedTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId IS NULL ORDER BY dateMillis DESC, id DESC")
    suspend fun getUnassignedTransactionsList(): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE transferGroupId = :transferGroupId")
    suspend fun deleteByTransferGroup(transferGroupId: String)
}
