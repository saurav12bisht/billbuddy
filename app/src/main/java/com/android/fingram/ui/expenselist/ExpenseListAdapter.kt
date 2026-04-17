package com.android.fingram.ui.expenselist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.fingram.databinding.ItemExpenseBinding
import com.android.fingram.domain.model.Expense
import com.android.fingram.ui.common.util.CurrencyFormatter

class ExpenseListAdapter(
    private val onClick: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseListAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClick(getItem(position))
                }
            }
        }

        fun bind(expense: Expense) {
            binding.tvCategoryName.text = expense.categoryId.toString()
            binding.tvAmount.text = CurrencyFormatter.formatUsdCents(expense.amountCents)
            if (!expense.note.isNullOrBlank()) {
                binding.tvNote.visibility = View.VISIBLE
                binding.tvNote.text = expense.note
            } else {
                binding.tvNote.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemExpenseBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Expense>() {
            override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
                return oldItem == newItem
            }
        }
    }
}
