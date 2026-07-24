package com.mohamed.playstation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.data.repository.StockMovementRepository
import com.mohamed.playstation.domain.model.MovementType
import com.mohamed.playstation.domain.model.InventoryItem
import com.mohamed.playstation.domain.usecase.InventoryUseCases
import com.mohamed.playstation.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryUseCases: InventoryUseCases,
    private val stockMovementRepository: StockMovementRepository
) : ViewModel() {

    data class StockMovementView(
        val id: Long,
        val inventoryItemId: Long,
        val productName: String,
        val quantityChange: Int,
        val movementType: MovementType,
        val timestamp: Date
    )

    private val _addEditUiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val addEditUiState: StateFlow<UiState<Unit>> = _addEditUiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _movementSearchQuery = MutableStateFlow("")
    private val _showArchived = MutableStateFlow(false)
    
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    private val activeItemsFlow = inventoryUseCases.getAllActiveItems()
    private val archivedItemsFlow = inventoryUseCases.getAllArchivedItems()

    val products: StateFlow<List<InventoryItem>> = combine(
        activeItemsFlow,
        archivedItemsFlow,
        _showArchived,
        _searchQuery
    ) { active, archived, showArchived, query ->
        val list = if (showArchived) archived else active
        if (query.isBlank()) list
        else list.filter { it.name.contains(query, ignoreCase = true) }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val movementsWithNames: StateFlow<List<StockMovementView>> = combine(
        stockMovementRepository.getAllMovementsWithNames(),
        _movementSearchQuery
    ) { list, query ->
        val mapped = list.map { row ->
            val movementType = MovementType.entries
                .firstOrNull { it.name == row.movementType }
                ?: MovementType.STOCK_IN
            StockMovementView(
                id = row.id,
                inventoryItemId = row.inventoryItemId,
                productName = row.productName ?: "deleted_archived",
                quantityChange = row.quantityChange,
                movementType = movementType,
                timestamp = row.timestamp
            )
        }
        if (query.isBlank()) mapped
        else mapped.filter { it.productName.contains(query, ignoreCase = true) }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setMovementSearchQuery(query: String) { _movementSearchQuery.value = query }
    fun toggleShowArchived() { _showArchived.value = !_showArchived.value }

    private fun mapExceptionToUiText(e: Exception): com.mohamed.playstation.core.utils.UiText {
        return when (e.message) {
            "DUPLICATE_PRODUCT_NAME" -> com.mohamed.playstation.core.utils.UiText.StringResource(com.mohamed.playstation.R.string.error_duplicate_product_name)
            "INSUFFICIENT_STOCK" -> com.mohamed.playstation.core.utils.UiText.StringResource(com.mohamed.playstation.R.string.insufficient_stock)
            "PRODUCT_NOT_FOUND" -> com.mohamed.playstation.core.utils.UiText.StringResource(com.mohamed.playstation.R.string.product_not_found)
            "INVALID_QUANTITY" -> com.mohamed.playstation.core.utils.UiText.StringResource(com.mohamed.playstation.R.string.invalid_quantity)
            else -> e.message?.let { com.mohamed.playstation.core.utils.UiText.DynamicString(it) } ?: com.mohamed.playstation.core.utils.UiText.StringResource(com.mohamed.playstation.R.string.error_occurred)
        }
    }

    fun resetUiState() {
        _addEditUiState.value = UiState.Idle
    }

    fun addNewProduct(name: String, sellPrice: Double, costPerUnit: Double, quantity: Int, minimumQuantity: Int, isPrepared: Boolean, unitLabel: String) {
        viewModelScope.launch {
            _addEditUiState.value = UiState.Loading
            try {
                val item = InventoryItem(
                    name = name.trim(),
                    sellPrice = sellPrice,
                    costPerUnit = costPerUnit,
                    quantity = quantity,
                    minimumQuantity = minimumQuantity,
                    isPrepared = isPrepared,
                    unitLabel = unitLabel
                )
                inventoryUseCases.insertItem(item)
                _addEditUiState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _addEditUiState.value = UiState.Error(mapExceptionToUiText(e))
            }
        }
    }

    fun updateProduct(item: InventoryItem) {
        viewModelScope.launch {
            _addEditUiState.value = UiState.Loading
            try {
                inventoryUseCases.updateItem(item)
                _addEditUiState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _addEditUiState.value = UiState.Error(mapExceptionToUiText(e))
            }
        }
    }

    fun archiveProduct(id: Long) {
        viewModelScope.launch {
            try {
                inventoryUseCases.archiveItem(id)
            } catch (e: Exception) {
                _addEditUiState.value = UiState.Error(mapExceptionToUiText(e))
            }
        }
    }

    fun restoreProduct(id: Long) {
        viewModelScope.launch {
            try {
                inventoryUseCases.restoreItem(id)
            } catch (e: Exception) {
                _addEditUiState.value = UiState.Error(mapExceptionToUiText(e))
            }
        }
    }

    fun addStockToProduct(id: Long, delta: Int) {
        viewModelScope.launch {
            try {
                inventoryUseCases.adjustStock(id, delta, "STOCK_IN")
            } catch (e: Exception) {
                _addEditUiState.value = UiState.Error(mapExceptionToUiText(e))
            }
        }
    }

    fun removeStockFromProduct(id: Long, delta: Int) {
        viewModelScope.launch {
            try {
                inventoryUseCases.adjustStock(id, -delta, "STOCK_OUT")
            } catch (e: Exception) {
                _addEditUiState.value = UiState.Error(mapExceptionToUiText(e))
            }
        }
    }
}
