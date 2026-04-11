package com.android.billreminder.domain.model

import com.android.billreminder.data.local.entity.ExpenseEntity

data class Expense(
    val id: Long = 0,
    val type: String, // "INCOME" or "EXPENSE"
    val amountCents: Long,
    val categoryId: Long,
    val accountId: Long,
    val note: String?,
    val dateMillis: Long,
    val createdAt: Long = System.currentTimeMillis()
)

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    type = type,
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    dateMillis = dateMillis,
    createdAt = createdAt
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    type = type,
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    dateMillis = dateMillis,
    createdAt = createdAt
)
