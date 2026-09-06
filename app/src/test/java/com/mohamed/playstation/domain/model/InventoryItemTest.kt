package com.mohamed.playstation.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryItemTest {

    @Test
    fun isLowStock_usesPreparedThresholdInsteadOfMinimumQuantity() {
        val preparedProduct = inventoryItem(
            quantity = InventoryItem.PREPARED_LOW_STOCK_THRESHOLD,
            minimumQuantity = 0,
            isPrepared = true
        )

        assertTrue(preparedProduct.isLowStock)
    }

    @Test
    fun isLowStock_preparedProductAboveThresholdIsAvailable() {
        val preparedProduct = inventoryItem(
            quantity = InventoryItem.PREPARED_LOW_STOCK_THRESHOLD + 1,
            minimumQuantity = 0,
            isPrepared = true
        )

        assertFalse(preparedProduct.isLowStock)
    }

    @Test
    fun isLowStock_normalProductUsesConfiguredMinimumAndExcludesEmptyStock() {
        assertTrue(inventoryItem(quantity = 3, minimumQuantity = 3, isPrepared = false).isLowStock)
        assertFalse(inventoryItem(quantity = 0, minimumQuantity = 3, isPrepared = false).isLowStock)
    }

    private fun inventoryItem(
        quantity: Int,
        minimumQuantity: Int,
        isPrepared: Boolean
    ) = InventoryItem(
        name = "Test product",
        sellPrice = 10.0,
        costPerUnit = 5.0,
        quantity = quantity,
        minimumQuantity = minimumQuantity,
        isPrepared = isPrepared,
        unitLabel = "piece"
    )
}
