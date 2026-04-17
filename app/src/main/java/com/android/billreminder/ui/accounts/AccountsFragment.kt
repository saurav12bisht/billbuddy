package com.android.billreminder.ui.accounts

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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
import com.android.billreminder.R
import com.android.billreminder.databinding.FragmentAccountsBinding
import com.android.billreminder.databinding.ItemAccountBinding
import com.android.billreminder.databinding.ItemWalletCreditCardBinding
import com.android.billreminder.databinding.ItemWalletGroupHeaderBinding
import com.android.billreminder.ui.common.BaseFragment
import com.android.billreminder.ui.common.util.CurrencyFormatter
import com.android.billreminder.ui.creditcards.CreditCardUiModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class AccountsFragment : BaseFragment<FragmentAccountsBinding>(FragmentAccountsBinding::inflate) {

    private val viewModel: AccountsViewModel by viewModels()
    private lateinit var adapter: WalletAdapter

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
            adapter        = this@AccountsFragment.adapter
            // Smooth item change animations via default ItemAnimator
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
            // Bounce the button on tap
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

                        val accountsCount = items.filterIsInstance<WalletListItem.Account>().size
                        val label = "$accountsCount linked source${if (accountsCount != 1) "s" else ""}"
                        binding.tvNumAccounts.text = label

                        // Also update the hero badge if it exists in the layout
                        binding.tvHeroAccountsBadge?.text = label
                    }
                }

                launch {
                    viewModel.totalBalance.collect { total ->
                        val formatted = CurrencyFormatter.formatUsdCents(total)
                        animateBalanceUpdate(binding.tvTotalBalance, formatted)
                        val color = if (total >= 0) R.color.white else R.color.expense_red
                        binding.tvTotalBalance.setTextColor(
                            ContextCompat.getColor(requireContext(), color)
                        )
                    }
                }

                // Summary strip totals (cash / bank / credit)
                launch {
                    viewModel.cashTotal.collect { total ->
                        binding.tvSummaryCash?.text = CurrencyFormatter.formatUsdCents(total)
                        animateSummaryStrip()
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

    /** Hero card slides up + fades in on first load. */
    private fun animateHeroCardEntrance() {
        val card = binding.appBarLayout
        card.alpha        = 0f
        card.translationY = 60f
        card.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(420)
            .setStartDelay(80)
            .setInterpolator(DecelerateInterpolator(1.8f))
            .start()
    }

    /** Softly cross-fades the balance label when the value changes. */
    private fun animateBalanceUpdate(view: android.widget.TextView, newText: String) {
        if (view.text == newText) return
        view.animate()
            .alpha(0f)
            .setDuration(120)
            .withEndAction {
                view.text = newText
                view.animate().alpha(1f).setDuration(160).start()
            }
            .start()
    }

    /** Stagger-animates the three summary pills in from below. */
    private fun animateSummaryStrip() {
        val pills = listOf(binding.llSummaryCash, binding.llSummaryBank, binding.llSummaryCredit)
        pills.forEachIndexed { index, pill ->
            pill ?: return@forEachIndexed
            pill.alpha        = 0f
            pill.translationY = 24f
            pill.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(280)
                .setStartDelay(index * 60L)
                .setInterpolator(DecelerateInterpolator(1.4f))
                .start()
        }
    }

    /** Brief scale bounce on FAB press for tactile feedback. */
    private fun animateFabPress(view: View) {
        view.animate()
            .scaleX(0.88f)
            .scaleY(0.88f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator(2.5f))
                    .start()
            }
            .start()
    }
}

