package com.android.billreminder.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.billreminder.R
import com.android.billreminder.databinding.ItemDayHeaderBinding
import com.android.billreminder.ui.common.util.CurrencyFormatter
import java.time.format.DateTimeFormatter
import java.util.Locale

class DayHeaderAdapter : ListAdapter<TransactionListItem.DayHeader, DayHeaderAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemDayHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: TransactionListItem.DayHeader) {
            binding.tvDayNumber.text = header.date.dayOfMonth.toString()
            binding.tvDayBadge.text = header.date.dayOfWeek.name.take(3)
            
            val badgeColor = when (header.date.dayOfWeek.value) {
                6, 7 -> R.color.day_badge_sat
                else -> R.color.day_badge_weekday
            }
            binding.tvDayBadge.background.setTint(ContextCompat.getColor(binding.root.context, badgeColor))
            
            binding.tvDateLabel.text = header.date.format(DateTimeFormatter.ofPattern("MM.yyyy"))
            binding.tvDailyIncome.text = CurrencyFormatter.formatUsdCents(header.incomeTotal)
            binding.tvDailyExpense.text = CurrencyFormatter.formatUsdCents(header.expenseTotal)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemDayHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<TransactionListItem.DayHeader>() {
        override fun areItemsTheSame(oldItem: TransactionListItem.DayHeader, newItem: TransactionListItem.DayHeader) = oldItem.date == newItem.date
        override fun areContentsTheSame(oldItem: TransactionListItem.DayHeader, newItem: TransactionListItem.DayHeader) = oldItem == newItem
    }
}
