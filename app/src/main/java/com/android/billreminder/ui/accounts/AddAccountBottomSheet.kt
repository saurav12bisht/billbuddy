package com.android.billreminder.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.android.billreminder.data.local.entity.AccountEntity
import com.android.billreminder.data.local.entity.AccountType
import com.android.billreminder.databinding.LayoutAddAccountBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddAccountBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutAddAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutAddAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSave.setOnClickListener {
            saveAccount()
        }
    }

    private fun saveAccount() {
        val name = binding.etName.text.toString().trim()
        val balanceStr = binding.etBalance.text.toString().trim()
        
        val selectedChipId = binding.cgIcon.checkedChipId
        val emoji = if (selectedChipId != View.NO_ID) {
            binding.root.findViewById<Chip>(selectedChipId).text.toString()
        } else {
            "🏦"
        }

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter bank name", Toast.LENGTH_SHORT).show()
            return
        }

        val balanceCents = (balanceStr.toDoubleOrNull() ?: 0.0 * 100).toLong()

        val account = AccountEntity(
            name = name,
            balanceCents = balanceCents,
            iconEmoji = emoji,
            colorHex = "#E3F2FD", // Default light blue for banks
            accountType = AccountType.BANK
        )

        viewModel.saveAccount(account)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddAccountBottomSheet"
        fun newInstance() = AddAccountBottomSheet()
    }
}
