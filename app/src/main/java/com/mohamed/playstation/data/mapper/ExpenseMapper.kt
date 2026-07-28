package com.mohamed.playstation.data.mapper

import com.mohamed.playstation.data.local.entity.ExpenseEntity
import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.domain.model.ExpenseCategory

/**
 * Mapper للتحويل بين ExpenseEntity و Expense
 */
object ExpenseMapper {

    fun toModel(entity: ExpenseEntity): Expense {
        return Expense(
            id = entity.id,
            amount = entity.amount,
            category = try {
                ExpenseCategory.valueOf(entity.category)
            } catch (e: IllegalArgumentException) {
                ExpenseCategory.OTHER
            },
            description = entity.description,
            expenseDate = entity.expenseDate,
            createdAt = entity.createdAt
        )
    }

    fun toEntity(model: Expense): ExpenseEntity {
        return ExpenseEntity(
            id = model.id,
            amount = model.amount,
            category = model.category.name,
            description = model.description,
            expenseDate = model.expenseDate,
            createdAt = model.createdAt
        )
    }

    fun toModelList(entities: List<ExpenseEntity>): List<Expense> {
        return entities.map(::toModel)
    }
}
