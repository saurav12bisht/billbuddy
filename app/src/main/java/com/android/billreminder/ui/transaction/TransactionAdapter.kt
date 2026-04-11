package com.android.billreminder.ui.transaction

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.billreminder.R
import com.android.billreminder.data.local.entity.TransactionEntity
import com.android.billreminder.databinding.ItemTransactionLegacyBinding
import com.android.billreminder.ui.common.util.CurrencyFormatter
import com.android.billreminder.ui.common.util.DateFormatter

class TransactionAdapter(
    private val onTransactionClick: (TransactionEntity) -> Unit
) : ListAdapter<TransactionEntity, TransactionAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemTransactionLegacyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener { onTransactionClick(item) }
    }

    class VH(private val b: ItemTransactionLegacyBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(t: TransactionEntity) {
            b.tvDate.text = DateFormatter.formatDateTime(t.date, false)
            b.tvType.text = if (t.type == "CREDIT_GIVEN") b.root.context.getString(R.string.udhaar_diya) else b.root.context.getString(R.string.paisa_mila)
            b.tvAmount.text = CurrencyFormatter.formatPaiseToRupee(t.amountPaise)
            b.tvAmount.setTextColor(b.root.context.getColor(if (t.type == "CREDIT_GIVEN") R.color.expense_red else R.color.income_blue))
            b.tvNote.text = t.note.ifEmpty { "-" }
        }
    }

    object Diff : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(a: TransactionEntity, b: TransactionEntity) = a.id == b.id
        override fun areContentsTheSame(a: TransactionEntity, b: TransactionEntity) = a == b
    }
}
