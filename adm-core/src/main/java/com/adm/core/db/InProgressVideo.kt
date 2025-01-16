package com.adm.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class InProgressVideoDB(
    @PrimaryKey
    val downloadId: Long,
    val url: String = "",
    var fileName: String = "",
    val destinationDirectory: String = "",
    val mimeType: String = "",
    val headers: String?= null,
    val showNotification: Boolean = false,
    val supportChunks: Boolean = false,
    val status: String = "",
    val downloadedSize: Long = 0,
    val totalSize: Long = 0,
)