package com.example.wealthtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.data.repository.InvestmentRepository
import com.example.wealthtracker.util.InvestmentTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvestmentViewModel @Inject constructor(
    private val repo: InvestmentRepository
) : ViewModel() {

    val investments: StateFlow<List<InvestmentEntity>> =
        repo.observeInvestments().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // Filters
    private val _typeFilter = MutableStateFlow<String?>(null)
    val typeFilter: StateFlow<String?> = _typeFilter
    val filteredInvestments: StateFlow<List<InvestmentEntity>> =
        combine(investments, _typeFilter) { list, filter ->
            val sorted = list.sortedByDescending { it.createdAt }
            if (filter.isNullOrBlank() || filter == "All") sorted else sorted.filter { it.investmentType == filter }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalCount: StateFlow<Int> = investments.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val totalAmount: StateFlow<Double> = investments.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    fun setTypeFilter(type: String?) { _typeFilter.value = type }

    // Snackbars/events
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun consumeMessage() { _message.value = null }

    fun addInvestment(amountInput: String, investmentType: String, bankName: String?, displayName: String? = null) {
        val amount = parseAmount(amountInput)
        val cleanType = investmentType.ifBlank { "Others" }
        if (amount == null || amount <= 0.0) { _message.value = "Enter a valid amount"; return }
        if (cleanType == "FD" && (bankName == null || bankName.isBlank())) {
            _message.value = "Please select bank for FD"; return
        }
        if (cleanType == "Others") {
            val name = displayName?.trim().orEmpty()
            if (name.isBlank()) { _message.value = "Please enter an investment name for Others"; return }
            viewModelScope.launch {
                repo.addInvestment(name, amount, cleanType, null)
                _message.value = "Added"
            }
            return
        }
        viewModelScope.launch {
            // Store type field as the investmentType for simpler display
            repo.addInvestment(cleanType, amount, cleanType, bankName)
            _message.value = "Added"
        }
    }

    fun delete(entity: InvestmentEntity) {
        viewModelScope.launch { repo.deleteInvestment(entity) }
    }

    fun deleteAll() {
        viewModelScope.launch { repo.deleteAll() }
        _message.value = "Cleared"
    }

    fun reAdd(entity: InvestmentEntity) {
        viewModelScope.launch {
            repo.addInvestment(
                type = entity.type,
                amount = entity.amount,
                investmentType = entity.investmentType,
                bankName = entity.bankName
            )
            _message.value = "Restored"
        }
    }

    fun updateInvestment(
        id: Long,
        newDisplayType: String,
        newAmountInput: String,
        newInvestmentType: String,
        newBank: String?
    ) {
        val amount = parseAmount(newAmountInput) ?: return
        val cleanType = newDisplayType.trim().ifEmpty { newInvestmentType }
        val current = investments.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            repo.updateInvestment(
                current.copy(
                    type = cleanType,
                    amount = amount,
                    investmentType = newInvestmentType,
                    bankName = if (newInvestmentType == "FD") newBank else null
                )
            )
            _message.value = "Updated"
        }
    }

    private fun parseAmount(input: String): Double? {
        val clean = input.replace(",", "").trim()
        if (clean.isEmpty()) return null
        return try {
            val bd = java.math.BigDecimal(clean).setScale(2, java.math.RoundingMode.HALF_UP)
            bd.toDouble()
        } catch (_: Exception) {
            null
        }
    }

    // Export / Import
    private val _exportContent = MutableStateFlow<String?>(null)
    val exportContent: StateFlow<String?> = _exportContent
    fun consumeExportContent() { _exportContent.value = null }

    fun exportCsv() {
        viewModelScope.launch {
            val data = repo.exportCsv(investments.value)
            _exportContent.value = data
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            val data = repo.exportJson(investments.value)
            _exportContent.value = data
        }
    }

    fun importCsv(csv: String) {
        viewModelScope.launch {
            repo.importCsv(csv)
            _message.value = "Imported CSV"
        }
    }

    fun importJson(json: String) {
        viewModelScope.launch {
            repo.importJson(json)
            _message.value = "Imported JSON"
        }
    }
}
