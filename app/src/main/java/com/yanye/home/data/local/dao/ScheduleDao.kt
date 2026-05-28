package com.yanye.home.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yanye.home.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query(
        """
        SELECT * FROM schedules
        WHERE isDeleted = 0
        ORDER BY startEpochDay ASC, startMinuteOfDay ASC, createdAt ASC
        """
    )
    fun observeSchedules(): Flow<List<ScheduleEntity>>

    @Query(
        """
        SELECT * FROM schedules
        WHERE isDeleted = 0
          AND coupleId = :coupleId
        ORDER BY startEpochDay ASC, startMinuteOfDay ASC, createdAt ASC
        """
    )
    fun observeSchedulesForCouple(coupleId: String): Flow<List<ScheduleEntity>>

    @Query(
        """
        SELECT * FROM schedules
        WHERE startEpochDay = :epochDay AND isDeleted = 0
        ORDER BY startMinuteOfDay ASC, createdAt ASC
        """
    )
    fun observeSchedulesForDay(epochDay: Long): Flow<List<ScheduleEntity>>

    @Query(
        """
        SELECT * FROM schedules
        WHERE startEpochDay = :epochDay
          AND isDeleted = 0
          AND coupleId = :coupleId
        ORDER BY startMinuteOfDay ASC, createdAt ASC
        """
    )
    fun observeSchedulesForDayAndCouple(epochDay: Long, coupleId: String): Flow<List<ScheduleEntity>>

    @Query(
        """
        SELECT * FROM schedules
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun findById(id: Long): ScheduleEntity?

    @Query(
        """
        SELECT * FROM schedules
        WHERE syncStatus != 'SYNCED'
           OR (remoteId IS NULL AND visibility != 'PRIVATE')
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncSchedules(): List<ScheduleEntity>

    @Query(
        """
        SELECT * FROM schedules
        WHERE (syncStatus != 'SYNCED'
           OR (remoteId IS NULL AND visibility != 'PRIVATE'))
          AND coupleId = :coupleId
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncSchedulesForCouple(coupleId: String): List<ScheduleEntity>

    @Query(
        """
        SELECT * FROM schedules
        WHERE remoteId = :remoteId
        LIMIT 1
        """
    )
    suspend fun findByRemoteId(remoteId: String): ScheduleEntity?

    @Query(
        """
        SELECT * FROM schedules
        WHERE remoteId = :remoteId AND coupleId = :coupleId
        LIMIT 1
        """
    )
    suspend fun findByRemoteIdForCouple(remoteId: String, coupleId: String): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Query(
        """
        UPDATE schedules
        SET isCompleted = 1,
            memoryId = :memoryId,
            syncStatus = CASE WHEN visibility = 'PRIVATE' THEN syncStatus ELSE 'PENDING_UPDATE' END,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markCompleted(id: Long, memoryId: Long?, updatedAt: Long)

    @Query(
        """
        UPDATE schedules
        SET isDeleted = 1,
            syncStatus = CASE WHEN visibility = 'PRIVATE' AND remoteId IS NULL THEN syncStatus ELSE 'PENDING_DELETE' END,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun softDeleteSchedule(id: Long, updatedAt: Long)

    @Query(
        """
        UPDATE schedules
        SET remoteId = :remoteId,
            coupleId = :coupleId,
            ownerUserId = :ownerUserId,
            syncStatus = 'SYNCED',
            remoteUpdatedAt = :remoteUpdatedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markSynced(
        id: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long,
        updatedAt: Long
    )

    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()

}
