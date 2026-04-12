package com.android.billreminder.ui.creditcards

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billreminder.databinding.FragmentCreditCardListBinding
import com.android.billreminder.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreditCardListFragment : BaseFragment<FragmentCreditCardListBinding>(FragmentCreditCardListBinding::inflate) {

    private val viewModel: CreditCardListViewModel by viewModels()
    private lateinit var adapter: CreditCardAdapter

    override fun onInit() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, 0)
            insets
        }

        setupRecyclerView()
        setupListeners()
        bindState()
    }

    private fun setupRecyclerView() {
        adapter = CreditCardAdapter { uiModel ->
            // Tap → navigate to detail screen
            val action = CreditCardListFragmentDirections.actionCreditCardListToDetail(
                cardId = uiModel.card.id
            )
            findNavController().navigate(action)
        }
        binding.rvCreditCards.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCreditCards.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.fabAddCreditCard.setOnClickListener {
            val action = CreditCardListFragmentDirections.actionCreditCardListToAddEdit(
                cardId = -1L,
                title = "Add Credit Card"
            )
            findNavController().navigate(action)
        }
    }

    private fun bindState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.creditCards.collect { cards ->
                    adapter.submitList(cards)
                    binding.tvEmptyState.visibility = if (cards.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                }
            }
        }
    }
}
