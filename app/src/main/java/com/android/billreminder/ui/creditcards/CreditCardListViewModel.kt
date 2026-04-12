package com.android.billreminder.ui.creditcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billreminder.domain.model.CreditCard
import com.android.billreminder.domain.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.*

data class CreditCardUiModel(
    val card: CreditCard,
    val currentCycleSpendCents: Long = 0,
    val nextDueDay: Int
)

@HiltViewModel
class CreditCardListViewModel @Inject constructor(
    private val repository: CreditCardRepository
) : ViewModel() {

    private val _creditCards = MutableStateFlow<List<CreditCardUiModel>>(emptyList())
    val creditCards = _creditCards.asStateFlow()

    init {
        loadCreditCards()
    }

    private fun loadCreditCards() = viewModelScope.launch {
        repository.getAllCreditCards().collect { cards ->
            val uiModels = cards.map { card ->
                // Calculate current cycle dates
                val (start, end) = calculateBillingCycle(card.billingDay)
                
                // Get total spend for this cycle
                // Since totalSpend is a Flow, this is a bit tricky to map in a simple list mapping.
                // For now, we'll just show the card, and ideally we'd combine flows.
                
                CreditCardUiModel(
                    card = card,
                    currentCycleSpendCents = 0, // Will be updated
                    nextDueDay = card.dueDay
                )
            }
            _creditCards.value = uiModels
            
            // Further logic to populate spend for each card
            uiModels.forEach { uiModel ->
                updateSpendForCard(uiModel)
            }
        }
    }

    private fun updateSpendForCard(uiModel: CreditCardUiModel) = viewModelScope.launch {
        val (start, end) = calculateBillingCycle(uiModel.card.billingDay)
        repository.getTotalSpendForCardInCycle(uiModel.card.id, start, end).collect { total ->
            _creditCards.update { list ->
                list.map { item ->
                    if (item.card.id == uiModel.card.id) {
                        item.copy(currentCycleSpendCents = total ?: 0L)
                    } else item
                }
            }
        }
    }

    private fun calculateBillingCycle(billingDay: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        
        val startCalendar = Calendar.getInstance()
        val endCalendar = Calendar.getInstance()
        
        if (today >= billingDay) {
            // Cycle started this month
            startCalendar.set(Calendar.DAY_OF_MONTH, billingDay)
            startCalendar.set(Calendar.HOUR_OF_DAY, 0)
            startCalendar.set(Calendar.MINUTE, 0)
            startCalendar.set(Calendar.SECOND, 0)
            
            endCalendar.time = startCalendar.time
            endCalendar.add(Calendar.MONTH, 1)
            endCalendar.add(Calendar.DAY_OF_MONTH, -1)
            endCalendar.set(Calendar.HOUR_OF_DAY, 23)
            endCalendar.set(Calendar.MINUTE, 59)
            endCalendar.set(Calendar.SECOND, 59)
        } else {
            // Cycle started last month
            startCalendar.add(Calendar.MONTH, -1)
            startCalendar.set(Calendar.DAY_OF_MONTH, billingDay)
            startCalendar.set(Calendar.HOUR_OF_DAY, 0)
            startCalendar.set(Calendar.MINUTE, 0)
            startCalendar.set(Calendar.SECOND, 0)
            
            endCalendar.time = startCalendar.time
            endCalendar.add(Calendar.MONTH, 1)
            endCalendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        
        return Pair(startCalendar.timeInMillis, endCalendar.timeInMillis)
    }
}
