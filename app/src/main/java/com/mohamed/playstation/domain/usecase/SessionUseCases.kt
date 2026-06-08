package com.mohamed.playstation.domain.usecase

import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.utils.SessionPricing
import com.mohamed.playstation.core.utils.SessionTimer
import com.mohamed.playstation.data.repository.SessionRepository
import com.mohamed.playstation.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionUseCases @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val receiptUseCases: ReceiptUseCases
) {

    private val endSessionMutex = Mutex()

    suspend fun startSession(
        deviceType: String,
        deviceNumber: Int,
        sessionMode: String,
        isMultiPlayer: Boolean,
        fixedDurationMinutes: Int?,
        pricing: SessionPricing.PricingSettings
    ): Long {
        val blockingSession = sessionRepository.getBlockingSessionForDevice(
            deviceType = deviceType,
            deviceNumber = deviceNumber
        )
        if (blockingSession != null) {
            throw DuplicateDeviceSessionException(deviceType, deviceNumber)
        }

        val sessionType = if (isMultiPlayer) {
            AppConstants.SESSION_TYPE_MULTI
        } else {
            AppConstants.SESSION_TYPE_SINGLE
        }

        val pricePerHour = if (sessionMode == AppConstants.SESSION_MODE_FIXED) {
            val minutes = fixedDurationMinutes ?: AppConstants.DEFAULT_FIXED_MINUTES
            val packagePrice = SessionPricing.fixedPackagePrice(
                pricing, deviceType, isMultiPlayer, minutes
            )
            if (minutes > 0) packagePrice / (minutes / 60.0) else packagePrice
        } else {
            SessionPricing.pricePerHour(pricing, deviceType, isMultiPlayer)
        }

        val session = Session(
            deviceType = deviceType,
            deviceNumber = deviceNumber,
            sessionType = sessionType,
            sessionMode = sessionMode,
            isMultiPlayer = isMultiPlayer,
            fixedDurationMinutes = if (sessionMode == AppConstants.SESSION_MODE_FIXED) {
                fixedDurationMinutes ?: AppConstants.DEFAULT_FIXED_MINUTES
            } else {
                null
            },
            startTime = Date(),
            status = AppConstants.SESSION_STATUS_ACTIVE,
            pricePerHour = pricePerHour
        )
        return sessionRepository.insertSession(session)
    }

    suspend fun pauseSession(session: Session) {
        val updatedSession = session.copy(
            status = AppConstants.SESSION_STATUS_PAUSED,
            pausedAt = Date(),
            updatedAt = Date()
        )
        sessionRepository.updateSession(updatedSession)
    }

    suspend fun resumeSession(session: Session) {
        val exactPausedDurationMs = if (session.pausedAt != null) {
            Date().time - session.pausedAt.time
        } else {
            0L
        }

        val wholeMinutes = exactPausedDurationMs / 60000
        val remainderMs = exactPausedDurationMs % 60000

        val updatedSession = session.copy(
            status = AppConstants.SESSION_STATUS_ACTIVE,
            pausedAt = null,
            startTime = Date(session.startTime.time + remainderMs),
            totalPausedMinutes = session.totalPausedMinutes + wholeMinutes,
            updatedAt = Date()
        )
        sessionRepository.updateSession(updatedSession)
    }

    suspend fun endSessionAndCreateReceipt(
        session: Session,
        currencyCode: String,
        pricing: SessionPricing.PricingSettings,
        paymentMethod: String? = null
    ): Long = endSessionMutex.withLock {
        val currentSession = sessionRepository.getSessionById(session.id) ?: session
        val existingReceipt = receiptUseCases.getReceiptBySessionId(currentSession.id)
        if (existingReceipt != null) {
            return@withLock existingReceipt.id
        }

        val endedSession = if (currentSession.isEnded()) {
            currentSession
        } else {
            var finalStartTime = currentSession.startTime
            var finalTotalPausedMinutes = currentSession.totalPausedMinutes

            if (currentSession.isPaused() && currentSession.pausedAt != null) {
                val exactPausedDurationMs = Date().time - currentSession.pausedAt.time
                val wholeMinutes = exactPausedDurationMs / 60000
                val remainderMs = exactPausedDurationMs % 60000
                finalTotalPausedMinutes = currentSession.totalPausedMinutes + wholeMinutes
                finalStartTime = Date(currentSession.startTime.time + remainderMs)
            }

            val livePricePerHour = SessionPricing.pricePerHour(
                pricing, currentSession.deviceType, currentSession.isMultiPlayer
            )
            val pricePerHour = if (currentSession.isOpen()) {
                livePricePerHour
            } else {
                currentSession.pricePerHour
            }
            val calculatedEndTime = if (currentSession.isFixed()) {
                SessionTimer.getFixedEndTimeMs(currentSession)?.let(::Date)
            } else {
                Date()
            }

            val updatedSession = currentSession.copy(
                status = AppConstants.SESSION_STATUS_ENDED,
                endTime = calculatedEndTime ?: Date(),
                pausedAt = null,
                startTime = finalStartTime,
                totalPausedMinutes = finalTotalPausedMinutes,
                pricePerHour = pricePerHour,
                updatedAt = Date()
            )


            sessionRepository.updateSession(updatedSession)
            updatedSession
        }

        val pricePerHour = if (currentSession.isEnded()) {
            endedSession.pricePerHour
        } else if (currentSession.isOpen()) {
            SessionPricing.pricePerHour(
                pricing,
                endedSession.deviceType,
                endedSession.isMultiPlayer
            )
        } else {
            endedSession.pricePerHour
        }

        return@withLock receiptUseCases.createReceiptFromSession(
            session = endedSession,
            currencyCode = currencyCode,
            pricePerHour = pricePerHour,
            paymentMethod = paymentMethod
        )
    }

    suspend fun updateSessionNotes(session: Session, notes: String) {
        sessionRepository.updateSession(
            session.copy(notes = notes, updatedAt = Date())
        )
    }

    suspend fun deleteSession(session: Session) {
        sessionRepository.deleteSession(session)
    }

    suspend fun getSessionById(sessionId: Long): Session? {
        return sessionRepository.getSessionById(sessionId)
    }

    fun getActiveSessions(): Flow<List<Session>> = sessionRepository.getActiveSessions()

    fun getPausedSessions(): Flow<List<Session>> = sessionRepository.getPausedSessions()

    fun getEndedSessions(): Flow<List<Session>> = sessionRepository.getEndedSessions()

    fun getAllSessions(): Flow<List<Session>> = sessionRepository.getAllSessions()

    fun getActiveSessionsCount(): Flow<Int> = sessionRepository.getActiveSessionsCount()

    fun getTodaySessions(): Flow<List<Session>> = sessionRepository.getTodaySessions()
}
