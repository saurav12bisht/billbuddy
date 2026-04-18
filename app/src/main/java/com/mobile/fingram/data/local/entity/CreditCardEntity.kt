package com.mobile.fingram.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardName: String,
    val bankName: String,
    val lastFourDigits: String,
    val billingDay: Int, // 1 to 31
    val dueDay: Int,     // 1 to 31
    val creditLimitCents: Long? = null,
    val cardNetwork: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
