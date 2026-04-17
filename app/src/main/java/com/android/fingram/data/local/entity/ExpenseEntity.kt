package com.android.fingram.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index("dateMillis"),
        Index("categoryId"),
        Index("accountId"),
        Index("creditCardId")
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
        ),
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                           // "INCOME" or "EXPENSE"
    val amountCents: Long,
    val categoryId: Long,
    val accountId: Long,
    val creditCardId: Long? = null,
    val note: String?,
    val dateMillis: Long,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * NORMAL  → Standard income / expense — counted immediately in reports.
     * CREDIT  → Credit card spend — excluded from totals until the bill is paid.
     */
    val transactionType: String = TransactionType.NORMAL
)

object TransactionType {
    const val NORMAL = "NORMAL"
    const val CREDIT = "CREDIT"
}
