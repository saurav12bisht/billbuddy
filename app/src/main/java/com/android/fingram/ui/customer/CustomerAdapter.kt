package com.android.fingram.ui.customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.fingram.data.local.dao.CustomerWithBalance
import com.android.fingram.databinding.ItemCustomerBinding
import com.android.fingram.ui.common.util.CurrencyFormatter

class CustomerAdapter(
    private val onClick: (CustomerWithBalance) -> Unit
) : ListAdapter<CustomerWithBalance, CustomerAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val b: ItemCustomerBinding,
        private val onClick: (CustomerWithBalance) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: CustomerWithBalance) {
            b.tvName.text = item.name
            b.tvPhone.text = com.android.fingram.ui.common.util.DateFormatter.formatPhone(item.phone)
            b.tvBalance.text = CurrencyFormatter.formatPaiseToRupee(item.balance)
            b.tvBalance.setTextColor(
                b.root.context.getColor(
                    when {
                        item.balance > 0 -> com.android.fingram.R.color.accent_red
                        item.balance < 0 -> com.android.fingram.R.color.paid_success
                        else -> com.android.fingram.R.color.text_secondary
                    }
                )
            )
            b.tvAvatar.text = item.name.firstOrNull()?.uppercase() ?: "?"
            b.root.setOnClickListener { onClick(item) }
        }
    }

    object Diff : DiffUtil.ItemCallback<CustomerWithBalance>() {
        override fun areItemsTheSame(a: CustomerWithBalance, b: CustomerWithBalance) = a.id == b.id
        override fun areContentsTheSame(a: CustomerWithBalance, b: CustomerWithBalance) = a == b
    }
}
