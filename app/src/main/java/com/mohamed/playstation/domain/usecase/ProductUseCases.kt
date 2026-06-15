package com.mohamed.playstation.domain.usecase

import com.mohamed.playstation.data.repository.ProductRepository
import com.mohamed.playstation.data.repository.StockMovementRepository
import com.mohamed.playstation.domain.model.MovementType
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.domain.model.SessionProductSummary
import com.mohamed.playstation.domain.model.StockMovement
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Cases للمنتجات
 */
class ProductUseCases @Inject constructor(
    private val productRepository: ProductRepository,
    private val stockMovementRepository: StockMovementRepository
) {

    suspend fun addProductToSession(
        sessionId: Long,
        name: String,
        price: Double,
        quantity: Int
    ): Long {
        require(sessionId > 0) { "Invalid session id" }
        require(name.isNotBlank()) { "Product name cannot be blank" }
        require(price > 0) { "Product price must be greater than zero" }
        require(quantity > 0) { "Quantity must be greater than zero" }

        val trimmedName = name.trim()
        val existing = productRepository.getProductByNameInSession(sessionId, trimmedName)

        return if (existing != null && existing.price == price) {
            val updated = existing.copy(quantity = existing.quantity + quantity)
            productRepository.updateProduct(updated)
            existing.id
        } else {
            val product = SessionProduct(
                sessionId = sessionId,
                name = trimmedName,
                price = price,
                quantity = quantity
            )
            productRepository.insertProduct(product)
        }
    }

    suspend fun addInventoryProductToSession(
        sessionId: Long,
        inventoryProductId: Long,
        quantity: Int
    ): Long {
        require(sessionId > 0) { "Invalid session id" }
        require(inventoryProductId > 0) { "Invalid inventory product id" }
        require(quantity > 0) { "Quantity must be greater than zero" }

        return productRepository.withTransaction {
            val inventoryProduct = productRepository.getInventoryProductById(inventoryProductId)
                ?: throw IllegalArgumentException("Inventory product not found")

            require(inventoryProduct.quantity >= quantity) {
                "Requested quantity exceeds available stock"
            }

            val stockReduced = productRepository.decreaseInventoryStockIfAvailable(
                productId = inventoryProductId,
                delta = quantity
            )
            require(stockReduced) {
                "Requested quantity exceeds available stock"
            }

            // Merging logic for inventory products (bill items)
            val existingInSession = productRepository.getProductByNameInSession(sessionId, inventoryProduct.name)
            val billProductId = if (existingInSession != null && existingInSession.price == inventoryProduct.price) {
                val updated = existingInSession.copy(quantity = existingInSession.quantity + quantity)
                productRepository.updateProduct(updated)
                existingInSession.id
            } else {
                val billProduct = SessionProduct(
                    sessionId = sessionId,
                    name = inventoryProduct.name,
                    price = inventoryProduct.price,
                    quantity = quantity
                )
                productRepository.insertProduct(billProduct)
            }

            stockMovementRepository.insertMovement(
                StockMovement(
                    productId = inventoryProductId,
                    quantityChange = -quantity,
                    movementType = MovementType.STOCK_OUT
                )
            )

            billProductId
        }
    }

    fun getInventoryProducts(): Flow<List<SessionProduct>> {
        return productRepository.getInventoryProducts()
    }

    fun getProductsBySessionId(sessionId: Long): Flow<List<SessionProduct>> {
        return productRepository.getProductsBySessionId(sessionId)
    }

    suspend fun getProductsBySessionIdOnce(sessionId: Long): List<SessionProduct> {
        return productRepository.getProductsBySessionIdOnce(sessionId)
    }

    fun getAllSessionProductSummaries(): Flow<List<SessionProductSummary>> {
        return productRepository.getAllSessionProductSummaries()
    }
}
