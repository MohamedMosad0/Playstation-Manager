package com.mohamed.playstation.data.repository

import com.mohamed.playstation.data.local.dao.ProductDao
import com.mohamed.playstation.data.mapper.ProductMapper
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.domain.model.SessionProductSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository للتعامل مع المنتجات
 */
@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao
) {

    suspend fun insertProduct(product: SessionProduct): Long {
        return productDao.insert(ProductMapper.toEntity(product))
    }

    fun getProductsBySessionId(sessionId: Long): Flow<List<SessionProduct>> {
        return productDao.getProductsBySessionId(sessionId).map(ProductMapper::toModelList)
    }

    suspend fun getProductsBySessionIdOnce(sessionId: Long): List<SessionProduct> {
        return ProductMapper.toModelList(productDao.getProductsBySessionIdOnce(sessionId))
    }

    fun getAllSessionProductSummaries(): Flow<List<SessionProductSummary>> {
        return productDao.getAllSessionProductSummaries().map { rows ->
            rows.map { row ->
                SessionProductSummary(
                    sessionId = row.sessionId,
                    totalQuantity = row.totalQuantity,
                    totalAmount = row.totalAmount
                )
            }
        }
    }
}
