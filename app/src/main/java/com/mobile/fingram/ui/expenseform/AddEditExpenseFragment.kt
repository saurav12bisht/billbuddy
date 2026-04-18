package com.mobile.fingram.ui.expenseform

import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.mobile.fingram.R
import com.mobile.fingram.data.local.entity.AccountEntity
import com.mobile.fingram.data.local.entity.TransactionType
import com.mobile.fingram.databinding.FragmentAddEditExpenseBinding
import com.mobile.fingram.domain.model.CreditCard
import com.mobile.fingram.domain.model.Expense
import com.mobile.fingram.ui.common.BaseFragment
import com.mobile.fingram.ui.common.util.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Which payment method the user has selected in the toggle. */
private enum class PaymentMethod { CASH, BANK, CREDIT_CARD }

@AndroidEntryPoint
class AddEditExpenseFragment : BaseFragment<FragmentAddEditExpenseBinding>(
    FragmentAddEditExpenseBinding::inflate
) {

    private val viewModel: AddEditExpenseViewModel by viewModels()
    private var dateMillis: Long = System.currentTimeMillis()
    private var existingExpenseId: Long = -1L
    private var createdAt: Long = 0L
    private var selectedCategoryId: Long = -1L

    // Payment state
    private var paymentMethod: PaymentMethod = PaymentMethod.CASH
    private var selectedAccountId: Long = -1L
    private var selectedCreditCardId: Long? = null

    // Live data
    private var accountsList: List<AccountEntity> = emptyList()
    private var creditCardsList: List<CreditCard> = emptyList()

    override fun onInit() {

        existingExpenseId = arguments?.getLong("expenseId", -1L) ?: -1L
        binding.toolbar.title = if (existingExpenseId > 0L) "Edit Expense" else "Add Expense"
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        setupDatePicker()
        setupPaymentToggle()
        bindState()

        if (existingExpenseId > 0L) loadExpense()
        else updateDateLabel()

        binding.btnSaveExpense.setOnClickListener { saveExpense() }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }
    }

    // ── Date picker ─────────────────────────────────────────────────────────

    private fun setupDatePicker() {
        binding.inputDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(dateMillis)
                .build()
            picker.addOnPositiveButtonClickListener { selected ->
                dateMillis = selected
                updateDateLabel()
            }
            picker.show(parentFragmentManager, "expense_date")
        }
    }

    // ── Payment method toggle ───────────────────────────────────────────────

    private fun setupPaymentToggle() {
        // Default to Cash
        binding.togglePaymentMethod.check(R.id.btnPayCash)
        updatePaymentUI(PaymentMethod.CASH)

        binding.togglePaymentMethod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val selected = when (checkedId) {
                R.id.btnPayCash  -> PaymentMethod.CASH
                R.id.btnPayBank  -> PaymentMethod.BANK
                R.id.btnPayCard  -> PaymentMethod.CREDIT_CARD
                else             -> PaymentMethod.CASH
            }
            paymentMethod = selected
            updatePaymentUI(selected)
        }
    }

    /**
     * Shows/hides the account picker and credit card picker based on
     * the selected payment method.
     */
    private fun updatePaymentUI(method: PaymentMethod) {
        val isCreditCard = method == PaymentMethod.CREDIT_CARD

        // Account picker: shown for Cash and Bank (but filtered to non-CC accounts)
        binding.tilAccount.isVisible = !isCreditCard
        if (!isCreditCard) {
            selectedCreditCardId = null
            refreshAccountDropdown()
        }

        // Credit card picker: shown only when Card is selected
        val hasCards = creditCardsList.isNotEmpty()
        binding.tilCreditCard.isVisible = isCreditCard && hasCards
        binding.tvCreditInfo.isVisible = isCreditCard && hasCards
        binding.tvNoCards.isVisible = isCreditCard && !hasCards

        if (!isCreditCard) {
            selectedCreditCardId = null
            binding.actvCreditCard.setText("")
        }
    }

    // ── State binding ───────────────────────────────────────────────────────

    private fun bindState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // UI State
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            AddEditExpenseUiState.Idle    -> Unit
                            AddEditExpenseUiState.Saving  -> binding.btnSaveExpense.isEnabled = false
                            is AddEditExpenseUiState.Saved -> {
                                binding.btnSaveExpense.isEnabled = true
                                Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()
                                viewModel.resetState()
                                findNavController().navigateUp()
                            }
                            is AddEditExpenseUiState.Error -> {
                                binding.btnSaveExpense.isEnabled = true
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.resetState()
                            }
                        }
                    }
                }

                // Accounts (non-CC real accounts)
                launch {
                    viewModel.accounts.collect { accounts ->
                        // Filter out the generic "Credit Card" account — users pick real cards via the toggle
                        accountsList = accounts.filter { a ->
                            !a.name.equals("Credit Card", ignoreCase = true)
                        }
                        refreshAccountDropdown()

                        // Auto-select first account for new entries
                        if (existingExpenseId <= 0L && selectedAccountId == -1L && accountsList.isNotEmpty()) {
                            selectedAccountId = accountsList.first().id
                            binding.actvAccount.setText(
                                "${accountsList.first().iconEmoji}  ${accountsList.first().name}", false
                            )
                        }
                    }
                }

                // Credit Cards
                launch {
                    viewModel.creditCards.collect { cards ->
                        creditCardsList = cards
                        refreshCreditCardDropdown()
                        // Re-evaluate "no cards" message if card toggle is already selected
                        if (paymentMethod == PaymentMethod.CREDIT_CARD) {
                            updatePaymentUI(PaymentMethod.CREDIT_CARD)
                        }
                    }
                }

                // Categories
                launch {
                    viewModel.categories.collect { cats ->
                        binding.cgCategory.removeAllViews()
                        cats.filter { it.name != "CC Payment" }.forEach { category ->
                            val chip = com.google.android.material.chip.Chip(
                                requireContext(), null,
                                com.google.android.material.R.style.Widget_Material3_Chip_Suggestion
                            ).apply {
                                id = category.id.toInt()
                                text = "${category.iconEmoji} ${category.name}"
                                isCheckable = true
                                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                                    requireContext().getColor(R.color.surface2)
                                )
                                setTextColor(requireContext().getColor(R.color.text_secondary))
                                chipStrokeWidth = 0f
                            }
                            binding.cgCategory.addView(chip)
                        }
                        binding.cgCategory.setOnCheckedStateChangeListener { _, checkedIds ->
                            if (checkedIds.isNotEmpty()) {
                                selectedCategoryId = checkedIds.first().toLong()
                                binding.etCategory.setText(
                                    cats.find { it.id == selectedCategoryId }?.name ?: ""
                                )
                            }
                        }
                        // Auto-select first category for new entry
                        if (existingExpenseId <= 0L && selectedCategoryId == -1L) {
                            val first = cats.firstOrNull { it.name != "CC Payment" }
                            if (first != null) {
                                selectedCategoryId = first.id
                                (binding.cgCategory.getChildAt(0) as? com.google.android.material.chip.Chip)?.isChecked = true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun refreshAccountDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            accountsList.map { "${it.iconEmoji}  ${it.name}" }
        )
        binding.actvAccount.setAdapter(adapter)
        binding.actvAccount.setOnItemClickListener { _, _, position, _ ->
            selectedAccountId = accountsList[position].id
        }
    }

    private fun refreshCreditCardDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            creditCardsList.map { "💳 ${it.bankName} — ${it.cardName} ••••${it.lastFourDigits}" }
        )
        binding.actvCreditCard.setAdapter(adapter)
        binding.actvCreditCard.setOnItemClickListener { _, _, position, _ ->
            selectedCreditCardId = creditCardsList[position].id
        }
        // Auto-select first card if only one exists
        if (creditCardsList.size == 1 && selectedCreditCardId == null) {
            selectedCreditCardId = creditCardsList.first().id
            binding.actvCreditCard.setText(
                "💳 ${creditCardsList.first().bankName} — ${creditCardsList.first().cardName} ••••${creditCardsList.first().lastFourDigits}",
                false
            )
        }
    }

    // ── Load for edit ────────────────────────────────────────────────────────

    private fun loadExpense() {
        viewLifecycleOwner.lifecycleScope.launch {
            val expense = viewModel.loadExpense(existingExpenseId) ?: run {
                findNavController().navigateUp()
                return@launch
            }
            createdAt = expense.createdAt
            dateMillis = expense.dateMillis
            selectedCategoryId = expense.categoryId
            selectedAccountId = expense.accountId
            selectedCreditCardId = expense.creditCardId
            updateDateLabel()
            binding.etAmount.setText((expense.amountCents / 100.0).toString())
            binding.etNote.setText(expense.note)

            // Restore payment method toggle
            if (expense.transactionType == TransactionType.CREDIT && expense.creditCardId != null) {
                paymentMethod = PaymentMethod.CREDIT_CARD
                binding.togglePaymentMethod.check(R.id.btnPayCard)
                updatePaymentUI(PaymentMethod.CREDIT_CARD)
            } else {
                paymentMethod = PaymentMethod.CASH
                binding.togglePaymentMethod.check(R.id.btnPayCash)
                updatePaymentUI(PaymentMethod.CASH)
            }
        }
    }

    private fun updateDateLabel() {
        binding.inputDate.setText(DateFormatter.formatMonthDayYear(dateMillis))
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    private fun saveExpense() {
        val amountStr = binding.etAmount.text?.toString()?.trim().orEmpty()
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            binding.tilAmount.error = getString(R.string.invalid_amount)
            return
        }
        binding.tilAmount.error = null

        if (selectedCategoryId == -1L) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        // Credit card specific validation
        if (paymentMethod == PaymentMethod.CREDIT_CARD) {
            if (creditCardsList.isEmpty()) {
                Toast.makeText(requireContext(), "Please add a credit card first in Settings → Credit Cards", Toast.LENGTH_LONG).show()
                return
            }
            if (selectedCreditCardId == null) {
                Toast.makeText(requireContext(), "Please select which credit card you used", Toast.LENGTH_SHORT).show()
                binding.actvCreditCard.requestFocus()
                return
            }
        }

        // For credit card payments we use a virtual account ID (the generic "Credit Card" account
        // or the first available if it's absent — we just need a valid accountId for the FK).
        val finalAccountId = if (paymentMethod == PaymentMethod.CREDIT_CARD) {
            // Use the first account id as a placeholder FK — the credit card ID is the real reference
            accountsList.firstOrNull()?.id ?: selectedAccountId
        } else {
            selectedAccountId
        }

        val transactionType = if (paymentMethod == PaymentMethod.CREDIT_CARD)
            TransactionType.CREDIT else TransactionType.NORMAL

        val note = binding.etNote.text?.toString()?.trim().orEmpty()

        viewModel.saveExpense(
            Expense(
                id = if (existingExpenseId > 0L) existingExpenseId else 0L,
                type = "EXPENSE",
                amountCents = (amount * 100).toLong(),
                categoryId = selectedCategoryId,
                accountId = finalAccountId,
                creditCardId = selectedCreditCardId,
                note = note.takeIf { it.isNotBlank() },
                dateMillis = dateMillis,
                createdAt = if (createdAt > 0) createdAt else System.currentTimeMillis(),
                transactionType = transactionType
            ),
            isEdit = existingExpenseId > 0L
        )
    }
}
