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
import com.android.billreminder.ui.common.util.PreferenceManager
import javax.inject.Inject
import kotlinx.coroutines.launch
import com.android.billreminder.R
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class CreditCardDetailFragment : BaseFragment<FragmentCreditCardDetailBinding>(
    FragmentCreditCardDetailBinding::inflate
) {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val viewModel: CreditCardDetailViewModel by viewModels()
    private lateinit var billAdapter: CreditCardBillAdapter
    private lateinit var spendsAdapter: com.android.billreminder.ui.transactions.TransactionsAdapter
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onInit() {

        billAdapter = CreditCardBillAdapter { bill ->
            showPayBillDialog(bill)
        }
        binding.rvBills.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBills.adapter = billAdapter

        spendsAdapter = com.android.billreminder.ui.transactions.TransactionsAdapter(
            onRowClick = {
                // Handle spend click (edit/delete)
            },
            onInfoClick = {
                showAccountingEducationDialog()
            }
        )
        binding.rvRecentSpends.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentSpends.adapter = spendsAdapter
        binding.rvRecentSpends.isNestedScrollingEnabled = false

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.ivInfoAccounting.setOnClickListener { showAccountingEducationDialog() }

        observeState()
    }

    private fun showAccountingEducationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Credit Card Spending")
            .setMessage("This transaction/spending is done using a Credit Card. It will not reflect in your monthly balance or total spend until the bill is paid.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    binding.progressBar.isVisible = state.isLoading

                    state.card?.let { card ->
                        binding.toolbar.title = "${card.bankName} ••••${card.lastFourDigits}"
                        
                        // Card Visual
                        binding.tvCardBankName.text = card.bankName.uppercase()
                        binding.tvCardName.text = card.cardName
                        binding.tvCardNumber.text = "••••  ••••  ••••  ${card.lastFourDigits}"
                        
                        // Billing Info
                        binding.tvBillingDay.text = "Starts on ${card.billingDay}${getDayOfMonthSuffix(card.billingDay)}"
                        binding.tvDueDay.text = "Due on ${card.dueDay}${getDayOfMonthSuffix(card.dueDay)}"
                    }

                    binding.tvCurrentCycleSpend.text =
                        currencyFormat.format(state.currentCycleSpend / 100.0)
                    binding.tvBilledDue.text =
                        currencyFormat.format(state.billedDueAmount / 100.0)
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
     * Shows a dialog to enter the payment amount, followed by an account picker.
     */
    private fun showPayBillDialog(bill: CreditCardBill) {
        val accounts = viewModel.uiState.value.accounts
        if (accounts.isEmpty()) {
            Toast.makeText(requireContext(), "No source accounts found. Please add a Bank or Cash account first.", Toast.LENGTH_SHORT).show()
            return
        }

        val remainingCents = bill.totalAmountCents - bill.paidAmountCents
        val dialogView = layoutInflater.inflate(R.layout.dialog_pay_bill, null)
        val etAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAmount)
        val tvRemaining = dialogView.findViewById<android.widget.TextView>(R.id.tvRemainingAmount)

        tvRemaining.text = currencyFormat.format(remainingCents / 100.0)
        etAmount.setText((remainingCents / 100.0).toString())

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Record Payment")
            .setView(dialogView)
            .setPositiveButton("Next") { dialog, _ ->
                val amountStr = etAmount.text.toString()
                val amountDouble = amountStr.toDoubleOrNull() ?: 0.0
                val amountCents = (amountDouble * 100).toLong()

                if (amountCents <= 0) {
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                showAccountPicker(bill, amountCents, accounts)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAccountPicker(bill: CreditCardBill, amountCents: Long, accounts: List<com.android.billreminder.data.local.entity.AccountEntity>) {
        val accountNames = accounts.map { "${it.iconEmoji}  ${it.name}" }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Pay ${currencyFormat.format(amountCents / 100.0)}")
            .setMessage("Select source account:")
            .setItems(accountNames) { _, index ->
                viewModel.payBill(bill, accounts[index].id, amountCents)
            }
            .setNegativeButton("Back") { _, _ ->
                showPayBillDialog(bill)
            }
            .show()
    }

    private fun getDayOfMonthSuffix(n: Int): String {
        if (n in 11..13) return "th"
        return when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
}
