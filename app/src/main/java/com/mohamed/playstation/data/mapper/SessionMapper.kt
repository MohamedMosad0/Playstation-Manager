package com.mohamed.playstation.data.mapper

import com.mohamed.playstation.data.local.entity.SessionEntity
import com.mohamed.playstation.domain.model.Session

object SessionMapper {

    fun toModel(entity: SessionEntity): Session {
        return Session(
            id = entity.id,
            deviceType = entity.deviceType,
            deviceNumber = entity.deviceNumber,
            sessionType = entity.sessionType,
            sessionMode = entity.sessionMode,
            isMultiPlayer = entity.isMultiPlayer,
            fixedDurationMinutes = entity.fixedDurationMinutes,
            startTime = entity.startTime,
            endTime = entity.endTime,
            pausedAt = entity.pausedAt,
            totalPausedMinutes = entity.totalPausedMinutes,
            status = entity.status,
            pricePerHour = entity.pricePerHour,
            notes = entity.notes,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(model: Session): SessionEntity {
        return SessionEntity(
            id = model.id,
            deviceType = model.deviceType,
            deviceNumber = model.deviceNumber,
            sessionType = model.sessionType,
            sessionMode = model.sessionMode,
            isMultiPlayer = model.isMultiPlayer,
            fixedDurationMinutes = model.fixedDurationMinutes,
            startTime = model.startTime,
            endTime = model.endTime,
            pausedAt = model.pausedAt,
            totalPausedMinutes = model.totalPausedMinutes,
            status = model.status,
            pricePerHour = model.pricePerHour,
            notes = model.notes,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt
        )
    }

    fun toModelList(entities: List<SessionEntity>): List<Session> {
        return entities.map { toModel(it) }
    }
}
