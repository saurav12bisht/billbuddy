package com.android.fingram.domain.model

import com.android.fingram.data.local.entity.ExpenseEntity
import com.android.fingram.data.local.entity.TransactionType

data class Expense(
    val id: Long = 0,
    val type: String,                           // "INCOME" or "EXPENSE"
    val amountCents: Long,
    val categoryId: Long,
    val accountId: Long,
    val creditCardId: Long? = null,
    val note: String?,
    val dateMillis: Long,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * NORMAL (default) → counted in reports immediately.
     * CREDIT → credit card spend, excluded until bill is paid.
     */
    val transactionType: String = TransactionType.NORMAL
)

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    type = type,
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    creditCardId = creditCardId,
    note = note,
    dateMillis = dateMillis,
    createdAt = createdAt,
    transactionType = transactionType
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    type = type,
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    creditCardId = creditCardId,
    note = note,
    dateMillis = dateMillis,
    createdAt = createdAt,
    transactionType = transactionType
)
