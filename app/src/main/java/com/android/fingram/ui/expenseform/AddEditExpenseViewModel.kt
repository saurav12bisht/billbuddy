package com.android.fingram.ui.expenseform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.fingram.data.local.entity.AccountEntity
import com.android.fingram.data.local.entity.CategoryEntity
import com.android.fingram.domain.model.CreditCard
import com.android.fingram.domain.model.Expense
import com.android.fingram.domain.repository.CreditCardRepository
import com.android.fingram.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    private val creditCardRepository: CreditCardRepository
) : ViewModel() {

    /** Accounts — fragment filters out the generic "Credit Card" account itself. */
    val accounts: Flow<List<AccountEntity>> = expenseRepository.getAllAccounts()

    /** All user-saved credit cards — shown when the "Card" payment toggle is active. */
    val creditCards: Flow<List<CreditCard>> = creditCardRepository.getAllCreditCards()

    /** Categories — fragment filters out "CC Payment" from user display. */
    val categories: Flow<List<CategoryEntity>> = expenseRepository.getAllCategories()

    private val _uiState = MutableStateFlow<AddEditExpenseUiState>(AddEditExpenseUiState.Idle)
    val uiState: StateFlow<AddEditExpenseUiState> = _uiState.asStateFlow()

    suspend fun loadExpense(id: Long): Expense? = expenseRepository.getExpenseById(id)

    fun saveExpense(expense: Expense, isEdit: Boolean) {
        viewModelScope.launch {
            _uiState.value = AddEditExpenseUiState.Saving
            try {
                val id = if (isEdit) {
                    expenseRepository.updateExpense(expense)
                    expense.id
                } else {
                    expenseRepository.saveExpense(expense)
                }
                _uiState.value = AddEditExpenseUiState.Saved(id)
            } catch (e: Exception) {
                _uiState.value = AddEditExpenseUiState.Error(e.message ?: "Failed to save")
            }
        }
    }

    fun resetState() {
        _uiState.value = AddEditExpenseUiState.Idle
    }
}
