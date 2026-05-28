package com.yanye.home.data.sync

import android.content.Context
import com.yanye.home.data.repository.FootprintRepository
import com.yanye.home.domain.model.CityLight
import com.yanye.home.domain.model.CityMemory
import com.yanye.home.domain.model.ProvinceLight
import com.yanye.home.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CloudBaseFootprintSyncService(
    context: Context,
    private val footprintRepository: FootprintRepository,
    private val syncSettings: SyncSettings = SyncSettings(context),
    private val httpClient: CloudBaseHttpClient = CloudBaseHttpClient()
) {
    suspend fun syncOnce(): Result<FootprintSyncResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.SYNC_FOOTPRINTS_URL.trim()
                if (endpoint.isBlank()) {
                    return@runCatching FootprintSyncResult(
                        uploaded = 0,
                        downloaded = 0,
                        skippedReason = "CloudBase 地图同步地址未配置"
                    )
                }

                val identity = syncSettings.identity()
                val pendingProvinceLights = footprintRepository.pendingSyncProvinceLights()
                    .map { provinceLight ->
                        PendingProvinceLightPayload(
                            provinceLight = provinceLight,
                            remoteId = provinceLight.remoteId
                                ?: SyncRules.remoteIdFromParts(
                                    "province_light",
                                    identity.coupleId,
                                    identity.localUserId,
                                    provinceLight.provinceName
                                )
                        )
                    }
                val pendingCityLights = footprintRepository.pendingSyncCityLights()
                    .map { cityLight ->
                        PendingCityLightPayload(
                            cityLight = cityLight,
                            remoteId = cityLight.remoteId
                                ?: SyncRules.remoteIdFromParts(
                                    "city_light",
                                    identity.coupleId,
                                    identity.localUserId,
                                    cityLight.provinceName,
                                    cityLight.cityName
                                )
                        )
                    }
                val pendingCityMemories = footprintRepository.pendingSyncCityMemories()
                    .map { cityMemory ->
                        PendingCityMemoryPayload(
                            cityMemory = cityMemory,
                            remoteId = cityMemory.remoteId
                                ?: SyncRules.remoteIdFromParts(
                                    "city_memory",
                                    identity.coupleId,
                                    identity.localUserId,
                                    cityMemory.provinceName,
                                    cityMemory.cityName,
                                    cityMemory.id.toString()
                                )
                        )
                    }

                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("envId", CloudBaseConfig.ENV_ID)
                        .put("coupleId", identity.coupleId)
                        .put("userId", identity.localUserId)
                        .put(
                            "provinceLights",
                            JSONArray(pendingProvinceLights.map { it.provinceLight.toJson(it.remoteId) })
                        )
                        .put(
                            "cityLights",
                            JSONArray(pendingCityLights.map { it.cityLight.toJson(it.remoteId) })
                        )
                        .put(
                            "cityMemories",
                            JSONArray(pendingCityMemories.map { it.cityMemory.toJson(it.remoteId) })
                        )
                )

                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "CloudBase 地图同步失败"))
                }

                val remoteProvinceLights = response.optJSONArray("provinceLights") ?: JSONArray()
                val remoteCityLights = response.optJSONArray("cityLights") ?: JSONArray()
                val remoteCityMemories = response.optJSONArray("cityMemories") ?: JSONArray()
                val syncedAt = System.currentTimeMillis()

                pendingProvinceLights.forEach { payload ->
                    val local = payload.provinceLight
                    footprintRepository.markProvinceLightSynced(
                        localId = local.id,
                        remoteId = payload.remoteId,
                        coupleId = identity.coupleId,
                        ownerUserId = SyncRules.ownerUserId(local.ownerUserId, identity),
                        remoteUpdatedAt = syncedAt
                    )
                }
                pendingCityLights.forEach { payload ->
                    val local = payload.cityLight
                    footprintRepository.markCityLightSynced(
                        localId = local.id,
                        remoteId = payload.remoteId,
                        coupleId = identity.coupleId,
                        ownerUserId = SyncRules.ownerUserId(local.ownerUserId, identity),
                        remoteUpdatedAt = syncedAt
                    )
                }
                pendingCityMemories.forEach { payload ->
                    val local = payload.cityMemory
                    footprintRepository.markCityMemorySynced(
                        localId = local.id,
                        remoteId = payload.remoteId,
                        coupleId = identity.coupleId,
                        ownerUserId = SyncRules.ownerUserId(local.ownerUserId, identity),
                        remoteUpdatedAt = syncedAt
                    )
                }

                for (index in 0 until remoteProvinceLights.length()) {
                    footprintRepository.saveRemoteProvinceLight(
                        remoteProvinceLights.getJSONObject(index).toProvinceLight()
                    )
                }
                for (index in 0 until remoteCityLights.length()) {
                    footprintRepository.saveRemoteCityLight(
                        remoteCityLights.getJSONObject(index).toCityLight()
                    )
                }
                for (index in 0 until remoteCityMemories.length()) {
                    footprintRepository.saveRemoteCityMemory(
                        remoteCityMemories.getJSONObject(index).toCityMemory()
                    )
                }

                FootprintSyncResult(
                    uploaded = pendingProvinceLights.size + pendingCityLights.size + pendingCityMemories.size,
                    downloaded = remoteProvinceLights.length() + remoteCityLights.length() + remoteCityMemories.length(),
                    skippedReason = null
                )
            }
        }
}

data class FootprintSyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val skippedReason: String?
)

private data class PendingProvinceLightPayload(
    val provinceLight: ProvinceLight,
    val remoteId: String
)

private data class PendingCityLightPayload(
    val cityLight: CityLight,
    val remoteId: String
)

