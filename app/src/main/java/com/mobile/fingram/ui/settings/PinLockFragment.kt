package com.mobile.fingram.ui.settings

import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mobile.fingram.R
import com.mobile.fingram.databinding.FragmentPinLockBinding
import com.mobile.fingram.ui.common.BaseFragment
import com.mobile.fingram.ui.common.BiometricManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PinLockFragment : BaseFragment<FragmentPinLockBinding>(FragmentPinLockBinding::inflate) {

    private val viewModel: PinLockViewModel by viewModels()
    private val dots get() = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)

    override fun onInit() {
        viewModel.pinEntered.observe(viewLifecycleOwner) { success ->
            if (success) findNavController().navigate(R.id.action_pin_lock_to_home)
        }
        viewModel.wrongPin.observe(viewLifecycleOwner) {
            vibratePhone()
            binding.tvWrongPin.visibility = View.VISIBLE
            binding.pinDots.animate().translationXBy(20f).setDuration(50).withEndAction {
                binding.pinDots.animate().translationXBy(-40f).setDuration(50).withEndAction {
                    binding.pinDots.animate().translationXBy(20f).setDuration(50).start()
                }.start()
            }.start()
        }
        viewModel.currentLength.observe(viewLifecycleOwner) { len ->
            binding.tvWrongPin.visibility = View.GONE
            dots.forEachIndexed { i, v -> v.setBackgroundResource(if (i < len) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty) }
        }
        
        viewModel.lockoutTimeRemaining.observe(viewLifecycleOwner) { remaining ->
            if (remaining > 0) {
                val seconds = remaining / 1000 + 1
                binding.tvPinTitle.text = "Try again in ${seconds}s"
                binding.tvPinTitle.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent_red))
                binding.numpad.alpha = 0.3f
                // Disable valid input is handled in ViewModel, but visual feedback is good
            } else {
                binding.tvPinTitle.text = getString(R.string.app_lock)
                binding.tvPinTitle.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white))
                binding.numpad.alpha = 1.0f
            }
        }

        // Check if biometric is available (hardware + enrolled)
        // We do NOT check isBiometricEnabled here for "recovery" because if they forgot PIN,
        // they should be able to recover using system biometrics even if they didn't explicitly enable it for app unlock.
        // OR, adhere to strict policy: only if enabled.
        // DECISION based on plan: Check if hardware is available. If they can pass system auth, we trust them.
        
        lifecycleScope.launch {
            val isBiometricAvailable = BiometricManager.isBiometricAvailable(requireContext())
            val isBiometricEnabled = viewModel.appPreferences.getBiometricEnabled()
            
            // 1. Normal Unlock: Show biometric icon if enabled AND available
            if (isBiometricAvailable && isBiometricEnabled) {
                binding.btnBiometric.visibility = View.VISIBLE
                binding.btnBiometric.setOnClickListener { showBiometricPrompt(isRecovery = false) }
                showBiometricPrompt(isRecovery = false)
            }

            // 2. Forgot PIN: Show button if valid biometrics are present on device
            if (isBiometricAvailable) {
                binding.btnForgotPin.visibility = View.VISIBLE
                binding.btnForgotPin.setOnClickListener {
                    showBiometricPrompt(isRecovery = true)
                }
            }
        }

        binding.btnBackspace.setOnClickListener { viewModel.backspace() }
        setupNumpad()
    }

    private fun showBiometricPrompt(isRecovery: Boolean) {
        BiometricManager.showBiometricPrompt(
            fragment = this,
            title = if (isRecovery) "Reset PIN" else "Unlock App",
            subtitle = if (isRecovery) "Verify identity to reset PIN" else "Use your fingerprint or face to unlock",
            negativeButtonText = "Cancel",
            onSuccess = {
                if (isRecovery) {
                    Toast.makeText(requireContext(), "Identity Verified. Please set a new PIN.", Toast.LENGTH_SHORT).show()
                    // Navigate to SetPinFragment to create NEW PIN
                    // We assume SetPinFragment handles "overwrite" logic correctly
                    findNavController().navigate(R.id.splashFragment)
                } else {
                    viewModel.authenticateSuccess()
                }
            },
            onError = { error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            },
            onCancel = {}
        )
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

    private fun vibratePhone() {
        val vibrator = requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }
}
