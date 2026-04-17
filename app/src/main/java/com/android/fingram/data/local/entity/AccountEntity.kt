package com.android.fingram.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    CASH,
    BANK,
    CARD_PROXY // Used for the "Credit Card" entry in accounts list
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balanceCents: Long = 0,
    val iconEmoji: String,
    val colorHex: String,
    val accountType: AccountType = AccountType.BANK
)
