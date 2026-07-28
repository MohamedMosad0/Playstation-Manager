package com.mohamed.playstation.data.repository

import androidx.room.withTransaction
import com.mohamed.playstation.core.utils.DateUtils
import com.mohamed.playstation.data.local.AppDatabase
import com.mohamed.playstation.data.local.dao.SessionDao
import com.mohamed.playstation.data.mapper.SessionMapper
import com.mohamed.playstation.domain.model.Session
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val database: AppDatabase
) {

    suspend fun insertSessionIfDeviceAvailable(session: Session): Long? = database.withTransaction {
        val blockingSession = sessionDao.getBlockingSessionForDevice(
            deviceType = session.deviceType,
            deviceNumber = session.deviceNumber
        )
        if (blockingSession != null) {
            null
        } else {
            sessionDao.insert(SessionMapper.toEntity(session))
        }
    }

    suspend fun getBlockingSessionForDevice(
        deviceType: String,
        deviceNumber: Int
    ): Session? {
        val entity = sessionDao.getBlockingSessionForDevice(deviceType, deviceNumber)
        return entity?.let { SessionMapper.toModel(it) }
    }

    suspend fun updateSession(session: Session) {
        val entity = SessionMapper.toEntity(session)
        sessionDao.update(entity)
    }

    suspend fun pauseSessionIfActive(sessionId: Long, pausedAt: Date, updatedAt: Date): Boolean {
        return sessionDao.pauseIfActive(sessionId, pausedAt, updatedAt) > 0
    }

    suspend fun resumeSessionIfPaused(
        sessionId: Long,
        startTime: Date,
        totalPausedMinutes: Long,
        updatedAt: Date
    ): Boolean {
        return sessionDao.resumeIfPaused(
            sessionId = sessionId,
            startTime = startTime,
            totalPausedMinutes = totalPausedMinutes,
            updatedAt = updatedAt
        ) > 0
    }

    suspend fun deleteSession(session: Session) {
        val entity = SessionMapper.toEntity(session)
        sessionDao.delete(entity)
    }

    suspend fun getSessionById(sessionId: Long): Session? {
        val entity = sessionDao.getSessionById(sessionId)
        return entity?.let { SessionMapper.toModel(it) }
    }

    fun getSessionByIdFlow(sessionId: Long): Flow<Session?> {
        return sessionDao.getSessionByIdFlow(sessionId).map { entity ->
            entity?.let { SessionMapper.toModel(it) }
        }
    }

    fun getActiveSessions(): Flow<List<Session>> {
        return sessionDao.getActiveSessions().map { entities ->
            SessionMapper.toModelList(entities)
        }
    }

    fun getPausedSessions(): Flow<List<Session>> {
        return sessionDao.getPausedSessions().map { entities ->
            SessionMapper.toModelList(entities)
        }
    }

    fun getEndedSessions(): Flow<List<Session>> {
        return sessionDao.getEndedSessions().map { entities ->
            SessionMapper.toModelList(entities)
        }
    }

    fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessions().map { entities ->
            SessionMapper.toModelList(entities)
        }
    }

    fun getActiveSessionsCount(): Flow<Int> {
        return sessionDao.getActiveSessionsCount()
    }

    fun getTodaySessions(): Flow<List<Session>> {
        val (start, end) = DateUtils.todayRange()
        return sessionDao.getTodaySessions(start, end).map { entities ->
            SessionMapper.toModelList(entities)
        }
    }

    suspend fun deleteAllSessions() {
        sessionDao.deleteAll()
    }
}
