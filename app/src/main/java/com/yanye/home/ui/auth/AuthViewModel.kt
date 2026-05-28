package com.yanye.home.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yanye.home.data.session.CloudBaseAuthService
import com.yanye.home.data.session.InviteCodeResult
import com.yanye.home.data.session.SessionSettings
import com.yanye.home.data.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionSettings = SessionSettings(application)
    private val authService = CloudBaseAuthService(application)

    private val _session = MutableStateFlow(sessionSettings.loadSession())
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    private val _inviteCode = MutableStateFlow<InviteCodeResult?>(null)
    val inviteCode: StateFlow<InviteCodeResult?> = _inviteCode.asStateFlow()

    private val _isUpdatingProfile = MutableStateFlow(false)
    val isUpdatingProfile: StateFlow<Boolean> = _isUpdatingProfile.asStateFlow()

    fun register(username: String, password: String) {
        viewModelScope.launch {
            authService.register(username, password)
                .onSuccess(::persistSession)
                .onFailure { error -> _authMessage.value = error.message ?: "注册失败" }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            authService.login(username, password)
                .onSuccess(::persistSession)
                .onFailure { error -> _authMessage.value = error.message ?: "登录失败" }
        }
    }

    fun createSpace(spaceName: String, spaceCode: String) {
        viewModelScope.launch {
            authService.createSpace(spaceName, spaceCode)
                .onSuccess(::persistSession)
                .onFailure { error -> _authMessage.value = error.message ?: "创建空间失败" }
        }
    }

    fun joinSpaceByInvite(inviteCode: String) {
        viewModelScope.launch {
            authService.joinSpaceByInvite(inviteCode)
                .onSuccess(::persistSession)
                .onFailure { error -> _authMessage.value = error.message ?: "加入空间失败" }
        }
    }

    fun refreshProfile() {
        val currentSession = sessionSettings.loadSession() ?: return
        viewModelScope.launch {
            authService.refreshProfile()
                .onSuccess(::persistSession)
                .onFailure { error ->
                    _session.value = currentSession
                    _authMessage.value = error.message ?: "刷新资料失败"
                }
        }
    }

    fun createInviteCode() {
        viewModelScope.launch {
            authService.createInviteCode()
                .onSuccess {
                    _inviteCode.value = it
                    _authMessage.value = null
                }
                .onFailure { error ->
                    _authMessage.value = error.message ?: "生成邀请码失败"
                }
        }
    }

    fun updateProfile(
        username: String,
        password: String,
        avatarUrl: String?,
        spaceName: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isUpdatingProfile.value = true
            _authMessage.value = null
            authService.updateProfile(username, password, avatarUrl, spaceName)
                .onSuccess {
                    persistSession(it)
                    onSuccess()
                }
                .onFailure { error -> _authMessage.value = error.message ?: "保存个人信息失败" }
            _isUpdatingProfile.value = false
        }
    }

    fun logout() {
        sessionSettings.clearSession()
        _session.value = null
        _authMessage.value = null
        _inviteCode.value = null
    }

    fun clearMessage() {
        _authMessage.value = null
    }

    fun clearInviteCode() {
        _inviteCode.value = null
    }

    private fun persistSession(session: UserSession) {
        sessionSettings.saveSession(session)
        _session.value = session
        _authMessage.value = null
    }
}
