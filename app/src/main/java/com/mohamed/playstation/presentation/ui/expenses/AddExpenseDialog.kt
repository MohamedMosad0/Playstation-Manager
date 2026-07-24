package com.mohamed.playstation.presentation.ui.expenses

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.AppFormatters
import com.mohamed.playstation.databinding.DialogAddExpenseBinding
import com.mohamed.playstation.domain.model.ExpenseCategory
import com.mohamed.playstation.presentation.viewmodel.ExpenseViewModel
import dagger.hilt.android.AndroidEntryPoint
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
        setupValidation()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_expense)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                saveExpense(dialog)
            }
        }
        return dialog
    }

    private fun setupCategoryGrid() {
        val adapter = CategoryGridAdapter { category ->
            selectedCategory = category
            binding.tvCategoryError.visibility = View.GONE
        }
        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvCategories.setHasFixedSize(true)
        binding.rvCategories.adapter = adapter
        adapter.submitList(ExpenseCategory.values().toList())
    }

    private fun setupValidation() {
        binding.etAmount.doAfterTextChanged {
            binding.tilAmount.error = null
        }
    }

    private fun setupDatePicker() {
        binding.etDate.setText(AppFormatters.formatDate(requireContext(), selectedDate))
        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    selectedDate = calendar.time
                    binding.etDate.setText(AppFormatters.formatDate(requireContext(), selectedDate))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun saveExpense(dialog: AlertDialog) {
        val amount = AppFormatters.parseDecimal(binding.etAmount.text)
        val category = selectedCategory
        val description = binding.etDescription.text.toString()

        var isValid = true
        if (amount == null || amount <= 0) {
            binding.tilAmount.error = getString(R.string.invalid_expense_amount)
            isValid = false
        }
        if (category == null) {
            binding.tvCategoryError.text = getString(R.string.select_expense_category)
            binding.tvCategoryError.visibility = View.VISIBLE
            isValid = false
        }
        if (!isValid || amount == null || category == null) return

        val saveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
        saveButton.isEnabled = false
        viewModel.addExpense(
            amount = amount,
            category = category,
            description = description,
            date = selectedDate,
            onSuccess = {
                if (isAdded) dismissAllowingStateLoss()
            },
            onError = {
                if (_binding != null) {
                    saveButton.isEnabled = true
                    Snackbar.make(binding.root, R.string.error_occurred, Snackbar.LENGTH_LONG).show()
                }
            }
        )
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
