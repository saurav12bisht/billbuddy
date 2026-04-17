package com.android.billreminder.ui.creditcards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billreminder.data.local.entity.AccountEntity
import com.android.billreminder.data.local.entity.ExpenseWithCategory
import com.android.billreminder.domain.model.CreditCard
import com.android.billreminder.domain.model.CreditCardBill
import com.android.billreminder.domain.repository.CreditCardRepository
import com.android.billreminder.domain.repository.ExpenseRepository
import com.android.billreminder.ui.common.util.CreditCardBillingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreditCardDetailUiState(
    val card: CreditCard? = null,
    val currentCycleSpend: Long = 0L,
    val outstandingAmount: Long = 0L,
    val billedDueAmount: Long = 0L,
    val recentSpends: List<ExpenseWithCategory> = emptyList(),
    val bills: List<CreditCardBill> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class CreditCardDetailViewModel @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val expenseRepository: ExpenseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Long = savedStateHandle["cardId"] ?: -1L

    private val _uiState = MutableStateFlow(CreditCardDetailUiState())
    val uiState: StateFlow<CreditCardDetailUiState> = _uiState.asStateFlow()

    init {
        loadCard()
    }

    private fun loadCard() {
        viewModelScope.launch {
            val card = creditCardRepository.getCreditCardById(cardId) ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Card not found") }
                return@launch
            }

            _uiState.update { it.copy(card = card) }

            val cycle = CreditCardBillingUtils.getCurrentCycle(card)
            val currentCycleStart = cycle.startDateMillis
            val currentCycleEnd = cycle.endDateMillis
            val dueDate = cycle.dueDateMillis

            // Generate or fetch the bill for the current cycle (lazy)
            creditCardRepository.generateOrGetBillForCycle(card, currentCycleStart, currentCycleEnd, dueDate)

            // Collect all live streams
            launch {
                creditCardRepository.getTotalSpendForCardInCycle(card.id, currentCycleStart, currentCycleEnd)
                    .collect { spend ->
                        _uiState.update { it.copy(currentCycleSpend = spend) }
                    }
            }

            launch {
                creditCardRepository.getOutstandingAmount(card.id)
                    .collect { outstanding ->
                        _uiState.update { it.copy(outstandingAmount = outstanding) }
                    }
            }

            launch {
                creditCardRepository.getBillsForCard(card.id)
                    .collect { bills ->
                        val billedDue = bills.filter { !it.isPaid }.sumOf { it.totalAmountCents - it.paidAmountCents }
                        _uiState.update { it.copy(bills = bills, billedDueAmount = billedDue, isLoading = false) }
                    }
            }

            launch {
                creditCardRepository.getExpensesForCardInCycle(card.id, currentCycleStart, currentCycleEnd)
                    .collect { spends ->
                        _uiState.update { it.copy(recentSpends = spends) }
                    }
            }

            launch {
                expenseRepository.getAllAccounts()
                    .collect { accounts ->
                        _uiState.update { it.copy(accounts = accounts.filter { a ->
                            a.accountType != com.android.billreminder.data.local.entity.AccountType.CARD_PROXY
                        }) }
                    }
            }
        }
    }

    /**
     * Pay a bill using the given bank account.
     * Finds the reserved "CC Payment" category automatically.
     */
    fun payBill(bill: CreditCardBill, payFromAccountId: Long, amountCents: Long) {
        viewModelScope.launch {
            try {
                val ccCategory = expenseRepository.getCategoryByName("CC Payment")
                    ?: expenseRepository.getFirstCategory()!! // Fallback

                creditCardRepository.markBillAsPaid(
                    bill = bill,
                    paidFromAccountId = payFromAccountId,
                    ccPaymentCategoryId = ccCategory.id,
                    amountCents = amountCents
                )
                _uiState.update { it.copy(successMessage = "Bill payment recorded! ✅") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Payment failed") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }
}
