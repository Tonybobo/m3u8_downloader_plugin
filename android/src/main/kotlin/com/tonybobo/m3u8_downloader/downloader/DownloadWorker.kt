package com.tonybobo.m3u8_downloader.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tonybobo.m3u8_downloader.FlutterBackgroundExecutor
import com.tonybobo.m3u8_downloader.R
import com.tonybobo.m3u8_downloader.bean.M3U8
import com.tonybobo.m3u8_downloader.bean.M3U8Task
import com.tonybobo.m3u8_downloader.db.TaskDao
import com.tonybobo.m3u8_downloader.db.TaskDbHelper
import com.tonybobo.m3u8_downloader.listeners.OnTaskDownloadListener
import com.tonybobo.m3u8_downloader.utils.M3U8Log

class DownloadWorker(context: Context , params: WorkerParameters ) : Worker(context , params)  {
    private var dbHelper: TaskDbHelper?= null
    private var taskDao: TaskDao?=null
    private var m3u8Task : M3U8Task? = null
    private var primaryId = 0
    private var msgStarted: String? = null
    private var msgInProgress: String? = null
    private var msgCanceled: String? = null
    private var msgFailed: String? = null
    private var msgPaused: String? = null
    private var msgComplete: String? = null
    private var lastCallUpdateNotification: Long = 0
    private val notificationIconRes: Int
        get() {
            try {
                val applicationInfo: ApplicationInfo = applicationContext.packageManager
                    .getApplicationInfo(
                        applicationContext.packageName,
                        PackageManager.GET_META_DATA
                    )
                val appIconResId: Int = applicationInfo.icon
                return applicationInfo.metaData.getInt(
                    "com.tonybobo.m3u8_downloader.NOTIFICATION_ICON",
                    appIconResId
                )
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            return 0
        }
    private val backgroundExecutor:FlutterBackgroundExecutor = FlutterBackgroundExecutor()
    private val m3u8DownloadTask  = M3U8DownloadTask(context)
    private val taskListener = object : OnTaskDownloadListener{
        private var lastLength : Long = 0
        private var downloadProgress : Float = 0f
        override fun onStart() {
            val fileName = inputData.getString(ARG_FILE_NAME)
            val url = inputData.getString(ARG_URL)

            M3U8Log.d("Start Downloading: TaskId: $id ,fileName: $fileName , url: $url")
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onStartDownload(m3u8: M3U8, curTs: Int) {
            m3u8Task!!.m3u8 = m3u8
            val totalTs  = m3u8.tsList.size
            val lastProgress = inputData.getFloat(ARG_LAST_PROGRESS , 0f)
            val fileName = inputData.getString(ARG_FILE_NAME)

            M3U8Log.d("onStartDownload: $curTs/$totalTs")
            if (totalTs > 0) downloadProgress = 1.0f * curTs/totalTs
            if (lastProgress > downloadProgress) downloadProgress = lastProgress

            updateNotification(applicationContext , fileName!! , DownloadStatus.RUNNING  ,downloadProgress.toInt() , m3u8Task!!.getFormatTotalSize() )
            backgroundExecutor.sendUpdateProcessEvent(id , DownloadStatus.RUNNING , m3u8Task!!.getFormatTotalSize(), downloadProgress , inputData , context)
        }

        override fun onDownloadItem(itemFileSize: Long, totalTs: Int, curTs: Int ) {
            if(!m3u8DownloadTask.isRunning) return
            if(totalTs > 0) downloadProgress = 1.0f * curTs/totalTs

            M3U8Log.d("onDownloadItem: $itemFileSize / ${m3u8Task!!.getTotalSize()} | $curTs / $totalTs  ,  downloadProgress:  $downloadProgress  , formatSize: ${m3u8Task!!.getFormatCurrentSize()}/${m3u8Task!!.getFormatTotalSize()} , speed: ${m3u8Task!!.getFormatSpeed()}")
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onProgress(curLength: Long) {
            if(curLength - lastLength > 0) {
                M3U8Log.d("Send Event to background Channel")
                m3u8Task!!.progress = downloadProgress
                m3u8Task!!.speed = curLength-lastLength
                val fileSize = m3u8Task!!.getFormatTotalSize()
                backgroundExecutor.sendUpdateProcessEvent(id , DownloadStatus.RUNNING ,fileSize, downloadProgress , inputData , context )
                updateNotification(applicationContext , inputData.getString(ARG_FILE_NAME)!! ,DownloadStatus.RUNNING, (downloadProgress * 100).toInt() , m3u8Task!!.getFormatTotalSize() )
                lastLength = curLength
            }
        }

        override fun onConvert() {
            val fileName = inputData.getString(ARG_FILE_NAME)
            val url = inputData.getString(ARG_URL)
            M3U8Log.d("Converting To MP4 : TaskId:  $id  FileName: $fileName , URL : $url")
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onSuccess(m3U8: M3U8) {
            val fileName = inputData.getString(ARG_FILE_NAME)
            dbHelper = TaskDbHelper.getInstance(context)
            taskDao = TaskDao(dbHelper!!)
            taskDao!!.updateTask(id.toString() , DownloadStatus.COMPLETED , downloadProgress.toInt() )
            backgroundExecutor.sendUpdateProcessEvent(id , DownloadStatus.COMPLETED ,m3u8Task!!.getFormatTotalSize(), downloadProgress , inputData , context )
            updateNotification(applicationContext , fileName!! ,DownloadStatus.COMPLETED, (downloadProgress * 100).toInt()  , m3u8Task!!.getFormatTotalSize() )
            M3U8Log.d("Success : TaskId: $id , FileName : $fileName ")
        }

        override fun onError(error: Throwable) {
            error.printStackTrace()
            dbHelper = TaskDbHelper.getInstance(context)
            taskDao = TaskDao(dbHelper!!)
            taskDao!!.updateTask(id.toString(), DownloadStatus.FAILED , downloadProgress.toInt()  )
            M3U8Log.e("onError: ${error.message}")
            backgroundExecutor.sendUpdateProcessEvent(id , DownloadStatus.FAILED , m3u8Task!!.getFormatTotalSize(), downloadProgress , inputData , context )
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onStop() {
            val fileName = inputData.getString(ARG_FILE_NAME)
            dbHelper = TaskDbHelper.getInstance(context)
            taskDao = TaskDao(dbHelper!!)
            taskDao!!.updateTask(id.toString() , DownloadStatus.PAUSED , downloadProgress.toInt() )
            M3U8Log.d("Paused Task id: $id , progress: $downloadProgress " )
            updateNotification(applicationContext , fileName!! ,DownloadStatus.CANCELED, -1 , m3u8Task!!.getFormatTotalSize() )
            backgroundExecutor.sendUpdateProcessEvent(id , DownloadStatus.PAUSED , m3u8Task!!.getFormatTotalSize(), downloadProgress , inputData , context )
        }

    }

    companion object{
        const val ARG_URL = "url"
        const val ARG_FILE_NAME = "file_name"
        const val ARG_CALLBACK_HANDLE = "callback_handle"
        const val ARG_DEBUG = "debug"
        const val ARG_STEP = "step"
        const val ARG_LAST_PROGRESS = "last_progress"
        private const val CHANNEL_ID= "M3U8_DOWNLOADER_NOTIFICATION"
    }

    init {
        Handler(context.mainLooper).post {
            backgroundExecutor.startBackgroundIsolate(context)
        }
    }

    override fun doWork(): Result {
        val fileName = inputData.getString(ARG_FILE_NAME)
        val url = inputData.getString(ARG_URL)
        dbHelper = TaskDbHelper.getInstance(applicationContext)
        taskDao = TaskDao(dbHelper!!)
        val task = taskDao!!.loadTask(id.toString())
        primaryId = task!!.primaryId
        if (url != null) {
            m3u8Task = M3U8Task(url)
            M3U8Log.d("DO WORK ::: fileName = $fileName , url = $url  , taskId = $id")
            if (fileName != null) {
                m3u8DownloadTask.download(url , taskListener , fileName)
            }
        }
        val res = applicationContext.resources
        msgStarted = res.getString(R.string.m3u8_downloader_notification_started)
        msgInProgress = res.getString(R.string.m3u8_downloader_notification_in_progress)
        msgCanceled = res.getString(R.string.m3u8_downloader_notification_canceled)
        msgFailed = res.getString(R.string.m3u8_downloader_notification_failed)
        msgPaused = res.getString(R.string.m3u8_downloader_notification_paused)
        msgComplete = res.getString(R.string.m3u8_downloader_notification_complete)
        setUpNotification(applicationContext)
        return Result.success()
    }

    private fun setUpNotification(context:Context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val res = applicationContext.resources
            val channelName:String = res.getString(R.string.m3u8_downloader_notification_channel_name)
            val channelDescription: String = res.getString(R.string.m3u8_downloader_notification_channel_description)
            val importance:Int = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID , channelName , importance)
            channel.description = channelDescription
            channel.setSound(null , null)

            val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)
            notificationManager.createNotificationChannel(channel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun updateNotification(context: Context, title:String, status: DownloadStatus, progress: Int , fileSize:String ){
        val builder = NotificationCompat.Builder(context, CHANNEL_ID).setContentTitle(title)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        when (status) {
            DownloadStatus.RUNNING -> {
                if (progress <= 0) {
                    builder.setContentText(msgStarted)
                        .setProgress(0, 0, true)
                    builder.setOngoing(true)
                        .setSmallIcon(notificationIconRes)
                    builder.setSubText(fileSize)
                } else if (progress < 100) {
                    builder.setContentText(msgInProgress)
                        .setProgress(100, progress, false)
                    builder.setOngoing(true)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                    builder.setSubText(fileSize)
                } else {
                    builder.setContentText(msgComplete).setProgress(0, 0, false)
                    builder.setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    builder.setSubText(fileSize)
                }
            }

            DownloadStatus.CANCELED -> {
                builder.setContentText(msgCanceled).setProgress(0, 0, false)
                builder.setOngoing(false)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.setSubText(fileSize)
            }

            DownloadStatus.FAILED -> {
                builder.setContentText(msgFailed).setProgress(0, 0, false)
                builder.setOngoing(false)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.setSubText(fileSize)
            }

            DownloadStatus.PAUSED -> {
                builder.setContentText(msgPaused).setProgress(0, 0, false)
                builder.setOngoing(false)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.setSubText(fileSize)
            }

            DownloadStatus.COMPLETED -> {
                builder.setContentText(msgComplete).setProgress(0, 0, false)
                builder.setOngoing(false)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.setSubText(fileSize)
            }

            else -> {
                builder.setProgress(0, 0, false)
                builder.setOngoing(false).setSmallIcon(notificationIconRes)
            }
        }
        NotificationManagerCompat.from(context).notify(primaryId, builder.build())
        lastCallUpdateNotification = System.currentTimeMillis()
    }


    override fun onStopped() {
        m3u8DownloadTask.stop()
        M3U8Log.d("$id Task Stopped")
    }

}


