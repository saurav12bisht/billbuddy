package com.android.billreminder.ui.accounts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    }

    private fun setupAdapter() {
        adapter = WalletAdapter(
            onHeaderClick = { viewModel.toggleGroup(it) },
            onCardClick = { cardModel -> 
                val bundle = android.os.Bundle().apply {
                    putLong("cardId", cardModel.card.id)
                }
                findNavController().navigate(R.id.action_accounts_to_creditCardDetail, bundle)
            }
        )
        binding.rvWallet.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AccountsFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            // Simplified: Default to Add Account for now
            AddAccountBottomSheet.newInstance().show(childFragmentManager, AddAccountBottomSheet.TAG)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.walletItems.collect { items ->
                        adapter.submitList(items)
                    }
                }

                launch {
                    viewModel.totalBalance.collect { total ->
                        binding.tvTotalBalance.text = CurrencyFormatter.formatUsdCents(total)
                        val color = if (total >= 0) R.color.white else R.color.expense_red
                        binding.tvTotalBalance.setTextColor(ContextCompat.getColor(requireContext(), color))
                    }
                }

                launch {
                    viewModel.walletItems.collect { items ->
                        val accountsCount = items.filterIsInstance<WalletListItem.Account>().size
                        binding.tvNumAccounts.text = getString(R.string.num_accounts, accountsCount)
                    }
                }
            }
        }
    }
}

// ── Unified Wallet Adapter ──────────────────────────────────────────────────

class WalletAdapter(
    private val onHeaderClick: (WalletGroupType) -> Unit,
    private val onCardClick: (CreditCardUiModel) -> Unit
) : ListAdapter<WalletListItem, RecyclerView.ViewHolder>(Diff) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ACCOUNT = 1
        private const val TYPE_CARD = 2
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is WalletListItem.Header -> TYPE_HEADER
        is WalletListItem.Account -> TYPE_ACCOUNT
        is WalletListItem.Card -> TYPE_CARD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(ItemWalletGroupHeaderBinding.inflate(inflater, parent, false))
            TYPE_ACCOUNT -> AccountVH(ItemAccountBinding.inflate(inflater, parent, false))
            TYPE_CARD -> CardVH(ItemWalletCreditCardBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HeaderVH -> holder.bind(item as WalletListItem.Header)
            is AccountVH -> holder.bind((item as WalletListItem.Account).entity)
            is CardVH -> holder.bind((item as WalletListItem.Card).uiModel)
        }
    }

    inner class HeaderVH(private val b: ItemWalletGroupHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(h: WalletListItem.Header) {
            b.tvHeaderTitle.text = when (h.type) {
                WalletGroupType.CASH -> "Cash"
                WalletGroupType.BANKS -> "Bank Accounts"
                WalletGroupType.CREDIT_CARDS -> "Credit Cards"
            }
            b.tvHeaderTotal.text = CurrencyFormatter.formatUsdCents(h.amountCents)
            
            // Animation for the arrow
            val rotation = if (h.isExpanded) 90f else -90f
            b.ivArrow.animate()
                .rotation(rotation)
                .setDuration(300)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .start()
            
            b.root.setOnClickListener { onHeaderClick(h.type) }
        }
    }

    class AccountVH(private val b: ItemAccountBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(a: com.android.billreminder.data.local.entity.AccountEntity) {
            b.tvAccountEmoji.text = a.iconEmoji
            b.vAccountIconBg.background.setTint(Color.parseColor(a.colorHex))
            b.tvAccountName.text = a.name
            b.tvBalance.text = CurrencyFormatter.formatUsdCents(a.balanceCents)
            
            val color = if (a.balanceCents >= 0) R.color.text_primary else R.color.expense_red
            b.tvBalance.setTextColor(ContextCompat.getColor(b.root.context, color))
            
            b.tvTransactionCount.visibility = View.GONE // Hide for cleaner unified look
        }
    }

    inner class CardVH(private val b: ItemWalletCreditCardBinding) : RecyclerView.ViewHolder(b.root) {
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        
        fun bind(uiModel: CreditCardUiModel) {
            val card = uiModel.card
            b.tvWalletBankName.text = card.bankName.uppercase()
            b.tvWalletCardName.text = card.cardName
            b.tvWalletCardNumber.text = "•••• ${card.lastFourDigits}"
            b.tvWalletOutstanding.text = currencyFormat.format(uiModel.outstandingAmountCents / 100.0)
            
            // Clean up: hide fields that are too detailed for the unified overview if necessary
            // or keep them for 'Professional Look'
            b.tvWalletCycleSpend.text = "Cycle Spend: " + currencyFormat.format(uiModel.currentCycleSpendCents / 100.0)
            
            b.root.setOnClickListener { onCardClick(uiModel) }
        }
    }

    object Diff : DiffUtil.ItemCallback<WalletListItem>() {
        override fun areItemsTheSame(o: WalletListItem, n: WalletListItem): Boolean {
            return when {
                o is WalletListItem.Header && n is WalletListItem.Header -> o.type == n.type
                o is WalletListItem.Account && n is WalletListItem.Account -> o.entity.id == n.entity.id
                o is WalletListItem.Card && n is WalletListItem.Card -> o.uiModel.card.id == n.uiModel.card.id
                else -> false
            }
        }
        override fun areContentsTheSame(o: WalletListItem, n: WalletListItem) = o == n
    }
}
