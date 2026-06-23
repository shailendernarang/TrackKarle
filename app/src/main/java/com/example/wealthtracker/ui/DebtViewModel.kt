package com.example.wealthtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wealthtracker.data.local.DebtDao
import com.example.wealthtracker.data.local.DebtEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebtViewModel @Inject constructor(
    private val debtDao: DebtDao
) : ViewModel() {

    val debts: StateFlow<List<DebtEntity>> = debtDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun consumeMessage() {
        _message.value = null
    }

    fun addDebt(
        name: String,
        debtType: String,
        principal: Double,
        outstanding: Double,
        rate: Double,
        emi: Double,
        startDate: String,
        tenureMonths: Int
    ) {
        viewModelScope.launch {
            debtDao.insert(
                DebtEntity(
                    name = name,
                    debtType = debtType,
                    principalAmount = principal,
                    outstandingBalance = outstanding,
                    interestRate = rate,
                    emiAmount = emi,
                    startDate = startDate,
                    tenureMonths = tenureMonths
                )
            )
            _message.value = "Debt added"
        }
    }

    fun updateDebt(entity: DebtEntity) {
        viewModelScope.launch {
            debtDao.update(entity)
            _message.value = "Updated"
        }
    }

    fun delete(entity: DebtEntity) {
        viewModelScope.launch {
            debtDao.delete(entity)
            _message.value = "Deleted"
        }
    }
}
