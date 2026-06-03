package com.mohamed.playstation.domain.usecase

import com.mohamed.playstation.data.repository.ProductRepository
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.domain.model.SessionProductSummary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Cases للمنتجات
 */
class ProductUseCases @Inject constructor(
    private val productRepository: ProductRepository
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

        val product = SessionProduct(
            sessionId = sessionId,
            name = name.trim(),
            price = price,
            quantity = quantity
        )

        return productRepository.insertProduct(product)
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
