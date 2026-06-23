package com.mohamed.playstation.presentation.ui.sessions

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohamed.playstation.R
import com.mohamed.playstation.databinding.DialogAddSessionProductBinding
import com.mohamed.playstation.domain.model.InventoryItem
import com.mohamed.playstation.presentation.viewmodel.SessionViewModel

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddProductDialog : DialogFragment() {

    private var _binding: DialogAddSessionProductBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionViewModel by viewModels({ requireParentFragment() })

    private var sessionId: Long = -1L
    private var inventoryProducts: List<InventoryItem> = emptyList()
    private var selectedProduct: InventoryItem? = null
    private lateinit var productAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionId = requireArguments().getLong(ARG_SESSION_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddSessionProductBinding.inflate(layoutInflater)
        setupProductDropdown()
        clearErrorsOnInput()
        observeInventoryProducts()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_product)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                submitProduct(dialog)
            }
        }

        return dialog
    }

    private fun setupProductDropdown() {
        productAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        binding.actInventoryProduct.setAdapter(productAdapter)
        binding.actInventoryProduct.setOnClickListener {
            binding.actInventoryProduct.showDropDown()
        }
        binding.actInventoryProduct.setOnItemClickListener { _, _, position, _ ->
            selectedProduct = inventoryProducts.getOrNull(position)
            selectedProduct?.let(::showSelectedProduct)
        }
    }

    private fun observeInventoryProducts() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.inventoryProducts.collect { products ->
                    updateInventoryProducts(products)
                }
            }
        }
    }

    private fun updateInventoryProducts(products: List<InventoryItem>) {
        inventoryProducts = products
        productAdapter.clear()
        productAdapter.addAll(products.map { it.name })
        productAdapter.notifyDataSetChanged()

        val selectedId = selectedProduct?.id ?: return
        val updatedSelection = products.firstOrNull { it.id == selectedId }
        selectedProduct = updatedSelection
        if (updatedSelection != null) {
            showSelectedProduct(updatedSelection, updateName = false)
        } else {
            clearSelectedProduct()
        }
    }

    private fun showSelectedProduct(product: InventoryItem, updateName: Boolean = true) {
        if (updateName) {
            binding.actInventoryProduct.setText(product.name, false)
        }
        binding.etProductPrice.setText(product.sellPrice.toString())
        binding.etAvailableQuantity.setText(product.quantity.toString())
        binding.tilInventoryProduct.error = null
        binding.tilRequestedQuantity.error = null
    }

    private fun clearSelectedProduct() {
        binding.actInventoryProduct.setText("", false)
        binding.etProductPrice.setText("")
        binding.etAvailableQuantity.setText("")
        selectedProduct = null
    }

    private fun clearErrorsOnInput() {
        binding.actInventoryProduct.doAfterTextChanged {
            binding.tilInventoryProduct.error = null
        }
        binding.etRequestedQuantity.doAfterTextChanged {
            binding.tilRequestedQuantity.error = null
        }
    }

    private fun submitProduct(dialog: AlertDialog) {

        val product = selectedProduct
        val quantity = binding.etRequestedQuantity.text?.toString()?.toIntOrNull()

        var isValid = true

        if (product == null) {
            binding.tilInventoryProduct.error = getString(R.string.select_inventory_product)
            isValid = false
        }

        if (quantity == null || quantity <= 0) {
            binding.tilRequestedQuantity.error = getString(R.string.invalid_quantity)
            isValid = false
        } else if (product != null && quantity > product.quantity) {
            binding.tilRequestedQuantity.error = getString(R.string.insufficient_stock)
            isValid = false
        }

        if (!isValid || product == null || quantity == null) return

        dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = false
        viewModel.addInventoryProductToSession(
            sessionId = sessionId,
            inventoryProductId = product.id,
            quantity = quantity,
            onSuccess = {
                if (isAdded) {
                    dismiss()
                }
            },
            onError = { error ->
                val currentBinding = _binding
                if (currentBinding != null) {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = true
                    val message = error.message.orEmpty()
                    when (message) {
                        "INSUFFICIENT_STOCK" -> currentBinding.tilRequestedQuantity.error = getString(R.string.insufficient_stock)
                        "PRODUCT_NOT_FOUND" -> currentBinding.tilInventoryProduct.error = getString(R.string.product_not_found)
                        "INVALID_QUANTITY" -> currentBinding.tilRequestedQuantity.error = getString(R.string.invalid_quantity)
                        else -> currentBinding.tilInventoryProduct.error = getString(R.string.error_occurred)
                    }
                }
            }
        )
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
