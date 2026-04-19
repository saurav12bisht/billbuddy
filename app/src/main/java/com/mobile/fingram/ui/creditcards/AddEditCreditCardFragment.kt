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
import androidx.core.widget.doOnTextChanged
import android.graphics.Color
import android.content.res.ColorStateList
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
        setupLivePreview()
        setupColorSwatches()
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

    private fun setupLivePreview() {
        binding.etBankName.doOnTextChanged { text, _, _, _ ->
            binding.tvLiveBankName.text = if (text.isNullOrBlank()) "BANK NAME" else text.toString().uppercase()
        }

        binding.etCardName.doOnTextChanged { text, _, _, _ ->
            binding.tvLiveCardName.text = if (text.isNullOrBlank()) "CARDHOLDER NAME" else text.toString().uppercase()
        }

        binding.etLastFour.doOnTextChanged { text, _, _, _ ->
            val lastFour = if (text.isNullOrBlank()) "1234" else text.toString().padEnd(4, '*')
            binding.tvLiveCardNumber.text = "****  ****  ****  $lastFour"
        }
    }

    private fun setupColorSwatches() {
        val swatches = mapOf(
            binding.colorPurple to "#B39DDB",
            binding.colorBlue to "#90CAF9",
            binding.colorGreen to "#A5D6A7",
            binding.colorOrange to "#FFCC80",
            binding.colorRed to "#EF9A9A",
            binding.colorDark to "#546E7A"
        )

        swatches.forEach { (view, hex) ->
            view.setOnClickListener {
                viewModel.updateColor(hex)
            }
        }
    }

    private fun loadCardData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val card = viewModel.getCreditCard(existingCardId) ?: return@launch
            binding.etCardName.setText(card.cardName)
            binding.etBankName.setText(card.bankName)
            binding.etLastFour.setText(card.lastFourDigits)
            binding.etBillingDay.setText(card.billingDay.toString())
            binding.etDueDay.setText(card.dueDay.toString())
            viewModel.updateColor(card.colorHex)
            
            // The doOnTextChanged listeners will automatically update the preview
            // when we set the text above.
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedColorHex.collect { hex ->
                    try {
                        val color = Color.parseColor(hex)
                        // Apply to the ConstraintLayout background, overriding the gradient
                        binding.clLivePreviewBg.backgroundTintList = ColorStateList.valueOf(color)
                    } catch (e: Exception) {
                        // fallback
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
            dueDay = dueDay,
            colorHex = viewModel.selectedColorHex.value
        )
    }

    private fun deleteCard() {
        viewLifecycleOwner.lifecycleScope.launch {
            val card = viewModel.getCreditCard(existingCardId) ?: return@launch
            viewModel.deleteCreditCard(card)
        }
    }
}
