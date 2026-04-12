package com.android.billreminder.ui.transactionform

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.billreminder.R
import com.android.billreminder.databinding.LayoutAddTransactionBinding
import com.android.billreminder.data.local.entity.CategoryEntity
import com.android.billreminder.data.local.entity.AccountEntity
import com.android.billreminder.domain.model.CreditCard
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class AddTransactionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddTransactionViewModel by viewModels()
    private var isCategoryExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val transactionId = arguments?.getLong("transactionId") ?: -1L
        if (transactionId != -1L) {
            viewModel.loadTransaction(transactionId)
        }

        setupListeners()
        observeState()
    }

    // ── Listeners ────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnTypeExpense.setOnClickListener { viewModel.setType("EXPENSE") }
        binding.btnTypeIncome.setOnClickListener { viewModel.setType("INCOME") }

        binding.llDatePicker.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setSelection(
                    viewModel.date.value
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                )
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val date = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                viewModel.setDate(date)
            }
            datePicker.show(childFragmentManager, "date_picker")
        }

        binding.btnSave.setOnClickListener {
            // Validate credit card selection if CC is chosen
            if (viewModel.isCreditCardSelected.value && viewModel.selectedCreditCardId.value == null) {
                binding.tvNoCardsWarning.isVisible = true
                binding.tvNoCardsWarning.text = "⚠️ Please select which credit card you used."
                return@setOnClickListener
            }
            viewModel.saveTransaction(
                binding.etAmount.text.toString(),
                binding.etNote.text.toString()
            ) { dismiss() }
        }
    }

    // ── State observation ─────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch { viewModel.type.collect { updateTypeToggle(it) } }

                launch {
                    viewModel.date.collect {
                        binding.tvDate.text =
                            it.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                    }
                }

                launch { 
                    viewModel.categories.collect { cats -> 
                        updateCategoryChips(cats, viewModel.selectedCategoryId.value) 
                    } 
                }

                // Top-level Payment Method (Cash, Bank, Credit Card)
                launch { 
                    viewModel.paymentMethodBaseAccounts.collect { accs -> 
                        updateAccountChips(accs, viewModel.selectedAccountId.value) 
                    } 
                }

                // Show/hide credit card picker section
                launch {
                    viewModel.isCreditCardSelected.collect { isCreditCard ->
                        binding.llCreditCardPicker.isVisible = isCreditCard
                        if (!isCreditCard) binding.tvNoCardsWarning.isVisible = false
                    }
                }

                // Show/hide bank account picker section
                launch {
                    viewModel.isBankSelected.collect { isBank ->
                        binding.llBankAccountPicker.isVisible = isBank
                    }
                }

                // Populate sub-pickers
                launch { 
                    viewModel.creditCards.collect { cards -> 
                        updateCreditCardChips(cards, viewModel.selectedCreditCardId.value) 
                    } 
                }
                launch { 
                    viewModel.bankAccounts.collect { accs -> 
                        updateBankAccountChips(accs, viewModel.selectedBankAccountId.value) 
                    } 
                }

                launch {
                    // Update header title
                    if (viewModel.isEditMode) {
                        binding.btnSave.text = "Update Transaction"
                    }
                }

                launch {
                    viewModel.amount.collect { amt ->
                        if (amt != null && binding.etAmount.text.toString() != amt) {
                            binding.etAmount.setText(amt)
                        }
                    }
                }

                launch {
                    viewModel.note.collect { note ->
                        if (note != null && binding.etNote.text.toString() != note) {
                            binding.etNote.setText(note)
                        }
                    }
                }
            }
        }
    }

    // ── UI update helpers ─────────────────────────────────────────────────────

    private fun updateTypeToggle(type: String) {
        val isExpense = type == "EXPENSE"
        binding.btnTypeExpense.apply {
            setBackgroundColor(
                if (isExpense) ContextCompat.getColor(requireContext(), R.color.expense_red)
                else Color.TRANSPARENT
            )
            setTextColor(
                if (isExpense) Color.WHITE
                else ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )
        }
        binding.btnTypeIncome.apply {
            setBackgroundColor(
                if (!isExpense) ContextCompat.getColor(requireContext(), R.color.income_blue)
                else Color.TRANSPARENT
            )
            setTextColor(
                if (!isExpense) Color.WHITE
                else ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )
        }
        binding.btnSave.apply {
            setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isExpense) R.color.expense_red else R.color.income_blue
                )
            )
            if (!viewModel.isEditMode) {
                text = getString(if (isExpense) R.string.save_expense else R.string.save_income)
            }
        }
    }

    private fun updateCategoryChips(categories: List<CategoryEntity>, selectedId: Long?) {
        binding.cgCategories.removeAllViews()
        binding.cgCategoriesExpanded.removeAllViews()

        val limit = 5
        val showMore = categories.size > limit

        // Helper to create a category chip
        fun createChip(category: CategoryEntity): Chip {
            return Chip(requireContext()).apply {
                text = "${category.iconEmoji} ${category.name}"
                isCheckable = true
                isChecked = category.id == selectedId
                setOnClickListener { viewModel.selectCategory(category.id) }
                
                // Style adjustment for selected state
                if (isChecked) {
                    setChipBackgroundColorResource(if (viewModel.type.value == "EXPENSE") R.color.expense_red else R.color.income_blue)
                    setTextColor(Color.WHITE)
                }
            }
        }

        if (!isCategoryExpanded) {
            // Compact Mode: Horizontal scroll
            binding.hsvCategories.isVisible = true
            binding.cgCategoriesExpanded.isVisible = false
            
            val visibleCats = if (showMore) categories.take(limit) else categories
            visibleCats.forEach { binding.cgCategories.addView(createChip(it)) }

            if (showMore) {
                val moreChip = Chip(requireContext()).apply {
                    text = "More ▾"
                    setOnClickListener { 
                        isCategoryExpanded = true
                        updateCategoryChips(categories, selectedId)
                    }
                }
                binding.cgCategories.addView(moreChip)
            }
            
            // Still always show "New" at the end of scroll
            val newChip = Chip(requireContext()).apply {
                text = "+ New"
                setOnClickListener { showAddCategoryDialog() }
            }
            binding.cgCategories.addView(newChip)
            
        } else {
            // Expanded Mode: Multi-line grid
            binding.hsvCategories.isVisible = false
            binding.cgCategoriesExpanded.isVisible = true

            categories.forEach { binding.cgCategoriesExpanded.addView(createChip(it)) }

            // Add new category chip at the end
            val newChip = Chip(requireContext()).apply {
                text = "+ Add New Category"
                setOnClickListener { showAddCategoryDialog() }
            }
            binding.cgCategoriesExpanded.addView(newChip)

            // Add "Show Less" chip
            val lessChip = Chip(requireContext()).apply {
                text = "Show Less ▴"
                setOnClickListener { 
                    isCategoryExpanded = false
                    updateCategoryChips(categories, selectedId)
                }
            }
            binding.cgCategoriesExpanded.addView(lessChip)
        }

        // Handle auto-selection if needed
        if (selectedId == null && categories.isNotEmpty() && !viewModel.isEditMode) {
            viewModel.selectCategory(categories[0].id)
        }
    }

    private fun showAddCategoryDialog() {
        AddCategoryBottomSheet.newInstance { name, emoji ->
            viewModel.insertNewCategory(name, emoji)
            // The flow collector will automatically refresh the list
        }.show(childFragmentManager, AddCategoryBottomSheet.TAG)
    }

    private fun updateAccountChips(accounts: List<AccountEntity>, selectedId: Long?) {
        binding.cgAccounts.removeAllViews()
        accounts.forEach { account ->
            val chip = Chip(requireContext()).apply {
                text = "${account.iconEmoji} ${account.name}"
                isCheckable = true
                id = View.generateViewId()
                isChecked = account.id == selectedId
                setOnClickListener { 
                    val isCard = account.name.contains("credit", ignoreCase = true) || account.name.contains("card", ignoreCase = true)
                    val isBank = account.name.contains("bank", ignoreCase = true) && !isCard
                    
                    if (isBank && viewModel.bankAccounts.value.isEmpty()) {
                        showAccountGuidanceDialog("Bank Account")
                        return@setOnClickListener
                    }
                    if (isCard && viewModel.creditCards.value.isEmpty()) {
                        showAccountGuidanceDialog("Credit Card")
                        return@setOnClickListener
                    }
                    
                    viewModel.selectAccount(account.id, account.name) 
                }
            }
            binding.cgAccounts.addView(chip)
        }
        if (selectedId == null && accounts.isNotEmpty() && !viewModel.isEditMode) {
            (binding.cgAccounts.getChildAt(0) as? Chip)?.isChecked = true
            viewModel.selectAccount(accounts[0].id, accounts[0].name)
        }
    }

    /**
     * Populates the bank account chips that appear when "Bank" payment method is selected.
     */
    private fun updateBankAccountChips(accounts: List<AccountEntity>, selectedId: Long?) {
        binding.cgBankAccounts.removeAllViews()
        accounts.forEach { account ->
            val chip = Chip(requireContext()).apply {
                text = "${account.iconEmoji} ${account.name}"
                isCheckable = true
                id = View.generateViewId()
                isChecked = account.id == selectedId
                setOnClickListener { viewModel.selectBankAccount(account.id) }
            }
            binding.cgBankAccounts.addView(chip)
        }
        if (selectedId == null && accounts.isNotEmpty() && !viewModel.isEditMode) {
            (binding.cgBankAccounts.getChildAt(0) as? Chip)?.isChecked = true
            viewModel.selectBankAccount(accounts[0].id)
        }
    }

    /**
     * Populates the credit card chips that appear when "Credit Card" account is selected.
     */
    private fun updateCreditCardChips(cards: List<CreditCard>, selectedId: Long?) {
        binding.cgCreditCards.removeAllViews()

        if (cards.isEmpty()) {
            binding.tvNoCardsWarning?.text =
                "⚠️ No credit cards saved. Add one in Settings → Credit Cards."
            binding.tvNoCardsWarning?.isVisible = viewModel.isCreditCardSelected.value
            return
        }

        binding.tvNoCardsWarning?.isVisible = false

        cards.forEach { card ->
            val chip = Chip(requireContext()).apply {
                text = "💳 ${card.bankName} ••••${card.lastFourDigits}"
                isCheckable = true
                id = View.generateViewId()
                isChecked = card.id == selectedId
                setOnClickListener { viewModel.selectCreditCard(card.id) }
            }
            binding.cgCreditCards.addView(chip)
        }
        if (selectedId == null && cards.isNotEmpty() && !viewModel.isEditMode) {
            (binding.cgCreditCards.getChildAt(0) as? Chip)?.isChecked = true
            viewModel.selectCreditCard(cards[0].id)
        }
    }

    private fun showAccountGuidanceDialog(type: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("$type Required")
            .setMessage("You haven't added any ${type.lowercase()}s yet. To track your ${type.lowercase()} spending, you need to add one first in the Accounts section.")
            .setPositiveButton("Go to Accounts") { _, _ ->
                findNavController().navigate(R.id.accountsFragment)
                dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
