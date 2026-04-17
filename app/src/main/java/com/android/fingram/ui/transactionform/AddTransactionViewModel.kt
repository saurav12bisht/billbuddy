package com.android.fingram.ui.transactionform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.fingram.data.local.entity.AccountEntity
import com.android.fingram.data.local.entity.AccountType
import com.android.fingram.data.local.entity.CategoryEntity
import com.android.fingram.data.local.entity.ExpenseEntity
import com.android.fingram.data.local.entity.TransactionType
import com.android.fingram.domain.model.CreditCard
import com.android.fingram.domain.repository.CreditCardRepository
import com.android.fingram.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.LocalDateTime
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

    private val _time = MutableStateFlow(LocalTime.now())
    val time: StateFlow<LocalTime> = _time.asStateFlow()

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

    private val _amount = MutableStateFlow<String?>(null)
    val amount: StateFlow<String?> = _amount.asStateFlow()

    private val _note = MutableStateFlow<String?>(null)
    val note: StateFlow<String?> = _note.asStateFlow()

    private var editingTransactionId: Long? = null
    private var originalTransaction: ExpenseEntity? = null
    private var hasAppliedLastTransactionDefaults = false

    val isEditMode: Boolean get() = editingTransactionId != null

    private val _categories = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<CategoryEntity>> = _type
        .flatMapLatest { currentType ->
            _categories.map { allCats ->
                allCats.filter { cat ->
                    cat.name != "CC Payment" && (cat.type == currentType || cat.type == CategoryEntity.TYPE_BOTH)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Main payment method chips (Cash, Bank, Credit Card) */
    val paymentMethodBaseAccounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .map { list: List<AccountEntity> -> 
            list.filter { it.name == "Cash" || it.name == "Bank" || it.name == "Credit Card" }
                .sortedBy { it.id } 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** List of specific bank accounts for the sub-picker */
    val bankAccounts: StateFlow<List<AccountEntity>> = repository.getAccountsByType(com.android.fingram.data.local.entity.AccountType.BANK.name)
        .map { list: List<AccountEntity> -> list.filter { it.name != "Bank" } } 
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditCards: StateFlow<List<CreditCard>> = creditCardRepository.getAllCreditCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        preselectLastUsedPaymentSource()
    }

    fun loadTransaction(id: Long) {
        viewModelScope.launch {
            val expense = repository.getExpenseByIdEntity(id) ?: return@launch
            editingTransactionId = id
            originalTransaction = expense
            
            _type.value = expense.type
            val instant = java.time.Instant.ofEpochMilli(expense.dateMillis)
            val zdt = instant.atZone(ZoneId.systemDefault())
            _date.value = zdt.toLocalDate()
            _time.value = zdt.toLocalTime()
            _selectedCategoryId.value = expense.categoryId
            _amount.value = com.android.fingram.ui.common.util.CurrencyFormatter.formatPaiseToRupeeWithoutSymbol(expense.amountCents)
            _note.value = expense.note
            
            // Payment method logic
            val isCredit = expense.transactionType == TransactionType.CREDIT
            if (isCredit) {
                // For credit, find the "Credit Card" base account ID
                paymentMethodBaseAccounts.first { it.isNotEmpty() }.find { it.name == "Credit Card" }?.let { baseAcc ->
                    _selectedAccountId.value = baseAcc.id
                    _isCreditCardSelected.value = true
                    _selectedCreditCardId.value = expense.creditCardId
                }
            } else {
                // Find if it's cash or bank
                // For bank, we need to know it IS a bank account. 
                // We'll peek at the account list.
                val account = repository.getAllAccounts().first { it.isNotEmpty() }.find { it.id == expense.accountId }
                if (account?.name == "Cash") {
                    _selectedAccountId.value = account.id
                    _isBankSelected.value = false
                } else if (account?.accountType == com.android.fingram.data.local.entity.AccountType.BANK) {
                    // It's a specific bank account. Select "Bank" base and then sub-picker
                    paymentMethodBaseAccounts.first { it.isNotEmpty() }.find { it.name == "Bank" }?.let { baseAcc ->
                        _selectedAccountId.value = baseAcc.id
                        _isBankSelected.value = true
                        _selectedBankAccountId.value = account.id
                    }
                }
            }
        }
    }

    private fun preselectLastUsedPaymentSource() {
        viewModelScope.launch {
            val lastTransaction = repository.getLatestTransactionEntity() ?: return@launch
            if (hasAppliedLastTransactionDefaults || isEditMode) return@launch

            val allAccounts = repository.getAllAccounts().first()
            val baseAccounts = allAccounts.filter {
                it.name == "Cash" || it.name == "Bank" || it.name == "Credit Card"
            }

            if (allAccounts.isEmpty() || baseAccounts.isEmpty()) return@launch

            if (lastTransaction.transactionType == TransactionType.CREDIT) {
                val lastCardId = lastTransaction.creditCardId ?: return@launch
                val baseCreditAccount = baseAccounts.firstOrNull { it.name == "Credit Card" } ?: return@launch
                val cardExists = creditCardRepository.getAllCreditCards().first().any { it.id == lastCardId }
                if (!cardExists) return@launch

                _selectedAccountId.value = baseCreditAccount.id
                _isCreditCardSelected.value = true
                _isBankSelected.value = false
                _selectedCreditCardId.value = lastCardId
                _selectedBankAccountId.value = null
                hasAppliedLastTransactionDefaults = true
                return@launch
            }

            val lastAccount = allAccounts.firstOrNull { it.id == lastTransaction.accountId } ?: return@launch
            when {
                lastAccount.name == "Cash" -> {
                    _selectedAccountId.value = lastAccount.id
                    _isCreditCardSelected.value = false
                    _isBankSelected.value = false
                    _selectedCreditCardId.value = null
                    _selectedBankAccountId.value = null
                    hasAppliedLastTransactionDefaults = true
                }

                lastAccount.accountType == AccountType.BANK && lastAccount.name != "Bank" -> {
                    val baseBankAccount = baseAccounts.firstOrNull { it.name == "Bank" } ?: return@launch
                    _selectedAccountId.value = baseBankAccount.id
                    _isCreditCardSelected.value = false
                    _isBankSelected.value = true
                    _selectedBankAccountId.value = lastAccount.id
                    _selectedCreditCardId.value = null
                    hasAppliedLastTransactionDefaults = true
                }
            }
        }
    }

    fun setType(type: String) { _type.value = type }
    fun setDate(date: LocalDate) { _date.value = date }
    fun setTime(time: LocalTime) { _time.value = time }
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
                id = editingTransactionId ?: 0L,
                type = _type.value,
                amountCents = amount,
                categoryId = catId,
                accountId = finalAccountId,
                creditCardId = if (isCreditCard) _selectedCreditCardId.value else null,
                note = note.takeIf { it.isNotBlank() },
                dateMillis = LocalDateTime.of(_date.value, _time.value)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
                transactionType = transactionType
            )

            // ✅ HANDLE BALANCE INTEGRITY
            if (isEditMode) {
                val old = originalTransaction!!
                
                // 1. Reverse old effect
                if (old.transactionType != TransactionType.CREDIT) {
                    val reverseDelta = if (old.type == "INCOME") -old.amountCents else old.amountCents
                    repository.updateAccountBalance(old.accountId, reverseDelta)
                }

                // 2. Apply new effect
                if (expense.transactionType != TransactionType.CREDIT) {
                    val newDelta = if (expense.type == "INCOME") amount else -amount
                    repository.updateAccountBalance(finalAccountId, newDelta)
                }

                repository.insertTransaction(expense)

            } else {
                repository.insertTransaction(expense)

                // 3. New transaction effect
                if (!isCreditCard) {
                    val delta = if (expense.type == "INCOME") amount else -amount
                    repository.updateAccountBalance(finalAccountId, delta)
                }
            }

            onComplete()
        }
    }

    fun insertNewCategory(name: String, emoji: String) {
        viewModelScope.launch {
            val typeForNewCat = if (_type.value == "INCOME") CategoryEntity.TYPE_INCOME else CategoryEntity.TYPE_EXPENSE
            val newId = repository.insertCategory(
                CategoryEntity(
                    name = name,
                    iconEmoji = emoji,
                    colorHex = "#F1EFE8", // Default neutral color
                    type = typeForNewCat
                )
            )
            _selectedCategoryId.value = newId
        }
    }
}
