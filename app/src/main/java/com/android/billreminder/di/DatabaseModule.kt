package com.android.billreminder.di

import android.content.Context
import com.android.billreminder.data.local.VyapaarDatabase
import com.android.billreminder.data.local.dao.BillDao
import com.android.billreminder.data.local.dao.CustomerDao
import com.android.billreminder.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VyapaarDatabase =
        VyapaarDatabase.getInstance(context)

    @Provides
    fun provideCustomerDao(db: VyapaarDatabase): CustomerDao = db.customerDao()

    @Provides
    fun provideTransactionDao(db: VyapaarDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideBillDao(db: VyapaarDatabase): BillDao = db.billDao()

    @Provides
    fun provideExpenseDao(db: VyapaarDatabase): com.android.billreminder.data.local.dao.ExpenseDao = db.expenseDao()

    @Provides
    fun provideAccountDao(db: VyapaarDatabase): com.android.billreminder.data.local.dao.AccountDao = db.accountDao()
}
