package com.android.billreminder.ui.customer

import android.os.Bundle
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.billreminder.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.android.billreminder.databinding.FragmentAddCustomerBinding
import com.android.billreminder.domain.model.Customer
import com.android.billreminder.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddCustomerFragment : BaseFragment<FragmentAddCustomerBinding>(FragmentAddCustomerBinding::inflate) {

    private val viewModel: CustomerViewModel by viewModels()
    private var existingCustomerId: Int = -1
    private var existingCustomerCreatedAt: Long = 0L

    override fun onInit() {
        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        existingCustomerId = arguments?.getInt("customerId", -1) ?: -1

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.btnSave.setOnClickListener { save() }

        if (existingCustomerId != -1) {
            setupEditMode()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is UiState.Success -> {
                            Toast.makeText(requireContext(), state.msg, Toast.LENGTH_SHORT).show()
                            val id = state.newCustomerId
                            viewModel.resetUiState() // Prevent loop
                            
                            val navOptions = androidx.navigation.NavOptions.Builder()
                                .setPopUpTo(R.id.splashFragment, false)
                                .build()

                            if (existingCustomerId != -1) {
                                // If editing, just pop back
                                findNavController().navigateUp()
                            } else if (id != null && id > 0) {
                                findNavController().navigate(
                                    R.id.splashFragment,
                                    android.os.Bundle().apply { putInt("customerId", id) },
                                    navOptions
                                )
                            } else {
                                findNavController().navigate(R.id.splashFragment, null, navOptions)
                            }
                        }
                        is UiState.Error -> {
                            Toast.makeText(requireContext(), state.msg, Toast.LENGTH_SHORT).show()
                            viewModel.resetUiState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupEditMode() {
        binding.toolbar.title = "Edit Customer"
        binding.btnSave.text = "Update Customer"
        
        viewLifecycleOwner.lifecycleScope.launch {
            val customer = viewModel.getCustomerById(existingCustomerId)
            if (customer != null) {
                binding.etName.setText(customer.name)
                binding.etPhone.setText(customer.phone)
                existingCustomerCreatedAt = customer.createdAt
            } else {
                Toast.makeText(requireContext(), "Customer not found", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun save() {
        val name = binding.etName.text?.toString()?.trim() ?: ""
        val phone = binding.etPhone.text?.toString()?.trim()?.filter { it.isDigit() } ?: ""
        if (name.length < 2) {
            binding.tilName.error = "Min 2 characters"
            return
        }
        binding.tilName.error = null
        if (phone.length != 10) {
            binding.tilPhone.error = "Exactly 10 digits"
            return
        }
        binding.tilPhone.error = null
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Check for duplicate phone, excluding current customer if editing
            val existing = viewModel.getCustomerByPhone(phone)
            if (existing != null && existing.id != existingCustomerId) {
                 com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Phone Number Exists")
                    .setMessage("A customer named '${existing.name}' already uses this phone number.")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }

            if (existingCustomerId != -1) {
                val updatedCustomer = Customer(
                    id = existingCustomerId,
                    name = name,
                    phone = phone,
                    businessName = "",
                    address = "",
                    openingBalance = 0L, // Preserve logic if we had it, but here simplify
                    openingBalanceType = "THEY_OWE",
                    createdAt = existingCustomerCreatedAt,
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.updateCustomer(updatedCustomer)
            } else {
                val newCustomer = Customer(
                    id = 0,
                    name = name,
                    phone = phone,
                    businessName = "",
                    address = "",
                    openingBalance = 0L,
                    openingBalanceType = "THEY_OWE"
                )
                viewModel.addCustomer(newCustomer)
            }
        }
    }
}
