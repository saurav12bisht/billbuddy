package com.android.billreminder.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billreminder.data.repository.TransactionRepository
import com.android.billreminder.domain.model.Transaction
import com.android.billreminder.ui.customer.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun addTransaction(tx: Transaction) {
        viewModelScope.launch {
            runCatching { transactionRepo.insert(tx) }
                .onSuccess { _uiState.value = UiState.Success("Saved") }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Error") }
        }
    }

    fun updateTransaction(tx: Transaction) {
        viewModelScope.launch {
            runCatching { transactionRepo.update(tx) }
                .onSuccess { _uiState.value = UiState.Success("Updated") }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Error updating") }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            runCatching { transactionRepo.deleteById(id) }
                .onSuccess { _uiState.value = UiState.Success("Deleted") }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Error deleting") }
        }
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        val entity = transactionRepo.getById(id) ?: return null
        return Transaction(
            id = entity.id,
            customerId = entity.customerId,
            type = entity.type,
            amountPaise = entity.amountPaise,
            date = entity.date,
            dueDate = entity.dueDate,
            interestPercent = entity.interestPercent,
            category = entity.category,
            note = entity.note,
            receiptPhotoPath = entity.receiptPhotoPath,
            isSettlement = entity.isSettlement,
            createdAt = entity.createdAt
        )
    }
}
