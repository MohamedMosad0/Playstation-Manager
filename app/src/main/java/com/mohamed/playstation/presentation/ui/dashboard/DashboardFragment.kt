package com.mohamed.playstation.presentation.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.databinding.FragmentDashboardBinding
import com.mohamed.playstation.domain.model.dashboard.DashboardData
import com.mohamed.playstation.presentation.ui.UiState
import com.mohamed.playstation.presentation.ui.expenses.AddExpenseDialog
import com.mohamed.playstation.presentation.ui.sessions.NewSessionDialog
import com.mohamed.playstation.presentation.viewmodel.dashboard.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var sessionAdapter: DashboardSessionAdapter
    private lateinit var expenseAdapter: DashboardExpenseAdapter
    private var revenueXAxisFormatter: IndexAxisValueFormatter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupCharts()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerViews() {
        sessionAdapter = DashboardSessionAdapter(
            onItemClick = { _ ->
                requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView).selectedItemId = R.id.sessionsFragment
            }
        )
        binding.rvRecentSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentSessions.adapter = sessionAdapter

        expenseAdapter = DashboardExpenseAdapter(viewModel.currency.value)
        binding.rvRecentExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentExpenses.adapter = expenseAdapter
    }

    private fun setupCharts() {
        binding.revenueChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            setFitBars(true)
            setDrawValueAboveBar(true)
            legend.isEnabled = false
            setTouchEnabled(false)
        }

        binding.expensePieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 50f
            setUsePercentValues(true)
            setDrawEntryLabels(true)
            setEntryLabelTextSize(11f)
            setEntryLabelColor(android.graphics.Color.WHITE)
            centerText = requireContext().getString(R.string.expenses)
            setCenterTextSize(16f)
            legend.isEnabled = true
            setTouchEnabled(false)
        }
    }

    private fun getCategoryFriendlyName(categoryName: String): String {
        return when (categoryName) {
            "ELECTRICITY" -> requireContext().getString(R.string.category_electricity)
            "INTERNET" -> requireContext().getString(R.string.category_internet)
            "MAINTENANCE" -> requireContext().getString(R.string.category_maintenance)
            "PURCHASES" -> requireContext().getString(R.string.category_purchases)
            "WATER" -> requireContext().getString(R.string.category_water)
            else -> requireContext().getString(R.string.category_other)
        }
    }

    private fun setupListeners() {
        binding.btnNewSession.setOnClickListener {
            NewSessionDialog().show(childFragmentManager, "NewSessionDialog")
        }
        binding.btnAddExpense.setOnClickListener {
            AddExpenseDialog.newInstance().show(childFragmentManager, "AddExpenseDialog")
        }
        binding.btnInventory.setOnClickListener {
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView).selectedItemId = R.id.inventoryFragment
        }
        binding.btnReceipts.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.dashboardFragment) {
                navController.navigate(R.id.action_dashboardFragment_to_receiptsFragment)
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }
            }
        }
    }

    private fun handleUiState(state: UiState<DashboardData>) {
        when (state) {
            is UiState.Loading -> {
                binding.progressBar.isVisible = true
            }
            is UiState.Success -> {
                binding.progressBar.isVisible = false
                bindData(state.data)
                animateUiIn()
            }
            is UiState.Error -> {
                binding.progressBar.isVisible = false
                android.widget.Toast.makeText(requireContext(), state.message.asString(requireContext()), android.widget.Toast.LENGTH_LONG).show()
            }
            is UiState.Empty -> {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun bindData(data: DashboardData) {
        val currencyStr = viewModel.currency.value
        
        binding.tvRevenue.text = CurrencyUtils.formatAmount(requireContext(), data.todayRevenue, currencyStr)
        binding.tvExpenses.text = CurrencyUtils.formatAmount(requireContext(), data.todayExpenses, currencyStr)
        binding.tvNetProfit.text = CurrencyUtils.formatAmount(requireContext(), data.netProfit, currencyStr)
        binding.tvSessions.text = data.sessionsToday.toString()
        binding.tvTotalProducts.text = data.totalProducts.toString()
        binding.tvLowStock.text = data.lowStockProducts.toString()

        sessionAdapter.submitList(data.recentSessions)
        binding.tvEmptySessions.isVisible = data.recentSessions.isEmpty()
        binding.rvRecentSessions.isVisible = data.recentSessions.isNotEmpty()

        expenseAdapter.submitList(data.recentExpenses)
        binding.tvEmptyExpenses.isVisible = data.recentExpenses.isEmpty()
        binding.rvRecentExpenses.isVisible = data.recentExpenses.isNotEmpty()

        bindCharts(data)
    }

    private fun bindCharts(data: DashboardData) {
        if (data.revenueChartData.isNotEmpty()) {
            val barEntries = data.revenueChartData.mapIndexed { index, point ->
                BarEntry(index.toFloat(), point.value)
            }
            val barDataSet = BarDataSet(barEntries, requireContext().getString(R.string.revenue)).apply {
                color = requireContext().getColor(R.color.ps_blue_primary)
                valueTextSize = 10f
                valueTextColor = requireContext().getColor(R.color.text_primary)
            }
            
            if (binding.revenueChart.data == null) {
                binding.revenueChart.data = BarData(barDataSet)
                revenueXAxisFormatter = IndexAxisValueFormatter(data.revenueChartData.map { it.label })
                binding.revenueChart.xAxis.apply {
                    valueFormatter = revenueXAxisFormatter
                    textColor = requireContext().getColor(R.color.text_primary)
                }
                binding.revenueChart.axisLeft.textColor = requireContext().getColor(R.color.text_primary)
                binding.revenueChart.animateY(1000)
            } else {
                binding.revenueChart.data = BarData(barDataSet)
                revenueXAxisFormatter?.values = data.revenueChartData.map { it.label }.toTypedArray()
                binding.revenueChart.invalidate()
            }
        }

        if (data.expenseChartData.isNotEmpty()) {
            val pieEntries = data.expenseChartData.map { point ->
                PieEntry(point.value, getCategoryFriendlyName(point.label))
            }
            val pieDataSet = PieDataSet(pieEntries, requireContext().getString(R.string.expenses)).apply {
                colors = listOf(
                    requireContext().getColor(R.color.chart_green),
                    requireContext().getColor(R.color.chart_red),
                    requireContext().getColor(R.color.chart_yellow),
                    requireContext().getColor(R.color.chart_blue),
                    requireContext().getColor(R.color.chart_purple)
                )
                valueTextSize = 12f
                valueTextColor = android.graphics.Color.WHITE
            }
            
            if (binding.expensePieChart.data == null) {
                binding.expensePieChart.data = PieData(pieDataSet)
                binding.expensePieChart.legend.textColor = requireContext().getColor(R.color.text_primary)
                binding.expensePieChart.setCenterTextColor(requireContext().getColor(R.color.text_primary))
                binding.expensePieChart.animateY(1000)
            } else {
                binding.expensePieChart.data = PieData(pieDataSet)
                binding.expensePieChart.invalidate()
            }
        } else {
            binding.expensePieChart.clear()
        }
    }

    private fun animateUiIn() {
        if (binding.gridKpi.alpha == 0f) {
            binding.gridKpi.animate().alpha(1f).setDuration(500).start()
            binding.layoutCharts.animate().alpha(1f).setStartDelay(200).setDuration(500).start()
            binding.layoutRecent.animate().alpha(1f).setStartDelay(400).setDuration(500).start()
        }
    }

    override fun onDestroyView() {
        binding.rvRecentSessions.adapter = null
        binding.rvRecentExpenses.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
