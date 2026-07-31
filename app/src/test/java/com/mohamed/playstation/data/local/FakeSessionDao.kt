package com.mohamed.playstation.data.local

import com.mohamed.playstation.data.local.dao.SessionDao
import com.mohamed.playstation.data.local.entity.SessionEntity
import java.util.Date
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSessionDao : SessionDao {

    private val idGenerator = AtomicLong(1L)
    private val sessionsFlow = MutableStateFlow<Map<Long, SessionEntity>>(emptyMap())

    override suspend fun insert(session: SessionEntity): Long {
        val id = if (session.id <= 0L) idGenerator.getAndIncrement() else session.id
        val stored = session.copy(id = id)
        sessionsFlow.value = sessionsFlow.value + (id to stored)
        return id
    }

    override suspend fun update(session: SessionEntity) {
        sessionsFlow.value = sessionsFlow.value + (session.id to session)
    }

    override suspend fun pauseIfActive(
        sessionId: Long,
        pausedAt: Date,
        updatedAt: Date
    ): Int {
        val current = sessionsFlow.value[sessionId] ?: return 0
        if (current.status == "active") {
            val updated = current.copy(status = "paused", pausedAt = pausedAt, updatedAt = updatedAt)
            sessionsFlow.value = sessionsFlow.value + (sessionId to updated)
            return 1
        }
        return 0
    }

    override suspend fun resumeIfPaused(
        sessionId: Long,
        startTime: Date,
        totalPausedMinutes: Long,
        updatedAt: Date
    ): Int {
        val current = sessionsFlow.value[sessionId] ?: return 0
        if (current.status == "paused") {
            val updated = current.copy(
                status = "active",
                pausedAt = null,
                startTime = startTime,
                totalPausedMinutes = totalPausedMinutes,
                updatedAt = updatedAt
            )
            sessionsFlow.value = sessionsFlow.value + (sessionId to updated)
            return 1
        }
        return 0
    }

    override suspend fun delete(session: SessionEntity) {
        sessionsFlow.value = sessionsFlow.value - session.id
    }

    override suspend fun getSessionById(sessionId: Long): SessionEntity? {
        return sessionsFlow.value[sessionId]
    }

    override suspend fun getBlockingSessionForDevice(
        deviceType: String,
        deviceNumber: Int
    ): SessionEntity? {
        return sessionsFlow.value.values.find {
            it.deviceType == deviceType &&
                    it.deviceNumber == deviceNumber &&
                    (it.status == "active" || it.status == "paused")
        }
    }

    override fun getSessionByIdFlow(sessionId: Long): Flow<SessionEntity?> {
        return sessionsFlow.map { map -> map[sessionId] }
    }

    override fun getActiveSessions(): Flow<List<SessionEntity>> {
        return sessionsFlow.map { map ->
            map.values.filter { it.status == "active" }.sortedByDescending { it.startTime }
        }
    }

    override fun getPausedSessions(): Flow<List<SessionEntity>> {
        return sessionsFlow.map { map ->
            map.values.filter { it.status == "paused" }.sortedByDescending { it.startTime }
        }
    }

    override fun getEndedSessions(): Flow<List<SessionEntity>> {
        return sessionsFlow.map { map ->
            map.values.filter { it.status == "ended" }.sortedByDescending { it.endTime }
        }
    }

    override fun getAllSessions(): Flow<List<SessionEntity>> {
        return sessionsFlow.map { map ->
            map.values.sortedByDescending { it.startTime }
        }
    }

    override suspend fun getAllOnce(): List<SessionEntity> {
        return sessionsFlow.value.values.toList()
    }

    override fun getActiveSessionsCount(): Flow<Int> {
        return sessionsFlow.map { map ->
            map.values.count { it.status == "active" }
        }
    }

    override fun getTodaySessions(startMs: Long, endMs: Long): Flow<List<SessionEntity>> {
        return sessionsFlow.map { map ->
            map.values.filter { it.startTime.time in startMs until endMs }
                .sortedByDescending { it.startTime }
        }
    }

    override suspend fun deleteAll() {
        sessionsFlow.value = emptyMap()
    }
}
