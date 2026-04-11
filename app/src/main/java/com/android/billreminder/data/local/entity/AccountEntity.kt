package com.android.billreminder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balanceCents: Long = 0,
    val iconEmoji: String,
    val colorHex: String
)
