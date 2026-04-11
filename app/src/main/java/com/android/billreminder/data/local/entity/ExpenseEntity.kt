package com.android.billreminder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index("dateMillis"),
        Index("categoryId"),
        Index("accountId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "INCOME" or "EXPENSE"
    val amountCents: Long,
    val categoryId: Long,
    val accountId: Long,
    val note: String?,
    val dateMillis: Long,
    val createdAt: Long = System.currentTimeMillis()
)
