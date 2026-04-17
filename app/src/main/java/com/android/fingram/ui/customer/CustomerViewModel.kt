package com.android.fingram.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.fingram.data.local.dao.CustomerWithBalance
import com.android.fingram.data.repository.CustomerRepository
import com.android.fingram.data.repository.TransactionRepository
import com.android.fingram.domain.model.Customer
import com.android.fingram.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val msg: String, val newCustomerId: Int? = null) : UiState()
    data class Error(val msg: String) : UiState()
}

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val customerRepo: CustomerRepository,
    private val transactionRepo: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun resetUiState() {
        _uiState.value = UiState.Idle
    }

    private val _searchQuery = MutableStateFlow("")
    
    val customersWithBalance: StateFlow<List<CustomerWithBalance>> =
        combine(
            customerRepo.getCustomersWithBalance(),
            _searchQuery
        ) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.phone.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addCustomer(customer: Customer) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching { customerRepo.insert(customer) }
                .onSuccess { id -> _uiState.value = UiState.Success("Customer Added", id.toInt()) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Error") }
        }
    }

    fun addTransaction(tx: Transaction) {
        viewModelScope.launch {
            runCatching { transactionRepo.insert(tx) }
                .onSuccess { _uiState.value = UiState.Success("Saved") }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Error") }
        }
    }

    suspend fun getCustomerById(id: Int) = customerRepo.getCustomerById(id)
    suspend fun getCustomerByPhone(phone: String) = customerRepo.getCustomerByPhone(phone)
    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching { customerRepo.update(customer) }
                .onSuccess { _uiState.value = UiState.Success("Customer Updated", customer.id) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Error") }
        }
    }

    fun softDelete(id: Int) {
        viewModelScope.launch { 
            customerRepo.softDelete(id)
            _uiState.value = UiState.Success("Customer Deleted")
        }
    }

    fun transactionsForCustomer(customerId: Int) = transactionRepo.getTransactionsForCustomer(customerId)
    suspend fun getBalance(customerId: Int): Long {
        val cust = customerRepo.getCustomerById(customerId) ?: return 0L
        val txBalance = transactionRepo.getNetBalance(customerId)
        val opening = cust.openingBalance * if (cust.openingBalanceType == "I_OWE") -1 else 1
        return txBalance + opening
    }
}
