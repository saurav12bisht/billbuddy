package com.android.billreminder.ui.bills

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.billreminder.R
import com.android.billreminder.databinding.ItemBillBinding
import com.android.billreminder.domain.model.Bill
import com.android.billreminder.ui.common.util.CurrencyFormatter
import com.android.billreminder.ui.common.util.DateFormatter

class BillListAdapter(
    private val onClick: (Bill) -> Unit
) : ListAdapter<Bill, BillListAdapter.BillViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val binding = ItemBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BillViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        val delay = position * 60L
        holder.itemView.translationX = 80f
        holder.itemView.alpha = 0f
        holder.itemView.animate()
            .translationX(0f).alpha(1f)
            .setStartDelay(delay).setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
            
        holder.bind(getItem(position))
    }

    class BillViewHolder(
        private val binding: ItemBillBinding,
        private val onClick: (Bill) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Bill) {
            binding.root.setOnClickListener { view ->
                view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    onClick(item)
                }.start()
            }
            binding.tvBillTitle.text = item.title
            binding.tvBillAmount.text = CurrencyFormatter.formatUsdCents(item.amountCents)
            binding.tvDateText.text = "Due " + DateFormatter.formatMonthDayYear(item.dueDate)
            
            // Dummy SMS check
            if (item.title.contains("sms", ignoreCase = true)) {
                binding.tvSmsBadge.visibility = View.VISIBLE
            } else {
                binding.tvSmsBadge.visibility = View.GONE
            }
            
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            
            val diffMs = item.dueDate - now
            val diffDays = (diffMs / dayMs).toInt()
            
            val accentColor: Int
            val statusText: String
            
            if (item.isPaid) {
                accentColor = R.color.paid_success
                statusText = "PAID"
            } else if (diffMs < 0) {
                accentColor = R.color.urgent_error
                statusText = "OVERDUE"
            } else if (diffDays <= 3) {
                accentColor = R.color.primary_blue
                statusText = "IN $diffDays DAYS"
            } else if (diffDays <= 7) {
                accentColor = R.color.primary_blue
                statusText = "IN $diffDays DAYS"
            } else {
                accentColor = android.R.color.white
                statusText = "IN $diffDays DAYS"
            }
            
            val resolvedColor = ContextCompat.getColor(binding.root.context, accentColor)
            binding.vBorderAccent.setBackgroundColor(resolvedColor)
            binding.tvBillStatusLabel.text = statusText
            binding.tvBillStatusLabel.setTextColor(resolvedColor)
            
            // set icon tint as well
            binding.ivCategoryIcon.setColorFilter(resolvedColor)
            
            // Pulse logic for SMS badge specifically if new or recently added (just pulsing the left border for demonstration)
            if (binding.tvSmsBadge.visibility == View.VISIBLE) {
                android.animation.ObjectAnimator.ofArgb(binding.vBorderAccent, "backgroundColor", 
                    ContextCompat.getColor(binding.root.context, android.R.color.white), resolvedColor).apply {
                    duration = 800
                    repeatCount = 3
                    repeatMode = android.animation.ValueAnimator.REVERSE
                    // evaluate as argb
                    setEvaluator(android.animation.ArgbEvaluator())
                    start()
                }
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Bill>() {
        override fun areItemsTheSame(oldItem: Bill, newItem: Bill): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Bill, newItem: Bill): Boolean = oldItem == newItem
    }
}
