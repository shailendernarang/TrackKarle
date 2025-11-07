package com.example.wealthtracker.data.repository

import com.example.wealthtracker.data.local.InvestmentEntity
import kotlinx.coroutines.flow.Flow

// Canonical repository interface implemented by data.local.DefaultInvestmentRepository
interface InvestmentRepository {
    fun observeInvestments(): Flow<List<InvestmentEntity>>
    suspend fun addInvestment(type: String, amount: Double, investmentType: String, bankName: String?)
    suspend fun deleteInvestment(entity: InvestmentEntity)
    suspend fun deleteByIds(ids: List<Long>)
    suspend fun deleteAll()
    suspend fun updateInvestment(entity: InvestmentEntity)
    suspend fun exportCsv(items: List<InvestmentEntity>): String
    suspend fun exportJson(items: List<InvestmentEntity>): String
    suspend fun importCsv(csv: String)
    suspend fun importJson(json: String)
}
