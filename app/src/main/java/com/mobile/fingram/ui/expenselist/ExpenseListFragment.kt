package com.mobile.fingram.ui.expenselist

import android.graphics.Canvas
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mobile.fingram.R
import com.mobile.fingram.databinding.FragmentExpenseListBinding
import com.mobile.fingram.ui.common.BaseFragment
import com.mobile.fingram.ui.common.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExpenseListFragment : BaseFragment<FragmentExpenseListBinding>(FragmentExpenseListBinding::inflate) {

    private val viewModel: ExpenseListViewModel by viewModels()
    private lateinit var expenseListAdapter: ExpenseListAdapter

    override fun onInit() {


        setupList()
        bindState()

        binding.fabAddExpense.setOnClickListener {
            findNavController().navigate(R.id.splashFragment)
        }
    }

    private fun setupList() {
        expenseListAdapter = ExpenseListAdapter { expense ->
            findNavController().navigate(
                R.id.splashFragment,
                android.os.Bundle().apply { putLong("expenseId", expense.id) }
            )
        }
        binding.expenseList.layoutManager = LinearLayoutManager(requireContext())
        binding.expenseList.adapter = expenseListAdapter

        ItemTouchHelper(createSwipeCallback()).attachToRecyclerView(binding.expenseList)
    }

    private fun bindState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvTotalSpentAmount.text = CurrencyFormatter.formatUsdCents(state.totalSpentThisMonth)
                    
                    expenseListAdapter.submitList(state.expenses)
                    binding.emptyState.visibility = if (state.expenses.isEmpty()) View.VISIBLE else View.GONE
                    binding.expenseList.visibility = if (state.expenses.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun createSwipeCallback(): ItemTouchHelper.SimpleCallback {
        return object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val expense = expenseListAdapter.currentList[viewHolder.bindingAdapterPosition]
                viewModel.deleteExpense(expense.id)
                Snackbar.make(binding.root, "Expense deleted", Snackbar.LENGTH_SHORT).show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                viewHolder.itemView.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.bill_swipe_delete)
                )
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
    }
}
