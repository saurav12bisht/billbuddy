package com.android.fingram.data.local.dao

import androidx.room.*
import com.android.fingram.data.local.entity.CategoryEntity
import com.android.fingram.data.local.entity.ExpenseEntity
import com.android.fingram.data.local.entity.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

data class MonthlyTotals(
    val totalIncome: Long,
    val totalExpense: Long,
    val totalCreditSpent: Long
)

data class DailyTotal(
    val dateMillis: Long,
    val incomeTotal: Long,
    val expenseTotal: Long
)

@Dao
interface ExpenseDao {

    /**
     * All NORMAL transactions (income + expense) for the month.
     * CREDIT-type card spends are explicitly excluded.
     */
    @Transaction
    @Query("""
        SELECT * FROM expenses 
        WHERE dateMillis BETWEEN :startMillis AND :endMillis 
        ORDER BY dateMillis DESC, createdAt DESC
    """)
    fun getAllByMonth(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>>

    /**
     * Monthly income/expense totals — excludes CREDIT transactions.
     */
    @Query("""
        SELECT 
            SUM(CASE WHEN type = 'INCOME' THEN amountCents ELSE 0 END) as totalIncome,
            SUM(CASE WHEN type = 'EXPENSE' AND transactionType = 'NORMAL' THEN amountCents ELSE 0 END) as totalExpense,
            SUM(CASE WHEN type = 'EXPENSE' AND transactionType = 'CREDIT' THEN amountCents ELSE 0 END) as totalCreditSpent
        FROM expenses 
        WHERE dateMillis BETWEEN :startMillis AND :endMillis
    """)
    fun getMonthlyTotals(startMillis: Long, endMillis: Long): Flow<MonthlyTotals>

    /**
     * Daily breakdown — excludes CREDIT transactions.
     */
    @Query("""
        SELECT 
            dateMillis,
            SUM(CASE WHEN type = 'INCOME' THEN amountCents ELSE 0 END) as incomeTotal,
            SUM(CASE WHEN type = 'EXPENSE' THEN amountCents ELSE 0 END) as expenseTotal
        FROM expenses 
        WHERE dateMillis BETWEEN :startMillis AND :endMillis
        AND transactionType = 'NORMAL'
        GROUP BY strftime('%Y-%m-%d', dateMillis / 1000, 'unixepoch', 'localtime')
        ORDER BY dateMillis DESC
    """)
    fun getDailyTotals(startMillis: Long, endMillis: Long): Flow<List<DailyTotal>>

    /**
     * Credit card spends (CREDIT type) for a specific card and period.
     * Used to show the credit card detail history.
     */
    @Transaction
    @Query("""
        SELECT * FROM expenses 
        WHERE creditCardId = :cardId
        AND dateMillis BETWEEN :startMillis AND :endMillis
        AND transactionType = 'CREDIT'
        ORDER BY dateMillis DESC
    """)
    fun getCreditSpendsByCardInPeriod(cardId: Long, startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>>

    /**
     * Unbilled credit spend total for a specific card in a date range.
     */
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM expenses 
        WHERE creditCardId = :cardId 
        AND dateMillis BETWEEN :startMillis AND :endMillis
        AND transactionType = 'CREDIT'
    """)
    fun getUnbilledSpendForCard(cardId: Long, startMillis: Long, endMillis: Long): Flow<Long>

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

    @Query("""
        SELECT * FROM expenses
        ORDER BY dateMillis DESC, createdAt DESC
        LIMIT 1
    """)
    suspend fun getLatestTransaction(): ExpenseEntity?

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // Helper for category manager
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id AND isDefault = 0")
    suspend fun deleteCategory(id: Long)

    /**
     * Total expense in range — excludes CREDIT transactions.
     */
    @Query("""
        SELECT SUM(amountCents) FROM expenses 
        WHERE type = 'EXPENSE' 
        AND dateMillis BETWEEN :start AND :end
        AND transactionType = 'NORMAL'
    """)
    fun getTotalAmountInRange(start: Long, end: Long): Flow<Long>

    /**
     * Check if a reserved category exists by name.
     */
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?
}
