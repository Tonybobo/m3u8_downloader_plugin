package com.tonybobo.m3u8_downloader.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tonybobo.m3u8_downloader.R
import com.tonybobo.m3u8_downloader.downloader.DownloadStatus

class Notification {
    companion object{
        private const val CHANNEL_ID = "M3U8_DOWNLOADER_NOTIFICATION"

        @SuppressLint("StaticFieldLeak")
        private var builder : NotificationCompat.Builder? = null

    }

    private var context:Context? = null

    fun build(c:Context){
        context = c
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val res = c.applicationContext.resources
            val channelName:String = res.getString(R.string.m3u8_downloader_notification_channel_name)
            val channelDescription:String = res.getString(R.string.m3u8_downloader_notification_channel_description)
            val importance:Int = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID , channelName , importance)
            channel.description = channelDescription
            channel.setSound(null , null)

            val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(c)
            notificationManager.createNotificationChannel(channel)
        }
        builder = NotificationCompat.Builder(c, CHANNEL_ID)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

    }

    @SuppressLint("SuspiciousIndentation")
    fun updateNotification(primaryId: Int, title: String?, status: DownloadStatus, progress: Int, intent: PendingIntent?, finalize: Boolean){
        if (builder == null) return
            when (status){
                DownloadStatus.RUNNING -> {
                    if(progress <= 0){
                        builder!!.setContentText("Pending Downloading")
                            .setProgress(0 , 0 , false)
                        builder!!.setOngoing(false)
                            .setSmallIcon(android.R.drawable.stat_sys_download)
                    }else if(progress < 100){
                        builder!!.setContentText("Downloading")
                            .setProgress(100 , progress , false)
                        builder!!.setOngoing(true)
                            .setSmallIcon(android.R.drawable.stat_sys_download)
                    }else{
                        builder!!.setContentText("$title Completed")
                            .setProgress(0 , 0 , false)
                        builder!!.setOngoing(false)
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    }
                }

                DownloadStatus.CANCELED -> {
                    builder!!.setContentText("Task Cancel")
                        .setProgress(0 , 0 , false)
                    builder!!.setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                }

                DownloadStatus.FAILED -> {
                    builder!!.setContentText("Task Failed")
                        .setProgress(0 , 0 , false)
                    builder!!.setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                }

                DownloadStatus.PAUSED -> {
                    builder!!.setContentText("Task Paused")
                        .setProgress(0 , 0 , false)
                    builder!!.setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                }

                DownloadStatus.COMPLETED -> {
                    builder!!.setContentText("$title Completed")
                        .setProgress(0 , 0 , false)
                    builder!!.setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                }

                else -> {
                    builder!!.setProgress(0 , 0 , false)
                    builder!!.setOngoing(false).setSmallIcon(android.R.drawable.stat_sys_download)
                }
            }

            M3U8Log.d("Update Notification: { title : $title , status: $status , progress: $progress}")
            NotificationManagerCompat.from(context!!).notify(primaryId , builder!!.build())
        }
}