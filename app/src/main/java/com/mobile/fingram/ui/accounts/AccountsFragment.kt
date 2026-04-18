package com.mobile.fingram.ui.accounts

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mobile.fingram.R
import com.mobile.fingram.databinding.FragmentAccountsBinding
import com.mobile.fingram.databinding.ItemAccountBinding
import com.mobile.fingram.databinding.ItemWalletCreditCardBinding
import com.mobile.fingram.databinding.ItemWalletGroupHeaderBinding
import com.mobile.fingram.ui.common.BaseFragment
import com.mobile.fingram.ui.common.util.CurrencyFormatter
import com.mobile.fingram.ui.creditcards.CreditCardUiModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountsFragment : BaseFragment<FragmentAccountsBinding>(FragmentAccountsBinding::inflate) {

    private val viewModel: AccountsViewModel by viewModels()
    private lateinit var adapter: WalletAdapter
    private var summaryStripAnimated = false

    override fun onInit() {
        setupAdapter()
        observeState()
        setupFab()
        animateHeroCardEntrance()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupAdapter() {
        adapter = WalletAdapter(
            onHeaderClick = { viewModel.toggleGroup(it) },
            onCardClick   = { cardModel ->
                val bundle = Bundle().apply { putLong("cardId", cardModel.card.id) }
                findNavController().navigate(R.id.action_accounts_to_creditCardDetail, bundle)
            }
        )
        binding.rvWallet.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AccountsFragment.adapter
            itemAnimator?.apply {
                addDuration    = 220
                removeDuration = 180
                changeDuration = 160
                moveDuration   = 200
            }
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            animateFabPress(binding.fabAdd)
            AddAccountBottomSheet.newInstance()
                .show(childFragmentManager, AddAccountBottomSheet.TAG)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Observe
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.walletItems.collect { items ->
                        adapter.submitList(items)
                        val count = items.filterIsInstance<WalletListItem.Account>().size
                        val label = "$count linked source${if (count != 1) "s" else ""}"
                        binding.tvNumAccounts.text = label
                        binding.tvHeroAccountsBadge?.text = label
                    }
                }

                launch {
                    viewModel.totalBalance.collect { total ->
                        val formatted = CurrencyFormatter.formatUsdCents(total)
                        animateBalanceUpdate(binding.tvTotalBalance, formatted, total)
                    }
                }

                launch {
                    viewModel.cashTotal.collect { total ->
                        binding.tvSummaryCash?.text = CurrencyFormatter.formatUsdCents(total)
                        if (!summaryStripAnimated) {
                            summaryStripAnimated = true
                            animateSummaryStrip()
                        }
                    }
                }
                launch {
                    viewModel.bankTotal.collect { total ->
                        binding.tvSummaryBank?.text = CurrencyFormatter.formatUsdCents(total)
                    }
                }
                launch {
                    viewModel.creditTotal.collect { total ->
                        binding.tvSummaryCredit?.text = CurrencyFormatter.formatUsdCents(-total)
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Animations
    // ─────────────────────────────────────────────────────────────────────────

    private fun animateHeroCardEntrance() {
        binding.appBarLayout.apply {
            alpha        = 0f
            translationY = 60f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setStartDelay(60)
                .setInterpolator(DecelerateInterpolator(1.8f))
                .start()
        }
        binding.fabAdd.apply {
            scaleX = 0f
            scaleY = 0f
            alpha  = 0f
            animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(320)
                .setStartDelay(260)
                .setInterpolator(OvershootInterpolator(2.2f))
                .start()
        }
    }

    /**
     * Correct industry-standard approach for negative balance on a coloured hero card.
     *
     * ✅ Balance text = ALWAYS WHITE. White on the blue gradient has perfect contrast
     *    (WCAG AA compliant). Never change this colour — it is the right choice.
     *
     * ✅ Negative state = communicated via a separate small badge BELOW the number
     *    (tvNegativeIndicator). This follows the same pattern used by CRED, Google Pay,
     *    PhonePe — the hero number shows the value, a secondary element shows its status.
     *
     * ❌ What NOT to do:
     *    - Don't colour the balance text red → low contrast on blue, clashing hues
     *    - Don't put a dark scrim box behind it → looks like a broken overlay
     */
    private fun animateBalanceUpdate(view: TextView, newText: String, balanceCents: Long) {
        if (view.text == newText) return

        val isNegative = balanceCents < 0

        // Balance text is always white — do not change this
        view.setTextColor(Color.WHITE)

        // Show / hide the negative badge beneath the balance
        binding.tvNegativeIndicator?.let { badge ->
            if (isNegative) {
                if (badge.visibility != View.VISIBLE) {
                    badge.visibility = View.VISIBLE
                    badge.alpha      = 0f
                    badge.scaleX     = 0.8f
                    badge.scaleY     = 0.8f
                    badge.animate()
                        .alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(280)
                        .setInterpolator(OvershootInterpolator(1.8f))
                        .start()
                }
            } else {
                if (badge.visibility == View.VISIBLE) {
                    badge.animate()
                        .alpha(0f).scaleX(0.8f).scaleY(0.8f)
                        .setDuration(200)
                        .withEndAction { badge.visibility = View.GONE }
                        .start()
                }
            }
        }

        // Cross-fade the balance number
        view.animate()
            .alpha(0f)
            .setDuration(130)
            .withEndAction {
                view.text = newText
                view.animate().alpha(1f).setDuration(180).start()
            }
            .start()

        // Shake when transitioning to negative
        if (isNegative) {
            ObjectAnimator.ofFloat(
                view, View.TRANSLATION_X,
                0f, -10f, 10f, -8f, 8f, -4f, 4f, 0f
            ).apply {
                duration     = 430
                startDelay   = 250
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }

    /** Stagger-animates the three summary pills up from below. */
    private fun animateSummaryStrip() {
        listOf(binding.llSummaryCash, binding.llSummaryBank, binding.llSummaryCredit)
            .forEachIndexed { i, pill ->
                pill ?: return@forEachIndexed
                pill.alpha        = 0f
                pill.translationY = 26f
                pill.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setStartDelay(420L + i * 70L)
                    .setInterpolator(DecelerateInterpolator(1.4f))
                    .start()
            }
    }

    /** Tactile bounce on FAB press. */
    private fun animateFabPress(view: View) {
        view.animate()
            .scaleX(0.88f).scaleY(0.88f).setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(220)
                    .setInterpolator(OvershootInterpolator(2.5f))
                    .start()
            }.start()
    }
}

// ── Wallet Adapter ──────────────────────────────────────────────────────────

class WalletAdapter(
    private val onHeaderClick: (WalletGroupType) -> Unit,
    private val onCardClick:   (CreditCardUiModel) -> Unit
) : ListAdapter<WalletListItem, RecyclerView.ViewHolder>(Diff) {

    companion object {
        private const val TYPE_HEADER  = 0
        private const val TYPE_ACCOUNT = 1
        private const val TYPE_CARD    = 2
        private const val ANIM_DURATION_MS       = 260L
        private const val ANIM_TRANSLATION_Y_DP  = 28f
        private const val ANIM_STAGGER_MS        = 50L
        private const val ANIM_MAX_STAGGER_ITEMS = 12
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is WalletListItem.Header  -> TYPE_HEADER
        is WalletListItem.Account -> TYPE_ACCOUNT
        is WalletListItem.Card    -> TYPE_CARD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER  -> HeaderVH(ItemWalletGroupHeaderBinding.inflate(inflater, parent, false))
            TYPE_ACCOUNT -> AccountVH(ItemAccountBinding.inflate(inflater, parent, false))
            TYPE_CARD    -> CardVH(ItemWalletCreditCardBinding.inflate(inflater, parent, false))
            else         -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HeaderVH  -> holder.bind(item as WalletListItem.Header)
            is AccountVH -> { holder.bind((item as WalletListItem.Account).entity); animateItem(holder.itemView, position) }
            is CardVH    -> { holder.bind((item as WalletListItem.Card).uiModel);   animateItem(holder.itemView, position) }
        }
    }

    private fun animateItem(view: View, position: Int) {
        val ty     = ANIM_TRANSLATION_Y_DP * view.context.resources.displayMetrics.density
        val delay  = minOf(position, ANIM_MAX_STAGGER_ITEMS) * ANIM_STAGGER_MS
        view.alpha        = 0f
        view.translationY = ty
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply { duration = ANIM_DURATION_MS; startDelay = delay },
                ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, ty, 0f).apply { duration = ANIM_DURATION_MS; startDelay = delay; interpolator = DecelerateInterpolator(1.5f) }
            )
            start()
        }
    }

    inner class HeaderVH(private val b: ItemWalletGroupHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(h: WalletListItem.Header) {
            b.tvHeaderTitle.text = when (h.type) {
                WalletGroupType.CASH         -> "Cash"
                WalletGroupType.BANKS        -> "Bank Accounts"
                WalletGroupType.CREDIT_CARDS -> "Credit Cards"
            }
            val amt = if (h.type == WalletGroupType.CREDIT_CARDS) -h.amountCents else h.amountCents
            b.tvHeaderTotal.text = CurrencyFormatter.formatUsdCents(amt)
            b.ivArrow.animate()
                .rotation(if (h.isExpanded) 90f else 0f)
                .setDuration(280).setInterpolator(OvershootInterpolator(1.5f)).start()
            b.root.setOnClickListener {
                b.root.animate().scaleX(0.97f).scaleY(0.97f).setDuration(70)
                    .withEndAction { b.root.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(OvershootInterpolator(2f)).start() }.start()
                onHeaderClick(h.type)
            }
        }
    }

    class AccountVH(private val b: ItemAccountBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(a: com.mobile.fingram.data.local.entity.AccountEntity) {
            b.tvAccountEmoji.text = a.iconEmoji
            b.vAccountIconBg.background.setTint(Color.parseColor(a.colorHex))
            b.tvAccountName.text  = a.name
            b.tvBalance.text      = CurrencyFormatter.formatUsdCents(a.balanceCents)
            b.tvBalance.setTextColor(
                ContextCompat.getColor(b.root.context,
                    if (a.balanceCents >= 0) R.color.text_primary else R.color.expense_red)
            )
            b.tvTransactionCount.visibility = View.GONE
            b.root.setOnClickListener {
                b.root.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                    .withEndAction { b.root.animate().scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(OvershootInterpolator(2f)).start() }.start()
            }
        }
    }

    inner class CardVH(private val b: ItemWalletCreditCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(uiModel: CreditCardUiModel) {
            val card = uiModel.card
            b.tvWalletBankName.text    = card.bankName.uppercase()
            b.tvWalletCardName.text    = card.cardName
            b.tvWalletCardNumber.text  = "•••• ${card.lastFourDigits}"
            b.tvWalletOutstanding.text = CurrencyFormatter.formatUsdCents(uiModel.outstandingAmountCents)
            b.tvWalletCycleSpend.text  = "Cycle spend: ${CurrencyFormatter.formatUsdCents(uiModel.currentCycleSpendCents)}"
            b.root.setOnClickListener { onCardClick(uiModel) }
        }
    }

    object Diff : DiffUtil.ItemCallback<WalletListItem>() {
        override fun areItemsTheSame(o: WalletListItem, n: WalletListItem) = when {
            o is WalletListItem.Header  && n is WalletListItem.Header  -> o.type == n.type
            o is WalletListItem.Account && n is WalletListItem.Account -> o.entity.id == n.entity.id
            o is WalletListItem.Card    && n is WalletListItem.Card    -> o.uiModel.card.id == n.uiModel.card.id
            else -> false
        }
        override fun areContentsTheSame(o: WalletListItem, n: WalletListItem) = o == n
    }
}