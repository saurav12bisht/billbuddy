package com.android.fingram.data.local.entity

import androidx.room.*

@Entity(
    tableName = "notification_logs",
    indices = [Index(value = ["billId", "reminderType"], unique = true)]
)
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val reminderType: String, // e.g., "DUE_7", "DUE_5", "DUE_3", "DUE_1", "OVERDUE"
    val sentAt: Long = System.currentTimeMillis()
)
