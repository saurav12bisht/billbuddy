package com.android.billreminder.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.android.billreminder.data.local.entity.AccountEntity
import com.android.billreminder.data.local.entity.AccountType
import com.android.billreminder.databinding.LayoutAddAccountBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddAccountBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutAddAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountsViewModel by viewModels()
    private var existingAccountId: Long = -1L

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

        existingAccountId = arguments?.getLong(ARG_ACCOUNT_ID, -1L) ?: -1L
        if (existingAccountId > 0L) {
            binding.tvTitle.text = "Edit Account"
            binding.btnSave.text = "Update Account"
            loadAccount()
        }

        binding.btnSave.setOnClickListener {
            saveAccount()
        }
    }

    private fun loadAccount() {
        viewLifecycleOwner.lifecycleScope.launch {
            val account = viewModel.getAccountById(existingAccountId) ?: return@launch
            binding.etName.setText(account.name)
            binding.etBalance.setText((account.balanceCents / 100.0).toString())

            for (index in 0 until binding.cgIcon.childCount) {
                val chip = binding.cgIcon.getChildAt(index) as? Chip ?: continue
                if (chip.text.toString() == account.iconEmoji) {
                    chip.isChecked = true
                    break
                }
            }
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

        val balanceCents = ((balanceStr.toDoubleOrNull() ?: 0.0) * 100).toLong()

        val account = AccountEntity(
            id = if (existingAccountId > 0L) existingAccountId else 0L,
            name = name,
            balanceCents = balanceCents,
            iconEmoji = emoji,
            colorHex = "#E3F2FD", // Default light blue for banks
            accountType = AccountType.BANK
        )

        if (existingAccountId > 0L) {
            viewModel.updateAccount(account)
        } else {
            viewModel.saveAccount(account)
        }
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddAccountBottomSheet"
        private const val ARG_ACCOUNT_ID = "accountId"

        fun newInstance(accountId: Long = -1L) = AddAccountBottomSheet().apply {
            arguments = Bundle().apply {
                putLong(ARG_ACCOUNT_ID, accountId)
            }
        }
    }
}
