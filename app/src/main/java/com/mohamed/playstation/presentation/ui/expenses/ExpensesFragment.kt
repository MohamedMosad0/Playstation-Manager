package com.mohamed.playstation.presentation.ui.expenses

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
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.databinding.FragmentExpensesBinding
import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.domain.model.filter.DateRangeFilter
import com.mohamed.playstation.presentation.viewmodel.ExpenseViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var adapter: ExpenseAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        setupDateFilter()
        observeData()
    }

    private fun setupDateFilter() {
        binding.cardDateRange.setOnClickListener {
            showDateRangePicker()
        }

        binding.chipGroupDateFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            
            when (checkedIds.first()) {
                R.id.chipToday -> viewModel.setDateFilter(DateRangeFilter.TODAY)
                R.id.chipThisWeek -> viewModel.setDateFilter(DateRangeFilter.THIS_WEEK)
                R.id.chipThisMonth -> viewModel.setDateFilter(DateRangeFilter.THIS_MONTH)
                R.id.chipLastMonth -> viewModel.setDateFilter(DateRangeFilter.LAST_MONTH)
                R.id.chipLast3Months -> viewModel.setDateFilter(DateRangeFilter.LAST_3_MONTHS)
            }
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.filter_custom))
            .build()
            
        picker.addOnPositiveButtonClickListener { selection ->
            val start = selection.first
            val end = selection.second
            if (start != null && end != null) {
                binding.chipGroupDateFilters.clearCheck()
                viewModel.setCustomDateRange(start, end)
            }
        }
        
        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(
            currency = viewModel.currency.value,
            onDelete = { expense -> showDeleteConfirmation(expense) }
        )
        binding.rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExpenses.setHasFixedSize(true)
        binding.rvExpenses.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAddExpense.setOnClickListener {
            AddExpenseDialog.newInstance().show(childFragmentManager, "AddExpenseDialog")
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    kotlinx.coroutines.flow.combine(
                        viewModel.expenses,
                        viewModel.currency
                    ) { list, currency ->
                        Pair(list, currency)
                    }.collect { (list, currency) ->
                        adapter.updateCurrency(currency)
                        adapter.submitList(list)
                        if (list.isNotEmpty()) {
                            binding.layoutEmpty.isVisible = false
                            binding.rvExpenses.isVisible = true
                        } else {
                            binding.layoutEmpty.isVisible = true
                            binding.rvExpenses.isVisible = false
                        }
                    }
                }
                launch {
                    kotlinx.coroutines.flow.combine(
                        viewModel.totalAmount,
                        viewModel.currency
                    ) { total, currency ->
                        Pair(total, currency)
                    }.collect { (total, currency) ->
                        binding.tvTotalExpenses.text = CurrencyUtils.formatAmount(requireContext(), total, currency)
                    }
                }
                launch {
                    viewModel.expenseCount.collect { count ->
                        binding.tvExpenseCount.text = count.toString()
                    }
                }
                launch {
                    viewModel.dateFilterFlow.collect { filter ->
                        // Update UI title based on the filter
                        val titleRes = when (filter) {
                            DateRangeFilter.TODAY -> R.string.today
                            DateRangeFilter.THIS_WEEK -> R.string.filter_this_week
                            DateRangeFilter.LAST_7_DAYS -> R.string.filter_last_7_days
                            DateRangeFilter.THIS_MONTH -> R.string.this_month
                            DateRangeFilter.LAST_MONTH -> R.string.filter_last_month
                            DateRangeFilter.LAST_30_DAYS -> R.string.filter_last_30_days
                            DateRangeFilter.LAST_3_MONTHS -> R.string.filter_last_3_months
                            DateRangeFilter.ALL_TIME -> R.string.all
                            DateRangeFilter.CUSTOM -> R.string.filter_custom
                        }
                        binding.tvDateRange.text = getString(titleRes)
                        // No programmatic chip updates needed because UI matches ViewModel flow defaults
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmation(expense: Expense) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(R.string.confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteExpense(expense)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        binding.rvExpenses.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
