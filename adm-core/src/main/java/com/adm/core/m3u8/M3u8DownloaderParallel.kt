package com.adm.core.m3u8

import android.content.Context
import android.util.Log
import com.adm.core.components.DownloadingState
import com.adm.core.services.downloader.CustomDownloaderImpl
import com.adm.core.services.downloader.MediaDownloader
import com.adm.core.services.logger.Logger
import com.down.m3u8_parser.listeners.M3u8ChunksPicker
import com.down.m3u8_parser.model.SingleStream
import com.down.m3u8_parser.parsers.M3u8ChunksPickerImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.io.File

class M3u8DownloaderParallel(
    private val context: Context,
    private val tempDirProvider: TempDirProvider = TempDirProviderImpl(context = context),
    private val m3U8PlaylistParser: M3u8ChunksPicker = M3u8ChunksPickerImpl(),
    private val videosMerger: VideosMerger,
    private val logger: Logger,
    private val maxParallelDownloads: MaxParallelDownloads

) : MediaDownloader {

    private val TAG = "M3u8Downloader"
    private var downloadingId = ""
    private var download = 0L
    private var totalChunks = 0L
    val scope = CoroutineScope(Dispatchers.IO)
    var isFailed = false
    var isCompleted = false
    var isPaused = false
    private val tempDirPath: File by lazy {
        tempDirProvider.provideTempDir("mp4Videos/${System.currentTimeMillis()}")
    }

    override suspend fun downloadMedia(
        url: String,
        fileName: String,
        directoryPath: String,
        mimeType: String,
        headers: Map<String, String>,
        showNotification: Boolean,
        supportChunks: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            isFailed = false
            isPaused = false
            isCompleted = false
            val streams: List<SingleStream> =
                m3U8PlaylistParser.getChunks(m3u8Link = url, headers = headers)
            if (streams.isEmpty())
                throw Exception("Invalid Url")
            totalChunks = streams.size.toLong()
            logger.logMessage("TAG", "New Save Directory = $tempDirPath")
            tempDirPath.createThisFolderIfNotExists()
            val channel = Channel<Unit>(16)
            val downloadJobs = mutableListOf<Deferred<Long>>()
            streams.forEachIndexed { index, stream ->
                val job = scope.async {
                    channel.send(Unit)  // Wait for a slot in the channel
                    Log.d("Cvrrr", "Base Url = sending $index")
                    val mediaDownloader = CustomDownloaderImpl(
                        context,
                        tempDirProvider,
                        videosMerger,
                        maxParallelDownloads,
                        logger
                    )
                    val baseUrl = url.substringBeforeLast("/")
                    val urlToDownload = if (stream.link.startsWith("http")) {
                        stream.link
                    } else {
                        baseUrl + "/${stream.link}"
                    }
                    logger.logMessage(
                        "TAG",
                        "Base Url = ${baseUrl}\nstreamLink=${stream.link}\nUrlToDownload = ${urlToDownload}"
                    )

                    val result = mediaDownloader.downloadMedia(
                        url = urlToDownload,
                        fileName = "${index}.${fileName.substringAfterLast(".")}",
                        directoryPath = tempDirPath.absolutePath,
                        mimeType = mimeType,
                        headers = headers,
                        showNotification = showNotification,
                        supportChunks = false
                    )
                    result.getOrThrow()
                    Log.d("Cvrrr", "Base Url = receiving $index")
                    channel.receive()
                    download += 1
                    download
                }
                downloadJobs.add(job)
            }

            downloadJobs.awaitAll()  // Wait for all downloads to finish
            Log.d("Cvrrr", "Base Url ${tempDirPath}")

            val result = videosMerger.mergeVideos(
                tempDirPath.absolutePath,
                File(directoryPath, fileName).path
            )
            result.getOrThrow()
            logger.logMessage("TAG", "Streams(${streams.size}) = " + streams.toString())
            isCompleted = true
            return@withContext Result.success(File(directoryPath, fileName).path)
        } catch (e: Exception) {
            if (e is CancellationException)
                throw e
            isFailed = true
            Log.d("Cvrrr", "Base Url  Exception $e")
            scope.cancel()
            return@withContext Result.failure(e)
        }
    }

    override fun getBytesInfo(): Pair<Long, Long> {
        return Pair(download * 1024, totalChunks * 1024)
    }

    override fun getCurrentStatus(): DownloadingState {
        Log.d(TAG, "getCurrentStatus: download=${download},totalChunks=${totalChunks}")
        return if (isPaused)
            DownloadingState.Paused
        else if (isFailed)
            DownloadingState.Failed
        else if (isCompleted) {
            DownloadingState.Success
        } else {
            DownloadingState.Progress
        }
    }

    override fun cancelDownloading() {
        isFailed = true
        scope.cancel()
    }

    override fun resumeDownloading() {
        if (isPaused) {
            isPaused = false

        }
    }

    override fun pauseDownloading() {
        isPaused = true
        scope.cancel()
    }


}
