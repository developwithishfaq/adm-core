package com.adm.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class DownloadNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val TAG="DownloadNotificationManager"
//        const val NOTIFICATION_ID = 1
    }

    fun log(msg:String){
        Log.d(TAG,msg)
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows notifications for file downloads"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createDownloadingNotification(fileName: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(fileName)
             .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setGroup("downloads")
            .setProgress(100, 0, false)
    }

    fun updateProgress(notificationBuilder: NotificationCompat.Builder,id:Int, progress: Int) {
        notificationBuilder.setProgress(100, progress, false)
        notificationManager.notify(id, notificationBuilder.build())
        log("updateProgress id= $id")

    }

    fun showDownloadFailedNotification(id:Int,fileName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download Failed")
            .setContentText("Failed to download $fileName")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .build()

        notificationManager.notify(id, notification)
    }
fun showDownloadSuccessNotification(id:Int,fileName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download Complete")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .build()

        notificationManager.notify(id, notification)
    }

    fun cancelNotification(id:Int) {
        notificationManager.cancel(id)
    }
}