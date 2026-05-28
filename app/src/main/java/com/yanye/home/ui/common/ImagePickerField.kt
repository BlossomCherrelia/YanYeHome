package com.yanye.home.ui.common

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanye.home.data.sync.CloudBaseImageUploadService
import com.yanye.home.ui.theme.YanYeColors
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImageUploadState(
    val isUploading: Boolean = false,
    val errorMessage: String? = null
)

@Composable
fun ImagePickerField(
    label: String,
    imageUri: String,
    onImageUriChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 280.dp,
    module: String = "images",
    onUploadStateChange: (ImageUploadState) -> Unit = {}
) {
    val context = LocalContext.current
    val uploadService = remember(context) { CloudBaseImageUploadService(context) }
    val scope = rememberCoroutineScope()
    var uploadState by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imageUri) {
        if (imageUri.isBlank()) {
            onUploadStateChange(ImageUploadState())
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        onImageUriChange(uri.toString())
        uploadState = "正在上传..."
        onUploadStateChange(ImageUploadState(isUploading = true))
        scope.launch {
            uploadState = runCatching {
                val remoteUrl = uploadService.uploadImage(uri, module)
                onImageUriChange(remoteUrl)
                onUploadStateChange(ImageUploadState())
                "已上传云端"
            }.getOrElse { error ->
                onUploadStateChange(
                    ImageUploadState(errorMessage = error.message ?: "未知错误")
                )
                "云端上传失败，暂存本地图片：${error.message ?: "未知错误"}"
            }
        }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (imageUri.isNotBlank()) {
                        TextButton(onClick = {
                            onImageUriChange("")
                            onUploadStateChange(ImageUploadState())
                        }) {
                            Text("移除", color = YanYeColors.Rose)
                        }
                    }
                    OutlinedButton(onClick = { launcher.launch(arrayOf("image/*")) }) {
                        Text(if (imageUri.isBlank()) "选择图片" else "更换图片")
                    }
                }
            }
            uploadState?.let { state ->
                Text(
                    text = state,
                    color = if (state.startsWith("云端上传失败")) YanYeColors.Rose else YanYeColors.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (imageUri.isNotBlank()) {
                ImagePreviewBox(
                    imageUri = imageUri,
                    contentDescription = label,
                    modifier = Modifier.fillMaxWidth(),
                    maxHeight = height
                )
            }
        }
    }
}

fun isLocalOnlyImageUri(imageUri: String): Boolean {
    val value = imageUri.trim()
    return value.startsWith("content://") || value.startsWith("file://")
}

@Composable
fun ImagePreviewBox(
    imageUri: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = "图片",
    minHeight: Dp = 120.dp,
    maxHeight: Dp = 280.dp
) {
    val context = LocalContext.current
    var previewBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri) {
        previewBitmap = withContext(Dispatchers.IO) {
            loadBitmap(context, imageUri)
        }
    }
    if (previewBitmap == null) return

    val bitmap = previewBitmap ?: return
    val aspectRatio = bitmap
        .takeIf { it.width > 0 && it.height > 0 }
        ?.let { it.width.toFloat() / it.height.toFloat() }
        ?: return
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(MaterialTheme.shapes.medium)
            .heightIn(min = minHeight, max = maxHeight),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

private fun loadBitmap(context: android.content.Context, imageUri: String?): Bitmap? {
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
