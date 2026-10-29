package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BudgetLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget_limits ORDER BY category ASC")
    fun getAllBudgets(): Flow<List<BudgetLimitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: BudgetLimitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetLimitEntity>)

    @Query("DELETE FROM budget_limits WHERE category = :category")
    suspend fun deleteBudget(category: String)

    @Query("DELETE FROM budget_limits")
    suspend fun clearAll()
}
