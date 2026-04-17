package com.android.fingram.ui.bills

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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.snackbar.Snackbar
import com.android.fingram.R
import com.android.fingram.databinding.FragmentBillListBinding
import com.android.fingram.ui.common.BaseFragment
import com.android.fingram.ui.common.util.AdManager
import com.android.fingram.ui.common.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BillListFragment : BaseFragment<FragmentBillListBinding>(FragmentBillListBinding::inflate) {

    private val viewModel: BillListViewModel by viewModels()
    private lateinit var billListAdapter: BillListAdapter

    override fun onInit() {
        // Entry animation
        binding.root.translationY = 40f
        binding.root.alpha = 0f
        binding.root.animate().translationY(0f).alpha(1f).setDuration(350)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()

        // FAB pulse
        android.animation.ObjectAnimator.ofPropertyValuesHolder(binding.fabAddBill,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.12f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.12f, 1f)
        ).apply { duration = 600; repeatCount = 2; start() }

        setupToolbar()
        setupList()
        bindState()
        loadBannerAd()
        binding.fabAddBill.setOnClickListener {
//            findNavController().navigate(R.id.addEditBillFragment)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_settings) {
                findNavController().navigate(R.id.settingsFragment)
                true
            } else {
                false
            }
        }
    }

    private fun setupList() {
        billListAdapter = BillListAdapter { bill ->
//            findNavController().navigate(
//                R.id.addEditBillFragment,
//                android.os.Bundle().apply { putLong("billId", bill.id) }
//            )
        }
        binding.billList.layoutManager = LinearLayoutManager(requireContext())
        binding.billList.adapter = billListAdapter
        ItemTouchHelper(createSwipeCallback()).attachToRecyclerView(binding.billList)
    }

    private fun bindState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.summary.collect { summary ->
                        // Animate amount
                        android.animation.ValueAnimator.ofFloat(0f, summary.totalOutstandingCents.toFloat()).apply {
                            duration = 800
                            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                            addUpdateListener { animator ->
                                val animatedValue = animator.animatedValue as Float
                                binding.tvOutstandingAmount.text = CurrencyFormatter.formatUsdCents(animatedValue.toLong())
                            }
                            start()
                        }
                        binding.tvDueSoonCount.text = "${summary.dueSoonCount} Upcoming"
                        binding.tvPaidCount.text = "${summary.paidCount} Paid"
                    }
                }
                launch {
                    viewModel.bills.collect { bills ->
                        binding.shimmerViewContainer.stopShimmer()
                        binding.shimmerViewContainer.visibility = View.GONE
                        binding.billList.visibility = View.VISIBLE
                        
                        billListAdapter.submitList(bills)
                        binding.emptyState.visibility = if (bills.isEmpty()) View.VISIBLE else View.GONE
                        
                        val overdueCount = bills.count { !it.isPaid && it.dueDate < System.currentTimeMillis() }
                        binding.tvOverdueCount.text = "$overdueCount Overdue"
                        
                        val totalBills = bills.size
                        val paidBills = bills.count { it.isPaid }
                        val percent = if (totalBills > 0) (paidBills * 100) / totalBills else 0
                        android.animation.ObjectAnimator.ofInt(binding.progressBarBills, "progress", 0, percent).apply {
                            duration = 1000
                            start()
                        }
                        binding.tvProgressLabel.text = "$paidBills of $totalBills bills settled"
                        
                        if (totalBills > 0 && paidBills == totalBills) {
                            val confettiView = binding.lottieConfetti
                            confettiView.visibility = View.VISIBLE
                            confettiView.playAnimation()
                            // hide after 2.5s
                            confettiView.postDelayed({
                                confettiView.visibility = View.GONE
                            }, 2500)
                        }
                    }
                }
            }
        }
    }

    private fun loadBannerAd() {
        val adView = AdView(requireContext()).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = AdManager.BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
        binding.adContainer.removeAllViews()
        binding.adContainer.addView(adView)
    }

    private fun createSwipeCallback(): ItemTouchHelper.SimpleCallback {
        return object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val bill = billListAdapter.currentList[viewHolder.bindingAdapterPosition]
                if (direction == ItemTouchHelper.RIGHT) {
                    viewModel.togglePaid(bill)
                    Snackbar.make(
                        binding.root,
                        if (bill.isPaid) R.string.bill_marked_unpaid else R.string.bill_marked_paid,
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.deleteBill(bill.id)
                    Snackbar.make(binding.root, R.string.bill_deleted, Snackbar.LENGTH_SHORT).show()
                }
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
                    ContextCompat.getColor(
                        requireContext(),
                        if (dX > 0) R.color.accent_red else R.color.bill_swipe_delete
                    )
                )
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
    }
}
