package com.android.billreminder.ui.creditcards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.billreminder.databinding.ItemCreditCardBinding
import com.android.billreminder.ui.common.util.CurrencyFormatter

class CreditCardAdapter(
    private val onCardClick: (CreditCardUiModel) -> Unit
) : ListAdapter<CreditCardUiModel, CreditCardAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemCreditCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemCreditCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uiModel = getItem(position)
        val card = uiModel.card
        
        holder.binding.apply {
            tvBankName.text = card.bankName.uppercase()
            tvCardName.text = card.cardName
            tvCardNumber.text = "**** **** **** ${card.lastFourDigits}"
            tvCycleTotal.text = CurrencyFormatter.formatUsdCents(uiModel.currentCycleSpendCents) // Uses rupee formatter internally or I should check
            tvDueDay.text = "${formatOrdinal(uiModel.nextDueDay)} of month"
            
            root.setOnClickListener { onCardClick(uiModel) }
        }
    }

    private fun formatOrdinal(d: Int): String {
        return "$d" + if (d in 11..13) "th" else when (d % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CreditCardUiModel>() {
        override fun areItemsTheSame(oldItem: CreditCardUiModel, newItem: CreditCardUiModel): Boolean {
            return oldItem.card.id == newItem.card.id
        }

        override fun areContentsTheSame(oldItem: CreditCardUiModel, newItem: CreditCardUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
