package com.yanye.home.data.session

import android.content.Context
import com.yanye.home.data.sync.CloudBaseConfig
import com.yanye.home.data.sync.CloudBaseHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class InviteCodeResult(
    val inviteCode: String,
    val expiresAt: Long?
)

class CloudBaseAuthService(
    context: Context,
    private val httpClient: CloudBaseHttpClient = CloudBaseHttpClient()
) {
    private val appContext = context.applicationContext
    private val sessionSettings = SessionSettings(appContext)

    suspend fun register(username: String, password: String): Result<UserSession> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.REGISTER_USER_URL.trim()
                if (endpoint.isBlank()) error("CloudBase 注册地址未配置")
                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("username", username.trim())
                        .put("password", password)
                )
                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "注册失败"))
                }
                response.requireSession()
            }
        }

    suspend fun login(username: String, password: String): Result<UserSession> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.LOGIN_USER_URL.trim()
                if (endpoint.isBlank()) error("CloudBase 登录地址未配置")
                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("username", username.trim())
                        .put("password", password)
                )
                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "登录失败"))
                }
                response.requireSession()
            }
        }

    suspend fun createSpace(spaceName: String, spaceCode: String): Result<UserSession> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.CREATE_COUPLE_SPACE_URL.trim()
                if (endpoint.isBlank()) error("CloudBase 创建空间地址未配置")
                val session = requireLoggedInSession()
                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("userId", session.userId)
                        .put("spaceName", spaceName.trim())
                        .put("spaceCode", spaceCode.trim())
                )
                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "创建空间失败"))
                }
                response.requireSession()
            }
        }

    suspend fun joinSpaceByInvite(inviteCode: String): Result<UserSession> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.JOIN_COUPLE_SPACE_BY_INVITE_URL.trim()
                if (endpoint.isBlank()) error("CloudBase 绑定空间地址未配置")
                val session = requireLoggedInSession()
                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("userId", session.userId)
                        .put("inviteCode", inviteCode.trim())
                )
                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "绑定空间失败"))
                }
                response.requireSession()
            }
        }

    suspend fun createInviteCode(): Result<InviteCodeResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.CREATE_INVITE_CODE_URL.trim()
                if (endpoint.isBlank()) error("CloudBase 邀请码地址未配置")
                val session = requireLoggedInSession()
                val spaceId = session.currentSpaceId ?: error("当前还没有情侣空间")
                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("userId", session.userId)
                        .put("spaceId", spaceId)
                )
                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "生成邀请码失败"))
                }
                InviteCodeResult(
                    inviteCode = response.optString("inviteCode"),
                    expiresAt = if (response.isNull("expiresAt")) null else response.optLong("expiresAt")
                )
            }
        }

    suspend fun refreshProfile(): Result<UserSession> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.GET_CURRENT_SESSION_PROFILE_URL.trim()
                if (endpoint.isBlank()) error("CloudBase 用户资料地址未配置")
                val session = requireLoggedInSession()
                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("userId", session.userId)
                )
                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "获取资料失败"))
                }
                response.requireSession()
            }
        }

    suspend fun updateProfile(
        username: String,
        password: String,
        avatarUrl: String?,
        spaceName: String
    ): Result<UserSession> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.UPDATE_SESSION_PROFILE_URL.trim()
                if (endpoint.isBlank()) error("CloudBase 个人信息地址未配置")
                val session = requireLoggedInSession()
                val body = JSONObject()
                    .put("userId", session.userId)
                    .put("username", username.trim())
                    .put("spaceName", spaceName.trim())
                if (!avatarUrl.isNullOrBlank()) {
                    body.put("avatarUrl", avatarUrl.trim())
                }
                if (password.isNotBlank()) {
                    body.put("password", password)
                }
                val response = httpClient.postJson(endpoint = endpoint, body = body)
                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "保存个人信息失败"))
                }
                response.requireSession()
            }
        }

    private fun requireLoggedInSession(): UserSession =
        sessionSettings.loadSession() ?: error("当前未登录")
}

private fun JSONObject.requireSession(): UserSession =
    optJSONObject("session")?.toUserSession()
        ?: error("CloudBase 未返回有效会话")

private fun JSONObject.toUserSession(): UserSession =
    UserSession(
        userId = optString("userId"),
        username = optString("username"),
        avatarUrl = nullableString("avatarUrl"),
        currentSpaceId = nullableString("currentSpaceId"),
        spaceName = nullableString("spaceName"),
        spaceCode = nullableString("spaceCode"),
        spaceStatus = nullableString("spaceStatus"),
        partnerUsername = nullableString("partnerUsername"),
        partnerAvatarUrl = nullableString("partnerAvatarUrl"),
        isSpaceOwner = optBoolean("isSpaceOwner", false)
    )

private fun JSONObject.nullableString(key: String): String? =
    when {
        isNull(key) -> null
        else -> optString(key).takeUnless { it.isBlank() || it == "null" }
    }
