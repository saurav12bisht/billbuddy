package com.android.billreminder.ui.transactions

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.billreminder.R
import com.android.billreminder.databinding.ItemDayHeaderBinding
import com.android.billreminder.databinding.ItemTransactionBinding
import com.android.billreminder.data.local.entity.ExpenseWithCategory
import com.android.billreminder.ui.common.util.CurrencyFormatter
import java.time.format.DateTimeFormatter

class TransactionsAdapter(
    private val onRowClick: (ExpenseWithCategory) -> Unit,
    private val onInfoClick: (ExpenseWithCategory) -> Unit = {}
) : ListAdapter<TransactionListItem, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1

        // Animation constants
        private const val ANIM_DURATION_MS = 280L
        private const val ANIM_TRANSLATION_Y_DP = 32f
        private const val ANIM_STAGGER_MS = 45L
        private const val ANIM_MAX_STAGGER_ITEMS = 15
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ViewType
    // ─────────────────────────────────────────────────────────────────────────

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransactionListItem.DayHeader -> TYPE_HEADER
            is TransactionListItem.TransactionRow -> TYPE_ROW
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemDayHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> RowViewHolder(
                ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bind
    // ─────────────────────────────────────────────────────────────────────────

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TransactionListItem.DayHeader -> {
                (holder as HeaderViewHolder).bind(item)
            }
            is TransactionListItem.TransactionRow -> {
                // Check whether this row is immediately after a DayHeader so we
                // can collapse the extra top margin that would otherwise create
                // a double-gap (header bottom-padding + row top-margin).
                val isFirstAfterHeader = position > 0 &&
                        getItem(position - 1) is TransactionListItem.DayHeader

                (holder as RowViewHolder).bind(
                    item       = item.item,
                    onClick    = onRowClick,
                    onInfoClick = onInfoClick,
                    isFirstAfterHeader = isFirstAfterHeader,
                    position   = position
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HeaderViewHolder
    // ─────────────────────────────────────────────────────────────────────────

    class HeaderViewHolder(
        private val binding: ItemDayHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(header: TransactionListItem.DayHeader) {
            binding.tvDayNumber.text = header.date.dayOfMonth.toString()
            binding.tvDayBadge.text  = header.date.dayOfWeek.name.take(3)

            val badgeColor = when (header.date.dayOfWeek.value) {
                6, 7 -> R.color.day_badge_sat
                else -> R.color.day_badge_weekday
            }
            binding.tvDayBadge.background.setTint(
                ContextCompat.getColor(binding.root.context, badgeColor)
            )

            binding.tvDateLabel.text = header.date.format(
                DateTimeFormatter.ofPattern("MM.yyyy")
            )

            binding.tvDailyIncome.text =
                "$ " + CurrencyFormatter.formatUsdCents(header.incomeTotal).removePrefix("$")
            binding.tvDailyExpense.text =
                "$ " + CurrencyFormatter.formatUsdCents(header.expenseTotal).removePrefix("$")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RowViewHolder
    // ─────────────────────────────────────────────────────────────────────────

    class RowViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: ExpenseWithCategory,
            onClick: (ExpenseWithCategory) -> Unit,
            onInfoClick: (ExpenseWithCategory) -> Unit,
            isFirstAfterHeader: Boolean,
            position: Int
        ) {
            // ── Fix extra top margin on first item after a day header ─────────
            val lp = binding.root.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null) {
                lp.topMargin = if (isFirstAfterHeader) 0
                else binding.root.context.resources
                    .getDimensionPixelSize(R.dimen.transaction_item_spacing)
                binding.root.layoutParams = lp
            }

            // ── Content ───────────────────────────────────────────────────────
            binding.tvCategoryEmoji.text = item.category.iconEmoji
            binding.vCategoryBg.background.setTint(Color.parseColor(item.category.colorHex))

            binding.tvCategoryName.text = item.category.name
            binding.tvNote.text         = item.expense.note ?: ""

            // Payment Mode label
            val paymentModeLabel = when {
                item.creditCard != null -> {
                    "Credit Card (${item.creditCard.bankName} ••••${item.creditCard.lastFourDigits})"
                }
                item.account.accountType == com.android.billreminder.data.local.entity.AccountType.CASH -> "CASH"
                item.account.accountType == com.android.billreminder.data.local.entity.AccountType.BANK -> "Bank Account"
                else -> item.account.name
            }
            binding.tvAccountName.text = paymentModeLabel

            // Amount colour + text
            val amountColor = if (item.expense.type == "INCOME") R.color.income_blue
            else R.color.expense_red
            binding.tvAmount.setTextColor(
                ContextCompat.getColor(binding.root.context, amountColor)
            )
            binding.tvAmount.text = CurrencyFormatter.formatUsdCents(item.expense.amountCents)

            // Info icon for Credit transactions
            val isCreditTx = item.expense.transactionType == "CREDIT"
            binding.ivInfoAccounting.visibility = if (isCreditTx) View.VISIBLE else View.GONE
            binding.ivInfoAccounting.setOnClickListener { onInfoClick(item) }

            binding.root.setOnClickListener { onClick(item) }

            // ── Slide-up + fade-in entrance animation ─────────────────────────
            animateItem(binding.root, position)
        }

        // ── Animation helper ─────────────────────────────────────────────────

        private fun animateItem(view: View, position: Int) {
            // Use bindingAdapterPosition so we never animate with stale -1 index
            val safePosition = bindingAdapterPosition
                .takeIf { it != RecyclerView.NO_ID.toInt() } ?: position

            // Cap the stagger so items far down the list don't wait too long
            val staggerIndex = minOf(safePosition, ANIM_MAX_STAGGER_ITEMS)
            val startDelay   = staggerIndex * ANIM_STAGGER_MS

            // Reset state before animating (important on RecyclerView recycling)
            view.alpha       = 0f
            view.translationY = ANIM_TRANSLATION_Y_DP

            val fadeIn  = ObjectAnimator.ofFloat(view, View.ALPHA,        0f, 1f)
            val slideUp = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y,
                ANIM_TRANSLATION_Y_DP, 0f)

            AnimatorSet().apply {
                playTogether(fadeIn, slideUp)
                duration         = ANIM_DURATION_MS
                this.startDelay  = startDelay
                interpolator     = DecelerateInterpolator(1.6f)
                start()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DiffCallback
    // ─────────────────────────────────────────────────────────────────────────

    object DiffCallback : DiffUtil.ItemCallback<TransactionListItem>() {

        override fun areItemsTheSame(
            oldItem: TransactionListItem,
            newItem: TransactionListItem
        ): Boolean = when {
            oldItem is TransactionListItem.DayHeader &&
                    newItem is TransactionListItem.DayHeader ->
                oldItem.date == newItem.date

            oldItem is TransactionListItem.TransactionRow &&
                    newItem is TransactionListItem.TransactionRow ->
                oldItem.item.expense.id == newItem.item.expense.id

            else -> false
        }

        override fun areContentsTheSame(
            oldItem: TransactionListItem,
            newItem: TransactionListItem
        ): Boolean = oldItem == newItem
    }
}