package com.example.wealthtracker.data.repository

import com.example.wealthtracker.data.local.InvestmentDao
import com.example.wealthtracker.data.local.InvestmentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DefaultInvestmentRepository @Inject constructor(
    private val dao: InvestmentDao
) : InvestmentRepository {
    override fun observeInvestments(): Flow<List<InvestmentEntity>> = dao.observeAll()

    override suspend fun addInvestment(type: String, amount: Double, investmentType: String, bankName: String?) {
        dao.insert(
            InvestmentEntity(
                type = type,
                amount = amount,
                investmentType = investmentType,
                bankName = bankName
            )
        )
    }

    override suspend fun addInvestmentFull(entity: InvestmentEntity) {
        dao.insert(entity)
    }

    override suspend fun deleteInvestment(entity: InvestmentEntity) {
        dao.delete(entity)
    }

    override suspend fun deleteByIds(ids: List<Long>) {
        if (ids.isNotEmpty()) dao.deleteByIds(ids)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    override suspend fun updateInvestment(entity: InvestmentEntity) {
        dao.update(entity)
    }

    override suspend fun exportCsv(items: List<InvestmentEntity>): String {
        val header = "id,type,amount,createdAt,investmentType,bankName"
        val rows = items.joinToString("\n") { e ->
            listOf(e.id, e.type, e.amount, e.createdAt, e.investmentType, e.bankName ?: "").joinToString(",")
        }
        return "$header\n$rows"
    }

    override suspend fun exportJson(items: List<InvestmentEntity>): String {
        val body = items.joinToString(",") { e ->
            "{" +
                "\"id\":${e.id},\"type\":\"${e.type}\",\"amount\":${e.amount},\"createdAt\":${e.createdAt},\"investmentType\":\"${e.investmentType}\",\"bankName\":\"${e.bankName ?: ""}\"" +
            "}"
        }
        return "[${body}]"
    }

    override suspend fun importCsv(csv: String) {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return
        val data = lines.drop(1)
        for (line in data) {
            val cols = line.split(",")
            if (cols.size >= 6) {
                val type = cols[1]
                val amount = cols[2].toDoubleOrNull() ?: continue
                val createdAt = cols[3].toLongOrNull() ?: System.currentTimeMillis()
                val invType = cols[4]
                val bank = cols[5].ifBlank { null }
                dao.insert(
                    InvestmentEntity(
                        type = type,
                        amount = amount,
                        createdAt = createdAt,
                        investmentType = invType,
                        bankName = bank
                    )
                )
            }
        }
    }

    override suspend fun importJson(json: String) {
        val items = json.trim().removePrefix("[").removeSuffix("]").split(Regex("},\\s*\\{"))
        for (raw in items) {
            val obj = raw.trim().removePrefix("{").removeSuffix("}")
            if (obj.isBlank()) continue
            val map = obj.split(",").associate {
                val parts = it.split(":", limit = 2)
                val key = parts[0].trim().removePrefix("\"").removeSuffix("\"")
                val value = parts[1].trim().removePrefix("\"").removeSuffix("\"")
                key to value
            }
            val type = map["type"] ?: continue
            val amount = map["amount"]?.toDoubleOrNull() ?: continue
            val createdAt = map["createdAt"]?.toLongOrNull() ?: System.currentTimeMillis()
            val invType = map["investmentType"] ?: "Others"
            val bank = map["bankName"].orEmpty().ifBlank { null }
            dao.insert(
                InvestmentEntity(
                    type = type,
                    amount = amount,
                    createdAt = createdAt,
                    investmentType = invType,
                    bankName = bank
                )
            )
        }
    }
}
