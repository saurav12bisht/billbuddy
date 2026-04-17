package com.android.billreminder.ui.creditcards

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billreminder.databinding.FragmentCreditCardListBinding
import com.android.billreminder.ui.common.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreditCardListFragment : BaseFragment<FragmentCreditCardListBinding>(FragmentCreditCardListBinding::inflate) {

    private val viewModel: CreditCardListViewModel by viewModels()
    private lateinit var adapter: CreditCardAdapter

    override fun onInit() {
        setupRecyclerView()
        setupListeners()
        bindState()
    }

    private fun setupRecyclerView() {
        adapter = CreditCardAdapter(
            onCardClick = { uiModel ->
                val action = CreditCardListFragmentDirections.actionCreditCardListToDetail(
                    cardId = uiModel.card.id
                )
                findNavController().navigate(action)
            },
            onEditClick = { uiModel ->
                val action = CreditCardListFragmentDirections.actionCreditCardListToAddEdit(
                    cardId = uiModel.card.id,
                    title = "Edit Credit Card"
                )
                findNavController().navigate(action)
            },
            onDeleteClick = { uiModel ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete credit card?")
                    .setMessage("This will remove ${uiModel.card.cardName} from your saved cards.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteCreditCard(uiModel.card)
                    }
                    .show()
            }
        )
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
                    binding.tvEmptyState.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}
