package com.android.billreminder.ui.accounts

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.android.billreminder.R
import com.android.billreminder.data.local.entity.AccountEntity
import com.android.billreminder.databinding.FragmentAccountsBinding
import com.android.billreminder.databinding.FragmentWalletTabBinding
import com.android.billreminder.databinding.ItemAccountBinding
import com.android.billreminder.databinding.ItemWalletCreditCardBinding
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

    override fun onInit() {
        setupTabs()
        observeState()
    }

    private fun setupTabs() {
        val adapter = WalletPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = true

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "🏦  Accounts"
                1 -> "💳  Credit Cards"
                else -> ""
            }
        }.attach()

        // Update FAB content/action based on current tab
        binding.viewPager.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> {
                        binding.fabAdd.text = "Add Account"
                        binding.fabAdd.setOnClickListener { 
                            AddAccountBottomSheet.newInstance().show(childFragmentManager, AddAccountBottomSheet.TAG)
                        }
                    }
                    1 -> {
                        binding.fabAdd.text = "Add Card"
                        binding.fabAdd.setOnClickListener {
                            findNavController().navigate(
                                R.id.action_accounts_to_creditCardList
                            )
                        }
                    }
                }
            }
        })
        binding.fabAdd.text = "Add Account"
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.accounts.collect { accounts ->
                    val total = accounts.sumOf { it.balanceCents }
                    binding.tvTotalBalance.text = CurrencyFormatter.formatUsdCents(total)
                    binding.tvNumAccounts.text = getString(R.string.num_accounts, accounts.size)
                }
            }
        }
    }

    // ── ViewPager2 Adapter ──────────────────────────────────────────────────

    class WalletPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount() = 2
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> AccountsTabFragment()
            1 -> CreditCardsTabFragment()
            else -> AccountsTabFragment()
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Tab 1: Accounts
// ──────────────────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class AccountsTabFragment : Fragment(R.layout.fragment_wallet_tab) {

    private val viewModel: AccountsViewModel by viewModels()
    private lateinit var adapter: AccountsTabAdapter

    override fun onViewCreated(
        view: android.view.View,
        savedInstanceState: android.os.Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWalletTabBinding.bind(view)
        adapter = AccountsTabAdapter()
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.accounts.collect { adapter.submitList(it) }
            }
        }
    }
}

class AccountsTabAdapter : ListAdapter<AccountEntity, AccountsTabAdapter.VH>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(private val b: ItemAccountBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(a: AccountEntity) {
            b.tvAccountEmoji.text = a.iconEmoji
            b.vAccountIconBg.background.setTint(Color.parseColor(a.colorHex))
            b.tvAccountName.text = a.name
            b.tvTransactionCount.text = b.root.context.getString(R.string.num_transactions, 0)
            val color = if (a.balanceCents >= 0) R.color.income_blue else R.color.expense_red
            b.tvBalance.setTextColor(androidx.core.content.ContextCompat.getColor(b.root.context, color))
            b.tvBalance.text = CurrencyFormatter.formatUsdCents(a.balanceCents)
        }
    }

    object Diff : DiffUtil.ItemCallback<AccountEntity>() {
        override fun areItemsTheSame(o: AccountEntity, n: AccountEntity) = o.id == n.id
        override fun areContentsTheSame(o: AccountEntity, n: AccountEntity) = o == n
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Tab 2: Credit Cards
// ──────────────────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class CreditCardsTabFragment : Fragment(R.layout.fragment_wallet_tab) {

    private val viewModel: WalletCreditCardsViewModel by viewModels()
    private lateinit var adapter: WalletCardsAdapter

    override fun onViewCreated(
        view: android.view.View,
        savedInstanceState: android.os.Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWalletTabBinding.bind(view)

        adapter = WalletCardsAdapter { cardUiModel ->
            // Navigate to card detail screen
            val navController = try {
                androidx.navigation.fragment.NavHostFragment.findNavController(this)
            } catch (e: Exception) { return@WalletCardsAdapter }

            navController.navigate(
                R.id.action_accounts_to_creditCardDetail,
                bundleOf("cardId" to cardUiModel.card.id)
            )
        }
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.creditCards.collect { cards ->
                    adapter.submitList(cards)
                }
            }
        }
    }
}

class WalletCardsAdapter(
    private val onClick: (CreditCardUiModel) -> Unit
) : ListAdapter<CreditCardUiModel, WalletCardsAdapter.VH>(Diff) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemWalletCreditCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemWalletCreditCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(uiModel: CreditCardUiModel) {
            val card = uiModel.card
            b.tvWalletBankName.text = card.bankName.uppercase()
            b.tvWalletCardName.text = card.cardName
            b.tvWalletCardNumber.text = "•••• ${card.lastFourDigits}"
            b.tvWalletCycleSpend.text = currencyFormat.format(uiModel.currentCycleSpendCents / 100.0)
            b.tvWalletOutstanding.text = currencyFormat.format(uiModel.outstandingAmountCents / 100.0)
            b.tvWalletDueDay.text = formatOrdinal(card.dueDay)
            b.root.setOnClickListener { onClick(uiModel) }
        }

        private fun formatOrdinal(d: Int) = "$d" + if (d in 11..13) "th" else when (d % 10) {
            1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th"
        }
    }

    object Diff : DiffUtil.ItemCallback<CreditCardUiModel>() {
        override fun areItemsTheSame(o: CreditCardUiModel, n: CreditCardUiModel) = o.card.id == n.card.id
        override fun areContentsTheSame(o: CreditCardUiModel, n: CreditCardUiModel) = o == n
    }
}
