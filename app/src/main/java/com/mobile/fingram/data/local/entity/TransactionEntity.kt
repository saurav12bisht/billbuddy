package com.mobile.fingram.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("customerId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = false) val id: Long = System.currentTimeMillis(),
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
    val createdAt: Long = System.currentTimeMillis()
)
