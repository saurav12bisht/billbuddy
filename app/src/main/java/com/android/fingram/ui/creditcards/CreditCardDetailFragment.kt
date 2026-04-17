package com.android.fingram.ui.creditcards

import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.fingram.R
import com.android.fingram.databinding.FragmentCreditCardDetailBinding
import com.android.fingram.domain.model.CreditCardBill
import com.android.fingram.ui.common.BaseFragment
import com.android.fingram.ui.common.util.CurrencyFormatter
import com.android.fingram.ui.common.util.PreferenceManager
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CreditCardDetailFragment : BaseFragment<FragmentCreditCardDetailBinding>(
    FragmentCreditCardDetailBinding::inflate
) {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val viewModel: CreditCardDetailViewModel by viewModels()
    private lateinit var billAdapter: CreditCardBillAdapter
    private lateinit var spendsAdapter: com.android.fingram.ui.transactions.TransactionsAdapter

    override fun onInit() {
        billAdapter = CreditCardBillAdapter { bill ->
            showPayBillDialog(bill)
        }
        binding.rvBills.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBills.adapter = billAdapter

        spendsAdapter = com.android.fingram.ui.transactions.TransactionsAdapter(
            onRowClick = {},
            onInfoClick = { showAccountingEducationDialog() }
        )
        binding.rvRecentSpends.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentSpends.adapter = spendsAdapter
        binding.rvRecentSpends.isNestedScrollingEnabled = false

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.ivInfoAccounting.setOnClickListener { showAccountingEducationDialog() }

        observeState()
    }

    private fun showAccountingEducationDialog() {
        AlertDialog.Builder(requireContext())
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
                        binding.toolbar.title = "${card.bankName} â€¢â€¢â€¢â€¢${card.lastFourDigits}"
                        binding.tvCardBankName.text = card.bankName.uppercase()
                        binding.tvCardName.text = card.cardName
                        binding.tvCardNumber.text = "â€¢â€¢â€¢â€¢  â€¢â€¢â€¢â€¢  â€¢â€¢â€¢â€¢  ${card.lastFourDigits}"
                        binding.tvBillingDay.text = "Starts on ${card.billingDay}${getDayOfMonthSuffix(card.billingDay)}"
                        binding.tvDueDay.text = "Due on ${card.dueDay}${getDayOfMonthSuffix(card.dueDay)}"
                    }

                    binding.tvCurrentCycleSpend.text = CurrencyFormatter.formatUsdCents(state.currentCycleSpend)
                    binding.tvBilledDue.text = CurrencyFormatter.formatUsdCents(state.billedDueAmount)
                    binding.tvOutstanding.text = CurrencyFormatter.formatUsdCents(state.outstandingAmount)

                    val spendItems = state.recentSpends.map { expenseWithCategory ->
                        com.android.fingram.ui.transactions.TransactionListItem.TransactionRow(expenseWithCategory)
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

    private fun showPayBillDialog(bill: CreditCardBill) {
        val accounts = viewModel.uiState.value.accounts
        if (accounts.isEmpty()) {
            Toast.makeText(requireContext(), "No bank accounts found. Please add a bank account first.", Toast.LENGTH_SHORT).show()
            return
        }

        val remainingCents = (bill.totalAmountCents - bill.paidAmountCents).coerceAtLeast(0L)
        if (remainingCents == 0L) {
            Toast.makeText(requireContext(), "This bill is already fully paid.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_pay_bill, null)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.etAmount)
        val tvRemaining = dialogView.findViewById<TextView>(R.id.tvRemainingAmount)
        val tilSourceAccount = dialogView.findViewById<TextInputLayout>(R.id.tilSourceAccount)
        val actSourceAccount = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.actSourceAccount)

        val accountLabels = accounts.map { "${it.iconEmoji}  ${it.name}" }
        val accountAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            accountLabels
        )
        actSourceAccount.setAdapter(accountAdapter)
        actSourceAccount.setText(accountLabels.first(), false)
        var selectedAccountIndex = 0
        actSourceAccount.setOnItemClickListener { _, _, position, _ ->
            selectedAccountIndex = position
            tilSourceAccount.error = null
        }

        tvRemaining.text = CurrencyFormatter.formatUsdCents(remainingCents)
        etAmount.setText((remainingCents / 100.0).toString())

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Record Payment")
            .setView(dialogView)
            .setPositiveButton("Next", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val amountStr = etAmount.text?.toString().orEmpty()
                val amountDouble = amountStr.toDoubleOrNull() ?: 0.0
                val amountCents = (amountDouble * 100).toLong()

                if (amountCents <= 0) {
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (amountCents > remainingCents) {
                    Toast.makeText(requireContext(), "Payment amount cannot exceed the remaining due", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val selectedLabel = actSourceAccount.text?.toString().orEmpty()
                val resolvedIndex = accountLabels.indexOf(selectedLabel).takeIf { it >= 0 } ?: selectedAccountIndex
                val selectedAccount = accounts.getOrNull(resolvedIndex)

                if (selectedAccount == null) {
                    tilSourceAccount.error = "Select a bank account"
                    return@setOnClickListener
                }

                dialog.dismiss()
                viewModel.payBill(bill, selectedAccount.id, amountCents)
            }
        }

        dialog.show()
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
