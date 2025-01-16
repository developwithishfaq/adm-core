package com.adm.core

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.adm.core.DownloadingWorker.Companion.KEY_INPUT_DATA
import com.adm.core.DownloadingWorker.Companion.log
import com.adm.core.db.InProgressRepository
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.inject
import org.koin.core.component.inject

class NetConnectedWorker(private val context: Context,params:WorkerParameters):CoroutineWorker(context,params),KoinComponent {
    val inProgressRepository:InProgressRepository by inject()

    override suspend fun doWork(): Result {
        inProgressRepository.getInProgressQueVideosSingle().forEach {

            val workerDownloadingModel = WorkerDownloadingModel(
                id = it.downloadId.toString(),
                url = it.url,
                fileName = it.fileName,
                destinationDirectory = it.destinationDirectory,
                mimeType = it.mimeType,
//                headers = it.headers,
                showNotification = it.showNotification,
                supportChunks = it.supportChunks,
             )
            log("Tryin to start Download Worker")


            DownloadingWorker.startWorker(context,it.downloadId.toString(),workerDownloadingModel)

        }
        return Result.success()
    }

    companion object{
        fun startNetWorker(context: Context){
            val downloadingBuilder = OneTimeWorkRequestBuilder<NetConnectedWorker>()

             val constraints = Constraints.Builder()
                 .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            downloadingBuilder.setConstraints(constraints)
            downloadingBuilder.addTag("net")
            WorkManager.getInstance(context)
                .beginUniqueWork("net", ExistingWorkPolicy.REPLACE, downloadingBuilder.build())
                .enqueue()
        }
    }
}