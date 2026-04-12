package com.android.billreminder.ui.transactions

import android.os.Bundle
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
import com.android.billreminder.ui.common.util.SwipeHintHelper
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class TransactionsFragment : BaseFragment<FragmentTransactionsBinding>(FragmentTransactionsBinding::inflate) {

    private val viewModel: TransactionsViewModel by viewModels()
    private lateinit var adapter: TransactionsAdapter

    // Guard so the swipe hint is only triggered once per fragment session
    // (SwipeHintHelper itself guards across app sessions via SharedPreferences)
    private var hintTriggered = false

    override fun onInit() {
        setupRecyclerView()
        setupListeners()
        observeState()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RecyclerView + swipe-to-delete
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = TransactionsAdapter { item ->
            val bundle = Bundle().apply {
                putLong("transactionId", item.expense.id)
            }
            findNavController().navigate(R.id.addTransactionBottomSheet, bundle)
        }

        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = adapter

        attachSwipeToDelete()
    }

    private fun attachSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            /* dragDirs  = */ 0,
            /* swipeDirs = */ ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false   // no drag-and-drop

            // Disable swipe on day-header rows
            override fun getSwipeDirs(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder
            ): Int {
                if (vh is TransactionsAdapter.HeaderViewHolder) return 0
                return super.getSwipeDirs(rv, vh)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_ID.toInt()) return

                val item = adapter.currentList.getOrNull(position) ?: return
                if (item !is TransactionListItem.TransactionRow) return

                val expense = item.item.expense
                viewModel.deleteTransaction(expense)

                Snackbar.make(binding.root, "Transaction deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        // Wire up undo via ViewModel if the repository supports re-insert
  //                      viewModel.undoDelete(expense)
                    }
                    .show()
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvTransactions)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Toolbar listeners
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnPrevMonth.setOnClickListener { viewModel.previousMonth() }
        binding.btnNextMonth.setOnClickListener { viewModel.nextMonth() }

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.addTransactionBottomSheet)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State observation
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Month label in toolbar
                launch {
                    viewModel.currentMonth.collect { ym ->
                        binding.tvDateTitle.text =
                            ym.format(DateTimeFormatter.ofPattern("MMM yyyy"))
                    }
                }

                // Income / expense / net totals
                launch {
                    viewModel.monthlyTotals.collect { totals ->
                        binding.tvTotalIncome.text =
                            CurrencyFormatter.formatUsdCents(totals.totalIncome)
                        binding.tvTotalExpense.text =
                            CurrencyFormatter.formatUsdCents(totals.totalExpense)
                        binding.tvMonthlyNetTotal.text =
                            CurrencyFormatter.formatUsdCents(totals.totalIncome - totals.totalExpense)
                    }
                }

                // Transaction list
                launch {
                    viewModel.groupedTransactions.collect { items ->
                        adapter.submitList(items) {
                            // submitList callback fires once DiffUtil has finished
                            // and the new list is fully rendered — safe to show hint.
                            handleSwipeHint(items)
                        }

                        binding.llEmpty.visibility =
                            if (items.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Swipe hint — first-time user education
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Triggers a one-time animated hint that slides the first transaction row
     * to the left so the user discovers the swipe-to-delete gesture naturally.
     *
     * Guards:
     *  • [hintTriggered]           – prevents re-triggering within the same
     *                                fragment session (e.g. month changes).
     *  • [SwipeHintHelper.isAlreadyShown] – prevents re-triggering across
     *                                sessions (stored in SharedPreferences).
     *  • items.isEmpty()           – only runs when there are actual rows.
     */
    private fun handleSwipeHint(items: List<TransactionListItem>) {
        if (hintTriggered) return
        if (items.isEmpty()) return
        if (SwipeHintHelper.isAlreadyShown(requireContext())) return

        hintTriggered = true
        SwipeHintHelper.showIfNeeded(requireContext(), binding.rvTransactions)
    }
}