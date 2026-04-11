package com.android.billreminder.ui.transaction

import android.os.Bundle
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.android.billreminder.R
import com.android.billreminder.databinding.FragmentAddTransactionBinding
import com.android.billreminder.domain.model.Transaction
import com.android.billreminder.ui.common.BaseFragment
import com.android.billreminder.ui.common.util.AdManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddTransactionFragment : BaseFragment<FragmentAddTransactionBinding>(FragmentAddTransactionBinding::inflate) {

    private val viewModel: TransactionViewModel by viewModels()
    private var existingTransactionId: Long = -1L
    private var existingTxDate: Long = 0L
    private var existingTxCreatedAt: Long = 0L
    private var customerId: Int = -1

    override fun onInit() {
        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        customerId = arguments?.getInt("customerId", -1) ?: -1
        existingTransactionId = arguments?.getLong("transactionId", -1L) ?: -1L

        if (customerId == -1 && existingTransactionId == -1L) {
            findNavController().navigateUp()
            return
        }
        
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.chipCredit.isChecked = true
        binding.btnSave.setOnClickListener { save() }

        if (existingTransactionId != -1L) {
            setupEditMode()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is com.android.billreminder.ui.customer.UiState.Success -> {
                            // Only show ad on new transaction, maybe? Or keep it simple.
                            // If editing, skip ad? No, ads are good.
                            AdManager.onTransactionSaved(requireActivity()) {
                                Snackbar.make(binding.root, getString(R.string.transaction_saved), Snackbar.LENGTH_SHORT).show()
                                findNavController().navigateUp()
                            }
                        }
                        is com.android.billreminder.ui.customer.UiState.Error -> Toast.makeText(requireContext(), state.msg, Toast.LENGTH_SHORT).show()
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupEditMode() {
        binding.toolbar.title = "Edit Transaction"
        binding.btnSave.text = "Update Transaction"
        
        viewLifecycleOwner.lifecycleScope.launch {
            val tx = viewModel.getTransactionById(existingTransactionId)
            if (tx != null) {
                // If editing, ensure we have the customer ID
                customerId = tx.customerId 
                
                // Populate fields
                binding.etAmount.setText((tx.amountPaise / 100.0).toString())
                binding.etNote.setText(tx.note)
                if (tx.type == "PAYMENT_RECEIVED") {
                    binding.chipPayment.isChecked = true
                } else {
                    binding.chipCredit.isChecked = true
                }
                
                // Store original dates
                existingTxDate = tx.date
                existingTxCreatedAt = tx.createdAt
            } else {
                Toast.makeText(requireContext(), "Transaction not found", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun save() {
        val amountStr = binding.etAmount.text?.toString()?.trim() ?: ""
        val amountRupees = amountStr.toDoubleOrNull() ?: 0.0
        if (amountRupees <= 0) {
            binding.tilAmount.error = "Enter amount > 0"
            return
        }
        binding.tilAmount.error = null
        val amountPaise = (amountRupees * 100).toLong()
        val type = if (binding.chipPayment.isChecked) "PAYMENT_RECEIVED" else "CREDIT_GIVEN"
        val note = binding.etNote.text?.toString()?.trim() ?: ""
        
        if (existingTransactionId != -1L) {
             val tx = Transaction(
                id = existingTransactionId,
                customerId = customerId,
                type = type,
                amountPaise = amountPaise,
                date = existingTxDate, // Keep original date
                dueDate = null,
                interestPercent = 0.0,
                category = "",
                note = note,
                createdAt = existingTxCreatedAt
            )
            viewModel.updateTransaction(tx)
        } else {
            val tx = Transaction(
                id = System.currentTimeMillis(), // Use Timestamp as ID
                customerId = customerId,
                type = type,
                amountPaise = amountPaise,
                date = System.currentTimeMillis(),
                dueDate = null,
                interestPercent = 0.0,
                category = "",
                note = note
            )
            viewModel.addTransaction(tx)
        }
    }
}
