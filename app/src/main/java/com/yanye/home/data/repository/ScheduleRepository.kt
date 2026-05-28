package com.yanye.home.data.repository

import com.yanye.home.data.local.dao.MemoryDao
import com.yanye.home.data.local.dao.ScheduleDao
import com.yanye.home.data.local.dao.WishDao
import com.yanye.home.data.local.entity.MemoryEntity
import com.yanye.home.data.local.entity.ScheduleEntity
import com.yanye.home.data.sync.SyncSettings
import com.yanye.home.domain.model.Memory
import com.yanye.home.domain.model.MemoryMood
import com.yanye.home.domain.model.Schedule
import com.yanye.home.domain.model.SyncStatus
import com.yanye.home.domain.model.Visibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScheduleRepository(
    private val scheduleDao: ScheduleDao,
    private val wishDao: WishDao,
    private val memoryDao: MemoryDao,
    private val syncSettings: SyncSettings
) {
    fun observeSchedules(): Flow<List<Schedule>> =
        scheduleDao.observeSchedulesForCouple(syncSettings.identity().coupleId)
            .map { schedules -> schedules.map(ScheduleEntity::toDomain) }

    fun observeMemories(): Flow<List<Memory>> =
        memoryDao.observeMemoriesForCouple(syncSettings.identity().coupleId)
            .map { memories -> memories.map(MemoryEntity::toDomain) }

    suspend fun saveSchedule(schedule: Schedule): Long {
        val now = System.currentTimeMillis()
        val previousSchedule = schedule.id.takeIf { it != 0L }
            ?.let { scheduleDao.findById(it)?.toDomain() }
        val linkedWishRemoteId = schedule.linkedWishId
            ?.let { wishId -> wishDao.findById(wishId)?.remoteId }
        val syncStatus = when {
            schedule.visibility == Visibility.Private -> schedule.syncStatus
            schedule.remoteId.isNullOrBlank() -> SyncStatus.PendingCreate
            else -> SyncStatus.PendingUpdate
        }
        val identity = syncSettings.identity()
        val scopedSchedule = schedule.copy(
            coupleId = schedule.coupleId ?: identity.coupleId,
            ownerUserId = schedule.ownerUserId ?: identity.localUserId
        )
        val entity = scopedSchedule.toEntity(
            createdAt = schedule.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now,
            linkedWishRemoteId = linkedWishRemoteId,
            syncStatus = syncStatus
        )

        val scheduleId = if (schedule.id == 0L) {
            scheduleDao.insertSchedule(entity)
        } else {
            scheduleDao.updateSchedule(entity)
            schedule.id
        }

        if (previousSchedule?.linkedWishId != null && previousSchedule.linkedWishId != schedule.linkedWishId) {
            wishDao.setLinkedSchedule(
                id = previousSchedule.linkedWishId,
                scheduleId = null,
                updatedAt = now
            )
        }
        schedule.linkedWishId?.let { wishId ->
            wishDao.setLinkedSchedule(
                id = wishId,
                scheduleId = scheduleId,
                updatedAt = now
            )
        }

        return scheduleId
    }

    suspend fun saveMemory(memory: Memory): Long {
        val now = System.currentTimeMillis()
        val syncStatus = if (memory.remoteId.isNullOrBlank()) {
            SyncStatus.PendingCreate
        } else {
            SyncStatus.PendingUpdate
        }
        val identity = syncSettings.identity()
        val scopedMemory = memory.copy(
            coupleId = memory.coupleId ?: identity.coupleId,
            ownerUserId = memory.ownerUserId ?: identity.localUserId
        )
        val entity = scopedMemory.toEntity(
            createdAt = memory.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now,
            syncStatus = syncStatus
        )

        return if (memory.id == 0L) {
            memoryDao.insertMemory(entity)
        } else {
            memoryDao.updateMemory(entity)
            memory.id
        }
    }

    suspend fun pendingSyncMemories(): List<Memory> =
        memoryDao.pendingSyncMemoriesForCouple(syncSettings.identity().coupleId)
            .map(MemoryEntity::toDomain)

    suspend fun saveRemoteMemory(memory: Memory): Long {
        if (memory.coupleId != syncSettings.identity().coupleId) return 0L
        val now = System.currentTimeMillis()
        val entity = memory.toEntity(
            createdAt = memory.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = memory.updatedAt.takeIf { it > 0 } ?: now,
            syncStatus = SyncStatus.Synced
        )
        val identity = syncSettings.identity()
        val local = memory.remoteId?.let { memoryDao.findByRemoteIdForCouple(it, identity.coupleId) }
        if (local == null && memory.isDeleted) {
            return 0L
        }

        return if (local == null) {
            memoryDao.insertMemory(entity)
        } else {
            memoryDao.updateMemory(entity.copy(id = local.id))
            local.id
        }
    }

    suspend fun markMemorySynced(
        localId: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long
    ) {
        memoryDao.markSynced(
            id = localId,
            remoteId = remoteId,
            coupleId = coupleId,
            ownerUserId = ownerUserId,
            remoteUpdatedAt = remoteUpdatedAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun pendingSyncSchedules(): List<Schedule> =
        scheduleDao.pendingSyncSchedulesForCouple(syncSettings.identity().coupleId)
            .map(ScheduleEntity::toDomain)

    suspend fun saveRemoteSchedule(schedule: Schedule): Long {
        if (schedule.coupleId != syncSettings.identity().coupleId) return 0L
        val identity = syncSettings.identity()
        val now = System.currentTimeMillis()
        val localLinkedWishId = schedule.linkedWishRemoteId
            ?.let { remoteId -> wishDao.findByRemoteIdForCouple(remoteId, identity.coupleId)?.id }
            ?: schedule.linkedWishId
        val entity = schedule.copy(linkedWishId = localLinkedWishId).toEntity(
            createdAt = schedule.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = schedule.updatedAt.takeIf { it > 0 } ?: now,
            linkedWishRemoteId = schedule.linkedWishRemoteId,
            syncStatus = SyncStatus.Synced
        )
        val local = schedule.remoteId?.let { scheduleDao.findByRemoteIdForCouple(it, identity.coupleId) }
        if (local == null && schedule.isDeleted) {
            return 0L
        }

        return if (local == null) {
            scheduleDao.insertSchedule(entity)
        } else {
            scheduleDao.updateSchedule(entity.copy(id = local.id))
            local.id
        }
    }

    suspend fun markSynced(
        localId: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long
    ) {
        scheduleDao.markSynced(
            id = localId,
            remoteId = remoteId,
            coupleId = coupleId,
            ownerUserId = ownerUserId,
            remoteUpdatedAt = remoteUpdatedAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun completeAndArchive(schedule: Schedule, memory: Memory): Long {
        val now = System.currentTimeMillis()
        val memoryId = memoryDao.insertMemory(
            memory.copy(
                scheduleId = schedule.id,
                linkedWishId = schedule.linkedWishId,
                dateEpochDay = schedule.startEpochDay,
                locationName = schedule.locationName,
                coupleId = memory.coupleId ?: syncSettings.identity().coupleId,
                ownerUserId = memory.ownerUserId ?: syncSettings.identity().localUserId,
                createdAt = memory.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PendingCreate
            ).toEntity(
                createdAt = memory.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PendingCreate
            )
        )
        scheduleDao.markCompleted(
            id = schedule.id,
            memoryId = memoryId,
            updatedAt = now
        )
        schedule.linkedWishId?.let { wishId ->
            wishDao.setWishCompleted(
                id = wishId,
                isCompleted = true,
                updatedAt = now
            )
        }
        return memoryId
    }

    suspend fun deleteSchedule(id: Long) {
        val schedule = scheduleDao.findById(id)?.toDomain()
        val now = System.currentTimeMillis()
        schedule?.linkedWishId?.let { wishId ->
            wishDao.setLinkedSchedule(
                id = wishId,
                scheduleId = null,
                updatedAt = now
            )
        }
        scheduleDao.softDeleteSchedule(
            id = id,
            updatedAt = now
        )
    }

    suspend fun clearSchedulesAndMemories() {
        val now = System.currentTimeMillis()
        memoryDao.deleteAllMemories()
        scheduleDao.deleteAllSchedules()
        wishDao.clearLinkedSchedules(updatedAt = now)
    }

}

private fun ScheduleEntity.toDomain(): Schedule =
    Schedule(
        id = id,
        title = title,
        startEpochDay = startEpochDay,
        startMinuteOfDay = startMinuteOfDay,
        endMinuteOfDay = endMinuteOfDay,
        locationName = locationName,
        reminderMinutesBefore = reminderMinutesBefore,
        budgetCents = budgetCents,
        participants = participants,
        linkedWishId = linkedWishId,
        linkedWishRemoteId = linkedWishRemoteId,
        isGuideMode = isGuideMode,
        guideRestaurants = guideRestaurants,
        guideActivities = guideActivities,
        guideRoute = guideRoute,
        backupPlan = backupPlan,
        note = note,
        isCompleted = isCompleted,
        memoryId = memoryId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        visibility = Visibility.fromStorageValue(visibility),
        sharedWithPartner = sharedWithPartner,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = SyncStatus.fromStorageValue(syncStatus),
        remoteUpdatedAt = remoteUpdatedAt
    )

private fun Schedule.toEntity(
    createdAt: Long,
    updatedAt: Long,
    linkedWishRemoteId: String? = this.linkedWishRemoteId,
    syncStatus: SyncStatus = this.syncStatus
): ScheduleEntity =
    ScheduleEntity(
        id = id,
        title = title,
        startEpochDay = startEpochDay,
        startMinuteOfDay = startMinuteOfDay,
        endMinuteOfDay = endMinuteOfDay,
        locationName = locationName,
        reminderMinutesBefore = reminderMinutesBefore,
        budgetCents = budgetCents,
        participants = participants,
        linkedWishId = linkedWishId,
        linkedWishRemoteId = linkedWishRemoteId,
        isGuideMode = isGuideMode,
        guideRestaurants = guideRestaurants,
        guideActivities = guideActivities,
        guideRoute = guideRoute,
        backupPlan = backupPlan,
        note = note,
        isCompleted = isCompleted,
        memoryId = memoryId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        visibility = visibility.storageValue,
        sharedWithPartner = sharedWithPartner,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = syncStatus.storageValue,
        remoteUpdatedAt = remoteUpdatedAt
    )

private fun MemoryEntity.toDomain(): Memory =
    Memory(
        id = id,
        title = title,
        dateEpochDay = dateEpochDay,
        scheduleId = scheduleId,
        linkedWishId = linkedWishId,
        locationName = locationName,
        photoUris = photoUris,
        foodNotes = foodNotes,
        expenseCents = expenseCents,
        mood = MemoryMood.fromStorageValue(mood),
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = SyncStatus.fromStorageValue(syncStatus),
        remoteUpdatedAt = remoteUpdatedAt
    )

private fun Memory.toEntity(
    createdAt: Long = this.createdAt,
    updatedAt: Long = this.updatedAt,
    syncStatus: SyncStatus = this.syncStatus
): MemoryEntity =
    MemoryEntity(
        id = id,
        title = title,
        dateEpochDay = dateEpochDay,
        scheduleId = scheduleId,
        linkedWishId = linkedWishId,
        locationName = locationName,
        photoUris = photoUris,
        foodNotes = foodNotes,
        expenseCents = expenseCents,
        mood = mood.storageValue,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = syncStatus.storageValue,
        remoteUpdatedAt = remoteUpdatedAt
    )
