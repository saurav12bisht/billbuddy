package com.mobile.fingram.domain.model

import com.mobile.fingram.data.local.entity.CreditCardBillEntity
import com.mobile.fingram.data.local.entity.CreditCardEntity

data class CreditCard(
    val id: Long = 0,
    val cardName: String,
    val bankName: String,
    val lastFourDigits: String,
    val billingDay: Int,
    val dueDay: Int,
    val creditLimitCents: Long? = null,
    val cardNetwork: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class CreditCardBill(
    val id: Long = 0,
    val cardId: Long,
    val billingCycleStartDate: Long,
    val billingCycleEndDate: Long,
    val dueDateMillis: Long,
    val totalAmountCents: Long,
    val paidAmountCents: Long = 0L,
    val minimumDueCents: Long? = null,
    val status: String = BILL_STATUS_OPEN,
    val isPaid: Boolean = false,
    val paidAt: Long? = null,
    val paidFromAccountId: Long? = null,
    val generatedExpenseId: Long? = null
) {
    companion object {
        const val BILL_STATUS_OPEN = "OPEN"
        const val BILL_STATUS_PARTIALLY_PAID = "PARTIALLY_PAID"
        const val BILL_STATUS_PAID = "PAID"
        const val BILL_STATUS_OVERDUE = "OVERDUE"
    }
}

fun CreditCardEntity.toDomain() = CreditCard(
    id = id,
    cardName = cardName,
    bankName = bankName,
    lastFourDigits = lastFourDigits,
    billingDay = billingDay,
    dueDay = dueDay,
    creditLimitCents = creditLimitCents,
    cardNetwork = cardNetwork,
    isActive = isActive,
    createdAt = createdAt
)

fun CreditCard.toEntity() = CreditCardEntity(
    id = id,
    cardName = cardName,
    bankName = bankName,
    lastFourDigits = lastFourDigits,
    billingDay = billingDay,
    dueDay = dueDay,
    creditLimitCents = creditLimitCents,
    cardNetwork = cardNetwork,
    isActive = isActive,
    createdAt = createdAt
)

fun CreditCardBillEntity.toDomain() = CreditCardBill(
    id = id,
    cardId = cardId,
    billingCycleStartDate = billingCycleStartDate,
    billingCycleEndDate = billingCycleEndDate,
    dueDateMillis = dueDateMillis,
    totalAmountCents = totalAmountCents,
    paidAmountCents = paidAmountCents,
    minimumDueCents = minimumDueCents,
    status = status,
    isPaid = isPaid,
    paidAt = paidAt,
    paidFromAccountId = paidFromAccountId,
    generatedExpenseId = generatedExpenseId
)

fun CreditCardBill.toEntity() = CreditCardBillEntity(
    id = id,
    cardId = cardId,
    billingCycleStartDate = billingCycleStartDate,
    billingCycleEndDate = billingCycleEndDate,
    dueDateMillis = dueDateMillis,
    totalAmountCents = totalAmountCents,
    paidAmountCents = paidAmountCents,
    minimumDueCents = minimumDueCents,
    status = status,
    isPaid = isPaid,
    paidAt = paidAt,
    paidFromAccountId = paidFromAccountId,
    generatedExpenseId = generatedExpenseId
)
