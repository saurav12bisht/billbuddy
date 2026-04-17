package com.android.fingram.data.local.dao

import androidx.room.*
import com.android.fingram.data.local.entity.NotificationLogEntity

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs WHERE billId = :billId AND reminderType = :reminderType")
    suspend fun getLog(billId: Long, reminderType: String): NotificationLogEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: NotificationLogEntity): Long

    @Query("DELETE FROM notification_logs WHERE billId = :billId")
    suspend fun deleteLogsForBill(billId: Long)
}
