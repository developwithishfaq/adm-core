package com.adm.core.services.downloader

import android.util.Log
import com.adm.core.components.DownloadingState
import com.adm.core.model.CustomDownloaderModel
import com.adm.core.utils.DownloaderPathsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID


class CustomDownloaderImpl(
) : MediaDownloader {
    private val TAG = "CustomDownloaderImpl"

    private var supportChunking = false
    private var isPaused = false

    private var model = CustomDownloaderModel()
    private var downloadedBytesSize = 0L
    private var downloadedId: String = UUID.randomUUID().toString()
    private var totalBytesSize = 0L
    private var downloadStatus: DownloadingState = DownloadingState.Idle

    override suspend fun downloadMedia(
        url: String,
        fileName: String,
        directoryPath: String,
        mimeType: String,
        headers: Map<String, String>,
        showNotification: Boolean,
        supportChunks: Boolean
    ): String {
        supportChunking = supportChunks
        log("downloadMedia(supportChunks=${supportChunks}):\nUrl=${url}\nPath=$directoryPath")
        model =
            CustomDownloaderModel(
                url = url,
                fileName = fileName,
                directoryPath = directoryPath,
                mimeType = mimeType,
                headers = headers,
                showNotification = showNotification
            )
        downloadStatus = DownloadingState.Idle
        downloadFile(false)
        downloadedId = UUID.randomUUID().toString()
        return downloadedId
    }

    override fun resumeDownloading() {
        if (isPaused) {
            isPaused = false
            log("Resuming download...")
            CoroutineScope(Dispatchers.IO).launch {
                downloadStatus = DownloadingState.Progress
                downloadFile(true)
            }
        }
    }

    private suspend fun downloadFile(forPauseResume: Boolean) {
        withContext(Dispatchers.IO) {
            val destFile = File(
                DownloaderPathsHelper.getDirInsideDownloads(model.directoryPath),
                model.fileName
            )
            log("destFile path=${destFile.path}")
            destFile.createNewFile()
            val existingFileSize = if (destFile.exists()) destFile.length() else 0L
            val connection = URL(model.url).openConnection() as HttpURLConnection
            if (supportChunking) {
                if (forPauseResume) {
                    connection.setRequestProperty("Range", "bytes=$existingFileSize-")
                }
                log("Headers=${model.headers}")
                model.headers.forEach {
                    connection.setRequestProperty(it.key, it.value)
                }
            }
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_PARTIAL || connection.responseCode == HttpURLConnection.HTTP_OK) {
                downloadStatus = DownloadingState.Progress
                val totalSize = connection.contentLength + existingFileSize
                log("Starting download. Total size: $totalSize bytes.")

                val inputStream = connection.inputStream
                val outputStream = RandomAccessFile(destFile, "rw")
                outputStream.seek(existingFileSize)

                val buffer = ByteArray(1024 * 16) // 16 KB buffer
                var bytesRead: Int

                try {
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (isPaused) {
                            downloadStatus = DownloadingState.Paused
                            log("Download paused.")
                            break
                        }
                        outputStream.write(buffer, 0, bytesRead)
                        val downloadedSize = outputStream.length()
                        downloadedBytesSize = downloadedSize
                        totalBytesSize = totalSize
                        downloadStatus = DownloadingState.Progress
                        log(
                            "Downloaded $downloadedSize / $totalSize bytes.\nUrl=${model.url}",
                            tag = "DownProgress"
                        )
                    }

                    if (!isPaused) {
                        downloadStatus = DownloadingState.Success
                        log("Download completed: ${destFile.path}")
                    }
                } catch (e: Exception) {
                    downloadStatus = DownloadingState.Failed
                    log("Error during download: ${e.message}")
                } finally {
                    inputStream.close()
                    outputStream.close()
                    connection.disconnect()
                }
            } else {
                log("Server responded with code: ${connection.responseCode}")
            }
        }
    }

    private fun log(msg: String, tag: String = TAG) {
        Log.d(tag, "CustomDownloaderImpl:$msg")
    }

    override fun pauseDownloading() {
        isPaused = true
    }

    override fun getBytesInfo(): Pair<Long, Long> {
        return Pair(downloadedBytesSize, totalBytesSize)
    }

    override fun getCurrentStatus(): DownloadingState {
        if (downloadedBytesSize >= totalBytesSize && downloadedBytesSize > 0 && totalBytesSize > 0) {
            downloadStatus = DownloadingState.Success
        }
        return downloadStatus
    }

    override fun cancelDownloading() {
        try {
        } catch (e: Exception) {
        }
    }


}