// ── Unified Wallet Adapter ──────────────────────────────────────────────────

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
            else         -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HeaderVH  -> holder.bind(item as WalletListItem.Header)
            is AccountVH -> {
                holder.bind((item as WalletListItem.Account).entity)
                animateItem(holder.itemView, position)
            }
            is CardVH    -> {
                holder.bind((item as WalletListItem.Card).uiModel)
                animateItem(holder.itemView, position)
            }
        }
    }

    // ── Item entrance animation (slide-up + fade) ─────────────────────────

    private fun animateItem(view: View, position: Int) {
        val density      = view.context.resources.displayMetrics.density
        val translationY = ANIM_TRANSLATION_Y_DP * density
        val stagger      = minOf(position, ANIM_MAX_STAGGER_ITEMS) * ANIM_STAGGER_MS

        view.alpha        = 0f
        view.translationY = translationY

        val fadeIn  = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
            duration   = ANIM_DURATION_MS
            startDelay = stagger
        }
        val slideUp = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, translationY, 0f).apply {
            duration     = ANIM_DURATION_MS
            startDelay   = stagger
            interpolator = DecelerateInterpolator(1.5f)
        }

        AnimatorSet().apply {
            playTogether(fadeIn, slideUp)
            start()
        }
    }

    // ── ViewHolders ───────────────────────────────────────────────────────

    inner class HeaderVH(
        private val b: ItemWalletGroupHeaderBinding
    ) : RecyclerView.ViewHolder(b.root) {

        fun bind(h: WalletListItem.Header) {
            // Title + emoji
            val (title, emoji) = when (h.type) {
                WalletGroupType.CASH         -> Pair("Cash", "💵")
                WalletGroupType.BANKS        -> Pair("Bank Accounts", "🏦")
                WalletGroupType.CREDIT_CARDS -> Pair("Credit Cards", "💳")
            }
            b.tvHeaderTitle.text = title

            // Show total amount on header
            val displayAmount = if (h.type == WalletGroupType.CREDIT_CARDS) -h.amountCents else h.amountCents
            b.tvHeaderTotal.text = CurrencyFormatter.formatUsdCents(displayAmount)

            // Smooth arrow rotation — points down when expanded, right when collapsed
            val targetRotation = if (h.isExpanded) 90f else 0f
            b.ivArrow.animate()
                .rotation(targetRotation)
                .setDuration(280)
                .setInterpolator(OvershootInterpolator(1.5f))
                .start()

            // Subtle press feedback + toggle
            b.root.setOnClickListener {
                animateHeaderPress(b.root)
                onHeaderClick(h.type)
            }
        }

        private fun animateHeaderPress(view: View) {
            view.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(70)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(180)
                        .setInterpolator(OvershootInterpolator(2f))
                        .start()
                }
                .start()
        }
    }

    class AccountVH(
        private val b: ItemAccountBinding
    ) : RecyclerView.ViewHolder(b.root) {

        fun bind(a: com.android.billreminder.data.local.entity.AccountEntity) {
            b.tvAccountEmoji.text = a.iconEmoji
            b.vAccountIconBg.background.setTint(Color.parseColor(a.colorHex))
            b.tvAccountName.text  = a.name
            b.tvBalance.text      = CurrencyFormatter.formatUsdCents(a.balanceCents)

            val color = if (a.balanceCents >= 0) R.color.text_primary else R.color.expense_red
            b.tvBalance.setTextColor(ContextCompat.getColor(b.root.context, color))

            b.tvTransactionCount.visibility = View.GONE

            // Ripple-friendly press scale
            b.root.setOnClickListener {
                b.root.animate()
                    .scaleX(0.97f).scaleY(0.97f).setDuration(80)
                    .withEndAction {
                        b.root.animate().scaleX(1f).scaleY(1f)
                            .setDuration(160)
                            .setInterpolator(OvershootInterpolator(2f))
                            .start()
                    }.start()
            }
        }
    }

    inner class CardVH(
        private val b: ItemWalletCreditCardBinding
    ) : RecyclerView.ViewHolder(b.root) {

        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        fun bind(uiModel: CreditCardUiModel) {
            val card = uiModel.card
            b.tvWalletBankName.text    = card.bankName.uppercase()
            b.tvWalletCardName.text    = card.cardName
            b.tvWalletCardNumber.text  = "•••• ${card.lastFourDigits}"
            b.tvWalletOutstanding.text = currencyFormat.format(uiModel.outstandingAmountCents / 100.0)
            b.tvWalletCycleSpend.text  =
                "Cycle spend: " + currencyFormat.format(uiModel.currentCycleSpendCents / 100.0)

            b.root.setOnClickListener { onCardClick(uiModel) }
        }
    }

    // ── DiffCallback ─────────────────────────────────────────────────────

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
