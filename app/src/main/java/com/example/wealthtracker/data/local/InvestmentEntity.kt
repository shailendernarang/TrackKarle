package com.example.wealthtracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val investmentType: String = "Others",
    val bankName: String? = null
)
