package com.mobile.fingram.ui.history

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.mobile.fingram.R
import com.mobile.fingram.databinding.FragmentHistoryBinding
import com.mobile.fingram.ui.bills.BillListAdapter
import com.mobile.fingram.ui.common.BaseFragment
import com.mobile.fingram.ui.common.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryFragment : BaseFragment<FragmentHistoryBinding>(FragmentHistoryBinding::inflate) {

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: BillListAdapter

    override fun onInit() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        adapter = BillListAdapter(onClick = {})
        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.adapter = adapter
        styleChart()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvTotalSpent.text = CurrencyFormatter.formatUsdCents(state.totalSpentCents)
                    adapter.submitList(state.paidBills)
                    renderChart(state.monthlyPoints)
                }
            }
        }
    }

    private fun styleChart() {
        binding.historyChart.description.isEnabled = false
        binding.historyChart.legend.isEnabled = false
        binding.historyChart.axisRight.isEnabled = false
        binding.historyChart.setTouchEnabled(false)
        binding.historyChart.setFitBars(true)
        binding.historyChart.axisLeft.setDrawGridLines(false)
        binding.historyChart.xAxis.setDrawGridLines(false)
        binding.historyChart.xAxis.granularity = 1f
    }

    private fun renderChart(points: List<HistoryMonthPoint>) {
        val entries = points.mapIndexed { index, point ->
            BarEntry(index.toFloat(), point.amountCents.toFloat() / 100f)
        }
        val labels = points.map { it.label }
        val dataSet = BarDataSet(entries, getString(R.string.upcoming_bills)).apply {
            color = requireContext().getColor(R.color.primary)
            valueTextColor = requireContext().getColor(R.color.text_secondary)
        }
        binding.historyChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.historyChart.data = BarData(dataSet as IBarDataSet).apply { barWidth = 0.55f }
        binding.historyChart.invalidate()
    }
}
