package com.android.billreminder.data.local.dao

import androidx.room.*
import com.android.billreminder.data.local.entity.CategoryEntity
import com.android.billreminder.data.local.entity.ExpenseEntity
import com.android.billreminder.data.local.entity.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

data class MonthlyTotals(
    val totalIncome: Long,
    val totalExpense: Long
)

data class DailyTotal(
    val dateMillis: Long,
    val incomeTotal: Long,
    val expenseTotal: Long
)

@Dao
interface ExpenseDao {

    @Transaction
    @Query("""
        SELECT * FROM expenses 
        WHERE dateMillis BETWEEN :startMillis AND :endMillis 
        ORDER BY dateMillis DESC, createdAt DESC
    """)
    fun getAllByMonth(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>>

    @Query("""
        SELECT 
            SUM(CASE WHEN type = 'INCOME' THEN amountCents ELSE 0 END) as totalIncome,
            SUM(CASE WHEN type = 'EXPENSE' THEN amountCents ELSE 0 END) as totalExpense
        FROM expenses 
        WHERE dateMillis BETWEEN :startMillis AND :endMillis
    """)
    fun getMonthlyTotals(startMillis: Long, endMillis: Long): Flow<MonthlyTotals>

    @Query("""
        SELECT 
            dateMillis,
            SUM(CASE WHEN type = 'INCOME' THEN amountCents ELSE 0 END) as incomeTotal,
            SUM(CASE WHEN type = 'EXPENSE' THEN amountCents ELSE 0 END) as expenseTotal
        FROM expenses 
        WHERE dateMillis BETWEEN :startMillis AND :endMillis
        GROUP BY dateMillis
        ORDER BY dateMillis DESC
    """)
    fun getDailyTotals(startMillis: Long, endMillis: Long): Flow<List<DailyTotal>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // Helper for category manager
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id AND isDefault = 0")
    suspend fun deleteCategory(id: Long)

    @Query("SELECT SUM(amountCents) FROM expenses WHERE type = 'EXPENSE' AND dateMillis BETWEEN :start AND :end")
    fun getTotalAmountInRange(start: Long, end: Long): Flow<Long>
}
