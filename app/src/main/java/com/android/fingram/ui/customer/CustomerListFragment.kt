package com.android.fingram.ui.customer

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.fingram.R
import com.android.fingram.databinding.FragmentCustomerListBinding
import com.android.fingram.ui.common.BaseFragment
import com.android.fingram.ui.common.util.AdManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle

@AndroidEntryPoint
class CustomerListFragment : BaseFragment<FragmentCustomerListBinding>(FragmentCustomerListBinding::inflate) {

    private val viewModel: CustomerViewModel by viewModels()
    private lateinit var adapter: CustomerAdapter

    override fun onInit() {
        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        adapter = CustomerAdapter { c ->
            findNavController().navigate(R.id.splashFragment, android.os.Bundle().apply { putInt("customerId", c.id) })
        }
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.customersWithBalance.collectLatest { list ->
                    adapter.submitList(list)
                    binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        binding.fab.setOnClickListener { findNavController().navigate(R.id.splashFragment) }

        val adView = AdView(requireContext()).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = AdManager.BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
        (binding.adContainer as? android.view.ViewGroup)?.addView(adView)
        binding.adContainer.visibility = View.VISIBLE

        binding.searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onSearchQueryChanged(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
}
