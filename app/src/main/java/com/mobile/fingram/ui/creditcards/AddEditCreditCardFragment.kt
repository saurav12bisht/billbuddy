package com.mobile.fingram.ui.creditcards

import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mobile.fingram.databinding.FragmentAddEditCreditCardBinding
import com.mobile.fingram.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditCreditCardFragment : BaseFragment<FragmentAddEditCreditCardBinding>(FragmentAddEditCreditCardBinding::inflate) {

    private val viewModel: AddEditCreditCardViewModel by viewModels()
    private val args: AddEditCreditCardFragmentArgs by navArgs()
    private var existingCardId: Long = -1L

    override fun onInit() {


        existingCardId = args.cardId
        setupToolbar()
        setupListeners()
        bindState()

        if (existingCardId > 0L) {
            loadCardData()
            binding.btnDelete.visibility = android.view.View.VISIBLE
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.title = args.title
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener { saveCard() }
        binding.btnDelete.setOnClickListener { deleteCard() }
    }

    private fun loadCardData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val card = viewModel.getCreditCard(existingCardId) ?: return@launch
            binding.etCardName.setText(card.cardName)
            binding.etBankName.setText(card.bankName)
            binding.etLastFour.setText(card.lastFourDigits)
            binding.etBillingDay.setText(card.billingDay.toString())
            binding.etDueDay.setText(card.dueDay.toString())
        }
    }

    private fun bindState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AddEditCreditCardUiState.Saved -> {
                            Toast.makeText(requireContext(), "Card saved successfully", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                        is AddEditCreditCardUiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun saveCard() {
        val name = binding.etCardName.text?.toString()?.trim().orEmpty()
        val bank = binding.etBankName.text?.toString()?.trim().orEmpty()
        val lastFour = binding.etLastFour.text?.toString()?.trim().orEmpty()
        val billingDay = binding.etBillingDay.text?.toString()?.toIntOrNull()
        val dueDay = binding.etDueDay.text?.toString()?.toIntOrNull()

        if (name.isBlank() || bank.isBlank() || lastFour.length != 4 || billingDay == null || dueDay == null) {
            Toast.makeText(requireContext(), "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveCreditCard(
            id = if (existingCardId > 0L) existingCardId else 0L,
            cardName = name,
            bankName = bank,
            lastFour = lastFour,
            billingDay = billingDay,
            dueDay = dueDay
        )
    }

    private fun deleteCard() {
        viewLifecycleOwner.lifecycleScope.launch {
            val card = viewModel.getCreditCard(existingCardId) ?: return@launch
            viewModel.deleteCreditCard(card)
        }
    }
}
