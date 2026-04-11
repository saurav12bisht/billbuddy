package com.android.billreminder.ui.accounts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.billreminder.R
import com.android.billreminder.databinding.FragmentAccountsBinding
import com.android.billreminder.databinding.ItemAccountBinding
import com.android.billreminder.data.local.entity.AccountEntity
import com.android.billreminder.ui.common.BaseFragment
import com.android.billreminder.ui.common.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountsFragment : BaseFragment<FragmentAccountsBinding>(FragmentAccountsBinding::inflate) {

    private val viewModel: AccountsViewModel by viewModels()
    private lateinit var adapter: AccountsAdapter

    override fun onInit() {
        setupRecyclerView()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = AccountsAdapter()
        binding.rvAccounts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAccounts.adapter = adapter
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.accounts.collect { accounts ->
                        adapter.submitList(accounts)
                        val total = accounts.sumOf { it.balanceCents }
                        binding.tvTotalBalance.text = CurrencyFormatter.formatUsdCents(total)
                        binding.tvNumAccounts.text = getString(R.string.num_accounts, accounts.size)
                    }
                }
            }
        }
    }

    class AccountsAdapter : ListAdapter<AccountEntity, AccountsAdapter.ViewHolder>(DiffCallback) {
        class ViewHolder(private val binding: ItemAccountBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(account: AccountEntity) {
                binding.tvAccountEmoji.text = account.iconEmoji
                binding.vAccountIconBg.background.setTint(Color.parseColor(account.colorHex))
                binding.tvAccountName.text = account.name
                binding.tvTransactionCount.text = binding.root.context.getString(R.string.num_transactions, 0)
                
                val color = if (account.balanceCents >= 0) R.color.income_blue else R.color.expense_red
                binding.tvBalance.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, color))
                binding.tvBalance.text = CurrencyFormatter.formatUsdCents(account.balanceCents)
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
        object DiffCallback : DiffUtil.ItemCallback<AccountEntity>() {
            override fun areItemsTheSame(old: AccountEntity, new: AccountEntity) = old.id == new.id
            override fun areContentsTheSame(old: AccountEntity, new: AccountEntity) = old == new
        }
    }
}
