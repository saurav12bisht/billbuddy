package com.android.billreminder.ui.expenseform

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.android.billreminder.R
import com.android.billreminder.databinding.FragmentAddEditExpenseBinding
import com.android.billreminder.domain.model.Expense
import com.android.billreminder.ui.common.BaseFragment
import com.android.billreminder.ui.common.util.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditExpenseFragment : BaseFragment<FragmentAddEditExpenseBinding>(FragmentAddEditExpenseBinding::inflate) {

    private val viewModel: AddEditExpenseViewModel by viewModels()
    private var dateMillis: Long = System.currentTimeMillis()
    private var existingExpenseId: Long = -1L
    private var createdAt: Long = 0L

    override fun onInit() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, 0)
            insets
        }
        existingExpenseId = arguments?.getLong("expenseId", -1L) ?: -1L
        setupToolbar()
        setupDropdowns()
        setupDatePicker()
        bindState()
        
        if (existingExpenseId > 0L) {
            loadExpense()
        } else {
            updateDateLabel()
        }
        
        binding.btnSaveExpense.setOnClickListener { saveExpense() }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.title = if (existingExpenseId > 0L) {
            "Edit Expense"
        } else {
            "Add Expense"
        }
    }

    private fun setupDropdowns() {
        val categories = arrayOf("Food 🍔", "Travel \uD83D\uDE97", "Shopping \uD83D\uDECD️", "Bills \uD83D\uDCB3", "Others")
        
        // Chip Group Category
        categories.forEach { category ->
            val chip = com.google.android.material.chip.Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Suggestion).apply {
                id = android.view.View.generateViewId()
                text = category
                isCheckable = true
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.surface2))
                setTextColor(requireContext().getColor(R.color.text_secondary))
                chipStrokeWidth = 0f
            }
            binding.cgCategory.addView(chip)
        }
        
        binding.cgCategory.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val selectedChip = group.findViewById<com.google.android.material.chip.Chip>(checkedIds.first())
                selectedChip?.let {
                    binding.etCategory.setText(it.text.toString())
                }
            } else {
                binding.etCategory.setText("")
            }
        }
        binding.cgCategory.isSelectionRequired = true
        
        if (binding.etCategory.text.isNullOrBlank() && binding.cgCategory.childCount > 0) {
            binding.etCategory.setText(categories.first(), false)
            (binding.cgCategory.getChildAt(0) as? com.google.android.material.chip.Chip)?.isChecked = true
        }
    }

    private fun setupDatePicker() {
        binding.inputDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Expense Date")
                .setSelection(dateMillis)
                .build()
            picker.addOnPositiveButtonClickListener { selected ->
                dateMillis = selected
                updateDateLabel()
            }
            picker.show(parentFragmentManager, "expense_date")
        }
    }

    private fun bindState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        AddEditExpenseUiState.Idle -> Unit
                        AddEditExpenseUiState.Saving -> binding.btnSaveExpense.isEnabled = false
                        is AddEditExpenseUiState.Saved -> {
                            binding.btnSaveExpense.isEnabled = true
                            Toast.makeText(requireContext(), "Expense saved", Toast.LENGTH_SHORT).show()
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
        }
    }

    private fun loadExpense() {
        viewLifecycleOwner.lifecycleScope.launch {
            val expense = viewModel.loadExpense(existingExpenseId) ?: run {
                findNavController().navigateUp()
                return@launch
            }
            createdAt = expense.createdAt
            dateMillis = expense.dateMillis
            updateDateLabel()
            binding.etAmount.setText((expense.amountCents / 100.0).toString())
            binding.etCategory.setText(expense.categoryId.toString(), false)
            binding.etNote.setText(expense.note)
            
            for (i in 0 until binding.cgCategory.childCount) {
                val chip = binding.cgCategory.getChildAt(i) as? com.google.android.material.chip.Chip
                if (chip?.text?.toString().equals(expense.categoryId.toString(), ignoreCase = true)) {
                    chip?.isChecked = true
                    break
                }
            }
        }
    }

    private fun updateDateLabel() {
        binding.inputDate.setText(DateFormatter.formatMonthDayYear(dateMillis))
    }

    private fun saveExpense() {
        val amount = binding.etAmount.text?.toString()?.trim().orEmpty().toDoubleOrNull()
        val category = binding.etCategory.text?.toString()?.trim().orEmpty()
        val note = binding.etNote.text?.toString()?.trim().orEmpty()

        var hasError = false
        if (amount == null || amount <= 0.0) {
            binding.tilAmount.error = getString(R.string.invalid_amount)
            hasError = true
        } else {
            binding.tilAmount.error = null
        }
        if (category.isBlank()) {
            binding.tilCategory.error = getString(R.string.required_field)
            hasError = true
        } else {
            binding.tilCategory.error = null
        }
        if (hasError) return

//        viewModel.saveExpense(
//            Expense(
//                id = existingExpenseId.takeIf { it > 0L } ?: 0L,
//                amountCents = ((amount ?: 0.0) * 100).toLong(),
//                category = category,
//                dateMillis = dateMillis,
//                note = note.takeIf { it.isNotBlank() },
//                createdAt = createdAt
//            ),
//            isEdit = existingExpenseId > 0L
//        )
    }
}
