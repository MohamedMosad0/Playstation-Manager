package com.mohamed.playstation.domain.usecase

import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.utils.SessionPricing
import com.mohamed.playstation.data.local.FakeAppDatabase
import com.mohamed.playstation.data.local.FakeSessionDao
import com.mohamed.playstation.data.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class SessionUseCasesTest {

    private lateinit var fakeDao: FakeSessionDao
    private lateinit var fakeDatabase: FakeAppDatabase
    private lateinit var sessionRepository: SessionRepository
    private lateinit var mockReceiptUseCases: ReceiptUseCases
    private lateinit var sessionUseCases: SessionUseCases

    private val defaultPricing = SessionPricing.PricingSettings(
        ps4HourPrice = 30.0,
        ps4HalfHourPrice = 15.0,
        ps4MultiExtra = 10.0,
        ps5HourPrice = 50.0,
        ps5HalfHourPrice = 25.0,
        ps5MultiExtra = 15.0,
        legacySinglePrice = 30.0,
        legacyMultiPrice = 40.0
    )

    @Before
    fun setUp() {
        fakeDao = FakeSessionDao()
        fakeDatabase = FakeAppDatabase()
        mockReceiptUseCases = mock()

        sessionRepository = SessionRepository(fakeDao, fakeDatabase)

        sessionUseCases = SessionUseCases(
            sessionRepository = sessionRepository,
            receiptUseCases = mockReceiptUseCases,
            database = fakeDatabase
        )
    }

    @Test
    fun startSession_createsActiveSessionInDatabase_andReturnsGeneratedId() = runTest {
        val sessionId = sessionUseCases.startSession(
            deviceType = AppConstants.DEVICE_PS4,
            deviceNumber = 1,
            sessionMode = AppConstants.SESSION_MODE_OPEN,
            isMultiPlayer = false,
            fixedDurationMinutes = null,
            pricing = defaultPricing
        )

        assertNotNull(sessionId)
        val createdSession = sessionRepository.getSessionById(sessionId)
        assertNotNull(createdSession)
        assertEquals(AppConstants.DEVICE_PS4, createdSession!!.deviceType)
        assertEquals(1, createdSession.deviceNumber)
        assertEquals(AppConstants.SESSION_STATUS_ACTIVE, createdSession.status)
    }

    @Test
    fun pauseAndResumeSession_updatesSessionStatusInDatabase() = runTest {
        val sessionId = sessionUseCases.startSession(
            deviceType = AppConstants.DEVICE_PS5,
            deviceNumber = 2,
            sessionMode = AppConstants.SESSION_MODE_OPEN,
            isMultiPlayer = false,
            fixedDurationMinutes = null,
            pricing = defaultPricing
        )

        val activeSession = sessionRepository.getSessionById(sessionId)!!
        assertEquals(AppConstants.SESSION_STATUS_ACTIVE, activeSession.status)

        // Pause session
        sessionUseCases.pauseSession(activeSession)
        val pausedSession = sessionRepository.getSessionById(sessionId)!!
        assertEquals(AppConstants.SESSION_STATUS_PAUSED, pausedSession.status)
        assertNotNull(pausedSession.pausedAt)

        // Resume session
        sessionUseCases.resumeSession(pausedSession)
        val resumedSession = sessionRepository.getSessionById(sessionId)!!
        assertEquals(AppConstants.SESSION_STATUS_ACTIVE, resumedSession.status)
        assertNull(resumedSession.pausedAt)
    }

    @Test
    fun deleteSession_removesSessionFromDatabase() = runTest {
        val sessionId = sessionUseCases.startSession(
            deviceType = AppConstants.DEVICE_PS4,
            deviceNumber = 5,
            sessionMode = AppConstants.SESSION_MODE_OPEN,
            isMultiPlayer = false,
            fixedDurationMinutes = null,
            pricing = defaultPricing
        )

        val sessionToDelete = sessionRepository.getSessionById(sessionId)!!
        sessionUseCases.deleteSession(sessionToDelete)

        assertNull(sessionRepository.getSessionById(sessionId))
    }
}
