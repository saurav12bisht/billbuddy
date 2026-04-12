package com.android.billreminder.ui.transactions

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.billreminder.R
import com.android.billreminder.databinding.ItemDayHeaderBinding
import com.android.billreminder.databinding.ItemTransactionBinding
import com.android.billreminder.data.local.entity.ExpenseWithCategory
import com.android.billreminder.ui.common.util.CurrencyFormatter
import com.android.billreminder.ui.common.util.ListAnimations
import java.time.format.DateTimeFormatter

class TransactionsAdapter(
    private val onRowClick: (ExpenseWithCategory) -> Unit
) : ListAdapter<TransactionListItem, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransactionListItem.DayHeader -> TYPE_HEADER
            is TransactionListItem.TransactionRow -> TYPE_ROW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(ItemDayHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> RowViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TransactionListItem.DayHeader -> (holder as HeaderViewHolder).bind(item)
            is TransactionListItem.TransactionRow -> (holder as RowViewHolder).bind(item.item, onRowClick)
        }
    }

    class HeaderViewHolder(private val binding: ItemDayHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: TransactionListItem.DayHeader) {
            binding.tvDayNumber.text = header.date.dayOfMonth.toString()
            binding.tvDayBadge.text = header.date.dayOfWeek.name.take(3)
            
            val badgeColor = when (header.date.dayOfWeek.value) {
                6, 7 -> R.color.day_badge_sat
                else -> R.color.day_badge_weekday
            }
            binding.tvDayBadge.background.setTint(ContextCompat.getColor(binding.root.context, badgeColor))
            
            binding.tvDateLabel.text = header.date.format(DateTimeFormatter.ofPattern("MM.yyyy"))
            
            binding.tvDailyIncome.text = "$ " + CurrencyFormatter.formatUsdCents(header.incomeTotal).removePrefix("$")
            binding.tvDailyExpense.text = "$ " + CurrencyFormatter.formatUsdCents(header.expenseTotal).removePrefix("$")
        }
    }

    class RowViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ExpenseWithCategory, onClick: (ExpenseWithCategory) -> Unit) {
            ListAnimations.animateItem(binding.root, adapterPosition)
            binding.tvCategoryEmoji.text = item.category.iconEmoji
            binding.vCategoryBg.background.setTint(Color.parseColor(item.category.colorHex))
            
            binding.tvCategoryName.text = item.category.name
            binding.tvNote.text = item.expense.note ?: ""
            
            // Show Account Name + Card Name if it's a credit spend
            val accountInfo = if (item.creditCard != null) {
                "${item.account.name} (${item.creditCard.bankName} ••${item.creditCard.lastFourDigits})"
            } else {
                item.account.name
            }
            binding.tvAccountName.text = accountInfo
            
            val color = if (item.expense.type == "INCOME") R.color.income_blue else R.color.expense_red
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.root.context, color))
            binding.tvAmount.text = CurrencyFormatter.formatUsdCents(item.expense.amountCents)
            
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<TransactionListItem>() {
        override fun areItemsTheSame(oldItem: TransactionListItem, newItem: TransactionListItem): Boolean {
            return if (oldItem is TransactionListItem.DayHeader && newItem is TransactionListItem.DayHeader) {
                oldItem.date == newItem.date
            } else if (oldItem is TransactionListItem.TransactionRow && newItem is TransactionListItem.TransactionRow) {
                oldItem.item.expense.id == newItem.item.expense.id
            } else false
        }

        override fun areContentsTheSame(oldItem: TransactionListItem, newItem: TransactionListItem) = oldItem == newItem
    }
}
