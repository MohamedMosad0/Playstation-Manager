package com.mohamed.playstation.presentation.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.AppFormatters
import com.mohamed.playstation.core.utils.UnitFormatUtils
import com.mohamed.playstation.core.utils.UnitType
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
                binding.etPrice.setText(AppFormatters.formatEditableAmount(requireContext(), match.sellPrice))
                binding.etMinQty.setText(AppFormatters.formatInteger(requireContext(), match.minimumQuantity))
                
                binding.tvCurrentAvailable.visibility = View.VISIBLE
                val unitName = match.unitLabel
                val pluralUnit = requireContext().getString(UnitFormatUtils.getPluralUnitRes(unitName))
                val definitePlural = requireContext().getString(UnitFormatUtils.getDefinitePluralUnitRes(unitName))
                
                if (match.isPrepared) {
                    val icon = if (unitName == UnitType.PACK.rawDbValue) "🍜" else "☕"
                binding.tvCurrentAvailable.text = "$icon ${getString(R.string.currently_available_format, AppFormatters.formatInteger(requireContext(), match.quantity), pluralUnit)}"
                    binding.etInitialQty.hint = getString(R.string.new_quantity_hint, definitePlural)
                    binding.btnSave.text = getString(R.string.action_add)
                    binding.etMinQty.visibility = View.GONE
                } else {
                binding.tvCurrentAvailable.text = "📦 ${getString(R.string.currently_available_format, AppFormatters.formatInteger(requireContext(), match.quantity), pluralUnit)}"
                    binding.etInitialQty.hint = getString(R.string.new_quantity_hint, definitePlural)
                    binding.btnSave.text = getString(R.string.action_add)
                    binding.etMinQty.visibility = View.VISIBLE
                }
            } else {
                binding.tvCurrentAvailable.visibility = View.GONE
                binding.etInitialQty.hint = getString(R.string.inventory_quantity_to_add)
                binding.btnSave.text = getString(R.string.inventory_save)
                binding.etMinQty.visibility = View.VISIBLE
            }
        }

        binding.btnSave.setOnClickListener {
            val rawName = binding.actProductSearch.text.toString()
            val name = rawName.trim()
            val price = AppFormatters.parseDecimal(binding.etPrice.text) ?: 0.0
            val minQty = AppFormatters.parseInteger(binding.etMinQty.text) ?: 0
            val qty = AppFormatters.parseInteger(binding.etInitialQty.text) ?: 0
            if (selectedProductId == null) {
                // creating new product: validate name, price, minQty, qty
                if (name.isBlank()) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        getString(R.string.required_field),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (price < 0.0) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_price),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (minQty < 0) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_quantity),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (qty < 0) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_quantity),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // create new product and add initial stock
                viewModel.addNewProduct(name, price, 0.0, qty, minQty, false, UnitType.PIECE.rawDbValue)

                // clear fields
                binding.actProductSearch.setText("")
                binding.etPrice.setText("")
                binding.etMinQty.setText("")
                binding.etInitialQty.setText("")
                binding.tvCurrentAvailable.visibility = View.GONE
                binding.etInitialQty.hint = getString(R.string.inventory_quantity_to_add)
                binding.btnSave.text = getString(R.string.inventory_save)
                binding.etMinQty.visibility = View.VISIBLE
                selectedProductId = null

            } else {
                // existing product: increase stock, validate qty
                if (qty < 0) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_quantity),
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
                binding.tvCurrentAvailable.visibility = View.GONE
                binding.etInitialQty.hint = getString(R.string.inventory_quantity_to_add)
                binding.btnSave.text = getString(R.string.inventory_save)
                binding.etMinQty.visibility = View.VISIBLE
                selectedProductId = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

