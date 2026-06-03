package com.mohamed.playstation.domain.usecase

import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.utils.SessionPricing
import com.mohamed.playstation.data.repository.SessionRepository
import com.mohamed.playstation.domain.model.Session
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

class SessionUseCases @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val receiptUseCases: ReceiptUseCases
) {

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
    ): Long {
        var finalStartTime = session.startTime
        var finalTotalPausedMinutes = session.totalPausedMinutes

        if (session.isPaused() && session.pausedAt != null) {
            val exactPausedDurationMs = Date().time - session.pausedAt.time
            val wholeMinutes = exactPausedDurationMs / 60000
            val remainderMs = exactPausedDurationMs % 60000
            finalTotalPausedMinutes = session.totalPausedMinutes + wholeMinutes
            finalStartTime = Date(session.startTime.time + remainderMs)
        }

        val livePricePerHour = SessionPricing.pricePerHour(
            pricing, session.deviceType, session.isMultiPlayer
        )
        val pricePerHour = if (session.isOpen()) {
            livePricePerHour
        } else {
            session.pricePerHour
        }

        val updatedSession = session.copy(
            status = AppConstants.SESSION_STATUS_ENDED,
            endTime = Date(),
            pausedAt = null,
            startTime = finalStartTime,
            totalPausedMinutes = finalTotalPausedMinutes,
            pricePerHour = pricePerHour,
            updatedAt = Date()
        )
        sessionRepository.updateSession(updatedSession)

        return receiptUseCases.createReceiptFromSession(
            session = updatedSession,
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
