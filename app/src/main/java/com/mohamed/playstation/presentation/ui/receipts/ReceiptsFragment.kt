package com.mohamed.playstation.presentation.ui.receipts

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
import com.google.android.material.datepicker.MaterialDatePicker
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.databinding.FragmentReceiptsBinding
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.SessionProductSummary
import com.mohamed.playstation.domain.model.filter.DateRangeFilter
import com.mohamed.playstation.presentation.ui.UiState
import com.mohamed.playstation.presentation.viewmodel.ReceiptViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReceiptsFragment : Fragment() {

    private var _binding: FragmentReceiptsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReceiptViewModel by viewModels()
    private lateinit var receiptAdapter: ReceiptAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupDateFilter()
        observeData()
    }

    private fun setupRecyclerView() {
        receiptAdapter = ReceiptAdapter { receiptId ->
            showReceiptDetail(receiptId)
        }
        binding.rvReceipts.adapter = receiptAdapter
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

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Revenue + Currency
                launch {
                    combine(
                        viewModel.periodRevenue,
                        viewModel.currency
                    ) { revenue, currency -> revenue to currency }
                        .collect { (revenue, currency) ->
                            binding.tvTodayRevenue.text = CurrencyUtils.formatAmount(requireContext(), revenue, currency)
                        }
                }

                // Receipts + Summaries
                launch {
                    combine(
                        viewModel.receipts,
                        viewModel.productSummaries
                    ) { state, summaries -> state to summaries }
                        .collect { (state, summaries) ->
                            handleUiState(state, summaries)
                        }
                }
                
                // Date Filter Title
                launch {
                    viewModel.dateFilterFlow.collect { filter ->
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
                    }
                }
            }
        }
    }

    private fun handleUiState(state: UiState<List<Receipt>>, summaries: Map<Long, SessionProductSummary>) {
        when (state) {
            is UiState.Loading -> {
                if (receiptAdapter.itemCount == 0) {
                    binding.progressBar.isVisible = true
                    binding.rvReceipts.isVisible = false
                }
                binding.emptyState.isVisible = false
            }

            is UiState.Success -> {
                binding.progressBar.isVisible = false
                binding.emptyState.isVisible = false
                binding.rvReceipts.isVisible = true
                
                val uiModels = state.data.map { receipt ->
                    com.mohamed.playstation.presentation.ui.receipts.mapper.ReceiptDisplayMapper.mapToListItem(
                        requireContext(),
                        receipt,
                        summaries[receipt.sessionId]
                    )
                }
                receiptAdapter.submitList(uiModels)
            }

            is UiState.Empty -> {
                binding.progressBar.isVisible = false
                binding.emptyState.isVisible = true
                binding.rvReceipts.isVisible = false
            }

            is UiState.Error -> {
                binding.progressBar.isVisible = false
                binding.emptyState.isVisible = true
                binding.rvReceipts.isVisible = false
                android.widget.Toast.makeText(requireContext(), state.message.asString(requireContext()), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReceiptDetail(receiptId: Long) {
        val dialog = ReceiptDetailDialog.newInstance(receiptId = receiptId)
        dialog.show(childFragmentManager, "ReceiptDetailDialog")
    }

    override fun onDestroyView() {
        binding.rvReceipts.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
