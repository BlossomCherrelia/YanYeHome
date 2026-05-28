package com.yanye.home.data.sync

import android.content.Context
import com.yanye.home.data.repository.MemoRepository
import com.yanye.home.domain.model.CareCycle
import com.yanye.home.domain.model.CareMood
import com.yanye.home.domain.model.CarePainLevel
import com.yanye.home.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CloudBaseCareCycleSyncService(
    context: Context,
    private val memoRepository: MemoRepository,
    private val syncSettings: SyncSettings = SyncSettings(context),
    private val httpClient: CloudBaseHttpClient = CloudBaseHttpClient()
) {
    suspend fun syncOnce(): Result<CareCycleSyncResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.SYNC_CARE_CYCLES_URL.trim()
                if (endpoint.isBlank()) {
                    return@runCatching CareCycleSyncResult(
                        uploaded = 0,
                        downloaded = 0,
                        skippedReason = "CloudBase 经期同步地址未配置"
                    )
                }

                val identity = syncSettings.identity()
                val pendingPayloads = memoRepository.pendingSyncCareCycles()
                    .map { cycle ->
                        PendingCareCyclePayload(
                            cycle = cycle,
                            remoteId = cycle.remoteId
                                ?: SyncRules.remoteIdFromParts("care_cycle", cycle.startEpochDay.toString())
                        )
                    }

                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("envId", CloudBaseConfig.ENV_ID)
                        .put("coupleId", identity.coupleId)
                        .put("userId", identity.localUserId)
                        .put("careCycles", JSONArray(pendingPayloads.map { it.cycle.toJson(it.remoteId) }))
                )

                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "CloudBase 经期同步失败"))
                }

                val remoteItems = response.optJSONArray("careCycles") ?: JSONArray()
                val syncedAt = System.currentTimeMillis()
                pendingPayloads.forEach { payload ->
                    val local = payload.cycle
                    memoRepository.markCareCycleSynced(
                        localId = local.id,
                        remoteId = payload.remoteId,
                        coupleId = identity.coupleId,
                        ownerUserId = SyncRules.ownerUserId(local.ownerUserId, identity),
                        remoteUpdatedAt = syncedAt
                    )
                }
                for (index in 0 until remoteItems.length()) {
                    memoRepository.saveRemoteCareCycle(remoteItems.getJSONObject(index).toCareCycle())
                }

                CareCycleSyncResult(
                    uploaded = pendingPayloads.size,
                    downloaded = remoteItems.length(),
                    skippedReason = null
                )
            }
        }
}

data class CareCycleSyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val skippedReason: String?
)

private data class PendingCareCyclePayload(
    val cycle: CareCycle,
    val remoteId: String
)

private fun CareCycle.toJson(remoteId: String): JSONObject =
    JSONObject()
        .put("remoteId", remoteId)
        .put("startEpochDay", startEpochDay)
        .put("endEpochDay", endEpochDay ?: JSONObject.NULL)
        .put("cycleLengthDays", cycleLengthDays)
        .put("painLevel", painLevel.storageValue)
        .put("mood", mood.storageValue)
        .put("avoidNotes", avoidNotes)
        .put("carePreference", carePreference)
        .put("shareReminderWithPartner", shareReminderWithPartner)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("isDeleted", isDeleted)
        .put("ownerUserId", ownerUserId)

private fun JSONObject.toCareCycle(): CareCycle {
    val remoteId = optString("remoteId").ifBlank { optString("_id") }
    val remoteUpdatedAt = optLong("remoteUpdatedAt", optLong("updatedAt", 0L))
    return CareCycle(
        remoteId = remoteId,
        coupleId = optString("coupleId").ifBlank { null },
        ownerUserId = optString("ownerUserId").ifBlank { null },
        startEpochDay = optLong("startEpochDay"),
        endEpochDay = if (isNull("endEpochDay")) null else optLong("endEpochDay"),
        cycleLengthDays = optInt("cycleLengthDays", 28),
        painLevel = CarePainLevel.fromStorageValue(optString("painLevel")),
        mood = CareMood.fromStorageValue(optString("mood")),
        avoidNotes = optString("avoidNotes"),
        carePreference = optString("carePreference"),
        shareReminderWithPartner = optBoolean("shareReminderWithPartner", false),
        createdAt = optLong("createdAt", remoteUpdatedAt),
        updatedAt = optLong("updatedAt", remoteUpdatedAt),
        isDeleted = optBoolean("isDeleted", false),
        syncStatus = SyncStatus.Synced,
        remoteUpdatedAt = remoteUpdatedAt
    )
}
