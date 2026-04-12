package com.android.billreminder.domain.model

import com.android.billreminder.data.local.entity.CreditCardEntity

data class CreditCard(
    val id: Long = 0,
    val cardName: String,
    val bankName: String,
    val lastFourDigits: String,
    val billingDay: Int,
    val dueDay: Int,
    val createdAt: Long = System.currentTimeMillis()
)

data class CreditCardBill(
    val id: Long = 0,
    val cardId: Long,
    val billingCycleStartDate: Long,
    val billingCycleEndDate: Long,
    val totalAmountCents: Long,
    val isPaid: Boolean = false,
    val paidAt: Long? = null
)

fun CreditCardEntity.toDomain() = CreditCard(
    id = id,
    cardName = cardName,
    bankName = bankName,
    lastFourDigits = lastFourDigits,
    billingDay = billingDay,
    dueDay = dueDay,
    createdAt = createdAt
)

fun CreditCard.toEntity() = CreditCardEntity(
    id = id,
    cardName = cardName,
    bankName = bankName,
    lastFourDigits = lastFourDigits,
    billingDay = billingDay,
    dueDay = dueDay,
    createdAt = createdAt
)
