package com.mobile.fingram.ui.settings

import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.mobile.fingram.R
import com.mobile.fingram.databinding.FragmentSettingsBinding
import com.mobile.fingram.ui.common.BaseFragment
import com.mobile.fingram.ui.common.util.AdManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
                    Toast.makeText(
                        requireContext(),
                        "PDF export unlock is wired. Generator is next.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        binding.cardCreditCards.setOnClickListener {
            findNavController().navigate(R.id.creditCardListFragment)
        }

        binding.accounts.setOnClickListener {
            findNavController().navigate(R.id.manageAccountsFragment)
        }

        binding.cardCurrency.setOnClickListener {
            showCurrencyPicker()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reminderSettings.collect { settings ->
                    if (binding.switchReminder7.isChecked != settings.sevenDays) {
                        binding.switchReminder7.isChecked = settings.sevenDays
                    }
                    if (binding.switchReminder3.isChecked != settings.threeDays) {
                        binding.switchReminder3.isChecked = settings.threeDays
                    }
                    if (binding.switchReminder1.isChecked != settings.oneDay) {
                        binding.switchReminder1.isChecked = settings.oneDay
                    }
                    binding.tvCurrencyValue.text = settings.currencySymbol
                }
            }
        }
    }

    private fun showCurrencyPicker() {
        val options = listOf(
            "$",
            "\u20B9",
            "\u20AC",
            "\u00A3",
            "\u00A5",
            "\u20A9",
            "\u20BD",
            "\u20B1",
            "\u20A6",
            "AED",
            "CHF"
        )
        val labels = options.map { symbol ->
            when (symbol) {
                "$" -> "$  Dollar"
                "\u20B9" -> "\u20B9  Rupee"
                "\u20AC" -> "\u20AC  Euro"
                "\u00A3" -> "\u00A3  Pound"
                "\u00A5" -> "\u00A5  Yen"
                "\u20A9" -> "\u20A9  Won"
                "\u20BD" -> "\u20BD  Ruble"
                "\u20B1" -> "\u20B1  Peso"
                "\u20A6" -> "\u20A6  Naira"
                "AED" -> "AED  Dirham"
                "CHF" -> "CHF  Franc"
                else -> symbol
            }
        }.toTypedArray()
        val current = viewModel.reminderSettings.value.currencySymbol
        val checkedItem = options.indexOf(current).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Currency Symbol")
            .setSingleChoiceItems(labels, checkedItem) { dialog, which ->
                viewModel.setCurrencySymbol(options[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
