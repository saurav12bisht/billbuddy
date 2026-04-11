package com.android.billreminder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bills",
    indices = [
        Index("dueDate"),
        Index("isPaid"),
        Index("category")
    ]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
