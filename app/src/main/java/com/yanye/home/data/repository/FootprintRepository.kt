package com.yanye.home.data.repository

import com.yanye.home.data.local.dao.FootprintDao
import com.yanye.home.data.local.entity.CityLightEntity
import com.yanye.home.data.local.entity.CityMemoryEntity
import com.yanye.home.data.local.entity.ProvinceLightEntity
import com.yanye.home.data.sync.SyncSettings
import com.yanye.home.domain.model.CityLight
import com.yanye.home.domain.model.CityMemory
import com.yanye.home.domain.model.ProvinceLight
import com.yanye.home.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FootprintRepository(
    private val footprintDao: FootprintDao,
    private val syncSettings: SyncSettings
) {
    fun observeProvinceLights(): Flow<List<ProvinceLight>> =
        footprintDao.observeProvinceLightsForCouple(syncSettings.identity().coupleId)
            .map { lights -> lights.map(ProvinceLightEntity::toDomain) }

    fun observeCityMemories(): Flow<List<CityMemory>> =
        footprintDao.observeCityMemoriesForCouple(syncSettings.identity().coupleId)
            .map { memories -> memories.map(CityMemoryEntity::toDomain) }

    fun observeCityLights(): Flow<List<CityLight>> =
        footprintDao.observeCityLightsForCouple(syncSettings.identity().coupleId)
            .map { lights -> lights.map(CityLightEntity::toDomain) }

    suspend fun saveProvinceLight(provinceLight: ProvinceLight): Long {
        val now = System.currentTimeMillis()
        val syncStatus = if (provinceLight.remoteId.isNullOrBlank()) {
            SyncStatus.PendingCreate
        } else {
            SyncStatus.PendingUpdate
        }
        val identity = syncSettings.identity()
        val scopedProvinceLight = provinceLight.copy(
            coupleId = provinceLight.coupleId ?: identity.coupleId,
            ownerUserId = provinceLight.ownerUserId ?: identity.localUserId
        )
        val entity = scopedProvinceLight.toEntity(
            createdAt = provinceLight.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now,
            syncStatus = syncStatus
        )

        return if (provinceLight.id == 0L) {
            footprintDao.insertProvinceLight(entity)
        } else {
            footprintDao.updateProvinceLight(entity)
            provinceLight.id
        }
    }

    suspend fun saveCityMemory(cityMemory: CityMemory): Long {
        val now = System.currentTimeMillis()
        val syncStatus = if (cityMemory.remoteId.isNullOrBlank()) {
            SyncStatus.PendingCreate
        } else {
            SyncStatus.PendingUpdate
        }
        val identity = syncSettings.identity()
        val scopedCityMemory = cityMemory.copy(
            coupleId = cityMemory.coupleId ?: identity.coupleId,
            ownerUserId = cityMemory.ownerUserId ?: identity.localUserId
        )
        val entity = scopedCityMemory.toEntity(
            createdAt = cityMemory.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now,
            syncStatus = syncStatus
        )

        return if (cityMemory.id == 0L) {
            footprintDao.insertCityMemory(entity)
        } else {
            footprintDao.updateCityMemory(entity)
            cityMemory.id
        }
    }

    suspend fun saveCityLight(cityLight: CityLight): Long {
        val now = System.currentTimeMillis()
        val syncStatus = if (cityLight.remoteId.isNullOrBlank()) {
            SyncStatus.PendingCreate
        } else {
            SyncStatus.PendingUpdate
        }
        val identity = syncSettings.identity()
        val scopedCityLight = cityLight.copy(
            coupleId = cityLight.coupleId ?: identity.coupleId,
            ownerUserId = cityLight.ownerUserId ?: identity.localUserId
        )
        val entity = scopedCityLight.toEntity(
            createdAt = cityLight.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now,
            syncStatus = syncStatus
        )

        return if (cityLight.id == 0L) {
            footprintDao.insertCityLight(entity)
        } else {
            footprintDao.updateCityLight(entity)
            cityLight.id
        }
    }

    suspend fun deleteProvinceLight(id: Long) {
        footprintDao.softDeleteProvinceLight(
            id = id,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteCityMemory(id: Long) {
        footprintDao.softDeleteCityMemory(
            id = id,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteCityLight(id: Long) {
        footprintDao.softDeleteCityLight(
            id = id,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun pendingSyncProvinceLights(): List<ProvinceLight> =
        footprintDao.pendingSyncProvinceLightsForCouple(syncSettings.identity().coupleId)
            .map(ProvinceLightEntity::toDomain)

    suspend fun pendingSyncCityLights(): List<CityLight> =
        footprintDao.pendingSyncCityLightsForCouple(syncSettings.identity().coupleId)
            .map(CityLightEntity::toDomain)

    suspend fun pendingSyncCityMemories(): List<CityMemory> =
        footprintDao.pendingSyncCityMemoriesForCouple(syncSettings.identity().coupleId)
            .map(CityMemoryEntity::toDomain)

    suspend fun saveRemoteProvinceLight(provinceLight: ProvinceLight): Long {
        val identity = syncSettings.identity()
        if (provinceLight.coupleId != identity.coupleId) return 0L
        val now = System.currentTimeMillis()
        val entity = provinceLight.toEntity(
            createdAt = provinceLight.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = provinceLight.updatedAt.takeIf { it > 0 } ?: now,
            syncStatus = SyncStatus.Synced
        )
        val local = provinceLight.remoteId
            ?.let { footprintDao.findProvinceLightByRemoteIdForCouple(it, identity.coupleId) }
            ?: footprintDao.findProvinceLightByName(provinceLight.provinceName, identity.coupleId)
        if (local == null && provinceLight.isDeleted) {
            return 0L
        }

        return if (local == null) {
            footprintDao.insertProvinceLight(entity)
        } else {
            footprintDao.updateProvinceLight(entity.copy(id = local.id))
            local.id
        }
    }

    suspend fun saveRemoteCityLight(cityLight: CityLight): Long {
        val identity = syncSettings.identity()
        if (cityLight.coupleId != identity.coupleId) return 0L
        val now = System.currentTimeMillis()
        val entity = cityLight.toEntity(
            createdAt = cityLight.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = cityLight.updatedAt.takeIf { it > 0 } ?: now,
            syncStatus = SyncStatus.Synced
        )
        val local = cityLight.remoteId
            ?.let { footprintDao.findCityLightByRemoteIdForCouple(it, identity.coupleId) }
            ?: footprintDao.findCityLightByName(cityLight.provinceName, cityLight.cityName, identity.coupleId)
        if (local == null && cityLight.isDeleted) {
            return 0L
        }

        return if (local == null) {
            footprintDao.insertCityLight(entity)
        } else {
            footprintDao.updateCityLight(entity.copy(id = local.id))
            local.id
        }
    }

    suspend fun saveRemoteCityMemory(cityMemory: CityMemory): Long {
        val identity = syncSettings.identity()
        if (cityMemory.coupleId != identity.coupleId) return 0L
        val now = System.currentTimeMillis()
        val entity = cityMemory.toEntity(
            createdAt = cityMemory.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = cityMemory.updatedAt.takeIf { it > 0 } ?: now,
            syncStatus = SyncStatus.Synced
        )
        val local = cityMemory.remoteId
            ?.let { footprintDao.findCityMemoryByRemoteIdForCouple(it, identity.coupleId) }
            ?: if (cityMemory.createdAt == 0L && cityMemory.updatedAt == 0L) {
                footprintDao.findCityMemoryByCity(cityMemory.provinceName, cityMemory.cityName, identity.coupleId)
            } else {
                null
            }
        if (local == null && cityMemory.isDeleted) {
            return 0L
        }

        return if (local == null) {
            footprintDao.insertCityMemory(entity)
        } else {
            footprintDao.updateCityMemory(entity.copy(id = local.id))
            local.id
        }
    }

    suspend fun markProvinceLightSynced(
        localId: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long
    ) {
        footprintDao.markProvinceLightSynced(
            id = localId,
            remoteId = remoteId,
            coupleId = coupleId,
            ownerUserId = ownerUserId,
            remoteUpdatedAt = remoteUpdatedAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun markCityLightSynced(
        localId: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long
    ) {
        footprintDao.markCityLightSynced(
            id = localId,
            remoteId = remoteId,
            coupleId = coupleId,
            ownerUserId = ownerUserId,
            remoteUpdatedAt = remoteUpdatedAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun markCityMemorySynced(
        localId: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long
    ) {
        footprintDao.markCityMemorySynced(
            id = localId,
            remoteId = remoteId,
            coupleId = coupleId,
            ownerUserId = ownerUserId,
            remoteUpdatedAt = remoteUpdatedAt,
            updatedAt = System.currentTimeMillis()
        )
    }
}

private fun ProvinceLightEntity.toDomain(): ProvinceLight =
    ProvinceLight(
        id = id,
        provinceName = provinceName,
        isLit = isLit,
        fillColorArgb = fillColorArgb,
        note = note,
        linkedScheduleId = linkedScheduleId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = SyncStatus.fromStorageValue(syncStatus),
        remoteUpdatedAt = remoteUpdatedAt
    )

private fun ProvinceLight.toEntity(
    createdAt: Long,
    updatedAt: Long,
    syncStatus: SyncStatus = this.syncStatus
): ProvinceLightEntity =
    ProvinceLightEntity(
        id = id,
        provinceName = provinceName,
        isLit = isLit,
        fillColorArgb = fillColorArgb,
        note = note,
        linkedScheduleId = linkedScheduleId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = syncStatus.storageValue,
        remoteUpdatedAt = remoteUpdatedAt
    )

private fun CityMemoryEntity.toDomain(): CityMemory =
    CityMemory(
        id = id,
        provinceName = provinceName,
        cityName = cityName,
        title = title,
        memoryType = memoryType,
        dateEpochDay = dateEpochDay,
        coverImageUri = coverImageUri,
        summary = summary,
        locationName = locationName,
        priceText = priceText,
        sortOrder = sortOrder,
        foods = foods,
        places = places,
        photoUris = photoUris,
        linkedScheduleId = linkedScheduleId,
        insideJoke = insideJoke,
        expenseCents = expenseCents,
        pitfallNotes = pitfallNotes,
        rating = rating,
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

private fun CityMemory.toEntity(
    createdAt: Long,
    updatedAt: Long,
    syncStatus: SyncStatus = this.syncStatus
): CityMemoryEntity =
    CityMemoryEntity(
        id = id,
        provinceName = provinceName,
        cityName = cityName,
        title = title,
        memoryType = memoryType,
        dateEpochDay = dateEpochDay,
        coverImageUri = coverImageUri,
        summary = summary,
        locationName = locationName,
        priceText = priceText,
        sortOrder = sortOrder,
        foods = foods,
        places = places,
        photoUris = photoUris,
        linkedScheduleId = linkedScheduleId,
        insideJoke = insideJoke,
        expenseCents = expenseCents,
        pitfallNotes = pitfallNotes,
        rating = rating,
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

private fun CityLightEntity.toDomain(): CityLight =
    CityLight(
        id = id,
        provinceName = provinceName,
        cityName = cityName,
        isLit = isLit,
        fillColorArgb = fillColorArgb,
        note = note,
        linkedScheduleId = linkedScheduleId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = SyncStatus.fromStorageValue(syncStatus),
        remoteUpdatedAt = remoteUpdatedAt
    )

private fun CityLight.toEntity(
    createdAt: Long,
    updatedAt: Long,
    syncStatus: SyncStatus = this.syncStatus
): CityLightEntity =
    CityLightEntity(
        id = id,
        provinceName = provinceName,
        cityName = cityName,
        isLit = isLit,
        fillColorArgb = fillColorArgb,
        note = note,
        linkedScheduleId = linkedScheduleId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = syncStatus.storageValue,
        remoteUpdatedAt = remoteUpdatedAt
    )
