package com.yanye.home.data.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class CloudBaseImageUploadService(
    context: Context,
    private val httpClient: CloudBaseHttpClient = CloudBaseHttpClient()
) {
    private val appContext = context.applicationContext
    private val syncSettings = SyncSettings(appContext)

    suspend fun uploadImage(localUri: Uri, module: String): String =
        withContext(Dispatchers.IO) {
            val endpoint = CloudBaseConfig.UPLOAD_IMAGE_URL.trim()
            if (endpoint.isBlank()) error("CloudBase 图片上传地址未配置")

            val identity = syncSettings.identity()
            val bytes = compressedImageBytes(localUri)
            val response = httpClient.postJson(
                endpoint,
                JSONObject()
                    .put("envId", CloudBaseConfig.ENV_ID)
                    .put("coupleId", identity.coupleId)
                    .put("userId", identity.localUserId)
                    .put("module", module)
                    .put("mimeType", "image/jpeg")
                    .put("base64", Base64.encodeToString(bytes, Base64.NO_WRAP))
            )
            if (!response.optBoolean("ok")) {
                error(response.optString("error", "CloudBase 图片上传失败"))
            }
            response.optString("url").ifBlank {
                response.optString("fileID").ifBlank { error("CloudBase 未返回图片地址") }
            }
        }

    private fun compressedImageBytes(uri: Uri): ByteArray {
        val original = appContext.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            ?: error("无法读取图片")
        val scaled = original.scaledToMaxDimension(MAX_IMAGE_DIMENSION)
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        if (scaled !== original) scaled.recycle()
        original.recycle()
        return output.toByteArray()
    }

    private fun Bitmap.scaledToMaxDimension(maxDimension: Int): Bitmap {
        val largestSide = maxOf(width, height)
        if (largestSide <= maxDimension) return this
        val scale = maxDimension.toFloat() / largestSide.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }

    private companion object {
        const val MAX_IMAGE_DIMENSION = 1080
        const val JPEG_QUALITY = 72
    }
}
