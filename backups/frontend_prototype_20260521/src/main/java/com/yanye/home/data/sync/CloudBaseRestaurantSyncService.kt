package com.yanye.home.data.sync

import android.content.Context
import com.yanye.home.data.repository.RestaurantRepository
import com.yanye.home.domain.model.Restaurant
import com.yanye.home.domain.model.RestaurantPriceLevel
import com.yanye.home.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CloudBaseRestaurantSyncService(
    context: Context,
    private val restaurantRepository: RestaurantRepository,
    private val syncSettings: SyncSettings = SyncSettings(context),
    private val httpClient: CloudBaseHttpClient = CloudBaseHttpClient()
) {
    suspend fun syncOnce(): Result<RestaurantSyncResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.SYNC_RESTAURANTS_URL.trim()
                if (endpoint.isBlank()) {
                    return@runCatching RestaurantSyncResult(
                        uploaded = 0,
                        downloaded = 0,
                        skippedReason = "CloudBase 餐厅池同步地址未配置"
                    )
                }

                val identity = syncSettings.identity()
                val pendingPayloads = restaurantRepository.pendingSyncRestaurants()
                    .map { restaurant ->
                        val remoteId = restaurant.remoteId ?: SyncRules.remoteId("restaurant", restaurant.id)
                        PendingRestaurantPayload(restaurant = restaurant, remoteId = remoteId)
                    }

                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("envId", CloudBaseConfig.ENV_ID)
                        .put("coupleId", identity.coupleId)
                        .put("userId", identity.localUserId)
                        .put("pendingRestaurants", JSONArray(pendingPayloads.map { it.restaurant.toJson(it.remoteId) }))
                )

                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "CloudBase 餐厅池同步失败"))
                }

                val remoteItems = response.optJSONArray("restaurants") ?: JSONArray()
                pendingPayloads.forEach { payload ->
                    val local = payload.restaurant
                    restaurantRepository.markSynced(
                        localId = local.id,
                        remoteId = payload.remoteId,
                        coupleId = identity.coupleId,
                        ownerUserId = SyncRules.ownerUserId(local.ownerUserId, identity),
                        remoteUpdatedAt = System.currentTimeMillis()
                    )
                }
                for (index in 0 until remoteItems.length()) {
                    val remote = remoteItems.getJSONObject(index).toRestaurant()
                    restaurantRepository.saveRemoteRestaurant(remote)
                }

                RestaurantSyncResult(
                    uploaded = pendingPayloads.size,
                    downloaded = remoteItems.length(),
                    skippedReason = null
                )
            }
        }
}

data class RestaurantSyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val skippedReason: String?
)

private data class PendingRestaurantPayload(
    val restaurant: Restaurant,
    val remoteId: String
)

private fun Restaurant.toJson(remoteId: String): JSONObject =
    JSONObject()
        .put("remoteId", remoteId)
        .put("name", name)
        .put("cuisine", cuisine)
        .put("tags", tags)
        .put("avoidNotes", avoidNotes)
        .put("priceLevel", priceLevel.storageValue)
        .put("address", address)
        .put("cityName", cityName)
        .put("canTakeout", canTakeout)
        .put("canDineIn", canDineIn)
        .put("isPitfall", isPitfall)
        .put("rating", rating)
        .put("note", note)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("isDeleted", isDeleted)
        .put("ownerUserId", ownerUserId)

private fun JSONObject.toRestaurant(): Restaurant {
    val remoteId = optString("remoteId").ifBlank { optString("_id") }
    val remoteUpdatedAt = optLong("remoteUpdatedAt", optLong("updatedAt", 0L))
    return Restaurant(
        remoteId = remoteId,
        coupleId = optString("coupleId").ifBlank { null },
        ownerUserId = optString("ownerUserId").ifBlank { null },
        name = optString("name"),
        cuisine = optString("cuisine"),
        tags = optString("tags"),
        avoidNotes = optString("avoidNotes"),
        priceLevel = RestaurantPriceLevel.fromStorageValue(optString("priceLevel")),
        address = optString("address"),
        cityName = optString("cityName"),
        canTakeout = optBoolean("canTakeout", true),
        canDineIn = optBoolean("canDineIn", true),
        isPitfall = optBoolean("isPitfall", false),
        rating = optInt("rating", 0),
        note = optString("note"),
        lastPickedAt = null,
        createdAt = optLong("createdAt", remoteUpdatedAt),
        updatedAt = optLong("updatedAt", remoteUpdatedAt),
        isDeleted = optBoolean("isDeleted", false),
        syncStatus = SyncStatus.Synced,
        remoteUpdatedAt = remoteUpdatedAt
    )
}
