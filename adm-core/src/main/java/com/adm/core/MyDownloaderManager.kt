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

        startService()


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

        CoroutineScope(Dispatchers.IO).launch {
            launch {
                WorkManager.getInstance(context).getWorkInfosByTagFlow(id.toString())
                    .collectLatest { workInfos ->
                        workInfos.forEach { workInfo ->
                            if (workInfo != null) {
                                when (workInfo.state) {
                                    WorkInfo.State.ENQUEUED -> {
                                        // Work is enqueued and waiting to be executed
                                        log("Work Enqueued")
                                    }

                                    WorkInfo.State.RUNNING -> {
                                        // Work is currently running
                                        val progress =
                                            workInfo.progress.getInt(
                                                "KEY_PROGRESS",
                                                0
                                            ) // Get progress if any
                                        log("Work Running - Progress: $progress%")
                                    }

                                    WorkInfo.State.SUCCEEDED -> {
                                        // Work completed successfully
                                        val outputData = workInfo.outputData
                                        log("Work Succeeded")
                                    }

                                    WorkInfo.State.FAILED -> {
                                        // Work failed
                                        val outputData = workInfo.outputData
                                        log("Work Failed $outputData ${workInfo.stopReason}")
                                    }

                                    WorkInfo.State.BLOCKED -> {
                                        // Work is blocked by other work
                                        log("Work Blocked")
                                    }

                                    WorkInfo.State.CANCELLED -> {
                                        // Work was cancelled
                                        log("Work Cancelled ${workInfo.stopReason}")
                                    }
                                }
                            }
                        }
                    }
            }
            launch {
                WorkManager.getInstance(context).getWorkInfosByTagFlow("net")
                    .collectLatest { workInfos ->
                        workInfos.forEach { workInfo ->
                            if (workInfo != null) {
                                when (workInfo.state) {
                                    WorkInfo.State.ENQUEUED -> {
                                        // Work is enqueued and waiting to be executed
                                        log("Net Work Enqueued")
                                    }

                                    WorkInfo.State.RUNNING -> {
                                        // Work is currently running
                                        val progress =
                                            workInfo.progress.getInt(
                                                "KEY_PROGRESS",
                                                0
                                            ) // Get progress if any
                                        log("Net Work Running - Progress: $progress%")
                                    }

                                    WorkInfo.State.SUCCEEDED -> {
                                        // Work completed successfully
                                        val outputData = workInfo.outputData
                                        log("Net Work Succeeded")
                                    }

                                    WorkInfo.State.FAILED -> {
                                        // Work failed
                                        val outputData = workInfo.outputData
                                        log("Net Work Failed $outputData ${workInfo.stopReason}")
                                    }

                                    WorkInfo.State.BLOCKED -> {
                                        // Work is blocked by other work
                                        log("Net Work Blocked")
                                    }

                                    WorkInfo.State.CANCELLED -> {
                                        // Work was cancelled
                                        log("Net Work Cancelled ${workInfo.stopReason}")
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }


    suspend fun pauseDownloading(id: Long) {
        progressManager.updateStatus(
            id.toString(),
            DownloadingState.Paused
        )
        WorkManager.getInstance(context).cancelUniqueWork(id.toString())
    }

    suspend fun resumeDownloading(id: Long) {
        val video=        inProgressRepository.getItemById(id)
        if (video!=null){

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

    fun deleteDownloading(id: Long) {

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
        inProgressRepository.addInQue(inProgressVideoDB)
    }

    private fun startService() {
        /*   val serviceIntent = Intent(context, DownloadService::class.java)
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
               ContextCompat.startForegroundService(context, serviceIntent)
           } else {
               context.startService(serviceIntent)
           }*/
    }


}
