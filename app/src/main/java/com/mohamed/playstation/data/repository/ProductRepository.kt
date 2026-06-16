package com.mohamed.playstation.data.repository

import androidx.room.withTransaction
import com.mohamed.playstation.data.local.AppDatabase
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
    private val productDao: ProductDao,
    private val database: AppDatabase
) {

    suspend fun <T> withTransaction(block: suspend () -> T): T {
        return database.withTransaction(block)
    }

    suspend fun insertProduct(product: SessionProduct): Long {
        return productDao.insert(ProductMapper.toEntity(product))
    }

    suspend fun updateProduct(product: SessionProduct) {
        val entity = ProductMapper.toEntity(product)
        productDao.update(entity)
    }

    fun getAllProducts(): Flow<List<SessionProduct>> {
        return productDao.getAllProducts().map(ProductMapper::toModelList)
    }

    fun getInventoryProducts(): Flow<List<SessionProduct>> {
        return productDao.getInventoryProducts().map(ProductMapper::toModelList)
    }

    fun getProductsBySessionId(sessionId: Long): Flow<List<SessionProduct>> {
        return productDao.getProductsBySessionId(sessionId).map(ProductMapper::toModelList)
    }

    suspend fun getProductsBySessionIdOnce(sessionId: Long): List<SessionProduct> {
        return ProductMapper.toModelList(productDao.getProductsBySessionIdOnce(sessionId))
    }

    suspend fun getProductById(productId: Long): SessionProduct? {
        val entity = productDao.getProductById(productId)
        return entity?.let { ProductMapper.toModel(it) }
    }

    suspend fun getInventoryProductById(productId: Long): SessionProduct? {
        val entity = productDao.getInventoryProductById(productId)
        return entity?.let { ProductMapper.toModel(it) }
    }

    suspend fun decreaseInventoryStockIfAvailable(productId: Long, delta: Int): Boolean {
        return productDao.decreaseInventoryStockIfAvailable(productId, delta) > 0
    }

    suspend fun increaseStock(productId: Long, delta: Int): Long? {
        val existing = productDao.getProductById(productId) ?: return null
        val updated = existing.copy(quantity = existing.quantity + delta)
        productDao.update(updated)
        return updated.id
    }

    suspend fun deleteProductById(productId: Long) {
        productDao.deleteById(productId)
    }

    suspend fun getProductByName(name: String): SessionProduct? {
        val entity = productDao.getProductByName(name)
        return entity?.let { ProductMapper.toModel(it) }
    }

    suspend fun getProductByNameExcluding(name: String, excludeId: Long): SessionProduct? {
        val entity = productDao.getProductByNameExcluding(name, excludeId)
        return entity?.let { ProductMapper.toModel(it) }
    }

    suspend fun getInventoryProductByName(name: String): SessionProduct? {
        val entity = productDao.getInventoryProductByName(name)
        return entity?.let { ProductMapper.toModel(it) }
    }

    suspend fun getInventoryProductByNameExcluding(name: String, excludeId: Long): SessionProduct? {
        val entity = productDao.getInventoryProductByNameExcluding(name, excludeId)
        return entity?.let { ProductMapper.toModel(it) }
    }

    suspend fun getProductByNameInSession(sessionId: Long, name: String): SessionProduct? {
        val entity = productDao.getProductByNameInSession(sessionId, name)
        return entity?.let { ProductMapper.toModel(it) }
    }

    suspend fun getProductByNameInSessionExcluding(sessionId: Long, name: String, excludeId: Long): SessionProduct? {
        val entity = productDao.getProductByNameInSessionExcluding(sessionId, name, excludeId)
        return entity?.let { ProductMapper.toModel(it) }
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
