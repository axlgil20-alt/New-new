package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SmartCartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartCartDao {
    @Query("SELECT * FROM smart_cart_items ORDER BY id ASC")
    fun getAllItems(): Flow<List<SmartCartItemEntity>>

    @Query("SELECT * FROM smart_cart_items ORDER BY id ASC")
    suspend fun getAllItemsList(): List<SmartCartItemEntity>

    @Query("UPDATE smart_cart_items SET isChecked = CASE WHEN isChecked = 1 THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun toggleItemChecked(id: Long)

    @Query("UPDATE smart_cart_items SET priceSource = :status WHERE id = :id")
    suspend fun updatePantryStatus(id: Long, status: String)

    @Query("UPDATE smart_cart_items SET priceSource = 'PANTRY_VERDE' WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND priceSource LIKE 'PANTRY_%'")
    suspend fun updatePantryStatusToGreenByName(name: String)

    @Query("UPDATE smart_cart_items SET priceSource = 'PANTRY_VERDE', isChecked = 0 WHERE id = :id")
    suspend fun markItemPurchasedAsGreen(id: Long)

    @Query("UPDATE smart_cart_items SET priceSource = 'PANTRY_VERDE', isChecked = 0 WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name))")
    suspend fun markItemPurchasedAsGreenByName(name: String)

    @Query("UPDATE smart_cart_items SET isChecked = 1 WHERE priceSource LIKE '%AMARILLO%' OR priceSource LIKE '%ROJO%' OR priceSource = 'REPOSICION' OR priceSource = 'OCR'")
    suspend fun incorporateNeedToBuyIntoCart()

    @Query("UPDATE smart_cart_items SET isChecked = 0 WHERE isChecked = 1")
    suspend fun uncheckAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: SmartCartItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SmartCartItemEntity>)

    @Update
    suspend fun updateItem(item: SmartCartItemEntity)

    @Delete
    suspend fun deleteItem(item: SmartCartItemEntity)

    @Query("DELETE FROM smart_cart_items WHERE isChecked = 1 AND priceSource NOT LIKE 'PANTRY_%'")
    suspend fun deleteCheckedItems()

    @Query("DELETE FROM smart_cart_items WHERE priceSource NOT LIKE 'PANTRY_%'")
    suspend fun clearCartItems()

    @Query("DELETE FROM smart_cart_items WHERE priceSource LIKE 'PANTRY_%'")
    suspend fun clearPantryItems()

    @Query("DELETE FROM smart_cart_items")
    suspend fun clearAll()
}
