package com.yanye.home.data.sync

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class CloudBaseHttpClient {
    fun postJson(endpoint: String, body: JSONObject): JSONObject {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            doOutput = true
            setRequestProperty("content-type", "application/json; charset=utf-8")
            setRequestProperty("accept", "application/json")
        }

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body.toString())
        }

        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (connection.responseCode !in 200..299) {
            error("CloudBase HTTP ${connection.responseCode}: $text")
        }

        return unwrapCloudBaseResponse(JSONObject(text))
    }

    private fun unwrapCloudBaseResponse(rawResponse: JSONObject): JSONObject {
        val bodyText = rawResponse.optString("body")
        return if (bodyText.isNotBlank() && rawResponse.has("statusCode")) {
            JSONObject(bodyText)
        } else {
            rawResponse
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 12_000
    }
}
