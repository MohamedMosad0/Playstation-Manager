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
import com.google.android.material.tabs.TabLayout
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.databinding.FragmentReceiptsBinding
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.SessionProductSummary
import com.mohamed.playstation.presentation.ui.UiState
import com.mohamed.playstation.presentation.viewmodel.ReceiptViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class ReceiptsFragment : Fragment() {

    private var _binding: FragmentReceiptsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReceiptViewModel by viewModels()
    private lateinit var receiptAdapter: ReceiptAdapter

    private val _selectedTab = MutableStateFlow(0)
    private var latestProductSummaries: Map<Long, SessionProductSummary> = emptyMap()

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
        setupTabs()
        observeData()
    }

    private fun setupRecyclerView() {
        receiptAdapter = ReceiptAdapter { receipt ->
            showReceiptDetail(receipt)
        }

        binding.rvReceipts.adapter = receiptAdapter
        receiptAdapter.submitProductSummaries(latestProductSummaries)
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                _selectedTab.value = tab?.position ?: 0
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    /**
     * مراقبة البيانات — reactive بالكامل
     * يجمع todayRevenue + currency معاً حتى يتحدث العرض عند تغيير أي منهما.
     */
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Revenue + Currency — combined reactively
                launch {
                    combine(
                        viewModel.todayRevenue,
                        viewModel.currency
                    ) { revenue, currency -> revenue to currency }
                        .collect { (revenue, currency) ->
                            val symbol = CurrencyUtils.getCurrencySymbol(currency)
                            binding.tvTodayRevenue.text = "${String.format(Locale.getDefault(), "%.2f", revenue)} $symbol"
                        }
                }

                launch {
                    viewModel.productSummaries.collect { summaries ->
                        latestProductSummaries = summaries
                        receiptAdapter.submitProductSummaries(summaries)
                    }
                }

                // Single reactive collector — switches source on tab change via flatMapLatest
                launch {
                    _selectedTab
                        .flatMapLatest { tab ->
                            if (tab == 0) viewModel.todayReceipts else viewModel.allReceipts
                        }
                        .collect { state ->
                            handleUiState(state)
                        }
                }
            }
        }
    }

    private fun handleUiState(state: UiState<List<Receipt>>) {
        when (state) {
            is UiState.Loading -> {
                binding.progressBar.isVisible = true
                binding.emptyState.isVisible = false
                binding.rvReceipts.isVisible = false
            }

            is UiState.Success -> {
                binding.progressBar.isVisible = false
                binding.emptyState.isVisible = false
                binding.rvReceipts.isVisible = true
                receiptAdapter.submitList(state.data)
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
            }
        }
    }

    private fun showReceiptDetail(receipt: Receipt) {
        val dialog = ReceiptDetailDialog.newInstance(receiptId = receipt.id)
        dialog.show(childFragmentManager, "ReceiptDetailDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
