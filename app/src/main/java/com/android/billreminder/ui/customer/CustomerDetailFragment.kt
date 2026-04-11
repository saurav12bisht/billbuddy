package com.android.billreminder.ui.customer

import android.content.Intent
import android.net.Uri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billreminder.R
import com.android.billreminder.databinding.FragmentCustomerDetailBinding
import com.android.billreminder.ui.common.BaseFragment
import com.android.billreminder.ui.common.util.CurrencyFormatter
import com.android.billreminder.ui.common.util.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle

@AndroidEntryPoint
class CustomerDetailFragment : BaseFragment<FragmentCustomerDetailBinding>(FragmentCustomerDetailBinding::inflate) {

    private val viewModel: CustomerViewModel by viewModels()
    private val transactionViewModel: com.android.billreminder.ui.transaction.TransactionViewModel by viewModels()
    private var customerId: Int = -1

    override fun onInit() {
        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        customerId = arguments?.getInt("customerId", -1) ?: -1
        if (customerId == -1) {
            findNavController().navigateUp()
            return
        }
        
        setupToolbar()
        
//        binding.fab.setOnClickListener { findNavController().navigate(R.id.addTransactionFragment, Bundle().apply { putInt("customerId", customerId) }) }

        val adapter = com.android.billreminder.ui.transaction.TransactionAdapter { tx ->
            showTransactionOptions(tx)
        }
        binding.transactionList.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    val customer = viewModel.getCustomerById(customerId)
                    customer?.let { c ->
                        binding.toolbar.title = c.name
                        binding.tvName.text = c.name
                        binding.tvPhone.text = DateFormatter.formatPhone(c.phone)
                        binding.tvAvatar.text = c.name.firstOrNull()?.uppercase() ?: "?"
                    }
                }
                launch {
                    viewModel.transactionsForCustomer(customerId).collectLatest { list ->
                        val customer = viewModel.getCustomerById(customerId) // optimization: fetch once or observe
                        adapter.submitList(list)
                        val balance = list.fold(0L) { acc, t ->
                            acc + when (t.type) {
                                "CREDIT_GIVEN" -> t.amountPaise
                                "PAYMENT_RECEIVED" -> -t.amountPaise
                                else -> 0L
                            }
                        }
                        val opening = (customer?.openingBalance ?: 0L) * if (customer?.openingBalanceType == "I_OWE") -1 else 1
                        val total = balance + opening
                        binding.tvBalance.text = CurrencyFormatter.formatPaiseToRupee(total)
                        binding.tvBalance.setTextColor(requireContext().getColor(
                            when {
                                total > 0 -> R.color.accent_red
//                                total < 0 -> R.color.color_paid
                                else -> R.color.text_secondary
                            }
                        ))
                    }
                }
                
                // Observe Transaction Deletion
                launch {
                    transactionViewModel.uiState.collectLatest { state ->
                         when (state) {
                            is UiState.Success -> {
                                if (state.msg == "Deleted") {
                                    android.widget.Toast.makeText(requireContext(), "Transaction Deleted", android.widget.Toast.LENGTH_SHORT).show()
                                    // List auto-updates via Flow
                                }
                            }
                            is UiState.Error -> android.widget.Toast.makeText(requireContext(), state.msg, android.widget.Toast.LENGTH_SHORT).show()
                            else -> {}
                        }
                    }
                }

                // Observe Customer Deletion
                launch {
                    viewModel.uiState.collectLatest { state ->
                        if (state is UiState.Success && state.msg == "Customer Deleted") {
                             android.widget.Toast.makeText(requireContext(), "Customer Deleted", android.widget.Toast.LENGTH_SHORT).show()
                             findNavController().navigateUp()
                        }
                    }
                }
            }
        }

        binding.btnCall.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val c = viewModel.getCustomerById(customerId) ?: return@launch
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91${c.phone}")))
            }
        }
        binding.btnWhatsApp.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val c = viewModel.getCustomerById(customerId) ?: return@launch
                val bal = viewModel.getBalance(customerId)
                val msg = getString(R.string.whatsapp_msg_template, c.name, CurrencyFormatter.formatPaiseToRupee(bal))
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91${c.phone}?text=${Uri.encode(msg)}")))
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.inflateMenu(R.menu.menu_customer_detail)
        binding.toolbar.overflowIcon?.setTint(android.graphics.Color.WHITE)
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_edit_customer -> {
//                    findNavController().navigate(
//                        R.id.addCustomerFragment,
//                        Bundle().apply { putInt("customerId", customerId) }
//                    )
                    true
                }
                R.id.action_delete_customer -> {
                    showDeleteCustomerDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showDeleteCustomerDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Customer?")
            .setMessage("This will hide the customer and all their transactions.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.softDelete(customerId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTransactionOptions(tx: com.android.billreminder.data.local.entity.TransactionEntity) {
        val options = arrayOf("Edit Transaction", "Delete Transaction")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Transaction Actions")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Edit
//                        findNavController().navigate(
//                            R.id.addTransactionFragment,
//                            Bundle().apply {
//                                putInt("customerId", customerId)
//                                putLong("transactionId", tx.id)
//                            }
//                        )
                    }
                    1 -> { // Delete
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete Transaction?")
                            .setMessage("Are you sure you want to delete this transaction?")
                            .setPositiveButton("Delete") { _, _ ->
                                transactionViewModel.deleteTransaction(tx.id)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .show()
    }
}
