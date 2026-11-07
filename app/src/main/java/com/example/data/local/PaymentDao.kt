package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BeneficiaryEntity
import com.example.data.model.PaymentCommitmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentCommitmentDao {
    @Query("SELECT * FROM payment_commitments ORDER BY isPaid ASC, dueDateMillis ASC, id ASC")
    fun getAllCommitments(): Flow<List<PaymentCommitmentEntity>>

    @Query("SELECT * FROM payment_commitments ORDER BY isPaid ASC, dueDateMillis ASC, id ASC")
    suspend fun getAllCommitmentsList(): List<PaymentCommitmentEntity>

    @Query("SELECT * FROM payment_commitments WHERE id = :id LIMIT 1")
    suspend fun getCommitmentById(id: Long): PaymentCommitmentEntity?

    @Query("SELECT * FROM payment_commitments WHERE linkedTransactionId = :txId LIMIT 1")
    suspend fun getCommitmentByTransactionId(txId: Long): PaymentCommitmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitment(commitment: PaymentCommitmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCommitments(commitments: List<PaymentCommitmentEntity>)

    @Update
    suspend fun updateCommitment(commitment: PaymentCommitmentEntity)

    @Delete
    suspend fun deleteCommitment(commitment: PaymentCommitmentEntity)

    @Query("DELETE FROM payment_commitments WHERE id = :id")
    suspend fun deleteCommitmentById(id: Long)

    @Query("DELETE FROM payment_commitments")
    suspend fun clearAll()
}

@Dao
interface BeneficiaryDao {
    @Query("SELECT * FROM payment_beneficiaries ORDER BY name ASC")
    fun getAllBeneficiaries(): Flow<List<BeneficiaryEntity>>

    @Query("SELECT * FROM payment_beneficiaries ORDER BY name ASC")
    suspend fun getAllBeneficiariesList(): List<BeneficiaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiary(beneficiary: BeneficiaryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBeneficiaries(beneficiaries: List<BeneficiaryEntity>)

    @Update
    suspend fun updateBeneficiary(beneficiary: BeneficiaryEntity)

    @Delete
    suspend fun deleteBeneficiary(beneficiary: BeneficiaryEntity)

    @Query("DELETE FROM payment_beneficiaries WHERE id = :id")
    suspend fun deleteBeneficiaryById(id: Long)

    @Query("DELETE FROM payment_beneficiaries")
    suspend fun clearAll()
}
