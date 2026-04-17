package com.android.billreminder.ui.creditcards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.billreminder.databinding.ItemCreditCardBillBinding
import com.android.billreminder.domain.model.CreditCardBill
import com.android.billreminder.ui.common.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreditCardBillAdapter(
    private val onPayClick: (CreditCardBill) -> Unit
) : ListAdapter<CreditCardBill, CreditCardBillAdapter.BillViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val binding = ItemCreditCardBillBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BillViewHolder(
        private val binding: ItemCreditCardBillBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bill: CreditCardBill) {
            val start = dateFormat.format(Date(bill.billingCycleStartDate))
            val end = dateFormat.format(Date(bill.billingCycleEndDate))
            val due = dateFormat.format(Date(bill.dueDateMillis))
            binding.tvCycleLabel.text = "$start â€“ $end"
            binding.tvDueDate.text = "Due: $due"
            binding.tvBillAmount.text = CurrencyFormatter.formatUsdCents(bill.totalAmountCents)

            if (bill.isPaid) {
                binding.tvStatusBadge.text = "PAID"
                binding.tvStatusBadge.setTextColor(binding.root.context.getColor(com.android.billreminder.R.color.paid_success))
                binding.btnPayNow.isVisible = false
                binding.tvPaidFrom.isVisible = true
                binding.tvPaidFrom.text = "Paid âœ…"
            } else {
                val now = System.currentTimeMillis()
                val isOverdue = bill.dueDateMillis < now
                binding.tvStatusBadge.text = if (isOverdue) "OVERDUE" else "UNPAID"
                binding.tvStatusBadge.setTextColor(
                    binding.root.context.getColor(com.android.billreminder.R.color.error_red)
                )
                binding.btnPayNow.isVisible = bill.totalAmountCents > 0
                binding.tvPaidFrom.isVisible = false
                binding.btnPayNow.setOnClickListener { onPayClick(bill) }
            }
        }
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<CreditCardBill>() {
            override fun areItemsTheSame(old: CreditCardBill, new: CreditCardBill) = old.id == new.id
            override fun areContentsTheSame(old: CreditCardBill, new: CreditCardBill) = old == new
        }
    }
}
