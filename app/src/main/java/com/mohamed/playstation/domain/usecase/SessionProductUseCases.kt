package com.mohamed.playstation.domain.usecase

import com.mohamed.playstation.data.repository.SessionProductRepository
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.domain.model.SessionProductSummary
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionProductUseCases @Inject constructor(
    private val sessionProductRepository: SessionProductRepository
) {
    fun getProductsBySessionId(sessionId: Long): Flow<List<SessionProduct>> {
        return sessionProductRepository.getProductsBySessionId(sessionId)
    }

    suspend fun getProductsBySessionIdOnce(sessionId: Long): List<SessionProduct> {
        return sessionProductRepository.getProductsBySessionIdOnce(sessionId)
    }

    fun getAllSessionProducts(): Flow<List<SessionProduct>> {
        return sessionProductRepository.getAllSessionProducts()
    }

    fun getAllSessionProductSummaries(): Flow<List<SessionProductSummary>> {
        return sessionProductRepository.getAllSessionProducts().map { products ->
            products.groupBy { it.sessionId }.map { (sessionId, sessionProducts) ->
                SessionProductSummary(
                    sessionId = sessionId,
                    totalQuantity = sessionProducts.sumOf { it.quantitySold },
                    totalAmount = SessionProduct.calculateTotalAmount(sessionProducts)
                )
            }
        }
    }

    suspend fun addInventoryProductToSession(sessionId: Long, inventoryItemId: Long, quantity: Int) {
        if (quantity <= 0) throw IllegalArgumentException("INVALID_QUANTITY")
        sessionProductRepository.addInventoryProductToSession(sessionId, inventoryItemId, quantity)
    }

    suspend fun removeSessionProductAndRestoreStock(sessionProductId: Long) {
        sessionProductRepository.removeSessionProductAndRestoreStock(sessionProductId)
    }
}
