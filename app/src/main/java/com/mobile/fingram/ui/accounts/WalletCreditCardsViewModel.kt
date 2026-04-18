package com.mobile.fingram.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.fingram.domain.repository.CreditCardRepository
import com.mobile.fingram.ui.creditcards.CreditCardUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class WalletCreditCardsViewModel @Inject constructor(
    private val creditCardRepository: CreditCardRepository
) : ViewModel() {

    private val _creditCards = MutableStateFlow<List<CreditCardUiModel>>(emptyList())
    val creditCards: StateFlow<List<CreditCardUiModel>> = _creditCards.asStateFlow()

    init {
        loadCards()
    }

    private fun loadCards() = viewModelScope.launch {
        creditCardRepository.getAllCreditCards().collect { cards ->
            _creditCards.value = cards.map { card ->
                CreditCardUiModel(card = card, nextDueDay = card.dueDay)
            }
            cards.forEach { card ->
                val (start, end) = currentCycleRange(card.billingDay)
                launch {
                    combine(
                        creditCardRepository.getTotalSpendForCardInCycle(card.id, start, end),
                        creditCardRepository.getOutstandingAmount(card.id)
                    ) { spend, outstanding -> spend to outstanding }
                        .collect { (spend, outstanding) ->
                            _creditCards.update { list ->
                                list.map { item ->
                                    if (item.card.id == card.id)
                                        item.copy(currentCycleSpendCents = spend, outstandingAmountCents = outstanding)
                                    else item
                                }
                            }
                        }
                }
            }
        }
    }

    private fun currentCycleRange(billingDay: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        if (today < billingDay) start.add(Calendar.MONTH, -1)
        start.set(Calendar.DAY_OF_MONTH, billingDay)
        start.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        end.time = start.time
        end.add(Calendar.MONTH, 1); end.add(Calendar.DAY_OF_MONTH, -1)
        end.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }
        return start.timeInMillis to end.timeInMillis
    }
}
