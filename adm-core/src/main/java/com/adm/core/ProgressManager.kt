package com.adm.core

import android.util.Log
import com.adm.core.components.DownloadingState
import com.adm.core.components.getDownloadingStatus
import com.adm.core.db.InProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


data class InProgressVideoUi(
    val id:String,
    val url: String = "",
    var fileName: String = "",
    val destinationDirectory: String = "",
    val mimeType: String = "",
    val status: DownloadingState = DownloadingState.Idle,
    val downloadedSize: Long = 0,
    val totalSize: Long = 0,
    val progress: Float = 0f
)

class ProgressManager(
    private val inProgressRepository: InProgressRepository
) {
    val scope = CoroutineScope(Dispatchers.IO)

    suspend fun updateProgress(id: String, downloaded: Long, total: Long) {
        var inProgressVideo = inProgressRepository.getItemById(id.toLong())
        if (inProgressVideo != null) {
            inProgressVideo = inProgressVideo.copy(downloadedSize = downloaded, totalSize = total)
            inProgressRepository.addInQue(inProgressVideo)
        }
    }

    suspend fun updateStatus(id: String, downState: DownloadingState) {
        var inProgressVideo = inProgressRepository.getItemById(id.toLong())
        if (inProgressVideo != null) {
            inProgressVideo = inProgressVideo.copy(status = downState.name)
            inProgressRepository.addInQue(inProgressVideo)
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


    val videosProgress = inProgressRepository.getAllQueVideos().map {
        it.map {
            InProgressVideoUi(
                id = it.downloadId.toString(),
                url = it.url,
                fileName = it.fileName,
                destinationDirectory = it.destinationDirectory,
                mimeType = it.mimeType,
                status = it.status.getDownloadingStatus(),
                downloadedSize = it.downloadedSize,
                totalSize = it.totalSize,
                progress = it.downloadedSize / it.totalSize.toFloat(),
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
}