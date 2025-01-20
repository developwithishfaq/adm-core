package com.adm.core

import android.util.Log
import com.adm.core.components.DownloadingState
import com.adm.core.components.getDownloadingStatus
import com.adm.core.db.InProgressRepository
import com.adm.core.db.InProgressVideoDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


data class InProgressVideoUi(
    val id: String,
    val url: String = "",
    var fileName: String = "",
    val destinationDirectory: String = "",
    val mimeType: String = "",
    val status: DownloadingState = DownloadingState.Idle,
    val downloadedSize: Long = 0,
    val totalSize: Long = 0,
    val progress: Float = 0f
)

@OptIn(FlowPreview::class)
class ProgressManager(
    private val inProgressRepository: InProgressRepository
) {
    val scope = CoroutineScope(Dispatchers.IO)

    val inprogressMap = mutableMapOf<String, InProgressVideoUi>()

    private val mapMutex = Mutex()
    private val _videosProgress = MutableStateFlow<List<InProgressVideoUi>>(emptyList())
    val videosProgress = _videosProgress.asStateFlow()

    init {

        scope.launch {
            _videosProgress.update {
                inProgressRepository.getAllQueVideosSingle().map {
                    val model = it.toUiModel()
                    inprogressMap[model.id] = model
                    model
                }
            }
            videosProgress
                .sample(1000)
                .collectLatest {
                    batchUpdateDatabase()
                    Log.d("cvrrr", "batchUpdateDatabase")
                }
        }
    }

    suspend fun updateProgress(id: String, downloaded: Long, total: Long) {
        Log.d(
            "cvrrr",
            "update Progress,  id=$id progress=${downloaded / total.toFloat()}} downloaded= $downloaded ${total}"
        )


        mapMutex.withLock {
            inprogressMap[id]?.let { video ->
                val updatedVideo = video.copy(
                    downloadedSize = downloaded,
                    totalSize = total,
                    progress = downloaded / total.toFloat(),
                )
                inprogressMap[id] = updatedVideo
                emitProgressUpdates()
             }
        }
    }

    suspend fun updateStatus(id: String, downState: DownloadingState) {
        mapMutex.withLock {
            inprogressMap[id]?.let { video ->
                val updatedVideo = video.copy(
                    status = downState
                )
                inprogressMap[id] = updatedVideo
                emitProgressUpdates()

            }
        }
    }

    suspend fun deleteVideo(id: String) {
        inprogressMap.remove(id.toString())
        inProgressRepository.deleteFromQue(id.toLong())
    }
    suspend fun addLocalVideo(db: InProgressVideoDB) {
        val video = inProgressRepository.getItemById(db.downloadId)
        if (video == null) {
            Log.d("cvv", "addLocalVideo $video")
            inProgressRepository.addInQue(db)
            inprogressMap[db.downloadId.toString()] = db.toUiModel()
            emitProgressUpdates()

        }
    }


    private suspend fun batchUpdateDatabase() {
        mapMutex.withLock {
            val videosToUpdate = inprogressMap.values.toList()
            val dbMap: Map<String, InProgressVideoDB> = inProgressRepository.getInProgressQueVideosSingle()
                .associateBy { it.downloadId.toString() }

            if (videosToUpdate.isNotEmpty()) {
                videosToUpdate.forEach { uiModel ->
                    val inProgressVideo= dbMap[uiModel.id]
                    if (inProgressVideo != null ) {
                        val inProgressVideoNew = inProgressVideo.copy(
                            status = uiModel.status.name,
                            downloadedSize = uiModel.downloadedSize,
                            totalSize = uiModel.totalSize,
                        )
                        if (inProgressVideoNew!=inProgressVideo){
                            inProgressRepository.addInQue(inProgressVideoNew)
                        }
                    }
                 }
            }
        }
    }

    fun emitProgressUpdates() {
        _videosProgress.value = inprogressMap.values.toList()

    }

//
//    val videosProgress = inProgressRepository.getAllQueVideos().map {
//        it.map {
//            InProgressVideoUi(
//                id = it.downloadId.toString(),
//                url = it.url,
//                fileName = it.fileName,
//                destinationDirectory = it.destinationDirectory,
//                mimeType = it.mimeType,
//                status = it.status.getDownloadingStatus(),
//                downloadedSize = it.downloadedSize,
//                totalSize = it.totalSize,
//                progress = it.downloadedSize / it.totalSize.toFloat(),
//            )
//        }
//    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
}

fun InProgressVideoUi.toInProgressDB(model: InProgressVideoDB): InProgressVideoDB {
    return model.copy(
        status = this.status.name,
        downloadedSize = this.downloadedSize,
        totalSize = this.totalSize
    )
}

fun InProgressVideoDB.toUiModel(): InProgressVideoUi {
    return InProgressVideoUi(
        id = this.downloadId.toString(),
        url = this.url,
        fileName = this.fileName,
        destinationDirectory = this.destinationDirectory,
        mimeType = this.mimeType,
        status = this.status.getDownloadingStatus(),
        downloadedSize = this.downloadedSize,
        totalSize = this.totalSize,
        progress = this.downloadedSize / this.totalSize.toFloat(),
    )
}