package com.android.billreminder.ui.transactionform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billreminder.data.local.entity.AccountEntity
import com.android.billreminder.data.local.entity.CategoryEntity
import com.android.billreminder.data.local.entity.ExpenseEntity
import com.android.billreminder.data.local.entity.TransactionType
import com.android.billreminder.domain.model.CreditCard
import com.android.billreminder.domain.repository.CreditCardRepository
import com.android.billreminder.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val creditCardRepository: CreditCardRepository
) : ViewModel() {

    private val _type = MutableStateFlow("EXPENSE")
    val type: StateFlow<String> = _type.asStateFlow()

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId: StateFlow<Long?> = _selectedAccountId.asStateFlow()

    /** True when the selected payment method is "Credit Card" */
    private val _isCreditCardSelected = MutableStateFlow(false)
    val isCreditCardSelected: StateFlow<Boolean> = _isCreditCardSelected.asStateFlow()

    /** True when the selected payment method is "Bank" */
    private val _isBankSelected = MutableStateFlow(false)
    val isBankSelected: StateFlow<Boolean> = _isBankSelected.asStateFlow()

    /** The specific credit card chosen */
    private val _selectedCreditCardId = MutableStateFlow<Long?>(null)
    val selectedCreditCardId: StateFlow<Long?> = _selectedCreditCardId.asStateFlow()

    /** The specific bank account chosen */
    private val _selectedBankAccountId = MutableStateFlow<Long?>(null)
    val selectedBankAccountId: StateFlow<Long?> = _selectedBankAccountId.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .map { it.filter { cat -> cat.name != "CC Payment" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Main payment method chips (Cash, Bank, Credit Card) */
    val paymentMethodBaseAccounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .map { list: List<AccountEntity> -> 
            list.filter { it.name == "Cash" || it.name == "Bank" || it.name == "Credit Card" }
                .sortedBy { it.id } 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** List of specific bank accounts for the sub-picker */
    val bankAccounts: StateFlow<List<AccountEntity>> = repository.getAccountsByType(com.android.billreminder.data.local.entity.AccountType.BANK.name)
        .map { list: List<AccountEntity> -> list.filter { it.name != "Bank" } } 
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditCards: StateFlow<List<CreditCard>> = creditCardRepository.getAllCreditCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setType(type: String) { _type.value = type }
    fun setDate(date: LocalDate) { _date.value = date }
    fun selectCategory(id: Long) { _selectedCategoryId.value = id }

    fun selectAccount(id: Long, name: String) {
        _selectedAccountId.value = id
        
        val isCard = name.contains("credit", ignoreCase = true) || name.contains("card", ignoreCase = true)
        val isBank = name.contains("bank", ignoreCase = true) && !isCard
        
        _isCreditCardSelected.value = isCard
        _isBankSelected.value = isBank
        
        if (!isCard) _selectedCreditCardId.value = null
        if (!isBank) _selectedBankAccountId.value = null
    }

    fun selectCreditCard(id: Long) {
        _selectedCreditCardId.value = id
    }

    fun selectBankAccount(id: Long) {
        _selectedBankAccountId.value = id
    }

    fun saveTransaction(amountStr: String, note: String, onComplete: () -> Unit) {
        val amount = amountStr.replace(",", "").toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        val catId = _selectedCategoryId.value
        
        // Determination of final account ID
        val finalAccountId = when {
            _isBankSelected.value -> _selectedBankAccountId.value
            _isCreditCardSelected.value -> _selectedAccountId.value // Proxy account ID
            else -> _selectedAccountId.value // Cash or default
        }

        if (amount <= 0 || catId == null || finalAccountId == null) return

        // Validations for sub-selections
        if (_isCreditCardSelected.value && _selectedCreditCardId.value == null) return
        if (_isBankSelected.value && _selectedBankAccountId.value == null) return

        viewModelScope.launch {
            val isCreditCard = _isCreditCardSelected.value
            val transactionType = if (isCreditCard) TransactionType.CREDIT else TransactionType.NORMAL

            val expense = ExpenseEntity(
                type = _type.value,
                amountCents = amount,
                categoryId = catId,
                accountId = finalAccountId,
                creditCardId = if (isCreditCard) _selectedCreditCardId.value else null,
                note = note.takeIf { it.isNotBlank() },
                dateMillis = _date.value.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                transactionType = transactionType
            )
            repository.insertTransaction(expense)

            // Updates bank balance only for non-credit transactions
            if (!isCreditCard) {
                val delta = if (expense.type == "INCOME") amount else -amount
                repository.updateAccountBalance(finalAccountId, delta)
            }

            onComplete()
        }
    }
}
