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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.databinding.FragmentExpensesBinding
import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.presentation.viewmodel.ExpenseViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(
            currency = viewModel.currency.value,
            onDelete = { expense -> showDeleteConfirmation(expense) }
        )
        binding.rvExpenses.layoutManager = LinearLayoutManager(requireContext())
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
                    viewModel.expenses.collect { list ->
                        adapter.submitList(list)
                        binding.tvEmpty.isVisible = list.isEmpty()
                        binding.rvExpenses.isVisible = list.isNotEmpty()
                    }
                }
                launch {
                    viewModel.totalAmount.collect { total ->
                        binding.tvTotalExpenses.text = CurrencyUtils.formatAmount(total, viewModel.currency.value)
                    }
                }
                launch {
                    viewModel.expenseCount.collect { count ->
                        binding.tvExpenseCount.text = count.toString()
                    }
                }
                launch {
                    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("ar"))
                    binding.tvCurrentMonth.text = monthFormat.format(Date())
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
        super.onDestroyView()
        _binding = null
    }
}
