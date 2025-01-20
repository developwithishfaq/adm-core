package com.adm.core

import android.content.Context
import android.util.Log
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.adm.core.components.DownloadingState
import com.adm.core.db.InProgressRepository
import com.adm.core.db.InProgressVideoDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyDownloaderManager(
    private val context: Context,
    private val inProgressRepository: InProgressRepository,
    private val progressManager: ProgressManager
) {

    companion object {
        const val TAG = "MyDownloadManager"
    }

    fun log(msg: String) {
        Log.d(TAG, msg)
    }

    suspend fun startDownloading(
        url: String,
        fileName: String,
        directoryPath: String,
        mimeType: String,
        headers: Map<String, String>,
        showNotification: Boolean,
        supportChunks: Boolean
    ) {
        log("startDownloading")

        val id = System.currentTimeMillis()
        insertIntoDB(
            id = id,
            url = url,
            fileName = fileName,
            directoryPath = directoryPath,
            mimeType = mimeType,
            headers = headers,
            showNotification = showNotification,
            supportChunks = supportChunks,
        )



        val workerDownloadingModel = WorkerDownloadingModel(
            id = id.toString(),
            url = url,
            fileName = fileName,
            destinationDirectory = directoryPath,
            mimeType = mimeType,
            headers = headers,
            showNotification = showNotification,
            supportChunks = supportChunks,
        )


        DownloadingWorker.startWorker(context, id.toString(), workerDownloadingModel)

     }


    suspend fun pauseDownloading(id: Long) {
        progressManager.updateStatus(
            id.toString(),
            DownloadingState.Paused
        )
        WorkManager.getInstance(context).cancelUniqueWork(id.toString())
    }

    suspend fun resumeDownloading(id: Long) {
        progressManager.updateStatus(
            id.toString(),
            DownloadingState.Progress
        )
        val video = inProgressRepository.getItemById(id)
        if (video != null) {

            val workerDownloadingModel = WorkerDownloadingModel(
                id = id.toString(),
                url = video.url,
                fileName = video.fileName,
                destinationDirectory = video.destinationDirectory,
                mimeType = video.mimeType,
//                headers = video.headers,
                showNotification = video.showNotification,
                supportChunks = video.supportChunks,
            )

            DownloadingWorker.startWorker(context, id.toString(), workerDownloadingModel)

        }

        WorkManager.getInstance(context).cancelAllWorkByTag(id.toString())
    }

    suspend fun deleteDownloading(id: Long) {
        progressManager.deleteVideo(id.toString())
    }

    suspend fun cancelDownloading(id: Long) {
        progressManager.updateStatus(id.toString(), DownloadingState.Paused)
    }

    suspend fun insertIntoDB(
        id: Long,
        url: String,
        fileName: String,
        directoryPath: String,
        mimeType: String,
        headers: Map<String, String>,
        showNotification: Boolean,
        supportChunks: Boolean
    ) {
        val inProgressVideoDB = InProgressVideoDB(
            downloadId = id,
            url = url,
            fileName = fileName,
            destinationDirectory = directoryPath,
            mimeType = mimeType,
//            headers = headers,
            showNotification = showNotification,
            supportChunks = supportChunks,
            status = DownloadingState.Progress.name
        )
        progressManager.addLocalVideo(inProgressVideoDB)
    }




}
