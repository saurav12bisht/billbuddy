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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CreditCardDetailUiState(
    val card: CreditCard? = null,
    val currentCycleSpend: Long = 0L,
    val outstandingAmount: Long = 0L,
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

            val (cycleStart, cycleEnd, dueDate) = calculateCurrentCycle(card)

            // Generate or fetch the bill for the current cycle (lazy)
            creditCardRepository.generateOrGetBillForCycle(card, cycleStart, cycleEnd, dueDate)

            // Collect all live streams
            launch {
                creditCardRepository.getTotalSpendForCardInCycle(card.id, cycleStart, cycleEnd)
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
                        _uiState.update { it.copy(bills = bills, isLoading = false) }
                    }
            }

            launch {
                creditCardRepository.getExpensesForCardInCycle(card.id, cycleStart, cycleEnd)
                    .collect { spends ->
                        _uiState.update { it.copy(recentSpends = spends) }
                    }
            }

            launch {
                expenseRepository.getAllAccounts()
                    .collect { accounts ->
                        _uiState.update { it.copy(accounts = accounts.filter { a ->
                            !a.name.contains("credit", ignoreCase = true) &&
                            !a.name.contains("card", ignoreCase = true)
                        }) }
                    }
            }
        }
    }

    /**
     * Pay a bill using the given bank account.
     * Finds the reserved "CC Payment" category automatically.
     */
    fun payBill(bill: CreditCardBill, payFromAccountId: Long) {
        viewModelScope.launch {
            try {
                val ccCategory = expenseRepository.getCategoryByName("CC Payment")
                    ?: expenseRepository.getFirstCategory()!! // Fallback

                creditCardRepository.markBillAsPaid(
                    bill = bill,
                    paidFromAccountId = payFromAccountId,
                    ccPaymentCategoryId = ccCategory.id
                )
                _uiState.update { it.copy(successMessage = "Bill marked as paid! ✅") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Payment failed") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }

    /**
     * Returns (cycleStartMillis, cycleEndMillis, dueDateMillis) for the current billing cycle.
     */
    private fun calculateCurrentCycle(card: CreditCard): Triple<Long, Long, Long> {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)

        val startCal = Calendar.getInstance()
        val endCal = Calendar.getInstance()
        val dueCal = Calendar.getInstance()

        if (today >= card.billingDay) {
            // Cycle started this month on billingDay
            startCal.set(Calendar.DAY_OF_MONTH, card.billingDay)
            setStartOfDay(startCal)

            endCal.time = startCal.time
            endCal.add(Calendar.MONTH, 1)
            endCal.add(Calendar.DAY_OF_MONTH, -1)
            setEndOfDay(endCal)

            dueCal.time = startCal.time
            dueCal.add(Calendar.MONTH, 1)
            dueCal.set(Calendar.DAY_OF_MONTH, card.dueDay)
            setEndOfDay(dueCal)
        } else {
            // Cycle started last month on billingDay
            startCal.add(Calendar.MONTH, -1)
            startCal.set(Calendar.DAY_OF_MONTH, card.billingDay)
            setStartOfDay(startCal)

            endCal.time = startCal.time
            endCal.add(Calendar.MONTH, 1)
            endCal.add(Calendar.DAY_OF_MONTH, -1)
            setEndOfDay(endCal)

            dueCal.time = startCal.time
            dueCal.add(Calendar.MONTH, 1)
            dueCal.set(Calendar.DAY_OF_MONTH, card.dueDay)
            setEndOfDay(dueCal)
        }

        return Triple(startCal.timeInMillis, endCal.timeInMillis, dueCal.timeInMillis)
    }

    private fun setStartOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    }
    private fun setEndOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
    }
}
