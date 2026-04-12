package com.android.billreminder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.android.billreminder.data.local.dao.*
import com.android.billreminder.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CustomerEntity::class, 
        TransactionEntity::class, 
        BillEntity::class, 
        ExpenseEntity::class, 
        CategoryEntity::class, 
        AccountEntity::class,
        CreditCardEntity::class,
        CreditCardBillEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class VyapaarDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun billDao(): BillDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun accountDao(): AccountDao
    abstract fun creditCardDao(): CreditCardDao

    companion object {
        @Volatile
        private var INSTANCE: VyapaarDatabase? = null

        fun getInstance(context: Context): VyapaarDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    VyapaarDatabase::class.java,
                    "vyapaar_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(DatabaseCallback())
                    .build()
                    .also { INSTANCE = it }
            }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    prepopulateData(database)
                }
            }
        }

        private suspend fun prepopulateData(db: VyapaarDatabase) {
            val expenseDao = db.expenseDao()
            val accountDao = db.accountDao()

            // Pre-populate Categories
            val categories = listOf(
                CategoryEntity(name = "Food", iconEmoji = "🍛", colorHex = "#E8F5E9", isDefault = true),
                CategoryEntity(name = "Transport", iconEmoji = "🚖", colorHex = "#FFF3E0", isDefault = true),
                CategoryEntity(name = "Household", iconEmoji = "🏠", colorHex = "#F3E5F5", isDefault = true),
                CategoryEntity(name = "Recharge", iconEmoji = "📱", colorHex = "#E3F2FD", isDefault = true),
                CategoryEntity(name = "Investment", iconEmoji = "📈", colorHex = "#E0F7FA", isDefault = true),
                CategoryEntity(name = "Shopping", iconEmoji = "🛍", colorHex = "#FFF8E1", isDefault = true),
                CategoryEntity(name = "Health", iconEmoji = "💊", colorHex = "#FFEBEE", isDefault = true),
                CategoryEntity(name = "Entertainment", iconEmoji = "🎬", colorHex = "#F3E5F5", isDefault = true),
                CategoryEntity(name = "Salary", iconEmoji = "💰", colorHex = "#E8F5E9", isDefault = true),
                CategoryEntity(name = "Freelance", iconEmoji = "💻", colorHex = "#E3F2FD", isDefault = true),
                CategoryEntity(name = "Other", iconEmoji = "📦", colorHex = "#F1EFE8", isDefault = true)
            )
            categories.forEach { expenseDao.insertCategory(it) }

            // Pre-populate Accounts
            val accounts = listOf(
                AccountEntity(name = "Cash", iconEmoji = "💵", colorHex = "#FFF3E0", balanceCents = 0),
                AccountEntity(name = "Bank", iconEmoji = "🏦", colorHex = "#E3F2FD", balanceCents = 0),
                AccountEntity(name = "Credit Card", iconEmoji = "💳", colorHex = "#F3E5F5", balanceCents = 0)
            )
            accounts.forEach { accountDao.insertAccount(it) }
        }
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS bills (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                amountCents INTEGER NOT NULL,
                dueDate INTEGER NOT NULL,
                category TEXT NOT NULL,
                isPaid INTEGER NOT NULL,
                repeatType TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                lastPaidAt INTEGER
            )
        """)
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                amountCents INTEGER NOT NULL,
                category TEXT NOT NULL,
                dateMillis INTEGER NOT NULL,
                note TEXT,
                createdAt INTEGER NOT NULL
            )
        """)
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create categories and accounts tables
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                iconEmoji TEXT NOT NULL,
                colorHex TEXT NOT NULL,
                isDefault INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                balanceCents INTEGER NOT NULL,
                iconEmoji TEXT NOT NULL,
                colorHex TEXT NOT NULL
            )
        """)

        // 2. We must ensure default category/account exist for foreign keys
        db.execSQL("INSERT OR IGNORE INTO categories (id, name, iconEmoji, colorHex, isDefault) VALUES (1, 'Food', '🍛', '#E8F5E9', 1)")
        db.execSQL("INSERT OR IGNORE INTO accounts (id, name, balanceCents, iconEmoji, colorHex) VALUES (1, 'Cash', 0, '💵', '#FFF3E0')")

        // 3. Migrate expenses table
        // Create new table
        db.execSQL("""
            CREATE TABLE expenses_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                amountCents INTEGER NOT NULL,
                categoryId INTEGER NOT NULL,
                accountId INTEGER NOT NULL,
                note TEXT,
                dateMillis INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)
        
        // Copy data (Map old category strings to ID 1, assume all old records were EXPENSE, account ID 1)
        db.execSQL("""
            INSERT INTO expenses_new (id, type, amountCents, categoryId, accountId, note, dateMillis, createdAt)
            SELECT id, 'EXPENSE', amountCents, 1, 1, note, dateMillis, createdAt FROM expenses
        """)
        
        // Drop old table and rename new one
        db.execSQL("DROP TABLE expenses")
        db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
        
        // Re-create indices
        db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_dateMillis ON expenses(dateMillis)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_categoryId ON expenses(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_accountId ON expenses(accountId)")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create credit_cards table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS credit_cards (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                cardName TEXT NOT NULL,
                bankName TEXT NOT NULL,
                lastFourDigits TEXT NOT NULL,
                billingDay INTEGER NOT NULL,
                dueDay INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """)

        // 2. Create credit_card_bills table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS credit_card_bills (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                cardId INTEGER NOT NULL,
                billingCycleStartDate INTEGER NOT NULL,
                billingCycleEndDate INTEGER NOT NULL,
                totalAmountCents INTEGER NOT NULL,
                isPaid INTEGER NOT NULL,
                paidAt INTEGER,
                FOREIGN KEY(cardId) REFERENCES credit_cards(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_credit_card_bills_cardId ON credit_card_bills(cardId)")

        // 3. Add creditCardId to expenses
        db.execSQL("""
            CREATE TABLE expenses_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                amountCents INTEGER NOT NULL,
                categoryId INTEGER NOT NULL,
                accountId INTEGER NOT NULL,
                creditCardId INTEGER,
                note TEXT,
                dateMillis INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(creditCardId) REFERENCES credit_cards(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """)

        db.execSQL("""
            INSERT INTO expenses_new (id, type, amountCents, categoryId, accountId, creditCardId, note, dateMillis, createdAt)
            SELECT id, type, amountCents, categoryId, accountId, NULL, note, dateMillis, createdAt FROM expenses
        """)

        db.execSQL("DROP TABLE expenses")
        db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_dateMillis ON expenses(dateMillis)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_categoryId ON expenses(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_accountId ON expenses(accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_creditCardId ON expenses(creditCardId)")
    }
}
