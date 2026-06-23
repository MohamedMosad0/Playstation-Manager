package com.mohamed.playstation.domain.usecase

import com.mohamed.playstation.data.repository.SessionProductRepository
import com.mohamed.playstation.domain.model.SessionProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

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

    fun getAllSessionProductSummaries(): Flow<List<com.mohamed.playstation.domain.model.SessionProductSummary>> {
        return sessionProductRepository.getAllSessionProducts().map { products ->
            products.groupBy { it.sessionId }.map { (sessionId, sessionProducts) ->
                com.mohamed.playstation.domain.model.SessionProductSummary(
                    sessionId = sessionId,
                    totalQuantity = sessionProducts.sumOf { it.quantitySold },
                    totalAmount = sessionProducts.sumOf { it.getLineTotal() }
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
