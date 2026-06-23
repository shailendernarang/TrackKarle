package com.example.wealthtracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val debtType: String,
    val principalAmount: Double,
    val outstandingBalance: Double,
    val interestRate: Double,
    val emiAmount: Double,
    val startDate: String = "",
    val tenureMonths: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
