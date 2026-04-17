package com.android.billreminder.ui.dashboard

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billreminder.R
import com.android.billreminder.databinding.FragmentDashboardBinding
import com.android.billreminder.ui.bills.BillListAdapter
import com.android.billreminder.ui.common.BaseFragment
import com.android.billreminder.ui.common.util.AdManager
import com.android.billreminder.ui.common.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : BaseFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate) {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var upcomingAdapter: BillListAdapter

    override fun onInit() {
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_settings) {
                findNavController().navigate(R.id.settingsFragment)
                true
            } else {
                false
            }
        }
        upcomingAdapter = BillListAdapter { bill ->
            findNavController().navigate(
                R.id.splashFragment,
                android.os.Bundle().apply { putLong("billId", bill.id) }
            )
        }
        binding.upcomingList.layoutManager = LinearLayoutManager(requireContext())
        binding.upcomingList.adapter = upcomingAdapter
        AdManager.maybeShowDashboardInterstitial(requireActivity()) {}
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvMonthlyTotal.text = CurrencyFormatter.formatUsdCents(state.totalBillsAmountCents)
                    binding.tvPaidAmount.text = CurrencyFormatter.formatUsdCents(state.paidAmountCents)
                    binding.tvRemainingAmount.text = CurrencyFormatter.formatUsdCents(state.remainingAmountCents)
                    binding.progressIndicator.progress = state.progressPercent
                    binding.tvProgress.text = getString(R.string.progress_percent, state.progressPercent)
                    upcomingAdapter.submitList(state.upcomingBills)
                    binding.emptyUpcoming.visibility = if (state.upcomingBills.isEmpty()) View.VISIBLE else View.GONE
                    
                    // Expense Overviews
                    binding.tvSpentToday.text = CurrencyFormatter.formatUsdCents(state.totalSpentTodayCents)
                    binding.tvSpentMonth.text = CurrencyFormatter.formatUsdCents(state.totalSpentThisMonthCents)

                    // New Financial Summary
                    binding.tvIncomeMonth.text = CurrencyFormatter.formatUsdCents(state.totalIncomeThisMonthCents)
                    binding.tvSummaryExpense.text = CurrencyFormatter.formatUsdCents(state.totalSpentThisMonthCents)
                    binding.tvSummaryBalance.text = CurrencyFormatter.formatUsdCents(state.totalIncomeThisMonthCents - state.totalSpentThisMonthCents)
                    binding.tvSummaryCCDue.text = CurrencyFormatter.formatUsdCents(state.totalCreditDueCents)
                }
            }
        }
    }
}
