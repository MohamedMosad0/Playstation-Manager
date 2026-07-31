package com.mohamed.playstation.data.repository

import app.cash.turbine.test
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.data.local.FakeAppDatabase
import com.mohamed.playstation.data.local.FakeSessionDao
import com.mohamed.playstation.domain.model.Session
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryTest {

    private lateinit var fakeDao: FakeSessionDao
    private lateinit var fakeDatabase: FakeAppDatabase
    private lateinit var repository: SessionRepository

    @Before
    fun setUp() {
        fakeDao = FakeSessionDao()
        fakeDatabase = FakeAppDatabase()
        repository = SessionRepository(fakeDao, fakeDatabase)
    }

    @Test
    fun insertSessionIfDeviceAvailable_insertsWhenAvailable_andBlocksDuplicate() = runTest {
        val session1 = Session(
            deviceType = AppConstants.DEVICE_PS4,
            deviceNumber = 1,
            sessionType = AppConstants.SESSION_TYPE_SINGLE,
            sessionMode = AppConstants.SESSION_MODE_OPEN,
            isMultiPlayer = false,
            startTime = Date(),
            status = AppConstants.SESSION_STATUS_ACTIVE,
            pricePerHour = 30.0
        )

        val id1 = repository.insertSessionIfDeviceAvailable(session1)
        assertNotNull(id1)
        assertEquals(1L, id1)

        val session2 = session1.copy(id = 0L)
        val id2 = repository.insertSessionIfDeviceAvailable(session2)
        assertNull("Duplicate active session for device should be blocked", id2)
    }

    @Test
    fun pauseAndResumeSession_updatesStateFlows() = runTest {
        val session = Session(
            deviceType = AppConstants.DEVICE_PS5,
            deviceNumber = 2,
            sessionType = AppConstants.SESSION_TYPE_SINGLE,
            sessionMode = AppConstants.SESSION_MODE_OPEN,
            isMultiPlayer = false,
            startTime = Date(),
            status = AppConstants.SESSION_STATUS_ACTIVE,
            pricePerHour = 50.0
        )

        val id = repository.insertSessionIfDeviceAvailable(session)!!

        repository.getActiveSessions().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(id, list[0].id)
            cancelAndIgnoreRemainingEvents()
        }

        val now = Date()
        val paused = repository.pauseSessionIfActive(id, now, now)
        assertTrue(paused)

        repository.getActiveSessions().test {
            val list = awaitItem()
            assertEquals(0, list.size)
            cancelAndIgnoreRemainingEvents()
        }

        repository.getPausedSessions().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(id, list[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteSession_removesSessionFromDatabase() = runTest {
        val session = Session(
            deviceType = AppConstants.DEVICE_PS4,
            deviceNumber = 3,
            sessionType = AppConstants.SESSION_TYPE_SINGLE,
            sessionMode = AppConstants.SESSION_MODE_OPEN,
            isMultiPlayer = false,
            startTime = Date(),
            status = AppConstants.SESSION_STATUS_ACTIVE,
            pricePerHour = 30.0
        )

        val id = repository.insertSessionIfDeviceAvailable(session)!!
        val inserted = repository.getSessionById(id)
        assertNotNull(inserted)

        repository.deleteSession(inserted!!)
        assertNull(repository.getSessionById(id))
    }
}
