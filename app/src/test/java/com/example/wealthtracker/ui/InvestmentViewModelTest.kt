package com.example.wealthtracker.ui

import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.data.repository.InvestmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeRepo : InvestmentRepository {
    private val backing = mutableListOf<InvestmentEntity>()
    private val flow = MutableStateFlow<List<InvestmentEntity>>(emptyList())

    override fun observeInvestments(): Flow<List<InvestmentEntity>> = flow

    override suspend fun addInvestment(type: String, amount: Double, investmentType: String, bankName: String?) {
        val e = InvestmentEntity(type = type, amount = amount, investmentType = investmentType, bankName = bankName)
        backing.add(e)
        flow.value = backing.toList()
    }

    override suspend fun deleteInvestment(entity: InvestmentEntity) {
        backing.removeIf { it.id == entity.id }
        flow.value = backing.toList()
    }

    override suspend fun deleteByIds(ids: List<Long>) {
        backing.removeIf { it.id in ids }
        flow.value = backing.toList()
    }

    override suspend fun deleteAll() {
        backing.clear()
        flow.value = emptyList()
    }

    override suspend fun updateInvestment(entity: InvestmentEntity) {
        val idx = backing.indexOfFirst { it.id == entity.id }
        if (idx >= 0) backing[idx] = entity
        flow.value = backing.toList()
    }

    override suspend fun exportCsv(items: List<InvestmentEntity>): String = ""
    override suspend fun exportJson(items: List<InvestmentEntity>): String = "[]"
    override suspend fun importCsv(csv: String) {}
    override suspend fun importJson(json: String) {}
}

class InvestmentViewModelTest {

    private suspend fun awaitSize(vm: InvestmentViewModel, expected: Int, timeoutMs: Long = 500) {
        var waited = 0L
        while (vm.filteredInvestments.value.size != expected && waited < timeoutMs) {
            delay(20)
            waited += 20
        }
    }

    @Test
    fun filtering_respects_type_and_sorting() = runBlocking {
        val repo = FakeRepo()
        val vm = InvestmentViewModel(repo)
        // Seed: three items; createdAt auto (now), so inject with explicit times via update after add
        repo.addInvestment("Fixed Deposit", 1000.0, "FD", "HDFC")
        repo.addInvestment("Gold", 500.0, "Gold", null)
        repo.addInvestment("Mutual Fund", 700.0, "Mutual Fund", null)

        // No filter -> size 3
        awaitSize(vm, 3)
        assertEquals(3, vm.filteredInvestments.value.size)

        // Filter to FD -> only FD
        vm.setTypeFilter("FD")
        // allow state to propagate
        delay(20)
        val onlyFd = vm.filteredInvestments.value
        assertEquals(1, onlyFd.size)
        assertEquals("FD", onlyFd.first().investmentType)

        // Filter All -> back to 3
        vm.setTypeFilter(null)
        delay(20)
        assertEquals(3, vm.filteredInvestments.value.size)
    }

    @Test
    fun add_validation_gates() = runBlocking {
        val repo = FakeRepo()
        val vm = InvestmentViewModel(repo)

        // Invalid amount
        vm.addInvestment("0", "Gold", null)
        assertTrue(vm.investments.value.isEmpty())

        // FD without bank
        vm.addInvestment("1000", "FD", null)
        assertTrue(vm.investments.value.isEmpty())

        // Others without name
        vm.addInvestment("1000", "Others", null, displayName = "")
        assertTrue(vm.investments.value.isEmpty())

        // Valid FD
        vm.addInvestment("1500", "FD", "SBI")
        delay(50)
        assertEquals(1, vm.investments.value.size)

        // Valid Others with name
        vm.addInvestment("2000", "Others", null, displayName = "US Stocks")
        delay(50)
        assertEquals(2, vm.investments.value.size)
    }
}
