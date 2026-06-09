package com.mohamed.playstation.presentation.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mohamed.playstation.databinding.FragmentInventoryNewStockBinding
import com.mohamed.playstation.presentation.viewmodel.InventoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NewStockTabFragment : Fragment() {

    private var _binding: FragmentInventoryNewStockBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels({ requireParentFragment() })

    private var selectedProductId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryNewStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val productNames = mutableListOf<String>()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            productNames
        )
        binding.actProductSearch.setAdapter(adapter)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.products.collect { list ->
                    productNames.clear()
                    productNames.addAll(list.map { it.name })
                    adapter.notifyDataSetChanged()
                }
            }
        }

        binding.actProductSearch.setOnItemClickListener { parent, viewP, position, id ->
            val name = parent.getItemAtPosition(position) as String
            val match = viewModel.products.value.firstOrNull { it.name == name }
            selectedProductId = match?.id
            if (match != null) {
                binding.etPrice.setText(match.price.toString())
                binding.etMinQty.setText(match.minimumQuantity.toString())
            }
        }

        binding.btnSave.setOnClickListener {
            val rawName = binding.actProductSearch.text.toString()
            val name = rawName.trim()
            val price = binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0
            val minQty = binding.etMinQty.text.toString().toIntOrNull() ?: 0
            val qty = binding.etInitialQty.text.toString().toIntOrNull() ?: 0

            val sessionId = viewModel.currentSessionId.value ?: return@setOnClickListener

            // Validation
            if (selectedProductId == null) {
                // creating new product: validate name, price, minQty, qty
                if (name.isBlank()) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        getString(com.mohamed.playstation.R.string.required_field),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (price < 0.0) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        getString(com.mohamed.playstation.R.string.invalid_price),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (minQty < 0) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        getString(com.mohamed.playstation.R.string.invalid_quantity),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (qty < 0) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        getString(com.mohamed.playstation.R.string.invalid_quantity),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // Check for duplicate product
                viewLifecycleOwner.lifecycleScope.launch {
                    val exists = viewModel.checkProductExists(sessionId, name)
                    if (exists) {
                        android.widget.Toast.makeText(
                            requireContext(),
                            getString(com.mohamed.playstation.R.string.product_exists),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    // create new product and add initial stock
                    viewModel.addNewProduct(sessionId, name, price, minQty, qty)

                    // clear fields
                    binding.actProductSearch.setText("")
                    binding.etPrice.setText("")
                    binding.etMinQty.setText("")
                    binding.etInitialQty.setText("")
                    selectedProductId = null
                }

            } else {
                // existing product: increase stock, validate qty
                if (qty < 0) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        getString(com.mohamed.playstation.R.string.invalid_quantity),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                viewModel.addStockToProduct(selectedProductId!!, qty)

                // clear fields
                binding.actProductSearch.setText("")
                binding.etPrice.setText("")
                binding.etMinQty.setText("")
                binding.etInitialQty.setText("")
                selectedProductId = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

