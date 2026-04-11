package com.android.billreminder.ui.billform

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
import com.android.billreminder.databinding.FragmentAddEditBillBinding
import com.android.billreminder.domain.model.Bill
import com.android.billreminder.ui.common.BaseFragment
import com.android.billreminder.ui.common.util.DateFormatter
import com.android.billreminder.worker.BillReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditBillFragment : BaseFragment<FragmentAddEditBillBinding>(FragmentAddEditBillBinding::inflate) {

    private val viewModel: AddEditBillViewModel by viewModels()
    private var dueDateMillis: Long = System.currentTimeMillis()
    private var existingBillId: Long = -1L
    private var createdAt: Long = 0L
    private var lastPaidAt: Long? = null

    override fun onInit() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, 0)
            insets
        }
        existingBillId = arguments?.getLong("billId", -1L) ?: -1L
        setupToolbar()
        setupDropdowns()
        setupDatePicker()
        bindState()
        if (existingBillId > 0L) {
            loadBill()
        } else {
            updateDueDateLabel()
        }
        binding.btnSaveBill.setOnClickListener { saveBill() }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.title = if (existingBillId > 0L) {
            getString(R.string.edit_bill)
        } else {
            getString(R.string.add_bill)
        }
    }

    private fun setupDropdowns() {
        val categories = resources.getStringArray(R.array.bill_categories)
        val repeatTypes = resources.getStringArray(R.array.bill_repeat_types)
        
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
        
        binding.etRepeatType.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, repeatTypes))
        binding.cgCategory.isSelectionRequired = true
        if (binding.etCategory.text.isNullOrBlank() && binding.cgCategory.childCount > 0) {
            binding.etCategory.setText(categories.first(), false)
            (binding.cgCategory.getChildAt(0) as? com.google.android.material.chip.Chip)?.isChecked = true
        }
        if (binding.etRepeatType.text.isNullOrBlank()) {
            binding.etRepeatType.setText(repeatTypes.first(), false)
        }
    }

    private fun setupDatePicker() {
        binding.inputDueDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_due_date)
                .setSelection(dueDateMillis)
                .build()
            picker.addOnPositiveButtonClickListener { selected ->
                dueDateMillis = selected
                updateDueDateLabel()
            }
            picker.show(parentFragmentManager, "bill_due_date")
        }
    }

    private fun bindState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        AddEditBillUiState.Idle -> Unit
                        AddEditBillUiState.Saving -> binding.btnSaveBill.isEnabled = false
                        is AddEditBillUiState.Saved -> {
                            binding.btnSaveBill.isEnabled = true
                            BillReminderScheduler.cancelBillReminders(requireContext(), state.billId)
                            if (!binding.switchPaid.isChecked) {
                                BillReminderScheduler.scheduleBillReminders(
                                    requireContext(),
                                    state.billId,
                                    dueDateMillis
                                )
                            }
                            Toast.makeText(requireContext(), R.string.bill_saved, Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                            findNavController().navigateUp()
                        }
                        is AddEditBillUiState.Error -> {
                            binding.btnSaveBill.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun loadBill() {
        viewLifecycleOwner.lifecycleScope.launch {
            val bill = viewModel.loadBill(existingBillId) ?: run {
                findNavController().navigateUp()
                return@launch
            }
            createdAt = bill.createdAt
            lastPaidAt = bill.lastPaidAt
            dueDateMillis = bill.dueDate
            updateDueDateLabel()
            binding.etAmount.setText((bill.amountCents / 100.0).toString())
            binding.etBillName.setText(bill.title)
            binding.etCategory.setText(bill.category, false)
            for (i in 0 until binding.cgCategory.childCount) {
                val chip = binding.cgCategory.getChildAt(i) as? com.google.android.material.chip.Chip
                if (chip?.text?.toString().equals(bill.category, ignoreCase = true)) {
                    chip?.isChecked = true
                    break
                }
            }
            binding.etRepeatType.setText(bill.repeatType, false)
            binding.switchPaid.isChecked = bill.isPaid
        }
    }

    private fun updateDueDateLabel() {
        binding.inputDueDate.setText(DateFormatter.formatMonthDayYear(dueDateMillis))
    }

    private fun saveBill() {
        val title = binding.etBillName.text?.toString()?.trim().orEmpty()
        val amount = binding.etAmount.text?.toString()?.trim().orEmpty().toDoubleOrNull()
        val category = binding.etCategory.text?.toString()?.trim().orEmpty()
        val repeatType = binding.etRepeatType.text?.toString()?.trim().orEmpty()

        var hasError = false
        if (title.isBlank()) {
            binding.tilBillName.error = getString(R.string.required_field)
            hasError = true
        } else {
            binding.tilBillName.error = null
        }
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
        if (repeatType.isBlank()) {
            binding.tilRepeatType.error = getString(R.string.required_field)
            hasError = true
        } else {
            binding.tilRepeatType.error = null
        }
        if (hasError) return

        viewModel.saveBill(
            Bill(
                id = existingBillId.takeIf { it > 0L } ?: 0L,
                title = title,
                amountCents = ((amount ?: 0.0) * 100).toLong(),
                dueDate = dueDateMillis,
                category = category,
                isPaid = binding.switchPaid.isChecked,
                repeatType = repeatType,
                createdAt = createdAt,
                lastPaidAt = if (binding.switchPaid.isChecked) lastPaidAt ?: System.currentTimeMillis() else null
            ),
            isEdit = existingBillId > 0L
        )
    }
}
