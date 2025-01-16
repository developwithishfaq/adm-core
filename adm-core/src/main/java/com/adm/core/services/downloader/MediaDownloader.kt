package com.adm.core.services.downloader

import com.adm.core.components.DownloadingState

interface MediaDownloader {
    suspend fun downloadMedia(
        url: String,
        fileName: String,
        directoryPath: String,
        mimeType: String,
        headers: Map<String, String>,
        showNotification: Boolean,
        supportChunks: Boolean
    ): Result<String>

    fun getBytesInfo(): Pair<Long, Long>
    fun getCurrentStatus(): DownloadingState
    fun cancelDownloading()
    fun resumeDownloading()
    fun pauseDownloading()
}