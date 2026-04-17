package com.android.fingram.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.fingram.data.local.entity.AccountEntity
import com.android.fingram.data.local.entity.AccountType
import com.android.fingram.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageAccountsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .map { accounts ->
            accounts.filter { account ->
                account.accountType == AccountType.BANK && account.name != "Bank"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }
}
