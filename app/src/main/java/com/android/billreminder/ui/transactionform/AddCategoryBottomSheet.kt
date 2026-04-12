package com.android.billreminder.ui.transactionform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.billreminder.databinding.LayoutAddCategoryBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddCategoryBottomSheet(
    private val onCategoryAdded: (String, String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutAddCategoryBinding? = null
    private val binding get() = _binding!!

    private val emojis = listOf(
        "🍔", "🚆", "🏠", "🎁", "💊", "🎬", "📚", "🎮", "👕", "☕",
        "💇", "🧺", "🐈", "🛠", "🏋", "✈", "💡", "🎨", "📱", "💼"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEmojiChips()

        binding.btnAddCategory.setOnClickListener {
            val name = binding.etCategoryName.text.toString().trim()
            val selectedChipId = binding.cgEmojis.checkedChipId
            val emoji = if (selectedChipId != View.NO_ID) {
                binding.cgEmojis.findViewById<Chip>(selectedChipId).text.toString()
            } else {
                emojis[0]
            }

            if (name.isNotEmpty()) {
                onCategoryAdded(name, emoji)
                dismiss()
            } else {
                binding.tilCategoryName.error = "Please enter a name"
            }
        }
    }

    private fun setupEmojiChips() {
        emojis.forEach { emoji ->
            val chip = Chip(requireContext()).apply {
                text = emoji
                isCheckable = true
                isChecked = emoji == emojis[0]
            }
            binding.cgEmojis.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddCategoryBottomSheet"
        fun newInstance(onCategoryAdded: (String, String) -> Unit) = AddCategoryBottomSheet(onCategoryAdded)
    }
}
