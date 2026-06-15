package com.mohamed.playstation.presentation.ui.inventory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.mohamed.playstation.R
import com.mohamed.playstation.databinding.FragmentInventoryProductsBinding
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.presentation.viewmodel.InventoryViewModel
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

        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }

        binding.btnNewProduct.setOnClickListener {
            showNewProductDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.products.collect { list ->
                    adapter.submitList(list)
                }
            }
        }
    }

    private fun showNewProductDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_product, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etProductName)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.etProductPrice)
        val etQty = dialogView.findViewById<TextInputEditText>(R.id.etQuantity)
        val etMinQty = dialogView.findViewById<TextInputEditText>(R.id.etMinimumQuantity)

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.new_product))
            .setView(dialogView)
            .setPositiveButton(R.string.save) { d, _ ->
                val name = etName.text.toString().trim()
                val price = etPrice.text.toString().toDoubleOrNull() ?: -1.0
                val qty = etQty.text.toString().toIntOrNull() ?: -1
                val minQty = etMinQty.text.toString().toIntOrNull() ?: -1

                // Validation
                if (name.isBlank()) {
                    Toast.makeText(requireContext(), getString(R.string.required_field), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (price < 0) {
                    Toast.makeText(requireContext(), getString(R.string.invalid_price), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (qty < 0) {
                    Toast.makeText(requireContext(), getString(R.string.invalid_quantity), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (minQty < 0) {
                    Toast.makeText(requireContext(), getString(R.string.invalid_quantity), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val sessionId = viewModel.currentSessionId.value ?: return@setPositiveButton

                // Check for duplicate
                viewLifecycleOwner.lifecycleScope.launch {
                    val exists = viewModel.checkProductExists(sessionId, name)
                    if (exists) {
                        Toast.makeText(requireContext(), getString(R.string.product_exists), Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // Create product
                    viewModel.addNewProduct(sessionId, name, price, minQty, qty)
                    d.dismiss()
                }
            }
            .setNegativeButton(R.string.cancel, null)

        builder.show()
    }

    private fun showEditDialog(product: SessionProduct) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_product, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etName)
        val etPrice = dialogView.findViewById<android.widget.EditText>(R.id.etPrice)
        val etMinQty = dialogView.findViewById<android.widget.EditText>(R.id.etMinQty)
        val etCurrentQty = dialogView.findViewById<android.widget.EditText>(R.id.etCurrentQty)

        etName.setText(product.name)
        etPrice.setText(product.price.toString())
        etMinQty.setText(product.minimumQuantity.toString())
        etCurrentQty.setText(product.quantity.toString())

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.edit))
            .setView(dialogView)
            .setPositiveButton(R.string.save_changes) { d, _ ->
                val newName = etName.text.toString().trim()
                val newPrice = etPrice.text.toString().toDoubleOrNull() ?: -1.0
                val newMin = etMinQty.text.toString().toIntOrNull() ?: -1
                val newQty = etCurrentQty.text.toString().toIntOrNull() ?: -1

                if (newName.isBlank()) {
                    Toast.makeText(requireContext(), getString(R.string.required_field), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newPrice < 0) {
                    Toast.makeText(requireContext(), getString(R.string.invalid_price), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newMin < 0) {
                    Toast.makeText(requireContext(), getString(R.string.invalid_quantity), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newQty < 0) {
                    Toast.makeText(requireContext(), getString(R.string.invalid_quantity), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val sessionId = viewModel.currentSessionId.value ?: return@setPositiveButton

                // Check for duplicate if name changed
                if (newName != product.name) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val exists = viewModel.checkProductExistsExcluding(sessionId, newName, product.id)
                        if (exists) {
                            Toast.makeText(requireContext(), getString(R.string.product_exists_rename), Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val updated = product.copy(name = newName, price = newPrice, minimumQuantity = newMin, quantity = newQty)
                        viewModel.updateProductWithQuantityChange(updated, product.quantity)
                        d.dismiss()
                    }
                } else {
                    val updated = product.copy(name = newName, price = newPrice, minimumQuantity = newMin, quantity = newQty)
                    viewModel.updateProductWithQuantityChange(updated, product.quantity)
                    d.dismiss()
                }
            }
            .setNegativeButton(R.string.cancel, null)

        builder.show()
    }

    private fun showDeleteDialog(product: SessionProduct) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete))
            .setMessage("Are you sure you want to delete this product?")
            .setPositiveButton(getString(R.string.confirm)) { d, _ ->
                viewModel.deleteProduct(product.id)
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
    private val onEdit: (SessionProduct) -> Unit,
    private val onDelete: (SessionProduct) -> Unit
) : ListAdapter<SessionProduct, ProductsAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SessionProduct>() {
            override fun areItemsTheSame(oldItem: SessionProduct, newItem: SessionProduct): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: SessionProduct, newItem: SessionProduct): Boolean = oldItem == newItem
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        private val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        private val tvMin: TextView = view.findViewById(R.id.tvMinQuantity)
        private val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)

        fun bind(item: SessionProduct) {
            tvName.text = item.name
            tvPrice.text = item.price.toString()
            tvQuantity.text = item.quantity.toString()
            tvMin.text = item.minimumQuantity.toString()

            // Low Stock Warning UI Logic
            val color = if (item.isLowStock) {
                ContextCompat.getColor(itemView.context, R.color.status_error)
            } else {
                ContextCompat.getColor(itemView.context, R.color.text_primary)
            }
            tvQuantity.setTextColor(color)

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
