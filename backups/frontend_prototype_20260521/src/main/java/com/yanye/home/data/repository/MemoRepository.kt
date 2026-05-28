package com.yanye.home.data.repository

import com.yanye.home.data.local.dao.MemoDao
import com.yanye.home.data.local.entity.CareCycleEntity
import com.yanye.home.data.local.entity.MemoEntity
import com.yanye.home.domain.model.CareCycle
import com.yanye.home.domain.model.CareMood
import com.yanye.home.domain.model.CarePainLevel
import com.yanye.home.domain.model.Memo
import com.yanye.home.domain.model.MemoCategory
import com.yanye.home.domain.model.SyncStatus
import com.yanye.home.domain.model.Visibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemoRepository(
    private val memoDao: MemoDao
) {
    fun observeMemos(): Flow<List<Memo>> =
        memoDao.observeMemos()
            .map { memos -> memos.map(MemoEntity::toDomain) }

    fun observeCareCycles(): Flow<List<CareCycle>> =
        memoDao.observeCareCycles()
            .map { cycles -> cycles.map(CareCycleEntity::toDomain) }

    suspend fun saveMemo(memo: Memo): Long {
        val now = System.currentTimeMillis()
        val entity = memo.toEntity(
            createdAt = memo.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now
        )
        return if (memo.id == 0L) {
            memoDao.insertMemo(entity)
        } else {
            memoDao.updateMemo(entity)
            memo.id
        }
    }

    suspend fun setMemoCompleted(id: Long, isCompleted: Boolean) {
        memoDao.setMemoCompleted(
            id = id,
            isCompleted = isCompleted,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteMemo(id: Long) {
        memoDao.softDeleteMemo(
            id = id,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun saveCareCycle(careCycle: CareCycle): Long {
        val now = System.currentTimeMillis()
        val syncStatus = if (careCycle.remoteId.isNullOrBlank()) {
            SyncStatus.PendingCreate
        } else {
            SyncStatus.PendingUpdate
        }
        return memoDao.insertCareCycle(
            careCycle.toEntity(
                createdAt = careCycle.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = now,
                syncStatus = syncStatus
            )
        )
    }

    suspend fun deleteCareCycle(id: Long) {
        memoDao.softDeleteCareCycle(
            id = id,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun clearCareCycles() {
        memoDao.softDeleteAllCareCycles(
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun pendingSyncCareCycles(): List<CareCycle> =
        memoDao.pendingSyncCareCycles().map(CareCycleEntity::toDomain)

    suspend fun saveRemoteCareCycle(careCycle: CareCycle): Long {
        val now = System.currentTimeMillis()
        val entity = careCycle.toEntity(
            createdAt = careCycle.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = careCycle.updatedAt.takeIf { it > 0 } ?: now,
            syncStatus = SyncStatus.Synced
        )
        val local = careCycle.remoteId?.let { memoDao.findCareCycleByRemoteId(it) }
            ?: memoDao.findCareCycleByStart(careCycle.startEpochDay)
        if (local == null && careCycle.isDeleted) {
            return 0L
        }

        return if (local == null) {
            memoDao.insertCareCycle(entity)
        } else {
            memoDao.insertCareCycle(entity.copy(id = local.id))
            local.id
        }
    }

    suspend fun markCareCycleSynced(
        localId: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long
    ) {
        memoDao.markCareCycleSynced(
            id = localId,
            remoteId = remoteId,
            coupleId = coupleId,
            ownerUserId = ownerUserId,
            remoteUpdatedAt = remoteUpdatedAt,
            updatedAt = System.currentTimeMillis()
        )
    }
}

private fun MemoEntity.toDomain(): Memo =
    Memo(
        id = id,
        title = title,
        content = content,
        category = MemoCategory.fromStorageValue(category),
        checklistItems = checklistItems,
        imageUris = imageUris,
        reminderAtMillis = reminderAtMillis,
        reminderEnabled = reminderEnabled,
        notificationChannelKey = notificationChannelKey,
        linkedScheduleId = linkedScheduleId,
        visibility = Visibility.fromStorageValue(visibility),
        sharedWithPartner = sharedWithPartner,
        isCompleted = isCompleted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

private fun Memo.toEntity(
    createdAt: Long,
    updatedAt: Long
): MemoEntity =
    MemoEntity(
        id = id,
        title = title,
        content = content,
        category = category.storageValue,
        checklistItems = checklistItems,
        imageUris = imageUris,
        reminderAtMillis = reminderAtMillis,
        reminderEnabled = reminderEnabled,
        notificationChannelKey = notificationChannelKey,
        linkedScheduleId = linkedScheduleId,
        visibility = visibility.storageValue,
        sharedWithPartner = sharedWithPartner,
        isCompleted = isCompleted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

private fun CareCycleEntity.toDomain(): CareCycle =
    CareCycle(
        id = id,
        startEpochDay = startEpochDay,
        endEpochDay = endEpochDay,
        cycleLengthDays = cycleLengthDays,
        painLevel = CarePainLevel.fromStorageValue(painLevel),
        mood = CareMood.fromStorageValue(mood),
        avoidNotes = avoidNotes,
        carePreference = carePreference,
        shareReminderWithPartner = shareReminderWithPartner,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = SyncStatus.fromStorageValue(syncStatus),
        remoteUpdatedAt = remoteUpdatedAt
    )

private fun CareCycle.toEntity(
    createdAt: Long,
    updatedAt: Long,
    syncStatus: SyncStatus = this.syncStatus
): CareCycleEntity =
    CareCycleEntity(
        id = id,
        startEpochDay = startEpochDay,
        endEpochDay = endEpochDay,
        cycleLengthDays = cycleLengthDays,
        painLevel = painLevel.storageValue,
        mood = mood.storageValue,
        avoidNotes = avoidNotes,
        carePreference = carePreference,
        shareReminderWithPartner = shareReminderWithPartner,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = syncStatus.storageValue,
        remoteUpdatedAt = remoteUpdatedAt
    )
