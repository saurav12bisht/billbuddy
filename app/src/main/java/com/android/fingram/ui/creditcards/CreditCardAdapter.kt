package com.android.fingram.ui.creditcards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.fingram.databinding.ItemCreditCardBinding
import com.android.fingram.ui.common.util.CurrencyFormatter

class CreditCardAdapter(
    private val onCardClick: (CreditCardUiModel) -> Unit,
    private val onEditClick: (CreditCardUiModel) -> Unit,
    private val onDeleteClick: (CreditCardUiModel) -> Unit
) : ListAdapter<CreditCardUiModel, CreditCardAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemCreditCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemCreditCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uiModel = getItem(position)
        val card = uiModel.card

        holder.binding.apply {
            tvBankName.text = card.bankName.uppercase()
            tvCardName.text = card.cardName
            tvCardNumber.text = "**** **** **** ${card.lastFourDigits}"
            tvCycleTotal.text = CurrencyFormatter.formatUsdCents(uiModel.currentCycleSpendCents)
            tvDueDay.text = "Due: ${formatOrdinal(uiModel.nextDueDay)}"
            root.setOnClickListener { onCardClick(uiModel) }
            btnEdit.setOnClickListener { onEditClick(uiModel) }
            btnDelete.setOnClickListener { onDeleteClick(uiModel) }
        }
    }

    private fun formatOrdinal(d: Int) = "$d" + if (d in 11..13) "th" else when (d % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CreditCardUiModel>() {
        override fun areItemsTheSame(old: CreditCardUiModel, new: CreditCardUiModel) = old.card.id == new.card.id
        override fun areContentsTheSame(old: CreditCardUiModel, new: CreditCardUiModel) = old == new
    }
}
