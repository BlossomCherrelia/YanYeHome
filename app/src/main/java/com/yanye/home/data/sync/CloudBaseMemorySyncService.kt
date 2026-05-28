package com.yanye.home.data.sync

import android.content.Context
import com.yanye.home.data.repository.ScheduleRepository
import com.yanye.home.domain.model.Memory
import com.yanye.home.domain.model.MemoryMood
import com.yanye.home.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CloudBaseMemorySyncService(
    context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val syncSettings: SyncSettings = SyncSettings(context),
    private val httpClient: CloudBaseHttpClient = CloudBaseHttpClient()
) {
    suspend fun syncOnce(): Result<MemorySyncResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.SYNC_MEMORIES_URL.trim()
                if (endpoint.isBlank()) {
                    return@runCatching MemorySyncResult(
                        uploaded = 0,
                        downloaded = 0,
                        skippedReason = "CloudBase 回忆同步地址未配置"
                    )
                }

                val identity = syncSettings.identity()
                val pendingPayloads = scheduleRepository.pendingSyncMemories()
                    .map { memory ->
                        val remoteId = memory.remoteId ?: SyncRules.remoteId("memory", identity, memory.id)
                        PendingMemoryPayload(memory = memory, remoteId = remoteId)
                    }

                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("envId", CloudBaseConfig.ENV_ID)
                        .put("coupleId", identity.coupleId)
                        .put("userId", identity.localUserId)
                        .put("pendingMemories", JSONArray(pendingPayloads.map { it.memory.toJson(it.remoteId) }))
                )

                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "CloudBase 回忆同步失败"))
                }

                val remoteItems = response.optJSONArray("memories") ?: JSONArray()
                pendingPayloads.forEach { payload ->
                    val local = payload.memory
                    scheduleRepository.markMemorySynced(
                        localId = local.id,
                        remoteId = payload.remoteId,
                        coupleId = identity.coupleId,
                        ownerUserId = SyncRules.ownerUserId(local.ownerUserId, identity),
                        remoteUpdatedAt = System.currentTimeMillis()
                    )
                }
                for (index in 0 until remoteItems.length()) {
                    scheduleRepository.saveRemoteMemory(remoteItems.getJSONObject(index).toMemory())
                }

                MemorySyncResult(
                    uploaded = pendingPayloads.size,
                    downloaded = remoteItems.length(),
                    skippedReason = null
                )
            }
        }
}

data class MemorySyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val skippedReason: String?
)

private data class PendingMemoryPayload(
    val memory: Memory,
    val remoteId: String
)

private fun Memory.toJson(remoteId: String): JSONObject =
    JSONObject()
        .put("remoteId", remoteId)
        .put("title", title)
        .put("dateEpochDay", dateEpochDay)
        .put("locationName", locationName)
        .put("photoUris", photoUris)
        .put("foodNotes", foodNotes)
        .put("expenseCents", expenseCents)
        .put("mood", mood.storageValue)
        .put("note", note)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("isDeleted", isDeleted)
        .put("ownerUserId", ownerUserId)

private fun JSONObject.toMemory(): Memory {
    val remoteId = optString("remoteId").ifBlank { optString("_id") }
    val remoteUpdatedAt = optLong("remoteUpdatedAt", optLong("updatedAt", 0L))
    return Memory(
        remoteId = remoteId,
        coupleId = optString("coupleId").ifBlank { null },
        ownerUserId = optString("ownerUserId").ifBlank { null },
        title = optString("title"),
        dateEpochDay = optLong("dateEpochDay"),
        scheduleId = null,
        linkedWishId = null,
        locationName = optString("locationName"),
        photoUris = optString("photoUris"),
        foodNotes = optString("foodNotes"),
        expenseCents = if (isNull("expenseCents")) null else optLong("expenseCents"),
        mood = MemoryMood.fromStorageValue(optString("mood")),
        note = optString("note"),
        createdAt = optLong("createdAt", remoteUpdatedAt),
        updatedAt = optLong("updatedAt", remoteUpdatedAt),
        isDeleted = optBoolean("isDeleted", false),
        syncStatus = SyncStatus.Synced,
        remoteUpdatedAt = remoteUpdatedAt
    )
}
