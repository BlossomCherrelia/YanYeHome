package com.yanye.home.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanye.home.R
import com.yanye.home.data.session.InviteCodeResult
import com.yanye.home.data.session.UserSession
import com.yanye.home.data.sync.CloudBaseImageUploadService
import com.yanye.home.ui.common.ImagePickerField
import com.yanye.home.ui.common.ImageUploadState
import com.yanye.home.ui.common.PageChrome
import com.yanye.home.ui.common.PrimaryPageHeader
import com.yanye.home.ui.common.SecondaryTopBar
import com.yanye.home.ui.common.WallpaperBackground
import com.yanye.home.ui.common.isLocalOnlyImageUri
import com.yanye.home.ui.theme.YanYeColors
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    session: UserSession,
    onCreateInvite: () -> Unit,
    onOpenProfile: () -> Unit,
    inviteCode: InviteCodeResult?
) {
    val hasPartner = !session.partnerUsername.isNullOrBlank()
    var showInviteDialog by remember { mutableStateOf(false) }
    WallpaperBackground(imageResId = R.drawable.simple_wallpaper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PageChrome.primaryPadding)
        ) {
            PrimaryPageHeader(
                title = "我的",
                actions = {
                    SmallProfileAvatar(
                        session = session,
                        onClick = onOpenProfile
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PairingProfileCard(session = session)

            if (!session.currentSpaceId.isNullOrBlank() && !hasPartner) {
                Spacer(modifier = Modifier.height(14.dp))
                InviteCard(
                    session = session,
                    inviteCode = inviteCode,
                    onCreateInvite = onCreateInvite
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            SettingsGroup(
                title = "关系维护",
                items = listOf(
                    SettingsItem("💕", "冲突修复", Color(0xFFFFE9F0), YanYeColors.Rose),
                    SettingsItem("🛡", "承诺墙", Color(0xFFFFE4EC), YanYeColors.Rose)
                )
            )
            SettingsGroup(
                title = "小宇宙",
                items = listOf(
                    SettingsItem("🪐", "冒险挑战", Color(0xFFF1DEFF), Color(0xFFB86CFF)),
                    SettingsItem("💟", "梗百科", Color(0xFFFFE4EC), YanYeColors.Rose),
                    SettingsItem("💌", "时光信箱", Color(0xFFFFE4EC), YanYeColors.Rose)
                )
            )
            if (hasPartner) {
                SettingsGroup(
                    title = "空间邀请码",
                    items = listOf(
                        SettingsItem(
                            icon = "✉",
                            title = "空间邀请码",
                            background = Color(0xFFFFE4EC),
                            accent = YanYeColors.Rose,
                            trailing = "查看",
                            onClick = { showInviteDialog = true }
                        )
                    )
                )
            }
        }
        if (showInviteDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showInviteDialog = false }
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.clickable(enabled = false) {}) {
                    InviteCard(
                        session = session,
                        inviteCode = inviteCode,
                        onCreateInvite = onCreateInvite
                    )
                    Text(
                        text = "×",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clickable { showInviteDialog = false }
                            .padding(top = 10.dp, end = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteCard(
    session: UserSession,
    inviteCode: InviteCodeResult?,
    onCreateInvite: () -> Unit
) {
    val context = LocalContext.current
    val code = inviteCode?.inviteCode?.takeIf { it.isNotBlank() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InviteEnvelopeIcon()
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            append("空间邀请码：")
                            withStyle(SpanStyle(color = YanYeColors.Rose)) {
                                append(code ?: "未生成")
                            }
                        },
                        style = MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = inviteCode?.expiresAt?.let { "有效期至 ${formatInviteExpiry(it)}" }
                            ?: session.partnerUsername?.let { "已绑定 $it，可重新生成给新设备使用" }
                            ?: "生成后发给对方加入这个空间",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCreateInvite,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = YanYeColors.Ink
                    ),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(0.8.dp, Color(0xFFFFC6D1))
                ) {
                    Text(
                        text = "生成邀请码",
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = {
                        code ?: return@Button
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("YanYeHome invite code", code))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = code != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YanYeColors.RoseSoft,
                        contentColor = YanYeColors.Rose,
                        disabledContainerColor = YanYeColors.RoseSoft,
                        disabledContentColor = YanYeColors.Muted
                    ),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(0.8.dp, Color(0xFFFFC6D1))
                ) {
                    Text("复制邀请码", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PairingProfileCard(session: UserSession) {
    val spaceName = session.spaceName?.takeIf { it.isNotBlank() } ?: "未命名空间"
    val partnerName = session.partnerUsername?.takeIf { it.isNotBlank() } ?: "等待加入"
    val hasPartner = !session.partnerUsername.isNullOrBlank()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(0.6.dp, Color(0xFFFFD4DC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFF4F6), Color(0xFFFFE7EC), Color.White)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "♡",
                color = Color.White.copy(alpha = 0.42f),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = spaceName,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PairingCurve(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 54.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White.copy(alpha = 0.78f), CircleShape)
                            .border(BorderStroke(0.6.dp, Color(0xFFFFD9E1)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♥", color = YanYeColors.Rose, style = MaterialTheme.typography.titleMedium)
                    }
                    PairAvatar(
                        name = session.username,
                        imageUri = session.avatarUrl,
                        background = Color.White,
                        contentColor = YanYeColors.Rose,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    PairAvatar(
                        name = partnerName,
                        imageUri = session.partnerAvatarUrl,
                        background = if (hasPartner) Color.White else YanYeColors.Soft,
                        contentColor = if (hasPartner) YanYeColors.Rose else YanYeColors.Muted,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                    if (!hasPartner) {
                        Text(
                            text = "等待匹配",
                            color = YanYeColors.Rose,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(top = 34.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingCurve(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0f, size.height * 0.56f)
            cubicTo(
                size.width * 0.28f,
                size.height * 0.08f,
                size.width * 0.72f,
                size.height * 0.92f,
                size.width,
                size.height * 0.44f
            )
        }
        drawPath(
            path = path,
            color = YanYeColors.Rose.copy(alpha = 0.52f),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun SmallProfileAvatar(
    session: UserSession,
    onClick: () -> Unit
) {
    AvatarBubble(
        name = session.username,
        imageUri = session.avatarUrl,
        modifier = Modifier
            .size(28.dp)
            .clickable(onClick = onClick),
        background = YanYeColors.RoseSoft,
        contentColor = YanYeColors.Rose,
        borderColor = YanYeColors.Rose.copy(alpha = 0.28f)
    )
}

@Composable
private fun PairAvatar(
    name: String,
    imageUri: String?,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AvatarBubble(
            name = name,
            imageUri = imageUri,
            modifier = Modifier.size(58.dp),
            background = background,
            contentColor = contentColor,
            borderColor = Color.White
        )
        Text(
            text = name,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun AvatarBubble(
    name: String,
    imageUri: String?,
    modifier: Modifier = Modifier,
    background: Color,
    contentColor: Color,
    borderColor: Color
) {
    val context = LocalContext.current
    var bitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(imageUri) {
        bitmap = withContext(Dispatchers.IO) {
            loadAvatarBitmap(context, imageUri)
        }
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .border(BorderStroke(2.dp, borderColor), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = avatarInitial(name),
                color = contentColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProfileEditorScreen(
    session: UserSession,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onSave: (String, String, String?, String) -> Unit,
    message: String?,
    isSaving: Boolean,
    onClearMessage: () -> Unit
) {
    var username by remember(session.userId) { mutableStateOf(session.username) }
    var password by remember(session.userId) { mutableStateOf("") }
    var avatarUrl by remember(session.userId) { mutableStateOf(session.avatarUrl.orEmpty()) }
    var spaceName by remember(session.currentSpaceId) { mutableStateOf(session.spaceName.orEmpty()) }
    var isEditing by remember(session.userId) { mutableStateOf(false) }
    var avatarUploadState by remember { mutableStateOf(ImageUploadState()) }
    val spaceId = session.spaceCode?.takeIf { it.isNotBlank() }
        ?: session.currentSpaceId?.takeIf { it.isNotBlank() }
        ?: "未绑定"
    val partnerName = session.partnerUsername?.takeIf { it.isNotBlank() } ?: "还没有绑定哦～"
    val canSave = username.trim().isNotBlank() &&
        spaceName.trim().isNotBlank() &&
        !avatarUploadState.isUploading &&
        avatarUploadState.errorMessage == null &&
        !isSaving

    LaunchedEffect(username, password, avatarUrl, spaceName) {
        if (message != null) {
            onClearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PageChrome.secondaryPadding)
    ) {
        SecondaryTopBar(
            title = "个人资料",
            actionText = if (isEditing) {
                if (isSaving) "保存中" else "保存"
            } else {
                "编辑"
            },
            onBack = onBack,
            onActionClick = {
                if (isEditing) {
                    if (canSave) {
                        onSave(username.trim(), password, avatarUrl.takeIf { it.isNotBlank() }, spaceName.trim())
                        password = ""
                        isEditing = false
                    }
                } else {
                    isEditing = true
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        ProfileHero(
            username = username,
            avatarUrl = avatarUrl,
            isEditing = isEditing,
            onAvatarUrlChange = { avatarUrl = it },
            onUploadStateChange = { avatarUploadState = it }
        )
        avatarUploadState.errorMessage?.let {
            Text(
                text = it,
                color = YanYeColors.Rose,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        ProfileReadonlyCard(
            isEditing = isEditing,
            username = username,
            onUsernameChange = { username = it.take(24) },
            spaceName = spaceName,
            onSpaceNameChange = { spaceName = it.take(24) },
            spaceId = spaceId,
            partnerName = partnerName,
            password = password,
            onPasswordChange = { password = it.take(32) }
        )

        message?.let {
            Text(
                text = it,
                color = YanYeColors.Rose,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = YanYeColors.RoseSoft,
                contentColor = YanYeColors.Rose
            ),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(0.8.dp, Color(0xFFFFC6D1))
        ) {
            Text("退出登录", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileHero(
    username: String,
    avatarUrl: String?,
    isEditing: Boolean,
    onAvatarUrlChange: (String) -> Unit,
    onUploadStateChange: (ImageUploadState) -> Unit
) {
    val context = LocalContext.current
    val uploadService = remember(context) { CloudBaseImageUploadService(context) }
    val scope = rememberCoroutineScope()
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        scope.launch {
            pendingBitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            }
        }
    }
    pendingBitmap?.let { bitmap ->
        AvatarCropDialog(
            bitmap = bitmap,
            onDismiss = { pendingBitmap = null },
            onConfirm = { cropped ->
                pendingBitmap = null
                val localUri = saveAvatarCrop(context, cropped)
                onAvatarUrlChange(localUri.toString())
                onUploadStateChange(ImageUploadState(isUploading = true))
                scope.launch {
                    runCatching {
                        uploadService.uploadImage(localUri, "avatars")
                    }.onSuccess { remoteUrl ->
                        onAvatarUrlChange(remoteUrl)
                        onUploadStateChange(ImageUploadState())
                    }.onFailure { error ->
                        onUploadStateChange(
                            ImageUploadState(errorMessage = error.message ?: "头像上传失败，请重新选择")
                        )
                    }
                }
            }
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        Text(
            text = "♡",
            color = YanYeColors.RoseSoft.copy(alpha = 0.86f),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 26.dp)
        )
        Text(
            text = "✦",
            color = YanYeColors.RoseSoft,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 34.dp)
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .then(if (isEditing) Modifier.clickable { launcher.launch(arrayOf("image/*")) } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                AvatarBubble(
                    name = username,
                    imageUri = avatarUrl,
                    modifier = Modifier.size(112.dp),
                    background = YanYeColors.RoseSoft,
                    contentColor = YanYeColors.Rose,
                    borderColor = Color.White
                )
                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.34f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📷", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
            Text(
                text = username,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun AvatarCropDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    var zoom by remember(bitmap) { mutableStateOf(1.15f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(centerSquareCrop(bitmap, zoom)) }) {
                Text("使用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        title = {
            Text("调整头像", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(YanYeColors.Soft),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "头像裁剪",
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer(scaleX = zoom, scaleY = zoom),
                        contentScale = ContentScale.Crop
                    )
                }
                Slider(
                    value = zoom,
                    onValueChange = { zoom = it },
                    valueRange = 1f..2.4f
                )
            }
        }
    )
}

private fun centerSquareCrop(bitmap: Bitmap, zoom: Float): Bitmap {
    val side = (minOf(bitmap.width, bitmap.height) / zoom).toInt().coerceAtLeast(1)
    val left = ((bitmap.width - side) / 2).coerceAtLeast(0)
    val top = ((bitmap.height - side) / 2).coerceAtLeast(0)
    return Bitmap.createBitmap(bitmap, left, top, side, side)
}

private fun saveAvatarCrop(context: Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
    }
    return Uri.fromFile(file)
}

@Composable
private fun ProfileReadonlyCard(
    isEditing: Boolean,
    username: String,
    onUsernameChange: (String) -> Unit,
    spaceName: String,
    onSpaceNameChange: (String) -> Unit,
    spaceId: String,
    partnerName: String,
    password: String,
    onPasswordChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, Color(0xFFFFDCE4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            ProfileFieldRow(
                label = "昵称",
                value = username,
                editable = isEditing,
                inputValue = username,
                onInputChange = onUsernameChange
            )
            ProfileDivider()
            ProfileFieldRow(
                label = "空间名",
                value = spaceName.ifBlank { "未命名空间" },
                editable = isEditing,
                inputValue = spaceName,
                onInputChange = onSpaceNameChange
            )
            ProfileDivider()
            ProfileFieldRow(
                label = "空间id",
                value = spaceId,
                editable = false,
                inputValue = spaceId,
                onInputChange = {}
            )
            ProfileDivider()
            ProfileFieldRow(
                label = "绑定对象",
                value = partnerName,
                editable = false,
                inputValue = partnerName,
                onInputChange = {}
            )
            ProfileDivider()
            ProfileFieldRow(
                label = "密码",
                value = "💗",
                editable = isEditing,
                inputValue = password,
                onInputChange = onPasswordChange,
                isPassword = true
            )
        }
    }
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        color = Color(0xFFFFDCE4).copy(alpha = 0.82f),
        thickness = 0.6.dp,
        modifier = Modifier.padding(start = 0.dp)
    )
}

@Composable
private fun ProfileFieldRow(
    label: String,
    value: String,
    editable: Boolean,
    inputValue: String,
    onInputChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(82.dp)
        )
        if (editable) {
            BasicTextField(
                value = inputValue,
                onValueChange = onInputChange,
                singleLine = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = YanYeColors.Ink),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .border(0.8.dp, YanYeColors.Line, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (inputValue.isBlank()) {
                            Text(
                                text = if (isPassword) "💗" else value,
                                color = YanYeColors.Muted.copy(alpha = 0.55f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        innerTextField()
                    }
                }
            )
        } else {
            Text(
                text = value,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AccountCard(
    session: UserSession,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "账号",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            InfoRow(label = "用户名", value = session.username)
            InfoRow(label = "用户 ID", value = session.userId)
            InfoRow(
                label = "情侣空间",
                value = session.spaceName ?: "第一阶段暂未接入空间创建/绑定"
            )
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YanYeColors.Ink,
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "退出登录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun avatarInitial(name: String): String =
    name.trim().firstOrNull()?.toString()?.uppercase() ?: "Y"

private fun loadAvatarBitmap(context: Context, imageUri: String?): Bitmap? {
    val uriText = imageUri?.takeIf(String::isNotBlank) ?: return null
    return runCatching {
        when {
            uriText.startsWith("content://") || uriText.startsWith("file://") ->
                context.contentResolver.openInputStream(Uri.parse(uriText))?.use(BitmapFactory::decodeStream)

            uriText.startsWith("http://") || uriText.startsWith("https://") ->
                URL(uriText).openStream().use(BitmapFactory::decodeStream)

            else -> BitmapFactory.decodeFile(File(uriText).absolutePath)
        }
    }.getOrNull()
}

private data class SettingsItem(
    val icon: String,
    val title: String,
    val background: Color,
    val accent: Color,
    val trailing: String = "后续接入",
    val onClick: (() -> Unit)? = null
)

@Composable
private fun InviteEnvelopeIcon() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(
                Brush.linearGradient(listOf(Color(0xFFFFDCE5), Color(0xFFFF8EAA))),
                RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "💌",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    items: List<SettingsItem>
) {
    Column(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 2.dp, start = 2.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.6.dp, YanYeColors.Line.copy(alpha = 0.72f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)) {
                items.forEachIndexed { index, item ->
                    SettingsGroupRow(item = item)
                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            color = YanYeColors.Line.copy(alpha = 0.68f),
                            thickness = 0.6.dp,
                            modifier = Modifier.padding(start = 46.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupRow(item: SettingsItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .then(if (item.onClick != null) Modifier.clickable { item.onClick.invoke() } else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(item.background, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = item.icon, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = item.title,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = item.trailing,
            color = YanYeColors.Rose,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "›",
            color = YanYeColors.Muted.copy(alpha = 0.65f),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private fun formatInviteExpiry(expiresAtMillis: Long): String =
    DateTimeFormatter.ofPattern("MM月dd日 HH:mm")
        .format(Instant.ofEpochMilli(expiresAtMillis).atZone(ZoneId.systemDefault()))
