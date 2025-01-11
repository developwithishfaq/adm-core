package com.adm.core.utils

import android.os.Environment
import java.io.File

object DownloaderPathsHelper {

    fun getDirInsideDownloads(folderName: String): String {
        val path =  if (folderName.startsWith("/")) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + folderName
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/" + folderName
        }
        File(path).mkdirs()
        return path
    }
}