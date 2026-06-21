package com.mohamed.playstation.presentation.ui.inventory
import android.os.Bundle
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mohamed.playstation.R
import com.mohamed.playstation.databinding.FragmentInventoryProductsBinding
import com.mohamed.playstation.domain.model.InventoryItem
import com.mohamed.playstation.presentation.viewmodel.InventoryViewModel
import com.mohamed.playstation.core.constants.AppConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductsTabFragment : Fragment() {

    private var _binding: FragmentInventoryProductsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels({ requireParentFragment() })

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

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter
        
        binding.emptyState.visibility = View.GONE
        binding.rvProducts.visibility = View.VISIBLE
        
        val controller = android.view.animation.AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_slide_up)
        binding.rvProducts.layoutAnimation = controller

        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }

        binding.btnNewProduct.setOnClickListener {
            showNewProductDialog()
        }
        
        // Hide/Show FAB on scroll
        binding.rvProducts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && binding.btnNewProduct.isShown) {
                    binding.btnNewProduct.hide()
                } else if (dy < 0 && !binding.btnNewProduct.isShown) {
                    binding.btnNewProduct.show()
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.products.collect { list ->
                    adapter.submitList(list) {
                        if (list.isEmpty()) {
                            binding.emptyState.visibility = View.VISIBLE
                            binding.rvProducts.visibility = View.GONE
                        } else {
                            binding.emptyState.visibility = View.GONE
                            binding.rvProducts.visibility = View.VISIBLE
                            binding.rvProducts.scheduleLayoutAnimation()
                        }
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
            val pc = etPreparationCost.text.toString().toDoubleOrNull() ?: 0.0
            val pu = etProduced.text.toString().toIntOrNull() ?: 0
            if (pc > 0 && pu > 0) {
                tvComputed.text = "تكلفة الوحدة المحسوبة: %.2f".format(pc / pu)
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
                val price  = etPrice.text.toString().toDoubleOrNull() ?: -1.0
                
                val isPrepared = toggleType.checkedButtonId == R.id.btnTypePrepared

                val qty: Int
                val minQty: Int
                val costPerUnit: Double

                if (isPrepared) {
                    val prepCost = etPreparationCost.text.toString().toDoubleOrNull() ?: 0.0
                    val produced = etProduced.text.toString().toIntOrNull() ?: 0
                    qty = produced
                    minQty = 0 // By rule
                    costPerUnit = if (produced > 0) prepCost / produced else 0.0
                    
                    if (produced <= 0) {
                        Toast.makeText(requireContext(), "يرجى إدخال عدد وحدات صحيح", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                } else {
                    qty = etQty.text.toString().toIntOrNull() ?: -1
                    minQty = etMinQty.text.toString().toIntOrNull() ?: -1
                    costPerUnit = etUnitCost.text.toString().toDoubleOrNull() ?: 0.0
                    
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

                val unitLabel = if (isPrepared) "كوب" else "قطعة"

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
        etPrice.setText(product.sellPrice.toString())
        etCostPerUnit.setText(if (product.costPerUnit > 0) product.costPerUnit.toString() else "")

        val isPrepared = product.isPrepared

        if (isPrepared) {
            tilCurrentQty.isVisible = false
            tilMinQty.isVisible = false
            tvAvailable.isVisible = true
            tilNewPreparedQty.isVisible = true
            tvPreparedQtyPreview.isVisible = true
            
            val unitName = product.unitLabel
            val pluralUnit = com.mohamed.playstation.core.utils.UnitFormatUtils.getPluralUnit(unitName)
            tvAvailable.text = "المتاح حالياً: ${product.quantity} $pluralUnit"
            
            etNewPreparedQty.doAfterTextChanged { text ->
                val delta = text?.toString()?.toIntOrNull() ?: 0
                val newTotal = product.quantity + delta
                tvPreparedQtyPreview.text = "سيصبح المتاح: $newTotal $pluralUnit"
            }
        } else {
            etMinQty.setText(product.minimumQuantity.toString())
            etCurrentQty.setText(product.quantity.toString())
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.edit))
            .setView(dialogView)
            .setPositiveButton(R.string.save_changes) { d, _ ->
                val newName        = etName.text.toString().trim()
                val newPrice       = etPrice.text.toString().toDoubleOrNull() ?: -1.0
                val newCostPerUnit = etCostPerUnit.text.toString().toDoubleOrNull() ?: 0.0

                val newMin: Int
                val newQty: Int
                
                if (isPrepared) {
                    newMin = product.minimumQuantity
                    val delta = etNewPreparedQty.text.toString().toIntOrNull() ?: 0
                    newQty = product.quantity + delta
                } else {
                    newMin = etMinQty.text.toString().toIntOrNull() ?: -1
                    newQty = etCurrentQty.text.toString().toIntOrNull() ?: -1
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
            .setTitle("أرشفة")
            .setMessage("سيتم إخفاء المنتج من القوائم النشطة مع الاحتفاظ بجميع الفواتير والتقارير والحركات السابقة.")
            .setPositiveButton("أرشفة") { d, _ ->
                viewModel.archiveProduct(product.id)
                d.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class ProductsAdapter(
    private val onEdit: (InventoryItem) -> Unit,
    private val onDelete: (InventoryItem) -> Unit
) : ListAdapter<InventoryItem, ProductsAdapter.VH>(DIFF) {

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
            tvName.text = item.name
            tvPrice.text = "${item.sellPrice} ج.م"
            tvUnitCost.text = "${item.costPerUnit} ج.م"
            
            val profit = item.sellPrice - item.costPerUnit
            tvProfit.text = "${"%.2f".format(profit)} ج.م"
            
            val isPrepared = item.isPrepared
            val unitName = item.unitLabel
            
            val definiteUnit = com.mohamed.playstation.core.utils.UnitFormatUtils.getDefiniteSingularUnit(unitName)
            tvUnitCostLabel.text = "تكلفة $definiteUnit:"
            tvProfitLabel.text = "ربح $definiteUnit:"
            
            // Quantity transition animation logic
            if (tvQuantity.text.toString().isNotEmpty() && tvQuantity.text.toString() != item.quantity.toString()) {
                tvQuantity.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(150).withEndAction {
                    tvQuantity.text = item.quantity.toString()
                    tvQuantity.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(150).start()
                }.start()
            } else {
                tvQuantity.text = item.quantity.toString()
            }

            if (isPrepared) {
                val icon = if (unitName == "علبة") "🍜" else "☕"
                tvStockLabel.text = "$icon المتاح:"
                tvTypeBadge.text = "منتج مُحضَّر"
                itemView.findViewById<TextView>(R.id.tvUnitLabel).text = unitName
            } else {
                tvStockLabel.text = "📦 المتاح:"
                tvTypeBadge.text = "منتج عادي"
                itemView.findViewById<TextView>(R.id.tvUnitLabel).text = unitName
            }

            // 3 States Status Logic
            val isOutOfStock = item.quantity == 0
            val isLowStock = if (isPrepared) {
                item.quantity <= 10 && !isOutOfStock
            } else {
                item.quantity <= item.minimumQuantity && !isOutOfStock
            }

            if (isOutOfStock) {
                chipStatus.text = "نفد"
                chipStatus.setChipBackgroundColorResource(R.color.error)
                chipStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                tvQuantity.setTextColor(ContextCompat.getColor(itemView.context, R.color.error))
            } else if (isLowStock) {
                chipStatus.text = "منخفض"
                chipStatus.setChipBackgroundColorResource(R.color.status_paused)
                chipStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                tvQuantity.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_paused))
            } else {
                chipStatus.text = "متوفر"
                chipStatus.setChipBackgroundColorResource(R.color.status_active)
                chipStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
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
