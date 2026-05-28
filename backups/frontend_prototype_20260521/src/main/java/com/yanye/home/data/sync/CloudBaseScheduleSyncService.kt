package com.yanye.home.data.sync

import android.content.Context
import com.yanye.home.data.repository.ScheduleRepository
import com.yanye.home.domain.model.Schedule
import com.yanye.home.domain.model.SyncStatus
import com.yanye.home.domain.model.Visibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CloudBaseScheduleSyncService(
    context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val syncSettings: SyncSettings = SyncSettings(context),
    private val httpClient: CloudBaseHttpClient = CloudBaseHttpClient()
) {
    suspend fun syncOnce(): Result<ScheduleSyncResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.SYNC_SCHEDULES_URL.trim()
                if (endpoint.isBlank()) {
                    return@runCatching ScheduleSyncResult(
                        uploaded = 0,
                        downloaded = 0,
                        skippedReason = "CloudBase 日程同步地址未配置"
                    )
                }

                val identity = syncSettings.identity()
                val pendingPayloads = scheduleRepository.pendingSyncSchedules()
                    .filter { schedule ->
                        SyncRules.shouldSyncSharedVisibility(schedule.visibility) ||
                            (schedule.syncStatus == SyncStatus.PendingDelete && schedule.remoteId != null)
                    }
                    .map { schedule ->
                        val remoteId = schedule.remoteId ?: SyncRules.remoteId("schedule", schedule.id)
                        PendingSchedulePayload(schedule = schedule, remoteId = remoteId)
                    }

                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("envId", CloudBaseConfig.ENV_ID)
                        .put("coupleId", identity.coupleId)
                        .put("userId", identity.localUserId)
                        .put("pendingSchedules", JSONArray(pendingPayloads.map { it.schedule.toJson(it.remoteId) }))
                )

                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "CloudBase 日程同步失败"))
                }

                val remoteItems = response.optJSONArray("schedules") ?: JSONArray()
                pendingPayloads.forEach { payload ->
                    val local = payload.schedule
                    scheduleRepository.markSynced(
                        localId = local.id,
                        remoteId = payload.remoteId,
                        coupleId = identity.coupleId,
                        ownerUserId = SyncRules.ownerUserId(local.ownerUserId, identity),
                        remoteUpdatedAt = System.currentTimeMillis()
                    )
                }
                for (index in 0 until remoteItems.length()) {
                    val remote = remoteItems.getJSONObject(index).toSchedule()
                    scheduleRepository.saveRemoteSchedule(remote)
                }

                ScheduleSyncResult(
                    uploaded = pendingPayloads.size,
                    downloaded = remoteItems.length(),
                    skippedReason = null
                )
            }
        }
}

data class ScheduleSyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val skippedReason: String?
)

private data class PendingSchedulePayload(
    val schedule: Schedule,
    val remoteId: String
)

private fun Schedule.toJson(remoteId: String): JSONObject =
    JSONObject()
        .put("remoteId", remoteId)
        .put("title", title)
        .put("startEpochDay", startEpochDay)
        .put("startMinuteOfDay", startMinuteOfDay)
        .put("endMinuteOfDay", endMinuteOfDay)
        .put("locationName", locationName)
        .put("reminderMinutesBefore", reminderMinutesBefore)
        .put("budgetCents", budgetCents)
        .put("participants", participants)
        .put("linkedWishRemoteId", linkedWishRemoteId)
        .put("isGuideMode", isGuideMode)
        .put("guideRestaurants", guideRestaurants)
        .put("guideActivities", guideActivities)
        .put("guideRoute", guideRoute)
        .put("backupPlan", backupPlan)
        .put("note", note)
        .put("isCompleted", isCompleted)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("visibility", visibility.storageValue)
        .put("sharedWithPartner", sharedWithPartner)
        .put("isDeleted", isDeleted)
        .put("ownerUserId", ownerUserId)

private fun JSONObject.toSchedule(): Schedule {
    val remoteId = optString("remoteId").ifBlank { optString("_id") }
    val remoteUpdatedAt = optLong("remoteUpdatedAt", optLong("updatedAt", 0L))
    return Schedule(
        remoteId = remoteId,
        coupleId = optString("coupleId").ifBlank { null },
        ownerUserId = optString("ownerUserId").ifBlank { null },
        title = optString("title"),
        startEpochDay = optLong("startEpochDay"),
        startMinuteOfDay = optInt("startMinuteOfDay", 19 * 60),
        endMinuteOfDay = if (isNull("endMinuteOfDay")) null else optInt("endMinuteOfDay"),
        locationName = optString("locationName"),
        reminderMinutesBefore = optInt("reminderMinutesBefore", 60),
        budgetCents = if (isNull("budgetCents")) null else optLong("budgetCents"),
        participants = optString("participants").ifBlank { "我们俩" },
        linkedWishRemoteId = optString("linkedWishRemoteId").ifBlank { null },
        isGuideMode = optBoolean("isGuideMode", false),
        guideRestaurants = optString("guideRestaurants"),
        guideActivities = optString("guideActivities"),
        guideRoute = optString("guideRoute"),
        backupPlan = optString("backupPlan"),
        note = optString("note"),
        isCompleted = optBoolean("isCompleted", false),
        memoryId = null,
        createdAt = optLong("createdAt", remoteUpdatedAt),
        updatedAt = optLong("updatedAt", remoteUpdatedAt),
        visibility = Visibility.fromStorageValue(optString("visibility")),
        sharedWithPartner = optBoolean("sharedWithPartner", true),
        isDeleted = optBoolean("isDeleted", false),
        syncStatus = SyncStatus.Synced,
        remoteUpdatedAt = remoteUpdatedAt
    )
}
