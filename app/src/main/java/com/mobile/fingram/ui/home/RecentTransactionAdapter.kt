package com.mobile.fingram.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mobile.fingram.R
import com.mobile.fingram.data.local.dao.RecentTransactionItem
import com.mobile.fingram.databinding.ItemRecentTransactionBinding
import com.mobile.fingram.ui.common.util.CurrencyFormatter
import com.mobile.fingram.ui.common.util.DateFormatter

class RecentTransactionAdapter(
    private val onClick: (customerId: Int) -> Unit
) : ListAdapter<RecentTransactionItem, RecentTransactionAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemRecentTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val b: ItemRecentTransactionBinding,
        private val onClick: (customerId: Int) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: RecentTransactionItem) {
            b.root.setOnClickListener { onClick(item.customerId) }
            b.tvCustomerName.text = item.customerName
            b.tvDate.text = DateFormatter.formatDateTime(item.date, false)
            val typeLabel = if (item.type == "CREDIT_GIVEN") b.root.context.getString(R.string.udhaar_diya) else b.root.context.getString(R.string.paisa_mila)
            b.tvTypeAmount.text = "$typeLabel â€” ${CurrencyFormatter.formatPaiseToRupee(item.amountPaise)}"
            b.tvTypeAmount.setTextColor(
                b.root.context.getColor(if (item.type == "CREDIT_GIVEN") R.color.accent_red else R.color.bottom_nav_colors)
            )
            b.tvNote.text = item.note.ifEmpty { "-" }
            b.tvAvatar.text = item.customerName.firstOrNull()?.uppercase() ?: "?"
        }
    }

    object Diff : DiffUtil.ItemCallback<RecentTransactionItem>() {
        override fun areItemsTheSame(a: RecentTransactionItem, b: RecentTransactionItem) = a.transactionId == b.transactionId
        override fun areContentsTheSame(a: RecentTransactionItem, b: RecentTransactionItem) = a == b
    }
}
