package com.android.billreminder.ui.creditcards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billreminder.data.local.entity.AccountEntity
import com.android.billreminder.data.local.entity.AccountType
import com.android.billreminder.data.local.entity.ExpenseWithCategory
import com.android.billreminder.domain.model.CreditCard
import com.android.billreminder.domain.model.CreditCardBill
import com.android.billreminder.domain.repository.CreditCardRepository
import com.android.billreminder.domain.repository.ExpenseRepository
import com.android.billreminder.ui.common.util.CreditCardBillingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
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

            creditCardRepository.generateOrGetBillForCycle(card, currentCycleStart, currentCycleEnd, dueDate)

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
                        val billedDue = bills
                            .filter { !it.isPaid }
                            .sumOf { (it.totalAmountCents - it.paidAmountCents).coerceAtLeast(0L) }
                        _uiState.update {
                            it.copy(
                                bills = bills,
                                billedDueAmount = billedDue,
                                isLoading = false
                            )
                        }
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
                        _uiState.update {
                            it.copy(
                                accounts = accounts.filter { account ->
                                    account.accountType == AccountType.BANK && account.name != "Bank"
                                }
                            )
                        }
                    }
            }
        }
    }

    fun payBill(bill: CreditCardBill, payFromAccountId: Long, amountCents: Long) {
        viewModelScope.launch {
            try {
                val ccCategory = expenseRepository.getCategoryByName("CC Payment")
                    ?: expenseRepository.getFirstCategory()
                    ?: throw IllegalStateException("CC Payment category not found")

                creditCardRepository.markBillAsPaid(
                    bill = bill,
                    paidFromAccountId = payFromAccountId,
                    ccPaymentCategoryId = ccCategory.id,
                    amountCents = amountCents
                )
                _uiState.update { it.copy(successMessage = "Payment recorded from your bank account.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Payment failed") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }
}
