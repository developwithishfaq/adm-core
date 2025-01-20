package com.down.adm_core

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adm.core.DownloaderCoreImpl
import com.adm.core.MyDownloaderManager
import com.adm.core.ProgressManager
import com.adm.core.components.DownloadingState
import com.adm.core.components.SupportedMimeTypes
import com.adm.core.m3u8.createThisFolderIfNotExists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class ScreenState(
    val progress: Float = 0f,
    val status: DownloadingState = DownloadingState.Idle
)

class MainScreenViewModel(
    private val myDownloaderManager: MyDownloaderManager,
    private val progressManager: ProgressManager
) : ViewModel() {

    private val _state = MutableStateFlow(ScreenState())
    val state = _state.asStateFlow()
    private lateinit var downloader: DownloaderCoreImpl


    fun download(context: Context, fileName: String, textUrl: String) {
        val (_, directory) = getFileNameAndDirectory()
        viewModelScope.launch {

            myDownloaderManager.startDownloading(
                url = textUrl,
                fileName = fileName,
                directoryPath = directory,
                showNotification = true,
                mimeType = SupportedMimeTypes.Video.mimeTye,
                supportChunks = true,
                headers = emptyMap()
            )
        }
        /* viewModelScope.apply {

             launch {
                 checkProgress(downloader)
             }
         }*/
    }

    fun pause(id: Long) {
        viewModelScope.launch {
            myDownloaderManager.pauseDownloading(id)
        }
    }

    fun resume(id: Long) {
        viewModelScope.launch {
            myDownloaderManager.resumeDownloading(id)
        }
    }

    private fun getFileNameAndDirectory(): Pair<String, String> {
        val directory = "Ishfaq"
//        val fileName = System.currentTimeMillis().toString() + ".mp4"
        val fileName = "Test" + ".mp4"
        val mainStorage =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val folder = File(mainStorage, directory)
        folder.path.createThisFolderIfNotExists()
        return Pair(fileName, folder.absolutePath)
    }

    private suspend fun checkProgress(downloader: DownloaderCoreImpl) {
        Log.d("cvv", "checkProgress: started")
        var runLoop = true
        while (runLoop) {
            val downState = downloader.getDownloadingState()

            val downloaded = downloader.getDownloadedSize()
            val total = downloader.getTotalSize()
            Log.d("cvv", "checkProgress(${downState})\ndownloaded=${downloaded},total=${total}")
            val progress =
                calculateProgress(downloaded, total)
            _state.update {
                it.copy(
                    progress = progress,
                    status = downState
                )
            }
            delay(1000)
            if (downState == DownloadingState.Success) {
                val path =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/" + downloader.getDestinationDirectory() + "/" + downloader.getFileName()
                        .substringBeforeLast(".")
                val list = (File(path).listFiles()?.toList() ?: emptyList()).fastMap {
                    it.path
                }
                Log.d("cvv", "checkProgress: File Path=${path}")
                withContext(Dispatchers.IO) {
                    File("$path.mp4").createNewFile()
//                    M3u8VideosMerger().mergeVideos(list, path + ".mp4")
                }
                runLoop = false
            }
            if (downState == DownloadingState.Failed)
                runLoop = false

        }
    }

    private fun calculateProgress(downloadedBytes: Long, totalBytes: Long): Float {
        return if (totalBytes > 0L) {
            val percentageIn100 = ((downloadedBytes.toFloat() / totalBytes) * 100) / 100f
            Log.d("cvv", "calculateProgress(${downloadedBytes}/${totalBytes}): $percentageIn100")
            percentageIn100
        } else {
            0f // Return 0.0 if totalBytes is 0 to avoid division by zero
        }
    }

    fun merge(context: Context) {
        viewModelScope.launch {
            val path =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/Ishfaq/1736792505728"

            val path2 =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/Ishfaq/razaaii.mp4"

            val file = File(path)
            val list = (file.listFiles()?.toList()?.mapNotNull { it } ?: emptyList())
            val sorted = list.sortedBy {
                it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE
            }
            Log.d(
                "cvv",
                "checkProgress(exists=${file.exists()})\nFile Path=${path}\nItems=${list.size}\nList=${sorted}"
            )
            mergeTsFiles(sorted.map { it.path }, path2)

        }
    }

    private val TAG = "VideoMerger"
    private val BUFFER_SIZE = 1 * 1024 * 1024 // 1MB


    fun mergeTsFiles(segmentPaths: List<String>, outputFilePath: String) {
        val outputFile = File(outputFilePath)
        try {
            // Create a FileOutputStream to write the merged file
            FileOutputStream(outputFile).use { outputStream ->
                for (segmentPath in segmentPaths) {
                    val segmentFile = File(segmentPath)
                    if (!segmentFile.exists()) {
                        Log.d(TAG, "Segment file not found: $segmentPath")
                        continue
                    }

                    // Read the segment file and write its bytes to the output file
                    FileInputStream(segmentFile).use { inputStream ->
                        val buffer = ByteArray(1024 * 1024) // 1 MB buffer
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                    }
                }
            }

            Log.d(TAG, "Merged TS file created at: $outputFilePath")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "Error merging TS files: ${e.message}")
        }
    }

    @OptIn(FlowPreview::class)
    val progress = progressManager.videosProgress.sample(1000).stateIn(
        viewModelScope, SharingStarted.Eagerly,
        emptyList()
    )

}