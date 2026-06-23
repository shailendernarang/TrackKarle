package com.example.wealthtracker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DebtEntity): Long

    @Delete
    suspend fun delete(entity: DebtEntity)

    @Update
    suspend fun update(entity: DebtEntity)

    @Query("DELETE FROM debts")
    suspend fun deleteAll()
}
