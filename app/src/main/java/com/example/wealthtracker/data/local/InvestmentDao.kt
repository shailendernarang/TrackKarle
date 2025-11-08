package com.example.wealthtracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InvestmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InvestmentEntity): Long

    @Delete
    suspend fun delete(entity: InvestmentEntity)

    @Update
    suspend fun update(entity: InvestmentEntity)

    @Query("DELETE FROM investments WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM investments")
    suspend fun deleteAll()

    // One-shot query used for migration from unencrypted to encrypted DB
    @Query("SELECT * FROM investments")
    suspend fun getAllOnce(): List<InvestmentEntity>
}
