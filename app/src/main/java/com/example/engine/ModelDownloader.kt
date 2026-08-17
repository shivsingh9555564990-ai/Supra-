package com.example.engine

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class DownloadState(val progress: Float, val status: String, val isComplete: Boolean = false, val error: Boolean = false)

class ModelDownloader(private val context: Context) {

    private fun getAvailableSpaceInMB(): Long {
        val stat = StatFs(context.filesDir.path)
        return (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
    }

    fun downloadModel(urlString: String, fileName: String, expectedSizeMB: Long): Flow<DownloadState> = flow {
        val file = File(context.filesDir, fileName)
        
        // Basic Space Check
        if (!file.exists() || file.length() < (expectedSizeMB * 1024 * 1024)) {
            val availableMB = getAvailableSpaceInMB()
            if (availableMB < expectedSizeMB + 100) { // Keep 100MB buffer
                emit(DownloadState(0f, "Error: Not enough storage. Need ${expectedSizeMB}MB, have ${availableMB}MB.", true, true))
                return@flow
            }
        }
        
        emit(DownloadState(0f, "Connecting to $fileName repo..."))

        var totalBytes = expectedSizeMB * 1024 * 1024
        var retryCount = 0
        val maxRetries = 15

        while (retryCount < maxRetries) {
            try {
                var downloadedBytes = if (file.exists()) file.length() else 0L

                // If already completed
                if (downloadedBytes >= (expectedSizeMB * 1024 * 1024 * 0.98).toLong() && downloadedBytes > 1024L) {
                    emit(DownloadState(1f, "$fileName ready in Internal Storage.", true))
                    return@flow
                }

                // Connect with Range header to resume from current byte
                var currentUrl = urlString
                var connection: HttpURLConnection? = null

                // Handle redirects explicitly
                for (redirect in 0 until 5) {
                    val url = URL(currentUrl)
                    connection = url.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.connectTimeout = 30000
                    connection.readTimeout = 45000
                    if (downloadedBytes > 0) {
                        connection.setRequestProperty("Range", "bytes=$downloadedBytes-")
                    }
                    connection.connect()

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == 307 || responseCode == 308) {
                        val newLocation = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (newLocation != null) {
                            currentUrl = newLocation
                            continue
                        }
                    }
                    break
                }

                if (connection == null) {
                    throw Exception("Failed to open connection")
                }

                val responseCode = connection.responseCode

                if (responseCode == 416) {
                    // Range Not Satisfiable: File already fully downloaded!
                    emit(DownloadState(1f, "$fileName ready in Internal Storage.", true))
                    connection.disconnect()
                    return@flow
                }

                val isPartial = responseCode == HttpURLConnection.HTTP_PARTIAL
                val isOk = responseCode == HttpURLConnection.HTTP_OK

                if (!isPartial && !isOk) {
                    throw Exception("HTTP Error: $responseCode")
                }

                val contentLength = connection.contentLengthLong
                if (isPartial) {
                    if (contentLength > 0) {
                        totalBytes = downloadedBytes + contentLength
                    }
                } else if (isOk) {
                    if (contentLength > 0) {
                        totalBytes = contentLength
                    }
                    downloadedBytes = 0L // Reset if server doesn't support Range
                }

                val input = BufferedInputStream(connection.inputStream, 65536)
                val output = FileOutputStream(file, isPartial)

                val data = ByteArray(65536)
                var count: Int
                var lastProgress = 0f

                while (input.read(data).also { count = it } != -1) {
                    downloadedBytes += count
                    output.write(data, 0, count)

                    val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f

                    if (progress - lastProgress > 0.005f || progress >= 1f) {
                        val downloadedMB = downloadedBytes.toFloat() / (1024 * 1024)
                        val totalMB = totalBytes.toFloat() / (1024 * 1024)
                        val downloadedStr = String.format("%.1f", downloadedMB)
                        val totalStr = String.format("%.1f", totalMB)

                        emit(DownloadState(
                            progress.coerceIn(0f, 1f),
                            "Downloading $fileName\n${(progress * 100).toInt()}%  ($downloadedStr MB / $totalStr MB)"
                        ))
                        lastProgress = progress
                    }
                }

                output.flush()
                output.close()
                input.close()
                connection.disconnect()

                if (downloadedBytes >= (totalBytes * 0.95)) {
                    emit(DownloadState(1f, "$fileName downloaded successfully.", true))
                    return@flow
                }
            } catch (e: Exception) {
                retryCount++
                if (retryCount >= maxRetries) {
                    emit(DownloadState(0f, "Download failed after multiple attempts: ${e.message}", true, true))
                    return@flow
                }
                val downloadedMB = if (file.exists()) file.length().toFloat() / (1024 * 1024) else 0f
                val downloadedStr = String.format("%.1f", downloadedMB)
                emit(DownloadState(
                    (if (totalBytes > 0) (file.length().toFloat() / totalBytes) else 0f).coerceIn(0f, 0.99f),
                    "Reconnecting (${retryCount}/$maxRetries)... Resuming from $downloadedStr MB"
                ))
                delay(2000L * retryCount)
            }
        }
    }.flowOn(Dispatchers.IO)
}
