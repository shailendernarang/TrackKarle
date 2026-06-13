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
    val bankName: String? = null,
    // FD
    val fdStartDate: String? = null,
    val fdMaturityDate: String? = null,
    val fdRate: Double? = null,
    val fdTenure: String? = null,
    // Mutual Fund
    val mfDate: String? = null,
    // PPF
    val ppfFy: String? = null,
    val ppfDate: String? = null,
    // NPS
    val npsTier: String? = null,
    val npsDate: String? = null,
    // Gold
    val goldType: String? = null,
    val goldDate: String? = null,
    // Health Insurance
    val hiPolicyName: String? = null,
    val hiRenewalDate: String? = null,
    // Stocks
    val stockName: String? = null,
    val stockDate: String? = null
)
