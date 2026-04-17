package com.android.fingram.ui.creditcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.fingram.domain.model.CreditCard
import com.android.fingram.domain.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class CreditCardUiModel(
    val card: CreditCard,
    val currentCycleSpendCents: Long = 0L,
    val outstandingAmountCents: Long = 0L,
    val nextDueDay: Int
)

@HiltViewModel
class CreditCardListViewModel @Inject constructor(
    private val repository: CreditCardRepository
) : ViewModel() {

    private val _creditCards = MutableStateFlow<List<CreditCardUiModel>>(emptyList())
    val creditCards: StateFlow<List<CreditCardUiModel>> = _creditCards.asStateFlow()

    init {
        loadCreditCards()
    }

    private fun loadCreditCards() = viewModelScope.launch {
        repository.getAllCreditCards().collect { cards ->
            // Emit immediately with zeros so the list shows fast
            _creditCards.value = cards.map { card ->
                CreditCardUiModel(card = card, nextDueDay = card.dueDay)
            }

            // Then update each with live spend + outstanding
            cards.forEach { card ->
                val (start, end) = calculateCurrentCycleRange(card.billingDay)

                launch {
                    combine(
                        repository.getTotalSpendForCardInCycle(card.id, start, end),
                        repository.getOutstandingAmount(card.id)
                    ) { spend, outstanding ->
                        Pair(spend, outstanding)
                    }.collect { (spend, outstanding) ->
                        _creditCards.update { list ->
                            list.map { item ->
                                if (item.card.id == card.id) {
                                    item.copy(
                                        currentCycleSpendCents = spend,
                                        outstandingAmountCents = outstanding
                                    )
                                } else item
                            }
                        }
                    }
                }
            }
        }
    }

    private fun calculateCurrentCycleRange(billingDay: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)

        val start = Calendar.getInstance()
        val end = Calendar.getInstance()

        if (today >= billingDay) {
            start.set(Calendar.DAY_OF_MONTH, billingDay)
        } else {
            start.add(Calendar.MONTH, -1)
            start.set(Calendar.DAY_OF_MONTH, billingDay)
        }
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)

        end.time = start.time
        end.add(Calendar.MONTH, 1)
        end.add(Calendar.DAY_OF_MONTH, -1)
        end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59)

        return Pair(start.timeInMillis, end.timeInMillis)
    }

    fun deleteCreditCard(card: CreditCard) = viewModelScope.launch {
        repository.deleteCreditCard(card)
    }
}
