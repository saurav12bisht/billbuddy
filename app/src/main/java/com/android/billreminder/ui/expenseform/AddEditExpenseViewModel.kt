package com.android.billreminder.ui.expenseform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billreminder.domain.model.Expense
import com.android.billreminder.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddEditExpenseUiState {
    object Idle : AddEditExpenseUiState()
    object Saving : AddEditExpenseUiState()
    data class Saved(val expenseId: Long) : AddEditExpenseUiState()
    data class Error(val message: String) : AddEditExpenseUiState()
}

@HiltViewModel
class AddEditExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val creditCardRepository: com.android.billreminder.domain.repository.CreditCardRepository
) : ViewModel() {

    val accounts = expenseRepository.getAllAccounts()
    val creditCards = creditCardRepository.getAllCreditCards()
    val categories = expenseRepository.getAllCategories()

    private val _uiState = MutableStateFlow<AddEditExpenseUiState>(AddEditExpenseUiState.Idle)
    val uiState: StateFlow<AddEditExpenseUiState> = _uiState.asStateFlow()

    suspend fun loadExpense(id: Long): Expense? = expenseRepository.getExpenseById(id)

    fun saveExpense(expense: Expense, isEdit: Boolean) {
        viewModelScope.launch {
            _uiState.value = AddEditExpenseUiState.Saving
            try {
                if (isEdit) {
                    expenseRepository.updateExpense(expense)
                    _uiState.value = AddEditExpenseUiState.Saved(expense.id)
                } else {
                    val newId = expenseRepository.saveExpense(expense)
                    _uiState.value = AddEditExpenseUiState.Saved(newId)
                }
            } catch (e: Exception) {
                _uiState.value = AddEditExpenseUiState.Error(e.message ?: "Failed to save expense")
            }
        }
    }

    fun resetState() {
        _uiState.value = AddEditExpenseUiState.Idle
    }
}
