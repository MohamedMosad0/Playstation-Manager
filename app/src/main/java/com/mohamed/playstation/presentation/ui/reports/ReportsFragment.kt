package com.mohamed.playstation.presentation.ui.reports

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.databinding.FragmentReportsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportsViewModel by viewModels()
    private lateinit var topProductsAdapter: TopProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupCharts()
        observeState()
    }

    private fun setupViews() {
        topProductsAdapter = TopProductsAdapter("EGP")
        binding.rvTopProducts.adapter = topProductsAdapter

        binding.cardDateRange.setOnClickListener {
            showDateRangePicker()
        }

        binding.chipGroupDateFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            
            when (checkedIds.first()) {
                R.id.chipToday -> viewModel.setDateFilter(DateRangeFilter.TODAY)
                R.id.chipLast7Days -> viewModel.setDateFilter(DateRangeFilter.LAST_7_DAYS)
                R.id.chipThisMonth -> viewModel.setDateFilter(DateRangeFilter.THIS_MONTH)
                R.id.chipLast30Days -> viewModel.setDateFilter(DateRangeFilter.LAST_30_DAYS)
                R.id.chipAllTime -> viewModel.setDateFilter(DateRangeFilter.ALL_TIME)
            }
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(com.mohamed.playstation.R.string.report_date_picker_title))
            .build()
            
        picker.addOnPositiveButtonClickListener { selection: Pair<Long, Long> ->
            val start = selection.first
            val end = selection.second
            if (start != null && end != null) {
                binding.chipGroupDateFilters.clearCheck()
                viewModel.setCustomDateRange(start, end)
            }
        }
        
        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun setupCharts() {
        val textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        
        // Bar Chart
        binding.barChartRevenue.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                this.textColor = textColor
                granularity = 1f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                this.textColor = textColor
                axisMinimum = 0f
            }
            
            axisRight.isEnabled = false
            legend.textColor = textColor
            animateY(1000)
        }

        // Pie Charts
        val setupPieChart: (com.github.mikephil.charting.charts.PieChart) -> Unit = { chart ->
            chart.apply {
                description.isEnabled = false
                isDrawHoleEnabled = true
                setHoleColor(Color.TRANSPARENT)
                setTransparentCircleColor(Color.WHITE)
                setTransparentCircleAlpha(50)
                holeRadius = 65f
                transparentCircleRadius = 70f
                setDrawCenterText(true)
                rotationAngle = 0f
                isRotationEnabled = true
                isHighlightPerTapEnabled = true
                setDrawEntryLabels(false) // Hide labels to prevent overlap
                
                legend.apply {
                    this.textColor = textColor
                    isWordWrapEnabled = true
                    yEntrySpace = 8f
                    xEntrySpace = 16f
                    formSize = 12f
                    textSize = 12f
                }
                
                animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
            }
        }
        
        setupPieChart(binding.pieChartRevenue)
        setupPieChart(binding.pieChartDevices)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: ReportsUiState) {
        val currency = state.currency
        topProductsAdapter = TopProductsAdapter(currency) // Recreate if currency changes
        binding.rvTopProducts.adapter = topProductsAdapter

        // Date Label
        if (state.dateRangeLabel == com.mohamed.playstation.R.string.filter_custom) {
            // Keep the custom date label format but we might want to format the range
            binding.tvDateRange.text = getString(com.mohamed.playstation.R.string.custom_period)
        } else {
            binding.tvDateRange.text = getString(state.dateRangeLabel)
        }

        // KPIs
        binding.tvTotalRevenue.text = CurrencyUtils.formatAmount(requireContext(), state.totalRevenue, currency)
        binding.tvTotalExpenses.text = CurrencyUtils.formatAmount(requireContext(), state.totalExpenses, currency)
        binding.tvSessionRevenue.text = CurrencyUtils.formatAmount(requireContext(), state.sessionRevenue, currency)
        binding.tvProductRevenue.text = CurrencyUtils.formatAmount(requireContext(), state.productRevenue, currency)
        binding.tvTotalSessions.text = state.totalSessions.toString()
        binding.tvAvgDuration.text = "${state.avgSessionDurationMinutes} ${getString(com.mohamed.playstation.R.string.minutes_suffix)}"
        binding.tvProductCost.text = CurrencyUtils.formatAmount(requireContext(), state.productCost, currency)
        binding.tvProductProfit.text = CurrencyUtils.formatAmount(requireContext(), state.productProfit, currency)
        binding.tvNetProfit.text = CurrencyUtils.formatAmount(requireContext(), state.netProfit, currency)

        // Colour net profit based on sign
        val netProfitColor = if (state.netProfit >= 0) {
            ContextCompat.getColor(requireContext(), R.color.chart_green)
        } else {
            ContextCompat.getColor(requireContext(), R.color.chart_red)
        }
        binding.tvNetProfit.setTextColor(netProfitColor)

        // Historical cost gap warning
        binding.cardHistoricalWarning.visibility = if (state.hasHistoricalCostGap) View.VISIBLE else View.GONE

        // Top Products
        if (state.topProducts.isEmpty()) {
            binding.tvNoProducts.visibility = View.VISIBLE
            binding.rvTopProducts.visibility = View.GONE
        } else {
            binding.tvNoProducts.visibility = View.GONE
            binding.rvTopProducts.visibility = View.VISIBLE
            topProductsAdapter.submitList(state.topProducts)
        }

        // Charts
        updateBarChart(state.revenueLast7Days)
        updateRevenuePieChart(state.revenueDistribution)
        updateDevicePieChart(state.deviceDistribution)
    }

    private fun updateBarChart(data: List<kotlin.Pair<String, Double>>) {
        if (data.isEmpty()) {
            binding.barChartRevenue.clear()
            return
        }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        data.forEachIndexed { index, pair ->
            entries.add(BarEntry(index.toFloat(), pair.second.toFloat()))
            labels.add(pair.first)
        }

        val dataSet = BarDataSet(entries, getString(com.mohamed.playstation.R.string.revenue_chart_label))
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.ps_blue_primary)
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        binding.barChartRevenue.data = barData
        binding.barChartRevenue.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.barChartRevenue.invalidate()
    }

    private fun updateRevenuePieChart(distribution: Map<String, Double>) {
        if (distribution.isEmpty()) {
            binding.pieChartRevenue.clear()
            return
        }

        val entries = ArrayList<PieEntry>()
        for ((label, value) in distribution) {
            val displayLabel = when (label) {
                "session_revenue" -> getString(com.mohamed.playstation.R.string.session_revenue)
                "product_revenue" -> getString(com.mohamed.playstation.R.string.product_revenue)
                else -> label
            }
            entries.add(PieEntry(value.toFloat(), displayLabel))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.ps_blue_primary),
            ContextCompat.getColor(requireContext(), R.color.chart_green)
        )
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        binding.pieChartRevenue.data = PieData(dataSet)
        binding.pieChartRevenue.invalidate()
    }

    private fun updateDevicePieChart(distribution: Map<String, Int>) {
        if (distribution.isEmpty()) {
            binding.pieChartDevices.clear()
            return
        }

        val entries = ArrayList<PieEntry>()
        for ((label, value) in distribution) {
            entries.add(PieEntry(value.toFloat(), label))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.ps_blue_primary),
            ContextCompat.getColor(requireContext(), R.color.ps_blue_dark)
        )
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        binding.pieChartDevices.data = PieData(dataSet)
        binding.pieChartDevices.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
