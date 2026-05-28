package com.yanye.home.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.ui.theme.YanYeColors

private enum class AuthPage {
    Welcome,
    Login,
    Register
}

@Composable
fun AuthEntryScreen(
    viewModel: AuthViewModel = viewModel()
) {
    val authMessage by viewModel.authMessage.collectAsState()
    var page by remember { mutableStateOf(AuthPage.Welcome) }

    when (page) {
        AuthPage.Welcome -> AuthWelcomePage(
            onLoginClick = {
                viewModel.clearMessage()
                page = AuthPage.Login
            },
            onRegisterClick = {
                viewModel.clearMessage()
                page = AuthPage.Register
            }
        )
        AuthPage.Login -> LoginPage(
            authMessage = authMessage,
            onBack = {
                viewModel.clearMessage()
                page = AuthPage.Welcome
            },
            onLogin = viewModel::login
        )
        AuthPage.Register -> RegisterPage(
            authMessage = authMessage,
            onBack = {
                viewModel.clearMessage()
                page = AuthPage.Welcome
            },
            onRegister = viewModel::register
        )
    }
}

@Composable
fun SpaceSetupScreen(
    viewModel: AuthViewModel = viewModel()
) {
    val authMessage by viewModel.authMessage.collectAsState()
    var inviteCode by remember { mutableStateOf("") }
    var spaceName by remember { mutableStateOf("") }
    var spaceCode by remember { mutableStateOf("") }
    val canJoin = inviteCode.trim().isNotBlank()
    val canCreate = spaceName.trim().isNotBlank() && spaceCode.matches(Regex("[A-Za-z0-9_]{4,24}"))

    LaunchedEffect(Unit) {
        viewModel.refreshProfile()
    }

    AuthSurface {
        BrandHeader(
            title = "选择情侣空间",
            subtitle = "注册后先绑定已有空间，或创建一个新的情侣空间。"
        )
        SectionTitle("已有情侣空间？去绑定")
        OutlinedTextField(
            value = inviteCode,
            onValueChange = { inviteCode = it.trim().take(12) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("输入邀请码") }
        )
        PrimaryAuthButton(
            text = "加入情侣空间",
            enabled = canJoin,
            onClick = { viewModel.joinSpaceByInvite(inviteCode) }
        )

        SectionTitle(
            text = "注册情侣空间",
            modifier = Modifier.padding(top = 22.dp)
        )
        OutlinedTextField(
            value = spaceName,
            onValueChange = { spaceName = it.take(20) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("空间名") }
        )
        OutlinedTextField(
            value = spaceCode,
            onValueChange = { value ->
                spaceCode = value.filter { it.isLetterOrDigit() || it == '_' }.take(24)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("空间id（字母/数字/下划线）") }
        )
        Text(
            text = "空间名可以中文。空间id需唯一，长度 4-24。",
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
        authMessage?.let { ErrorText(message = it) }
        PrimaryAuthButton(
            text = "创建情侣空间",
            enabled = canCreate,
            onClick = { viewModel.createSpace(spaceName, spaceCode) }
        )
    }
}

@Composable
private fun AuthWelcomePage(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    AuthSurface {
        BrandHeader(
            title = "妍叶之庭",
            subtitle = "先完成账号注册或登录，再创建或绑定情侣空间。"
        )
        PrimaryAuthButton(text = "登录", onClick = onLoginClick)
        SecondaryAuthButton(text = "注册", onClick = onRegisterClick)
    }
}

@Composable
private fun LoginPage(
    authMessage: String?,
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val canSubmit = username.trim().isNotBlank() && password.isNotBlank()

    AuthSurface {
        BrandHeader(
            title = "登录账号",
            subtitle = "这一步已经切到 CloudBase 方案。登录成功后，没有空间会进入创建/绑定页。"
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it.take(20) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("用户名") }
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it.take(32) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("密码") },
            visualTransformation = PasswordVisualTransformation()
        )
        authMessage?.let { ErrorText(message = it) }
        PrimaryAuthButton(
            text = "立即登录",
            enabled = canSubmit,
            onClick = { onLogin(username, password) }
        )
        TextButton(onClick = onBack) {
            Text("返回", color = YanYeColors.Muted)
        }
    }
}

@Composable
private fun RegisterPage(
    authMessage: String?,
    onBack: () -> Unit,
    onRegister: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val canSubmit = username.trim().isNotBlank() && password.trim().length >= 6

    AuthSurface {
        BrandHeader(
            title = "注册账号",
            subtitle = "用户名允许中文。注册成功后会进入情侣空间创建/绑定页。"
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it.take(20) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("用户名") }
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it.take(32) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("密码（至少 6 位）") },
            visualTransformation = PasswordVisualTransformation()
        )
        authMessage?.let { ErrorText(message = it) }
        PrimaryAuthButton(
            text = "立即注册",
            enabled = canSubmit,
            onClick = { onRegister(username, password) }
        )
        TextButton(onClick = onBack) {
            Text("返回", color = YanYeColors.Muted)
        }
    }
}

@Composable
private fun AuthSurface(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YanYeColors.Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun BrandHeader(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .padding(bottom = 20.dp)
            .background(Color.White, MaterialTheme.shapes.large)
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = subtitle,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = YanYeColors.Ink,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun PrimaryAuthButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = YanYeColors.Ink,
            contentColor = Color.White,
            disabledContainerColor = YanYeColors.Soft,
            disabledContentColor = YanYeColors.Muted
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun SecondaryAuthButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = YanYeColors.Ink
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        color = YanYeColors.Rose,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}
