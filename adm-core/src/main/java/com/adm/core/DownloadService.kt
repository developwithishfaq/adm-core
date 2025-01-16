package com.adm.core

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class DownloadService : Service() {

    private val progressManager: ProgressManager by inject()
    private val channelId = "download_channel"
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(
            this
        )
    }
    var isCreated = false


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startDownload()
    }


    val SERVICE_NOTIFICATION_ID=123
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotificationBuilder(  "Starting service").build() // Initial notification

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, SERVICE_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, notification)
        }
         return START_STICKY
    }


    @SuppressLint("MissingPermission")
    fun startDownload() {

        CoroutineScope(Dispatchers.IO).launch {
            progressManager.videosProgress.collectLatest { list ->
                list.forEach {
                    val builder = createNotificationBuilder(it.fileName)
                    if (!isCreated) {
                        if (isNotiPermissionGranted()) {
                            notificationManager.notify(it.id.toLong().toInt(), builder.build())
                        }
                        isCreated = true
                    }

                    updateNotification(it.id.toLong().toInt(), (it.progress * 100).toInt(), builder)

                }

            }

        }
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(
        downloadId: Int,
        progress: Int,
        builder: NotificationCompat.Builder,
        message: String? = null
    ) {
        builder.setProgress(100, progress, progress < 0)
        builder.setContentText(message ?: "Downloading...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isNotiPermissionGranted()) {
                notificationManager.notify(downloadId, builder.build())
            }
        }

    }

    private fun createNotificationBuilder(
        title: String,
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download) // Replace with your icon
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setGroup("downloads") // Optional: Group notifications
            .setGroupSummary(false) // Only first notification is summary
            .setProgress(100, 0, false) // Initial progress
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Download Channel"
            val descriptionText = "Channel for download notifications"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}

fun Context.isNotiPermissionGranted(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PERMISSION_GRANTED
}