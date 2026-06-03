package com.mohamed.playstation.presentation.ui.sessions

import android.app.Dialog
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohamed.playstation.R
import com.mohamed.playstation.databinding.DialogAddProductBinding
import com.mohamed.playstation.presentation.viewmodel.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddProductDialog : DialogFragment() {

    private var _binding: DialogAddProductBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionViewModel by viewModels({ requireParentFragment() })

    private var sessionId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionId = arguments?.getLong(ARG_SESSION_ID) ?: 0L
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddProductBinding.inflate(layoutInflater)
        clearErrorsOnInput()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_product)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                if (saveProduct()) {
                    dismiss()
                }
            }
        }

        return dialog
    }

    private fun clearErrorsOnInput() {
        binding.etProductName.doAfterTextChanged { binding.tilProductName.error = null }
        binding.etProductPrice.doAfterTextChanged { binding.tilProductPrice.error = null }
        binding.etQuantity.doAfterTextChanged { binding.tilQuantity.error = null }
    }

    private fun saveProduct(): Boolean {
        if (sessionId <= 0L) return false

        val name = binding.etProductName.text?.toString()?.trim().orEmpty()
        val price = binding.etProductPrice.text?.toString()?.toDoubleOrNull()
        val quantity = binding.etQuantity.text?.toString()?.toIntOrNull()

        var isValid = true

        if (name.isBlank()) {
            binding.tilProductName.error = getString(R.string.required_field)
            isValid = false
        }

        if (price == null || price <= 0) {
            binding.tilProductPrice.error = getString(R.string.invalid_price)
            isValid = false
        }

        if (quantity == null || quantity <= 0) {
            binding.tilQuantity.error = getString(R.string.invalid_quantity)
            isValid = false
        }

        if (!isValid) return false

        viewModel.addProduct(
            sessionId = sessionId,
            name = name,
            price = price!!,
            quantity = quantity!!
        )
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val ARG_SESSION_ID = "session_id"

        fun newInstance(sessionId: Long): AddProductDialog {
            return AddProductDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_SESSION_ID, sessionId)
                }
            }
        }
    }
}
