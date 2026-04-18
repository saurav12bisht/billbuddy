package com.mobile.fingram.domain.model

import com.mobile.fingram.data.local.entity.BillEntity

data class Bill(
    val id: Long = 0,
    val title: String,
    val amountCents: Long,
    val dueDate: Long,
    val category: String,
    val isPaid: Boolean = false,
    val repeatType: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastPaidAt: Long? = null
)

fun Bill.toEntity(): BillEntity = BillEntity(
    id = id,
    title = title,
    amountCents = amountCents,
    dueDate = dueDate,
    category = category,
    isPaid = isPaid,
    repeatType = repeatType,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastPaidAt = lastPaidAt
)

fun BillEntity.toDomain(): Bill = Bill(
    id = id,
    title = title,
    amountCents = amountCents,
    dueDate = dueDate,
    category = category,
    isPaid = isPaid,
    repeatType = repeatType,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastPaidAt = lastPaidAt
)
