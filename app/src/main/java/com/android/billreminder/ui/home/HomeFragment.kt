package com.android.billreminder.ui.home

import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.android.billreminder.R
import com.android.billreminder.databinding.FragmentHomeBinding
import com.android.billreminder.ui.common.BaseFragment
import com.android.billreminder.ui.common.util.AdManager
import com.android.billreminder.ui.common.util.CurrencyFormatter
import com.android.billreminder.ui.common.util.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var recentAdapter: RecentTransactionAdapter

    override fun onInit() {
        setupToolbar()
        setupBalanceCard()
        setupQuickActions()
        setupRecentList()
        setupOverdue()
        loadAd()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_settings) {
                findNavController().navigate(R.id.settingsFragment)
                true
            } else false
        }
    }

    private fun setupBalanceCard() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.balance.collect { b ->
                    binding.tvUdhaarGiven.text = CurrencyFormatter.formatPaiseToRupee(b.totalUdhaarGiven)
                    binding.tvTotalReceived.text = CurrencyFormatter.formatPaiseToRupee(b.totalReceived)
                    binding.tvNetOutstanding.text = CurrencyFormatter.formatPaiseToRupee(b.netOutstanding)
                    binding.tvNetOutstanding.setTextColor(
                        when {
                            b.netOutstanding > 0 -> requireContext().getColor(R.color.accent_red)
                            b.netOutstanding < 0 -> requireContext().getColor(R.color.bottom_nav_colors)
                            else -> requireContext().getColor(android.R.color.white)
                        }
                    )
                }
            }
        }
        binding.balanceCard.setOnClickListener {
            findNavController().navigate(R.id.splashFragment)
        }
    }

    private fun setupQuickActions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.customersWithBalance.collect { list ->
                    binding.tvCustomersCount.text = list.size.toString()
                }
            }
        }

//        binding.btnNewCustomer.setOnClickListener { findNavController().navigate(R.id.action_home_to_add_customer) }
//        binding.btnAddUdhaar.setOnClickListener { findNavController().navigate(R.id.customerListFragment) }
//        binding.btnAllCustomers.setOnClickListener { findNavController().navigate(R.id.customerListFragment) }
//        binding.btnSummary.setOnClickListener { findNavController().navigate(R.id.summaryFragment) }
//        binding.fab.setOnClickListener { findNavController().navigate(R.id.action_home_to_add_customer) }
    }

    private fun setupRecentList() {
        recentAdapter = RecentTransactionAdapter(
            onClick = { customerId -> findNavController().navigate(com.android.billreminder.R.id.splashFragment, android.os.Bundle().apply { putInt("customerId", customerId) }) }
        )
        binding.recentList.layoutManager = LinearLayoutManager(requireContext())
        binding.recentList.adapter = recentAdapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentWithCustomerName.collectLatest { list ->
                    recentAdapter.submitList(list)
                    binding.emptyRecent.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupOverdue() {
        // TODO: bind overdue list when we have overdue flow in ViewModel
    }

    private fun loadAd() {
        val adView = AdView(requireContext()).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = AdManager.BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
        (binding.adContainer as? android.view.ViewGroup)?.addView(adView)
        binding.adContainer.visibility = View.VISIBLE
    }
}
