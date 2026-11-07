package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ShoppingTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingTemplateDao {
    @Query("SELECT * FROM shopping_templates ORDER BY id ASC")
    fun getAllTemplates(): Flow<List<ShoppingTemplateEntity>>

    @Query("SELECT * FROM shopping_templates ORDER BY id ASC")
    suspend fun getAllTemplatesList(): List<ShoppingTemplateEntity>

    @Query("SELECT * FROM shopping_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Long): ShoppingTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ShoppingTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<ShoppingTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: ShoppingTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: ShoppingTemplateEntity)

    @Query("DELETE FROM shopping_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long)

    @Query("DELETE FROM shopping_templates")
    suspend fun clearAll()
}
