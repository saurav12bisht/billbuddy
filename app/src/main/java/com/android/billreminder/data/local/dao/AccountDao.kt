package com.android.billreminder.data.local.dao

import androidx.room.*
import com.android.billreminder.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE accountType = :type ORDER BY name ASC")
    fun getAccountsByType(type: String): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Query("UPDATE accounts SET balanceCents = balanceCents + :delta WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, delta: Long)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)
}
