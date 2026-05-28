package com.yanye.home.data.repository

import com.yanye.home.data.local.dao.MemoDao
import com.yanye.home.data.local.entity.CareCycleEntity
import com.yanye.home.data.local.entity.MemoEntity
import com.yanye.home.data.sync.SyncSettings
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
    private val memoDao: MemoDao,
    private val syncSettings: SyncSettings
) {
    fun observeMemos(): Flow<List<Memo>> =
        memoDao.observeMemosForCouple(syncSettings.identity().coupleId)
            .map { memos -> memos.map(MemoEntity::toDomain) }

    fun observeCareCycles(): Flow<List<CareCycle>> =
        memoDao.observeCareCyclesForCouple(syncSettings.identity().coupleId)
            .map { cycles -> cycles.map(CareCycleEntity::toDomain) }

    suspend fun saveMemo(memo: Memo): Long {
        val now = System.currentTimeMillis()
        val syncStatus = when {
            memo.visibility == Visibility.Private && !memo.remoteId.isNullOrBlank() -> SyncStatus.PendingDelete
            memo.visibility == Visibility.Private -> SyncStatus.Synced
            memo.remoteId.isNullOrBlank() -> SyncStatus.PendingCreate
            else -> SyncStatus.PendingUpdate
        }
        val identity = syncSettings.identity()
        val scopedMemo = memo.copy(
            coupleId = memo.coupleId ?: identity.coupleId,
            ownerUserId = memo.ownerUserId ?: identity.localUserId
        )
        val entity = scopedMemo.toEntity(
            createdAt = memo.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now,
            syncStatus = syncStatus
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

    suspend fun pendingSyncMemos(): List<Memo> =
        memoDao.pendingSyncMemosForCouple(syncSettings.identity().coupleId).map(MemoEntity::toDomain)

    suspend fun saveRemoteMemo(memo: Memo): Long {
        if (memo.coupleId != syncSettings.identity().coupleId) return 0L
        val now = System.currentTimeMillis()
        val entity = memo.toEntity(
            createdAt = memo.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = memo.updatedAt.takeIf { it > 0 } ?: now,
            syncStatus = SyncStatus.Synced
        )
        val identity = syncSettings.identity()
        val local = memo.remoteId?.let { memoDao.findMemoByRemoteIdForCouple(it, identity.coupleId) }
        if (local == null && memo.isDeleted) {
            return 0L
        }

        return if (local == null) {
            memoDao.insertMemo(entity)
        } else {
            memoDao.updateMemo(entity.copy(id = local.id))
            local.id
        }
    }

    suspend fun markMemoSynced(
        localId: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long
    ) {
        memoDao.markMemoSynced(
            id = localId,
            remoteId = remoteId,
            coupleId = coupleId,
            ownerUserId = ownerUserId,
            remoteUpdatedAt = remoteUpdatedAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun markMemoPrivateRevokeSynced(
        localId: Long,
        ownerUserId: String
    ) {
        memoDao.markMemoPrivateRevokeSynced(
            id = localId,
            ownerUserId = ownerUserId,
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
        val identity = syncSettings.identity()
        val scopedCareCycle = careCycle.copy(
            coupleId = careCycle.coupleId ?: identity.coupleId,
            ownerUserId = careCycle.ownerUserId ?: identity.localUserId
        )
        return memoDao.insertCareCycle(
            scopedCareCycle.toEntity(
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
        memoDao.softDeleteAllCareCyclesForCouple(
            coupleId = syncSettings.identity().coupleId,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun pendingSyncCareCycles(): List<CareCycle> =
        memoDao.pendingSyncCareCyclesForCouple(syncSettings.identity().coupleId)
            .map(CareCycleEntity::toDomain)

    suspend fun saveRemoteCareCycle(careCycle: CareCycle): Long {
        val identity = syncSettings.identity()
        if (careCycle.coupleId != identity.coupleId) return 0L
        val now = System.currentTimeMillis()
        val entity = careCycle.toEntity(
            createdAt = careCycle.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = careCycle.updatedAt.takeIf { it > 0 } ?: now,
            syncStatus = SyncStatus.Synced
        )
        val local = careCycle.remoteId?.let { memoDao.findCareCycleByRemoteIdForCouple(it, identity.coupleId) }
            ?: memoDao.findCareCycleByStart(careCycle.startEpochDay, identity.coupleId)
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
        dueLabel = dueLabel,
        reminderAtMillis = reminderAtMillis,
        reminderEnabled = reminderEnabled,
        notificationChannelKey = notificationChannelKey,
        linkedScheduleId = linkedScheduleId,
        visibility = Visibility.fromStorageValue(visibility),
        sharedWithPartner = sharedWithPartner,
        isCompleted = isCompleted,
        showOnHome = showOnHome,
        homeSortOrder = homeSortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = SyncStatus.fromStorageValue(syncStatus),
        remoteUpdatedAt = remoteUpdatedAt
    )

private fun Memo.toEntity(
    createdAt: Long,
    updatedAt: Long,
    syncStatus: SyncStatus = this.syncStatus
): MemoEntity =
    MemoEntity(
        id = id,
        title = title,
        content = content,
        category = category.storageValue,
        checklistItems = checklistItems,
        imageUris = imageUris,
        dueLabel = dueLabel,
        reminderAtMillis = reminderAtMillis,
        reminderEnabled = reminderEnabled,
        notificationChannelKey = notificationChannelKey,
        linkedScheduleId = linkedScheduleId,
        visibility = visibility.storageValue,
        sharedWithPartner = sharedWithPartner,
        isCompleted = isCompleted,
        showOnHome = showOnHome,
        homeSortOrder = homeSortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = syncStatus.storageValue,
        remoteUpdatedAt = remoteUpdatedAt
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
