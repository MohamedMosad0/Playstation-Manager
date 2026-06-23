package com.mohamed.playstation.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mohamed.playstation.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): SessionEntity?

    @Query(
        """
        SELECT * FROM sessions
        WHERE deviceType = :deviceType
          AND deviceNumber = :deviceNumber
          AND status IN ('active', 'paused')
        LIMIT 1
        """
    )
    suspend fun getBlockingSessionForDevice(
        deviceType: String,
        deviceNumber: Int
    ): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSessionByIdFlow(sessionId: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE status = 'active' ORDER BY startTime DESC")
    fun getActiveSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE status = 'paused' ORDER BY startTime DESC")
    fun getPausedSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE status = 'ended' ORDER BY endTime DESC")
    fun getEndedSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions WHERE status = 'active'")
    fun getActiveSessionsCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM sessions
        WHERE startTime >= :startMs AND startTime < :endMs
        ORDER BY startTime DESC
        """
    )
    fun getTodaySessions(startMs: Long, endMs: Long): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
