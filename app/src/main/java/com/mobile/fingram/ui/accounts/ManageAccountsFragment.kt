package com.mobile.fingram.ui.accounts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mobile.fingram.data.local.entity.AccountEntity
import com.mobile.fingram.databinding.FragmentManageAccountsBinding
import com.mobile.fingram.databinding.ItemManageAccountBinding
import com.mobile.fingram.ui.common.BaseFragment
import com.mobile.fingram.ui.common.util.CurrencyFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManageAccountsFragment : BaseFragment<FragmentManageAccountsBinding>(FragmentManageAccountsBinding::inflate) {

    private val viewModel: ManageAccountsViewModel by viewModels()
    private lateinit var adapter: ManageAccountsAdapter

    override fun onInit() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        setupRecyclerView()
        binding.fabAddAccount.setOnClickListener {
            AddAccountBottomSheet.newInstance().show(childFragmentManager, AddAccountBottomSheet.TAG)
        }
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = ManageAccountsAdapter(
            onEdit = { account ->
                AddAccountBottomSheet.newInstance(account.id)
                    .show(childFragmentManager, AddAccountBottomSheet.TAG)
            },
            onDelete = { account ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete account?")
                    .setMessage("This will remove ${account.name} from your linked accounts.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteAccount(account)
                    }
                    .show()
            }
        )
        binding.rvAccounts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAccounts.adapter = adapter
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.accounts.collect { accounts ->
                    adapter.submitList(accounts)
                    binding.tvEmptyState.visibility = if (accounts.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}

private class ManageAccountsAdapter(
    private val onEdit: (AccountEntity) -> Unit,
    private val onDelete: (AccountEntity) -> Unit
) : ListAdapter<AccountEntity, ManageAccountsAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemManageAccountBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            account: AccountEntity,
            onEdit: (AccountEntity) -> Unit,
            onDelete: (AccountEntity) -> Unit
        ) {
            binding.tvAccountEmoji.text = account.iconEmoji
            binding.tvAccountName.text = account.name
            binding.tvAccountType.text = "Bank account"
            binding.tvBalance.text = CurrencyFormatter.formatUsdCents(account.balanceCents)
            binding.btnEdit.setOnClickListener { onEdit(account) }
            binding.btnDelete.setOnClickListener { onDelete(account) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemManageAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onEdit, onDelete)
    }

    object DiffCallback : DiffUtil.ItemCallback<AccountEntity>() {
        override fun areItemsTheSame(oldItem: AccountEntity, newItem: AccountEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AccountEntity, newItem: AccountEntity) = oldItem == newItem
    }
}
