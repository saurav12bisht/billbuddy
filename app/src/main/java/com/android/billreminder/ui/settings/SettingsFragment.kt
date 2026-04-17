package com.android.billreminder.ui.settings

import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.billreminder.R
import com.android.billreminder.databinding.FragmentSettingsBinding
import com.android.billreminder.ui.common.BaseFragment
import com.android.billreminder.ui.common.util.AdManager
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onInit() {
        binding.switchReminder7.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setReminder7Days(isChecked)
        }
        binding.switchReminder3.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setReminder3Days(isChecked)
        }
        binding.switchReminder1.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setReminder1Day(isChecked)
        }

        binding.btnWatchAd.setOnClickListener {
            AdManager.showRewarded(
                requireActivity(),
                onRewarded = {
                    Toast.makeText(requireContext(), "PDF export unlock is wired. Generator is next.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        binding.cardCreditCards.setOnClickListener {
            findNavController().navigate(R.id.creditCardListFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reminderSettings.collect { settings ->
                    if (binding.switchReminder7.isChecked != settings.sevenDays) binding.switchReminder7.isChecked = settings.sevenDays
                    if (binding.switchReminder3.isChecked != settings.threeDays) binding.switchReminder3.isChecked = settings.threeDays
                    if (binding.switchReminder1.isChecked != settings.oneDay) binding.switchReminder1.isChecked = settings.oneDay
                }
            }
        }
    }
}
