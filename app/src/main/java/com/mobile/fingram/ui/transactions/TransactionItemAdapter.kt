package com.mobile.fingram.ui.transactions

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mobile.fingram.R
import com.mobile.fingram.databinding.ItemTransactionBinding
import com.mobile.fingram.ui.common.util.CurrencyFormatter

class TransactionItemAdapter(
    private val onClick: (TransactionListItem.TransactionRow) -> Unit
) : ListAdapter<TransactionListItem.TransactionRow, TransactionItemAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: TransactionListItem.TransactionRow, onClick: (TransactionListItem.TransactionRow) -> Unit) {
            val item = row.item
            binding.tvCategoryEmoji.text = item.category.iconEmoji
            binding.vCategoryBg.background.setTint(Color.parseColor(item.category.colorHex))
            
            binding.tvCategoryName.text = item.category.name
            binding.tvNote.text = item.expense.note ?: ""
            binding.tvAccountName.text = "Account" // For now, static until we fetch account name
            
            val color = if (item.expense.type == "INCOME") R.color.income_blue else R.color.expense_red
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.root.context, color))
            binding.tvAmount.text = CurrencyFormatter.formatUsdCents(item.expense.amountCents)
            
            binding.root.setOnClickListener { onClick(row) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    object DiffCallback : DiffUtil.ItemCallback<TransactionListItem.TransactionRow>() {
        override fun areItemsTheSame(oldItem: TransactionListItem.TransactionRow, newItem: TransactionListItem.TransactionRow) = 
            oldItem.item.expense.id == newItem.item.expense.id
        override fun areContentsTheSame(oldItem: TransactionListItem.TransactionRow, newItem: TransactionListItem.TransactionRow) = 
            oldItem == newItem
    }
}
