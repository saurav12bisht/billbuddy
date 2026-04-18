package com.mobile.fingram.ui.settings

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.mobile.fingram.R
import com.mobile.fingram.databinding.FragmentSetPinBinding
import com.mobile.fingram.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetPinFragment : BaseFragment<FragmentSetPinBinding>(FragmentSetPinBinding::inflate) {

    private val viewModel: SetPinViewModel by viewModels()
    private val dots get() = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)

    override fun onInit() {
        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { 
            findNavController().navigateUp()
        }

        viewModel.currentStep.observe(viewLifecycleOwner) { step ->
            when (step) {
                SetPinStep.ENTER_PIN -> {
                    binding.tvPinTitle.text = "Enter PIN"
                    binding.tvPinSubtitle.text = "Enter a 4-digit PIN"
                    binding.tvError.visibility = View.GONE
                }
                SetPinStep.CONFIRM_PIN -> {
                    binding.tvPinTitle.text = "Re-enter PIN"
                    binding.tvPinSubtitle.text = "Confirm your PIN"
                    binding.tvError.visibility = View.GONE
                }
            }
        }

        viewModel.currentLength.observe(viewLifecycleOwner) { len ->
            dots.forEachIndexed { i, v -> 
                v.setBackgroundResource(if (i < len) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty) 
            }
            // Update save button state when length changes
            val canSave = viewModel.canSave()
            binding.btnSave.isEnabled = canSave
            if (canSave) {
                binding.btnSave.backgroundTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.primary_blue))
            } else {
                binding.btnSave.backgroundTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.primary_variant))
            }
        }

        // Observe step changes to update save button state
        viewModel.currentStep.observe(viewLifecycleOwner) {
            val canSave = viewModel.canSave()
            binding.btnSave.isEnabled = canSave
            if (canSave) {
                binding.btnSave.backgroundTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.bottom_nav_colors))
            } else {
                binding.btnSave.backgroundTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.primary_variant))
            }
        }

        viewModel.pinMismatch.observe(viewLifecycleOwner) {
            binding.tvError.visibility = View.VISIBLE
            binding.pinDots.animate().translationXBy(20f).setDuration(50).withEndAction {
                binding.pinDots.animate().translationXBy(-40f).setDuration(50).withEndAction {
                    binding.pinDots.animate().translationXBy(20f).setDuration(50).start()
                }.start()
            }.start()
            // Clear the PIN and go back to enter step
            viewModel.resetToEnterPin()
        }

        viewModel.pinSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                findNavController().navigateUp()
            }
        }

        binding.btnBackspace.setOnClickListener { viewModel.backspace() }
        binding.btnSave.setOnClickListener { viewModel.savePin() }
        setupNumpad()
    }

    private fun setupNumpad() {
        val ids = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )
        ids.forEachIndexed { i, id ->
            binding.root.findViewById<View>(id)?.setOnClickListener { viewModel.addDigit(i) }
        }
    }
}
