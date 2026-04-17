package com.android.fingram.ui.accounts

import com.android.fingram.data.local.entity.AccountEntity
import com.android.fingram.ui.creditcards.CreditCardUiModel

sealed class WalletListItem {
    data class Header(
        val type: WalletGroupType,
        val amountCents: Long,
        val isExpanded: Boolean
    ) : WalletListItem()

    data class Account(val entity: AccountEntity) : WalletListItem()
    
    data class Card(val uiModel: CreditCardUiModel) : WalletListItem()
}

enum class WalletGroupType {
    CASH,
    BANKS,
    CREDIT_CARDS
}
