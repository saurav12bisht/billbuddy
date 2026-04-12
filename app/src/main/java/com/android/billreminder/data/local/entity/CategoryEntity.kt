package com.android.billreminder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconEmoji: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val type: String = TYPE_EXPENSE // Default to expense for safety
) {
    companion object {
        const val TYPE_EXPENSE = "EXPENSE"
        const val TYPE_INCOME = "INCOME"
        const val TYPE_BOTH = "BOTH"
    }
}
