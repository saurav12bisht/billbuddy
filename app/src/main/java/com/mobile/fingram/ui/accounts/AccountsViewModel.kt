package com.mobile.fingram.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.fingram.data.local.entity.AccountEntity
import com.mobile.fingram.data.local.entity.AccountType
import com.mobile.fingram.domain.repository.CreditCardRepository
import com.mobile.fingram.domain.repository.ExpenseRepository
import com.mobile.fingram.ui.creditcards.CreditCardUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val creditCardRepository: CreditCardRepository
) : ViewModel() {

    private val expandedGroups = MutableStateFlow(setOf(WalletGroupType.CASH, WalletGroupType.BANKS, WalletGroupType.CREDIT_CARDS))

    private val accounts = repository.getAllAccounts()
    
    // Credit card data stream similar to WalletCreditCardsViewModel
    private val creditCardModels = creditCardRepository.getAllCreditCards().flatMapLatest { cards ->
        if (cards.isEmpty()) return@flatMapLatest flowOf(emptyList<CreditCardUiModel>())
        
        val flows = cards.map { card ->
            val range = currentCycleRange(card.billingDay)
            combine(
                creditCardRepository.getTotalSpendForCardInCycle(card.id, range.first, range.second),
                creditCardRepository.getOutstandingAmount(card.id)
            ) { spend, outstanding ->
                CreditCardUiModel(
                    card = card,
                    currentCycleSpendCents = spend,
                    outstandingAmountCents = outstanding,
                    nextDueDay = card.dueDay
                )
            }
        }
        combine(flows) { it.toList() }
    }

    val walletItems: StateFlow<List<WalletListItem>> = combine(
        accounts,
        creditCardModels,
        expandedGroups
    ) { accList, cardList, expanded ->
        val list = mutableListOf<WalletListItem>()

        // 1. CASH
        val cashAccounts = accList.filter { it.accountType == AccountType.CASH }
        val cashTotal = cashAccounts.sumOf { it.balanceCents }
        list.add(WalletListItem.Header(WalletGroupType.CASH, cashTotal, expanded.contains(WalletGroupType.CASH)))
        if (expanded.contains(WalletGroupType.CASH)) {
            list.addAll(cashAccounts.map { WalletListItem.Account(it) })
        }

        // 2. BANKS
        val bankAccounts = accList.filter { it.accountType == AccountType.BANK && it.name != "Bank" }
        val bankTotal = bankAccounts.sumOf { it.balanceCents }
        list.add(WalletListItem.Header(WalletGroupType.BANKS, bankTotal, expanded.contains(WalletGroupType.BANKS)))
        if (expanded.contains(WalletGroupType.BANKS)) {
            list.addAll(bankAccounts.map { WalletListItem.Account(it) })
        }

        // 3. CREDIT CARDS
        val cardTotal = cardList.sumOf { it.outstandingAmountCents }
        list.add(WalletListItem.Header(WalletGroupType.CREDIT_CARDS, cardTotal, expanded.contains(WalletGroupType.CREDIT_CARDS)))
        if (expanded.contains(WalletGroupType.CREDIT_CARDS)) {
            list.addAll(cardList.map { WalletListItem.Card(it) })
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashTotal: StateFlow<Long> = walletItems.map { items ->
        items.filterIsInstance<WalletListItem.Header>().find { it.type == WalletGroupType.CASH }?.amountCents ?: 0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val bankTotal: StateFlow<Long> = walletItems.map { items ->
        items.filterIsInstance<WalletListItem.Header>().find { it.type == WalletGroupType.BANKS }?.amountCents ?: 0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val creditTotal: StateFlow<Long> = walletItems.map { items ->
        items.filterIsInstance<WalletListItem.Header>().find { it.type == WalletGroupType.CREDIT_CARDS }?.amountCents ?: 0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalBalance: StateFlow<Long> = combine(cashTotal, bankTotal) { cash, banks ->
        cash + banks
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun toggleGroup(type: WalletGroupType) {
        expandedGroups.update { 
            if (it.contains(type)) it - type else it + type
        }
    }

    fun saveAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.insertAccount(account)
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    suspend fun getAccountById(accountId: Long): AccountEntity? {
        return repository.getAccountById(accountId)
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    private fun currentCycleRange(billingDay: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val start = Calendar.getInstance()
        if (today < billingDay) start.add(Calendar.MONTH, -1)
        start.set(Calendar.DAY_OF_MONTH, billingDay)
        start.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val end = Calendar.getInstance()
        end.time = start.time
        end.add(Calendar.MONTH, 1); end.add(Calendar.DAY_OF_MONTH, -1)
        end.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }
        return start.timeInMillis to end.timeInMillis
    }
}
