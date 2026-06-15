package com.mohamed.playstation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.data.repository.ProductRepository
import com.mohamed.playstation.data.repository.SessionRepository
import com.mohamed.playstation.data.repository.StockMovementRepository
import com.mohamed.playstation.domain.model.MovementType
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.domain.model.StockMovement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val productRepository: ProductRepository,
    private val stockMovementRepository: StockMovementRepository
) : ViewModel() {

    data class StockMovementView(
        val id: Long,
        val productId: Long,
        val productName: String,
        val quantityChange: Int,
        val movementType: MovementType,
        val timestamp: Date
    )

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _movementSearchQuery = MutableStateFlow("")

    val products: StateFlow<List<SessionProduct>> = productRepository.getInventoryProducts()
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) list
            else list.filter { it.name.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allResolvedMovements: StateFlow<List<StockMovementView>> = stockMovementRepository.getAllMovements()
        .map { list ->
            list.map { m ->
                val product = try { productRepository.getProductById(m.productId) } catch (e: Exception) { null }
                StockMovementView(
                    id = m.id,
                    productId = m.productId,
                    productName = product?.name ?: "[Deleted Product]",
                    quantityChange = m.quantityChange,
                    movementType = m.movementType,
                    timestamp = m.timestamp
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val movementsWithNames: StateFlow<List<StockMovementView>> = combine(
        allResolvedMovements,
        _movementSearchQuery
    ) { resolvedList, query ->
        if (query.isBlank()) resolvedList
        else resolvedList.filter { it.productName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMovementSearchQuery(query: String) {
        _movementSearchQuery.value = query
    }

    init {
        // load current session id
        viewModelScope.launch {
            sessionRepository.getAllSessions()
                .map { sessions -> sessions.firstOrNull()?.id }
                .collect { id -> _currentSessionId.value = id }
        }
    }

    fun addNewProduct(sessionId: Long, name: String, price: Double, minimumQuantity: Int, initialQuantity: Int) {
        viewModelScope.launch {
            try {
                val product = SessionProduct(
                    sessionId = sessionId,
                    name = name.trim(),
                    price = price,
                    quantity = initialQuantity,
                    minimumQuantity = minimumQuantity
                )
                val id = productRepository.insertProduct(product)
                // Record stock movement
                stockMovementRepository.insertMovement(
                    StockMovement(
                        productId = id,
                        quantityChange = initialQuantity,
                        movementType = MovementType.STOCK_IN
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    fun addStockToProduct(productId: Long, delta: Int) {
        viewModelScope.launch {
            try {
                val res = productRepository.increaseStock(productId, delta)
                if (res != null) {
                    stockMovementRepository.insertMovement(
                        StockMovement(
                            productId = productId,
                            quantityChange = delta,
                            movementType = MovementType.STOCK_IN
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    fun updateProductWithQuantityChange(product: SessionProduct, oldQuantity: Int) {
        viewModelScope.launch {
            try {
                productRepository.updateProduct(product)
                
                // Create movement if quantity changed
                val quantityChange = product.quantity - oldQuantity
                if (quantityChange != 0) {
                    val movementType = if (quantityChange > 0) {
                        MovementType.STOCK_IN
                    } else {
                        MovementType.STOCK_OUT
                    }
                    
                    stockMovementRepository.insertMovement(
                        StockMovement(
                            productId = product.id,
                            quantityChange = quantityChange,
                            movementType = movementType
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    fun updateProduct(product: SessionProduct) {
        viewModelScope.launch {
            try {
                productRepository.updateProduct(product)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun checkProductExists(sessionId: Long, name: String): Boolean {
        return try {
            productRepository.getInventoryProductByName(name) != null
        } catch (_: Exception) {
            false
        }
    }

    suspend fun checkProductExistsExcluding(sessionId: Long, name: String, excludeId: Long): Boolean {
        return try {
            productRepository.getInventoryProductByNameExcluding(name, excludeId) != null
        } catch (_: Exception) {
            false
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            try {
                productRepository.deleteProductById(productId)
                stockMovementRepository.deleteByProductId(productId)
            } catch (_: Exception) {
            }
        }
    }
}
