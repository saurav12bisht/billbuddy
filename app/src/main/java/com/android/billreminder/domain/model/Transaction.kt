package com.android.billreminder.domain.model

import com.android.billreminder.data.local.entity.TransactionEntity

data class Transaction(
    val id: Long,
    val customerId: Int,
    val type: String,
    val amountPaise: Long,
    val date: Long,
    val dueDate: Long? = null,
    val interestPercent: Double = 0.0,
    val category: String = "",
    val note: String = "",
    val receiptPhotoPath: String? = null,
    val isSettlement: Boolean = false,
    val createdAt: Long = 0L
) {
    fun toEntity(): TransactionEntity = TransactionEntity(
        id = id,
        customerId = customerId,
        type = type,
        amountPaise = amountPaise,
        date = date,
        dueDate = dueDate,
        interestPercent = interestPercent,
        category = category,
        note = note,
        receiptPhotoPath = receiptPhotoPath,
        isSettlement = isSettlement,
        createdAt = createdAt
    )
}

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    customerId = customerId,
    type = type,
    amountPaise = amountPaise,
    date = date,
    dueDate = dueDate,
    interestPercent = interestPercent,
    category = category,
    note = note,
    receiptPhotoPath = receiptPhotoPath,
    isSettlement = isSettlement,
    createdAt = createdAt
)
