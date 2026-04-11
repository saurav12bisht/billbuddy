package com.android.billreminder.ui.transactionform

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
import com.android.billreminder.R
import com.android.billreminder.databinding.LayoutAddTransactionBinding
import com.android.billreminder.data.local.entity.CategoryEntity
import com.android.billreminder.data.local.entity.AccountEntity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class AddTransactionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddTransactionViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnTypeExpense.setOnClickListener { viewModel.setType("EXPENSE") }
        binding.btnTypeIncome.setOnClickListener { viewModel.setType("INCOME") }

        binding.llDatePicker.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setSelection(viewModel.date.value.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .build()
            
            datePicker.addOnPositiveButtonClickListener { selection ->
                val date = Instant.ofEpochMilli(selection).atZone(ZoneId.systemDefault()).toLocalDate()
                viewModel.setDate(date)
            }
            datePicker.show(childFragmentManager, "date_picker")
        }

        binding.btnSave.setOnClickListener {
            viewModel.saveTransaction(
                binding.etAmount.text.toString(),
                binding.etNote.text.toString()
            ) {
                dismiss()
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.type.collect { type ->
                        updateTypeToggle(type)
                    }
                }
                launch {
                    viewModel.date.collect { date ->
                        binding.tvDate.text = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                    }
                }
                launch {
                    viewModel.categories.collect { categories ->
                        updateCategoryChips(categories)
                    }
                }
                launch {
                    viewModel.accounts.collect { accounts ->
                        updateAccountChips(accounts)
                    }
                }
            }
        }
    }

    private fun updateTypeToggle(type: String) {
        val isExpense = type == "EXPENSE"
        binding.btnTypeExpense.apply {
            setBackgroundColor(if (isExpense) ContextCompat.getColor(requireContext(), R.color.expense_red) else Color.TRANSPARENT)
            setTextColor(if (isExpense) Color.WHITE else ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
        binding.btnTypeIncome.apply {
            setBackgroundColor(if (!isExpense) ContextCompat.getColor(requireContext(), R.color.income_blue) else Color.TRANSPARENT)
            setTextColor(if (!isExpense) Color.WHITE else ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
        binding.btnSave.apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), if (isExpense) R.color.expense_red else R.color.income_blue))
            text = getString(if (isExpense) R.string.save_expense else R.string.save_income)
        }
    }

    private fun updateCategoryChips(categories: List<CategoryEntity>) {
        binding.cgCategories.removeAllViews()
        categories.forEachIndexed { index, category ->
            val chip = Chip(requireContext()).apply {
                text = "${category.iconEmoji} ${category.name}"
                isCheckable = true
                id = View.generateViewId()
                setOnClickListener { viewModel.selectCategory(category.id) }
            }
            binding.cgCategories.addView(chip)
            if (index == 0) {
                chip.isChecked = true
                viewModel.selectCategory(category.id)
            }
        }
        // Add "+ New" chip
        val newChip = Chip(requireContext()).apply {
            text = getString(R.string.new_category)
            setOnClickListener { /* Open Category Manager */ }
        }
        binding.cgCategories.addView(newChip)
    }

    private fun updateAccountChips(accounts: List<AccountEntity>) {
        binding.cgAccounts.removeAllViews()
        accounts.forEachIndexed { index, account ->
            val chip = Chip(requireContext()).apply {
                text = "${account.iconEmoji} ${account.name}"
                isCheckable = true
                id = View.generateViewId()
                setOnClickListener { viewModel.selectAccount(account.id) }
            }
            binding.cgAccounts.addView(chip)
            if (index == 0) {
                chip.isChecked = true
                viewModel.selectAccount(account.id)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
