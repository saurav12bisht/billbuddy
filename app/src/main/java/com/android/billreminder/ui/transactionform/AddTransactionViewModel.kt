package com.android.billreminder.ui.transactionform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billreminder.data.local.entity.AccountEntity
import com.android.billreminder.data.local.entity.CategoryEntity
import com.android.billreminder.data.local.entity.ExpenseEntity
import com.android.billreminder.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _type = MutableStateFlow("EXPENSE")
    val type: StateFlow<String> = _type.asStateFlow()

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId: StateFlow<Long?> = _selectedAccountId.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setType(type: String) {
        _type.value = type
    }

    fun setDate(date: LocalDate) {
        _date.value = date
    }

    fun selectCategory(id: Long) {
        _selectedCategoryId.value = id
    }

    fun selectAccount(id: Long) {
        _selectedAccountId.value = id
    }

    fun saveTransaction(amountStr: String, note: String, onComplete: () -> Unit) {
        val amount = amountStr.replace(",", "").toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        val catId = _selectedCategoryId.value
        val accId = _selectedAccountId.value ?: 1L // Default to Cash if not selected

        if (amount <= 0 || catId == null) return

        viewModelScope.launch {
            val expense = ExpenseEntity(
                type = _type.value,
                amountCents = amount,
                categoryId = catId,
                accountId = accId,
                note = note,
                dateMillis = _date.value.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            repository.insertTransaction(expense)
            
            // Update account balance
            val delta = if (expense.type == "INCOME") amount else -amount
            repository.updateAccountBalance(accId, delta)
            
            onComplete()
        }
    }
}
