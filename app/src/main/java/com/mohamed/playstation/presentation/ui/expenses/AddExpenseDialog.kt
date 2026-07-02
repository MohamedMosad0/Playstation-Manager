package com.mohamed.playstation.presentation.ui.expenses

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohamed.playstation.R
import com.mohamed.playstation.databinding.DialogAddExpenseBinding
import com.mohamed.playstation.domain.model.ExpenseCategory
import com.mohamed.playstation.presentation.viewmodel.ExpenseViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddExpenseDialog : DialogFragment() {

    private var _binding: DialogAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExpenseViewModel by viewModels({ requireParentFragment() })
    private var selectedCategory: ExpenseCategory? = null
    private var selectedDate = Calendar.getInstance().time

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddExpenseBinding.inflate(layoutInflater)

        setupCategoryGrid()
        setupDatePicker()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_expense)
            .setView(binding.root)
            .setPositiveButton(R.string.save) { _, _ -> saveExpense() }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    private fun setupCategoryGrid() {
        val adapter = CategoryGridAdapter { category ->
            selectedCategory = category
        }
        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvCategories.adapter = adapter
        adapter.submitList(ExpenseCategory.values().toList())
    }

    private fun setupDatePicker() {
        binding.etDate.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate))
        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    selectedDate = calendar.time
                    binding.etDate.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun saveExpense() {
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val category = selectedCategory
        val description = binding.etDescription.text.toString()

        if (amount != null && amount > 0 && category != null) {
            viewModel.addExpense(amount, category, description, selectedDate)
        }
    }

    override fun onDestroyView() {
        binding.rvCategories.adapter = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AddExpenseDialog()
    }
}
