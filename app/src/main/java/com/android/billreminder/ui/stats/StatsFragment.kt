package com.android.billreminder.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.billreminder.R
import com.android.billreminder.databinding.FragmentStatsBinding
import com.android.billreminder.databinding.ItemCategoryStatBinding
import com.android.billreminder.ui.common.BaseFragment
import com.android.billreminder.ui.common.util.CurrencyFormatter
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class StatsFragment : BaseFragment<FragmentStatsBinding>(FragmentStatsBinding::inflate) {

    private val viewModel: StatsViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryStatsAdapter

    override fun onInit() {
        setupChart()
        setupRecyclerView()
        setupListeners()
        observeState()
    }

    private fun setupChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 60f
            transparentCircleRadius = 0f
            setDrawEntryLabels(false)
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryStatsAdapter()
        binding.rvCategoryBreakdown.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategoryBreakdown.adapter = categoryAdapter
    }

    private fun setupListeners() {
        binding.btnPrevMonth.setOnClickListener { viewModel.previousMonth() }
        binding.btnNextMonth.setOnClickListener { viewModel.nextMonth() }
        binding.btnTypeExpense.setOnClickListener { viewModel.setType(StatsFilterType.EXPENSE) }
        binding.btnTypeIncome.setOnClickListener { viewModel.setType(StatsFilterType.INCOME) }
        binding.btnTypeCreditCardDue.setOnClickListener { viewModel.setType(StatsFilterType.CREDIT_CARD_DUE) }
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
                    viewModel.type.collect { type ->
                        updateTypeButtons(type)
                    }
                }
                launch {
                    viewModel.categoryStats.collect { stats ->
                        updateUi(stats)
                    }
                }
            }
        }
    }

    private fun updateTypeButtons(type: StatsFilterType) {
        val expenseSelected = type == StatsFilterType.EXPENSE
        val incomeSelected = type == StatsFilterType.INCOME
        val creditSelected = type == StatsFilterType.CREDIT_CARD_DUE

        binding.btnTypeExpense.setBackgroundColor(
            if (expenseSelected) ContextCompat.getColor(requireContext(), R.color.expense_red) else Color.TRANSPARENT
        )
        binding.btnTypeExpense.setTextColor(
            if (expenseSelected) Color.WHITE else ContextCompat.getColor(requireContext(), R.color.text_secondary)
        )

        binding.btnTypeIncome.setBackgroundColor(
            if (incomeSelected) ContextCompat.getColor(requireContext(), R.color.income_blue) else Color.TRANSPARENT
        )
        binding.btnTypeIncome.setTextColor(
            if (incomeSelected) Color.WHITE else ContextCompat.getColor(requireContext(), R.color.text_secondary)
        )

        binding.btnTypeCreditCardDue.setBackgroundColor(
            if (creditSelected) ContextCompat.getColor(requireContext(), R.color.text_primary) else Color.TRANSPARENT
        )
        binding.btnTypeCreditCardDue.setTextColor(
            if (creditSelected) Color.WHITE else ContextCompat.getColor(requireContext(), R.color.text_secondary)
        )
    }

    private fun updateUi(stats: List<CategoryStat>) {
        categoryAdapter.submitList(stats)
        
        if (stats.isEmpty()) {
            binding.pieChart.clear()
            binding.pieChart.invalidate()
            return
        }

        val entries = stats.map { PieEntry(it.amountCents.toFloat(), it.name) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = stats.map { Color.parseColor(it.colorHex) }
            setDrawValues(false)
            sliceSpace = 2f
        }
        
        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.centerText = CurrencyFormatter.formatUsdCents(stats.sumOf { it.amountCents })
        binding.pieChart.animateY(800)
        binding.pieChart.invalidate()
    }

    class CategoryStatsAdapter : ListAdapter<CategoryStat, CategoryStatsAdapter.ViewHolder>(DiffCallback) {
        class ViewHolder(private val binding: ItemCategoryStatBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(stat: CategoryStat) {
                binding.vColorDot.background.setTint(Color.parseColor(stat.colorHex))
                binding.tvCatName.text = stat.name
                binding.tvAmount.text = CurrencyFormatter.formatUsdCents(stat.amountCents)
                binding.tvPercentage.text = String.format("%.1f%%", stat.percentage)
                binding.pbCategory.progress = stat.percentage.toInt()
                binding.pbCategory.progressDrawable.setTint(Color.parseColor(stat.colorHex))
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            ItemCategoryStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
        object DiffCallback : DiffUtil.ItemCallback<CategoryStat>() {
            override fun areItemsTheSame(old: CategoryStat, new: CategoryStat) = old.categoryId == new.categoryId
            override fun areContentsTheSame(old: CategoryStat, new: CategoryStat) = old == new
        }
    }
}
