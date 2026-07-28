package com.mohamed.playstation.presentation.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.AppFormatters
import com.mohamed.playstation.databinding.FragmentInventoryHistoryBinding
import com.mohamed.playstation.domain.model.MovementType
import com.mohamed.playstation.presentation.viewmodel.InventoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MovementHistoryTabFragment : Fragment() {

    private var _binding: FragmentInventoryHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels({ requireParentFragment() })

    private lateinit var adapter: MovementsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MovementsAdapter()
        binding.rvMovements.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMovements.setHasFixedSize(true)
        binding.rvMovements.adapter = adapter
        binding.emptyState.visibility = View.GONE
        binding.rvMovements.visibility = View.VISIBLE

        searchWatcher = binding.etSearch.doAfterTextChanged { text ->
            viewModel.setMovementSearchQuery(text?.toString() ?: "")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.movementsWithNames.collect { list ->
                    adapter.submitList(list) {
                        if (list.isEmpty()) {
                            binding.emptyState.visibility = View.VISIBLE
                            binding.rvMovements.visibility = View.GONE
                        } else {
                            binding.emptyState.visibility = View.GONE
                            binding.rvMovements.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private var searchWatcher: android.text.TextWatcher? = null

    override fun onDestroyView() {
        searchWatcher?.let { binding.etSearch.removeTextChangedListener(it) }
        searchWatcher = null
        binding.rvMovements.adapter = null
        super.onDestroyView()
        _binding = null
    }
}

class MovementsAdapter : ListAdapter<InventoryViewModel.StockMovementView, MovementsAdapter.VH>(DIFF) {
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<InventoryViewModel.StockMovementView>() {
            override fun areItemsTheSame(oldItem: InventoryViewModel.StockMovementView, newItem: InventoryViewModel.StockMovementView): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: InventoryViewModel.StockMovementView, newItem: InventoryViewModel.StockMovementView): Boolean = oldItem == newItem
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvProduct: TextView = view.findViewById(R.id.tvProductName)
        private val tvMovementDetail: TextView = view.findViewById(R.id.tvMovementDetail)
        private val tvTime: TextView = view.findViewById(R.id.tvTimestamp)

        fun bind(item: InventoryViewModel.StockMovementView) {
            val sign = if (item.quantityChange > 0) "+" else ""
            
            // Movement Types
            val typeText: String
            val colorRes: Int
            when (item.movementType) {
                MovementType.SALE,
                MovementType.SALE_REVERT -> {
                    typeText = itemView.context.getString(R.string.movement_sales)
                    colorRes = R.color.status_paused
                }
                MovementType.MANUAL_ADD,
                MovementType.STOCK_IN,
                MovementType.INITIAL -> {
                    typeText = itemView.context.getString(R.string.movement_add)
                    colorRes = R.color.status_active
                }
                MovementType.MANUAL_DEDUCT,
                MovementType.STOCK_OUT -> {
                    typeText = itemView.context.getString(R.string.movement_withdraw_sale)
                    colorRes = R.color.status_error
                }
            }

            tvMovementDetail.text = "$typeText • $sign${AppFormatters.formatInteger(itemView.context, item.quantityChange)}"
            tvMovementDetail.setTextColor(ContextCompat.getColor(itemView.context, colorRes))

            tvProduct.text = item.productName
            tvTime.text = AppFormatters.formatDateTimeWithMonth(itemView.context, item.timestamp)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_stock_movement, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}
