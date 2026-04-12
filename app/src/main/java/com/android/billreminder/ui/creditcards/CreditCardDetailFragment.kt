package com.android.billreminder.ui.creditcards

import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billreminder.databinding.FragmentCreditCardDetailBinding
import com.android.billreminder.domain.model.CreditCardBill
import com.android.billreminder.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class CreditCardDetailFragment : BaseFragment<FragmentCreditCardDetailBinding>(
    FragmentCreditCardDetailBinding::inflate
) {

    private val viewModel: CreditCardDetailViewModel by viewModels()
    private lateinit var billAdapter: CreditCardBillAdapter
    private lateinit var spendsAdapter: com.android.billreminder.ui.transactions.TransactionsAdapter
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onInit() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, 0)
            insets
        }

        billAdapter = CreditCardBillAdapter { bill ->
            showPayBillDialog(bill)
        }
        binding.rvBills.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBills.adapter = billAdapter

        spendsAdapter = com.android.billreminder.ui.transactions.TransactionsAdapter {
            // Handle spend click (edit/delete)
        }
        binding.rvRecentSpends.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentSpends.adapter = spendsAdapter
        binding.rvRecentSpends.isNestedScrollingEnabled = false

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    binding.progressBar.isVisible = state.isLoading

                    state.card?.let { card ->
                        binding.toolbar.title = "${card.bankName} ••••${card.lastFourDigits}"
                    }

                    binding.tvCurrentCycleSpend.text =
                        currencyFormat.format(state.currentCycleSpend / 100.0)
                    binding.tvOutstanding.text =
                        currencyFormat.format(state.outstandingAmount / 100.0)

                    // Map ExpenseWithCategory to TransactionRow for the adapter
                    val spendItems = state.recentSpends.map { expenseWithCategory: com.android.billreminder.data.local.entity.ExpenseWithCategory ->
                        com.android.billreminder.ui.transactions.TransactionListItem.TransactionRow(expenseWithCategory)
                    }
                    spendsAdapter.submitList(spendItems)
                    binding.tvNoSpends.isVisible = !state.isLoading && state.recentSpends.isEmpty()

                    billAdapter.submitList(state.bills)
                    binding.tvNoBills.isVisible = !state.isLoading && state.bills.isEmpty()

                    state.successMessage?.let { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        viewModel.clearMessage()
                    }
                    state.error?.let { err ->
                        Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                        viewModel.clearMessage()
                    }
                }
            }
        }
    }

    /**
     * Shows an account picker so the user can choose which bank account to pay from.
     */
    private fun showPayBillDialog(bill: CreditCardBill) {
        val accounts = viewModel.uiState.value.accounts
        if (accounts.isEmpty()) {
            Toast.makeText(requireContext(), "No bank accounts found. Add one in Settings.", Toast.LENGTH_SHORT).show()
            return
        }

        val accountNames = accounts.map { "${it.iconEmoji}  ${it.name}" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Pay Bill of ${currencyFormat.format(bill.totalAmountCents / 100.0)}")
            .setMessage("Select the account to pay from:")
            .setItems(accountNames) { _, index ->
                viewModel.payBill(bill, accounts[index].id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
