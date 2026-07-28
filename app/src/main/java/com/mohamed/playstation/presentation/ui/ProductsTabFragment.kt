package com.mohamed.playstation.presentation.ui.inventory

import android.os.Bundle
import android.text.BidiFormatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mohamed.playstation.R
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.utils.AppFormatters
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.core.utils.UnitFormatUtils
import com.mohamed.playstation.core.utils.UnitType
import com.mohamed.playstation.databinding.FragmentInventoryProductsBinding
import com.mohamed.playstation.domain.model.InventoryItem
import com.mohamed.playstation.presentation.viewmodel.InventoryViewModel
import com.mohamed.playstation.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductsTabFragment : Fragment() {

    private var _binding: FragmentInventoryProductsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels({ requireParentFragment() })
    private val settingsViewModel: SettingsViewModel by activityViewModels()

    private var scrollListener: RecyclerView.OnScrollListener? = null
    private lateinit var adapter: ProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ProductsAdapter(onEdit = { product ->
            showEditDialog(product)
        }, onDelete = { product ->
            showDeleteDialog(product)
        })

        binding.rvProducts.adapter = adapter
        
        binding.emptyState.visibility = View.GONE
        binding.rvProducts.visibility = View.VISIBLE

        searchWatcher = binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }

        binding.btnNewProduct.setOnClickListener {
            showNewProductDialog()
        }
        
        // Hide/Show FAB on scroll
        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && binding.btnNewProduct.isShown) {
                    binding.btnNewProduct.hide()
                } else if (dy < 0 && !binding.btnNewProduct.isShown) {
                    binding.btnNewProduct.show()
                }
            }
        }
        binding.rvProducts.addOnScrollListener(scrollListener!!)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.products.collect { list ->
                        adapter.submitList(list) {
                            if (list.isEmpty()) {
                                binding.emptyState.visibility = View.VISIBLE
                                binding.rvProducts.visibility = View.GONE
                            } else {
                                binding.emptyState.visibility = View.GONE
                                binding.rvProducts.visibility = View.VISIBLE
                            }
                        }
                    }
                }
                launch {
                    settingsViewModel.currency.collect { currency ->
                        adapter.currencyCode = currency
                        adapter.notifyItemRangeChanged(0, adapter.itemCount)
                    }
                }
            }
        }
    }

    private fun showNewProductDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_product, null)
        val etName     = dialogView.findViewById<TextInputEditText>(R.id.etProductName)
        val etPrice    = dialogView.findViewById<TextInputEditText>(R.id.etProductPrice)
        val tilQty     = dialogView.findViewById<TextInputLayout>(R.id.tilQuantity)
        val etQty      = dialogView.findViewById<TextInputEditText>(R.id.etQuantity)
        val tilMinQty  = dialogView.findViewById<TextInputLayout>(R.id.tilMinimumQuantity)
        val etMinQty   = dialogView.findViewById<TextInputEditText>(R.id.etMinimumQuantity)

        val toggleType         = dialogView.findViewById<MaterialButtonToggleGroup>(R.id.toggleProductType)
        val tilUnitCost        = dialogView.findViewById<TextInputLayout>(R.id.tilUnitCost)
        val etUnitCost         = dialogView.findViewById<TextInputEditText>(R.id.etUnitCost)
        val tilPreparationCost = dialogView.findViewById<TextInputLayout>(R.id.tilPreparationCost)
        val etPreparationCost  = dialogView.findViewById<TextInputEditText>(R.id.etPreparationCost)
        val tilProduced        = dialogView.findViewById<TextInputLayout>(R.id.tilProducedUnits)
        val etProduced         = dialogView.findViewById<TextInputEditText>(R.id.etProducedUnits)
        val tvComputed         = dialogView.findViewById<TextView>(R.id.tvComputedCost)

        toggleType.check(R.id.btnTypeNormal)

        val updatePreview = {
            val pc = AppFormatters.parseDecimal(etPreparationCost.text) ?: 0.0
            val pu = AppFormatters.parseInteger(etProduced.text) ?: 0
            if (pc > 0 && pu > 0) {
                tvComputed.text = requireContext().getString(R.string.computed_unit_cost, pc / pu)
                tvComputed.visibility = View.VISIBLE
            } else {
                tvComputed.visibility = View.GONE
            }
        }
        etPreparationCost.doAfterTextChanged { updatePreview() }
        etProduced.doAfterTextChanged { updatePreview() }

        toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val isPrepared = checkedId == R.id.btnTypePrepared
            
            tilQty.isVisible = !isPrepared
            tilMinQty.isVisible = !isPrepared
            tilUnitCost.isVisible = !isPrepared
            
            tilPreparationCost.isVisible = isPrepared
            tilProduced.isVisible = isPrepared
            tvComputed.isVisible = isPrepared && tvComputed.text.isNotEmpty()
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.new_product))
            .setView(dialogView)
            .setPositiveButton(R.string.save) { d, _ ->
                val name   = etName.text.toString().trim()
                val price  = AppFormatters.parseDecimal(etPrice.text) ?: -1.0
                
                val isPrepared = toggleType.checkedButtonId == R.id.btnTypePrepared

                val qty: Int
                val minQty: Int
                val costPerUnit: Double

                if (isPrepared) {
                    val prepCost = AppFormatters.parseDecimal(etPreparationCost.text) ?: 0.0
                    val produced = AppFormatters.parseInteger(etProduced.text) ?: 0
                    qty = produced
                    minQty = 0 // By rule
                    costPerUnit = if (produced > 0) prepCost / produced else 0.0
                    
                    if (produced <= 0) {
                        Toast.makeText(requireContext(), R.string.invalid_unit_count, Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                } else {
                    qty = AppFormatters.parseInteger(etQty.text) ?: -1
                    minQty = AppFormatters.parseInteger(etMinQty.text) ?: -1
                    costPerUnit = AppFormatters.parseDecimal(etUnitCost.text) ?: 0.0
                    
                    if (qty < 0 || minQty < 0) {
                        Toast.makeText(requireContext(), getString(R.string.invalid_quantity), Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }

                if (name.isBlank()) {
                    Toast.makeText(requireContext(), getString(R.string.required_field), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (price < 0) {
                    Toast.makeText(requireContext(), getString(R.string.invalid_price), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val unitLabel = if (isPrepared) UnitType.CUP.rawDbValue else UnitType.PIECE.rawDbValue

                viewModel.addNewProduct(name, price, costPerUnit, qty, minQty, isPrepared, unitLabel)
                d.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)

        builder.show()
    }

    private fun showEditDialog(product: InventoryItem) {
        val dialogView     = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_product, null)
        val etName         = dialogView.findViewById<android.widget.EditText>(R.id.etName)
        val etPrice        = dialogView.findViewById<android.widget.EditText>(R.id.etPrice)
        val tilCurrentQty  = dialogView.findViewById<TextInputLayout>(R.id.tilCurrentQty)
        val etCurrentQty   = dialogView.findViewById<android.widget.EditText>(R.id.etCurrentQty)
        val tilMinQty      = dialogView.findViewById<TextInputLayout>(R.id.tilMinQty)
        val etMinQty       = dialogView.findViewById<android.widget.EditText>(R.id.etMinQty)
        val etCostPerUnit  = dialogView.findViewById<android.widget.EditText>(R.id.etCostPerUnit)
        val tvAvailable    = dialogView.findViewById<TextView>(R.id.tvAvailableStock)
        val tilNewPreparedQty = dialogView.findViewById<TextInputLayout>(R.id.tilNewPreparedQty)
        val etNewPreparedQty  = dialogView.findViewById<android.widget.EditText>(R.id.etNewPreparedQty)
        val tvPreparedQtyPreview = dialogView.findViewById<TextView>(R.id.tvPreparedQtyPreview)

        etName.setText(product.name)
        etPrice.setText(AppFormatters.formatEditableAmount(requireContext(), product.sellPrice))
        etCostPerUnit.setText(
            if (product.costPerUnit > 0) {
                AppFormatters.formatEditableAmount(requireContext(), product.costPerUnit)
            } else {
                ""
            }
        )

        val isPrepared = product.isPrepared

        if (isPrepared) {
            tilCurrentQty.isVisible = false
            tilMinQty.isVisible = false
            tvAvailable.isVisible = true
            tilNewPreparedQty.isVisible = true
            tvPreparedQtyPreview.isVisible = true
            
            val unitName = UnitFormatUtils.getLocalizedName(requireContext(), product.unitLabel)
            val pluralUnitRes = UnitFormatUtils.getPluralUnitRes(product.unitLabel)
            val pluralUnit = requireContext().getString(pluralUnitRes)
            tvAvailable.text = requireContext().getString(
                R.string.currently_available_format,
                AppFormatters.formatInteger(requireContext(), product.quantity),
                pluralUnit
            )
            
            etNewPreparedQty.doAfterTextChanged { text ->
                val delta = AppFormatters.parseInteger(text) ?: 0
                val newTotal = product.quantity + delta
                tvPreparedQtyPreview.text = requireContext().getString(
                    R.string.will_be_available_format,
                    AppFormatters.formatInteger(requireContext(), newTotal),
                    pluralUnit
                )
            }
        } else {
            etMinQty.setText(AppFormatters.formatInteger(requireContext(), product.minimumQuantity))
            etCurrentQty.setText(AppFormatters.formatInteger(requireContext(), product.quantity))
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.edit))
            .setView(dialogView)
            .setPositiveButton(R.string.save_changes) { d, _ ->
                val newName        = etName.text.toString().trim()
                val newPrice       = AppFormatters.parseDecimal(etPrice.text) ?: -1.0
                val newCostPerUnit = AppFormatters.parseDecimal(etCostPerUnit.text) ?: 0.0

                val newMin: Int
                val newQty: Int
                
                if (isPrepared) {
                    newMin = product.minimumQuantity
                    val delta = AppFormatters.parseInteger(etNewPreparedQty.text) ?: 0
                    newQty = product.quantity + delta
                } else {
                    newMin = AppFormatters.parseInteger(etMinQty.text) ?: -1
                    newQty = AppFormatters.parseInteger(etCurrentQty.text) ?: -1
                    if (newMin < 0 || newQty < 0) {
                        Toast.makeText(requireContext(), getString(R.string.invalid_quantity), Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }

                if (newName.isBlank()) {
                    Toast.makeText(requireContext(), getString(R.string.required_field), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newPrice < 0) {
                    Toast.makeText(requireContext(), getString(R.string.invalid_price), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newName != product.name) {
                    val updated = product.copy(
                        name = newName, sellPrice = newPrice,
                        minimumQuantity = newMin, quantity = newQty,
                        costPerUnit = newCostPerUnit
                    )
                    viewModel.updateProduct(updated)
                    d.dismiss()
                } else {
                    val updated = product.copy(
                        name = newName, sellPrice = newPrice,
                        minimumQuantity = newMin, quantity = newQty,
                        costPerUnit = newCostPerUnit
                    )
                    viewModel.updateProduct(updated)
                    d.dismiss()
                }
            }
            .setNegativeButton(R.string.cancel, null)

        builder.show()
    }

    private fun showDeleteDialog(product: InventoryItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.archive_title))
            .setMessage(getString(R.string.archive_message))
            .setPositiveButton(getString(R.string.archive_action)) { d, _ ->
                viewModel.archiveProduct(product.id)
                d.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private var searchWatcher: android.text.TextWatcher? = null

    override fun onDestroyView() {
        searchWatcher?.let { binding.etSearch.removeTextChangedListener(it) }
        searchWatcher = null
        scrollListener?.let { binding.rvProducts.removeOnScrollListener(it) }
        scrollListener = null
        binding.rvProducts.adapter = null
        super.onDestroyView()
        _binding = null
    }
}

class ProductsAdapter(
    private val onEdit: (InventoryItem) -> Unit,
    private val onDelete: (InventoryItem) -> Unit
) : ListAdapter<InventoryItem, ProductsAdapter.VH>(DIFF) {

    var currencyCode: String = AppConstants.DEFAULT_CURRENCY

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<InventoryItem>() {
            override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean = oldItem == newItem
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        private val tvUnitCostLabel: TextView = view.findViewById(R.id.tvUnitCostLabel)
        private val tvUnitCost: TextView = view.findViewById(R.id.tvUnitCost)
        private val tvProfitLabel: TextView = view.findViewById(R.id.tvProfitLabel)
        private val tvProfit: TextView = view.findViewById(R.id.tvProfit)
        private val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        private val tvStockLabel: TextView = view.findViewById(R.id.tvStockLabel)
        private val tvTypeBadge: TextView = view.findViewById(R.id.tvTypeBadge)
        private val chipStatus: com.google.android.material.chip.Chip = view.findViewById(R.id.chipStatus)
        private val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)

        fun bind(item: InventoryItem) {
            tvName.text = BidiFormatter.getInstance().unicodeWrap(item.name)
            tvPrice.text = CurrencyUtils.formatAmount(itemView.context, item.sellPrice, currencyCode)
            tvUnitCost.text = CurrencyUtils.formatAmount(itemView.context, item.costPerUnit, currencyCode)
            
            val profit = item.sellPrice - item.costPerUnit
            tvProfit.text = CurrencyUtils.formatAmount(itemView.context, profit, currencyCode)
            
            val isPrepared = item.isPrepared
            val unitName = item.unitLabel
            
            val definiteUnitRes = UnitFormatUtils.getDefiniteSingularUnitRes(unitName)
            val definiteUnit = itemView.context.getString(definiteUnitRes)
            tvUnitCostLabel.text = itemView.context.getString(R.string.cost_of_unit, definiteUnit)
            tvProfitLabel.text = itemView.context.getString(R.string.profit_of_unit, definiteUnit)
            
            tvQuantity.text = AppFormatters.formatInteger(itemView.context, item.quantity)

            if (isPrepared) {
                val icon = if (unitName == UnitType.PACK.rawDbValue) "🍜" else "☕"
                tvStockLabel.text = "$icon ${itemView.context.getString(R.string.available_label)}"
                tvTypeBadge.text = itemView.context.getString(R.string.prepared_product_badge)
                itemView.findViewById<TextView>(R.id.tvUnitLabel).text = UnitFormatUtils.getLocalizedName(itemView.context, unitName)
            } else {
                tvStockLabel.text = "📦 ${itemView.context.getString(R.string.available_label)}"
                tvTypeBadge.text = itemView.context.getString(R.string.normal_product_label)
                itemView.findViewById<TextView>(R.id.tvUnitLabel).text = UnitFormatUtils.getLocalizedName(itemView.context, unitName)
            }

            // 3 States Status Logic
            val isOutOfStock = item.quantity == 0
            val isLowStock = if (isPrepared) {
                item.quantity <= 10 && !isOutOfStock
            } else {
                item.quantity <= item.minimumQuantity && !isOutOfStock
            }

            if (item.quantity == 0) {
                chipStatus.text = itemView.context.getString(R.string.inventory_status_out_of_stock)
                chipStatus.setChipBackgroundColorResource(R.color.status_error)
                chipStatus.setTextColor(itemView.context.getColor(R.color.white))
                tvQuantity.setTextColor(ContextCompat.getColor(itemView.context, R.color.error))
            } else if (item.quantity <= item.minimumQuantity) {
                chipStatus.text = itemView.context.getString(R.string.inventory_status_low_stock)
                chipStatus.setChipBackgroundColorResource(R.color.status_paused)
                chipStatus.setTextColor(itemView.context.getColor(R.color.white))
                tvQuantity.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_paused))
            } else {
                chipStatus.text = itemView.context.getString(R.string.inventory_status_available)
                chipStatus.setChipBackgroundColorResource(R.color.status_active)
                chipStatus.setTextColor(itemView.context.getColor(R.color.white))
                tvQuantity.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            }

            btnEdit.setOnClickListener { onEdit(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_inventory_product, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}