private data class PendingCityMemoryPayload(
    val cityMemory: CityMemory,
    val remoteId: String
)

private fun ProvinceLight.toJson(remoteId: String): JSONObject =
    JSONObject()
        .put("remoteId", remoteId)
        .put("provinceName", provinceName)
        .put("isLit", isLit)
        .put("fillColorArgb", fillColorArgb)
        .put("note", note)
        .put("linkedScheduleId", linkedScheduleId ?: JSONObject.NULL)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("isDeleted", isDeleted)
        .put("ownerUserId", ownerUserId)

private fun CityLight.toJson(remoteId: String): JSONObject =
    JSONObject()
        .put("remoteId", remoteId)
        .put("provinceName", provinceName)
        .put("cityName", cityName)
        .put("isLit", isLit)
        .put("fillColorArgb", fillColorArgb)
        .put("note", note)
        .put("linkedScheduleId", linkedScheduleId ?: JSONObject.NULL)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("isDeleted", isDeleted)
        .put("ownerUserId", ownerUserId)

private fun CityMemory.toJson(remoteId: String): JSONObject =
    JSONObject()
        .put("remoteId", remoteId)
        .put("provinceName", provinceName)
        .put("cityName", cityName)
        .put("title", title)
        .put("memoryType", memoryType)
        .put("dateEpochDay", dateEpochDay)
        .put("coverImageUri", coverImageUri)
        .put("summary", summary)
        .put("locationName", locationName)
        .put("priceText", priceText)
        .put("sortOrder", sortOrder)
        .put("foods", foods)
        .put("places", places)
        .put("photoUris", photoUris)
        .put("linkedScheduleId", linkedScheduleId ?: JSONObject.NULL)
        .put("insideJoke", insideJoke)
        .put("expenseCents", expenseCents ?: JSONObject.NULL)
        .put("pitfallNotes", pitfallNotes)
        .put("rating", rating)
        .put("note", note)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("isDeleted", isDeleted)
        .put("ownerUserId", ownerUserId)

private fun JSONObject.toProvinceLight(): ProvinceLight {
    val remoteId = optString("remoteId").ifBlank { optString("_id") }
    val remoteUpdatedAt = optLong("remoteUpdatedAt", optLong("updatedAt", 0L))
    return ProvinceLight(
        remoteId = remoteId,
        coupleId = optString("coupleId").ifBlank { null },
        ownerUserId = optString("ownerUserId").ifBlank { null },
        provinceName = optString("provinceName"),
        isLit = optBoolean("isLit", true),
        fillColorArgb = optInt("fillColorArgb", -34150),
        note = optString("note"),
        linkedScheduleId = if (isNull("linkedScheduleId")) null else optLong("linkedScheduleId"),
        createdAt = optLong("createdAt", remoteUpdatedAt),
        updatedAt = optLong("updatedAt", remoteUpdatedAt),
        isDeleted = optBoolean("isDeleted", false),
        syncStatus = SyncStatus.Synced,
        remoteUpdatedAt = remoteUpdatedAt
    )
}

private fun JSONObject.toCityLight(): CityLight {
    val remoteId = optString("remoteId").ifBlank { optString("_id") }
    val remoteUpdatedAt = optLong("remoteUpdatedAt", optLong("updatedAt", 0L))
    return CityLight(
        remoteId = remoteId,
        coupleId = optString("coupleId").ifBlank { null },
        ownerUserId = optString("ownerUserId").ifBlank { null },
        provinceName = optString("provinceName"),
        cityName = optString("cityName"),
        isLit = optBoolean("isLit", true),
        fillColorArgb = optInt("fillColorArgb", -34150),
        note = optString("note"),
        linkedScheduleId = if (isNull("linkedScheduleId")) null else optLong("linkedScheduleId"),
        createdAt = optLong("createdAt", remoteUpdatedAt),
        updatedAt = optLong("updatedAt", remoteUpdatedAt),
        isDeleted = optBoolean("isDeleted", false),
        syncStatus = SyncStatus.Synced,
        remoteUpdatedAt = remoteUpdatedAt
    )
}

private fun JSONObject.toCityMemory(): CityMemory {
    val remoteId = optString("remoteId").ifBlank { optString("_id") }
    val remoteUpdatedAt = optLong("remoteUpdatedAt", optLong("updatedAt", 0L))
    return CityMemory(
        remoteId = remoteId,
        coupleId = optString("coupleId").ifBlank { null },
        ownerUserId = optString("ownerUserId").ifBlank { null },
        provinceName = optString("provinceName"),
        cityName = optString("cityName"),
        title = optString("title"),
        memoryType = optString("memoryType").ifBlank { "MOMENT" },
        dateEpochDay = optLong("dateEpochDay"),
        coverImageUri = optString("coverImageUri"),
        summary = optString("summary"),
        locationName = optString("locationName"),
        priceText = optString("priceText"),
        sortOrder = optInt("sortOrder", 0),
        foods = optString("foods"),
        places = optString("places"),
        photoUris = optString("photoUris"),
        linkedScheduleId = if (isNull("linkedScheduleId")) null else optLong("linkedScheduleId"),
        insideJoke = optString("insideJoke"),
        expenseCents = if (isNull("expenseCents")) null else optInt("expenseCents"),
        pitfallNotes = optString("pitfallNotes"),
        rating = optInt("rating", 0),
        note = optString("note"),
        createdAt = optLong("createdAt", remoteUpdatedAt),
        updatedAt = optLong("updatedAt", remoteUpdatedAt),
        isDeleted = optBoolean("isDeleted", false),
        syncStatus = SyncStatus.Synced,
        remoteUpdatedAt = remoteUpdatedAt
    )
}
