package com.down.adm_core.m3u8

import android.util.Log
import com.adm.core.components.DownloadingState
import com.adm.core.services.downloader.CustomDownloaderImpl
import com.adm.core.services.downloader.MediaDownloader
import com.down.m3u8_parser.listeners.M3u8ChunksPicker
import com.down.m3u8_parser.model.SingleStream
import com.down.m3u8_parser.parsers.M3u8ChunksPickerImpl
import java.io.File
import java.util.UUID

class M3u8Downloader(
    private val m3U8PlaylistParser: M3u8ChunksPicker = M3u8ChunksPickerImpl(),
) : MediaDownloader {

    private val TAG = "M3u8Downloader"
    private var downloadingId = ""
    private var download = 0L
    private var totalChunks = 0L
    private val downloader = HashMap<String, CustomDownloaderImpl>()

    override suspend fun downloadMedia(
        url: String,
        fileName: String,
        directoryPath: String,
        mimeType: String,
        headers: Map<String, String>,
        showNotification: Boolean,
        supportChunks: Boolean
    ): String {
        val streams: List<SingleStream> =
            m3U8PlaylistParser.getChunks(m3u8Link = url, headers = headers)
        totalChunks = streams.size.toLong()

        val newDirectory = directoryPath + "/${fileName.substringBeforeLast(".")}"
        log("New Save Directory = $newDirectory")
        File(newDirectory).mkdirs()
        streams.forEachIndexed { index, stream ->
            val mediaDownloader = CustomDownloaderImpl()
            val baseUrl = url.substringBeforeLast("/")
            val urlToDownload = if (stream.link.startsWith("http")) {
                stream.link
            } else {
                baseUrl + "/${stream.link}"
            }
            log("Base Url = ${baseUrl}\nstreamLink=${stream.link}\nUrlToDownload = ${urlToDownload}")
            val id = mediaDownloader.downloadMedia(
                url = urlToDownload,
                fileName = "${index}.${fileName.substringAfterLast(".")}",
                directoryPath = newDirectory,
                mimeType = mimeType,
                headers = headers + stream.headers,
                showNotification = showNotification,
                supportChunks = true
            )
            download += 1
            downloader[id] = mediaDownloader
        }
        log("Streams(${streams.size}) = " + streams.toString())
        downloadingId = UUID.randomUUID().toString()
        return downloadingId
    }

    override fun getBytesInfo(): Pair<Long, Long> {
        return Pair(download * 1024, totalChunks * 1024)
    }

    override fun getCurrentStatus(): DownloadingState {
        Log.d(TAG, "getCurrentStatus: download=${download},totalChunks=${totalChunks}")
        return if (download == totalChunks && totalChunks > 0) {
            DownloadingState.Success
        } else {
            DownloadingState.Progress
        }
    }

    override fun cancelDownloading() {

    }

    override fun resumeDownloading() {

    }

    override fun pauseDownloading() {

    }

    fun log(msg: String) {
        Log.d(TAG, "M3u8Downloader:$msg")
    }
}
