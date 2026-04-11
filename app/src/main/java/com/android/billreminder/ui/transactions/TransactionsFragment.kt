package com.android.billreminder.ui.transactions

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.billreminder.R
import com.android.billreminder.databinding.FragmentTransactionsBinding
import com.android.billreminder.ui.common.BaseFragment
import com.android.billreminder.ui.common.util.CurrencyFormatter
import com.google.android.material.snackbar.Snackbar
import com.android.billreminder.data.local.entity.ExpenseWithCategory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class TransactionsFragment : BaseFragment<FragmentTransactionsBinding>(FragmentTransactionsBinding::inflate) {

    private val viewModel: TransactionsViewModel by viewModels()
    private lateinit var adapter: TransactionsAdapter

    override fun onInit() {
        setupRecyclerView()
        setupListeners()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = TransactionsAdapter { row ->
            // Handle item click (e.g. edit)
        }
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = adapter

        // Swipe to delete
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            
            override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
                if (vh is TransactionsAdapter.HeaderViewHolder) return 0
                return super.getSwipeDirs(rv, vh)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList[position]
                if (item is TransactionListItem.TransactionRow) {
                    val expense = item.item.expense
                    viewModel.deleteTransaction(expense)
                    
                    Snackbar.make(binding.root, "Transaction deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO") {
                            // Logic to re-insert would go here if repository supported it easily
                        }.show()
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvTransactions)
    }

    private fun setupListeners() {
        binding.btnPrevMonth.setOnClickListener { viewModel.previousMonth() }
        binding.btnNextMonth.setOnClickListener { viewModel.nextMonth() }
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.addTransactionBottomSheet)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentMonth.collect { ym ->
                        binding.tvDateTitle.text = ym.format(DateTimeFormatter.ofPattern("MMM yyyy"))
                    }
                }
                
                launch {
                    viewModel.monthlyTotals.collect { totals ->
                        binding.tvTotalIncome.text = CurrencyFormatter.formatUsdCents(totals.totalIncome)
                        binding.tvTotalExpense.text = CurrencyFormatter.formatUsdCents(totals.totalExpense)
                        binding.tvMonthlyNetTotal.text = CurrencyFormatter.formatUsdCents(totals.totalIncome - totals.totalExpense)
                    }
                }

                launch {
                    viewModel.groupedTransactions.collect { items ->
                        adapter.submitList(items)
                        binding.llEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }
}
