package com.tonybobo.m3u8_downloader.downloader

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.provider.MediaStore.Video.Media
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tonybobo.m3u8_downloader.FlutterBackgroundExecutor
import com.tonybobo.m3u8_downloader.bean.M3U8
import com.tonybobo.m3u8_downloader.bean.M3U8Task
import com.tonybobo.m3u8_downloader.db.TaskDao
import com.tonybobo.m3u8_downloader.db.TaskDbHelper
import com.tonybobo.m3u8_downloader.listeners.OnTaskDownloadListener
import com.tonybobo.m3u8_downloader.utils.M3U8Log
import com.tonybobo.m3u8_downloader.utils.M3U8Util
import java.io.File

class DownloadWorker(context: Context , params: WorkerParameters ) : Worker(context , params)  {
    private var dbHelper: TaskDbHelper?= null
    private var taskDao: TaskDao?=null
    private var m3u8Task : M3U8Task? = null
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

        override fun onStartDownload(m3u8: M3U8, curTs: Int) {
            m3u8Task!!.m3u8 = m3u8
            val totalTs  = m3u8.tsList.size
            val lastProgress = inputData.getFloat(ARG_LAST_PROGRESS , 0f)

            M3U8Log.d("onStartDownload: $curTs/$totalTs")
            if (totalTs > 0) downloadProgress = 1.0f * curTs/totalTs
            if (lastProgress > downloadProgress) downloadProgress = lastProgress

            backgroundExecutor.sendUpdateProcessEvent(id , DownloadStatus.RUNNING , downloadProgress , inputData , context)
        }

        override fun onDownloadItem(itemFileSize: Long, totalTs: Int, curTs: Int ) {
            // save last downloaded ts filename for resuming
            if(!m3u8DownloadTask.isRunning) return
            if(totalTs > 0) downloadProgress = 1.0f * curTs/totalTs

            M3U8Log.d("onDownloadItem: $itemFileSize / ${m3u8Task!!.getTotalSize()} | $curTs / $totalTs  ,  downloadProgress:  $downloadProgress ")
        }

        override fun onProgress(curLength: Long) {
            // use a timer to send message periodically
            if(curLength - lastLength > 0) {
                M3U8Log.d("Send Event to background Channel")
                backgroundExecutor.sendUpdateProcessEvent(id , DownloadStatus.RUNNING , downloadProgress , inputData , context )
                lastLength = curLength
            }
        }

        override fun onConvert() {
            val fileName = inputData.getString(ARG_FILE_NAME)
            val url = inputData.getString(ARG_URL)
            M3U8Log.d("Converting To MP4 : TaskId:  $id  FileName: $fileName , URL : $url")
        }

        override fun onSuccess(m3U8: M3U8) {
            val fileName = inputData.getString(ARG_FILE_NAME)
            dbHelper = TaskDbHelper.getInstance(context)
            taskDao = TaskDao(dbHelper!!)
            taskDao!!.updateTask(id.toString() , DownloadStatus.COMPLETED , downloadProgress.toInt() )
            backgroundExecutor.sendUpdateProcessEvent(id , DownloadStatus.COMPLETED , downloadProgress , inputData , context )
            M3U8Log.d("Success : TaskId: $id , FileName : $fileName ")
        }

        override fun onError(error: Throwable) {
            error.printStackTrace()
            dbHelper = TaskDbHelper.getInstance(context)
            taskDao = TaskDao(dbHelper!!)
            taskDao!!.updateTask(id.toString(), DownloadStatus.FAILED , downloadProgress.toInt()  )
            M3U8Log.e("onError: ${error.message}")
            backgroundExecutor.sendUpdateProcessEvent(id , DownloadStatus.FAILED , downloadProgress , inputData , context )
        }

        override fun onStop() {
            dbHelper = TaskDbHelper.getInstance(context)
            taskDao = TaskDao(dbHelper!!)
            taskDao!!.updateTask(id.toString() , DownloadStatus.PAUSED , downloadProgress.toInt() )
            M3U8Log.d("Paused Task id: $id , progress: $downloadProgress " )
            backgroundExecutor.sendUpdateProcessEvent(id , DownloadStatus.PAUSED , downloadProgress , inputData , context )
        }

    }

    companion object{
        const val ARG_URL = "url"
        const val ARG_FILE_NAME = "file_name"
        const val ARG_CALLBACK_HANDLE = "callback_handle"
        const val ARG_DEBUG = "debug"
        const val ARG_STEP = "step"
        const val ARG_LAST_PROGRESS = "last_progress"
    }

    init {
        Handler(context.mainLooper).post {
            backgroundExecutor.startBackgroundIsolate(context)
        }
    }

    override fun doWork(): Result {
        val fileName = inputData.getString(ARG_FILE_NAME)
        val url = inputData.getString(ARG_URL)
        if (url != null) {
            m3u8Task = M3U8Task(url)
            M3U8Log.d("DO WORK ::: fileName = $fileName , url = $url  , taskId = $id")
            if (fileName != null) {
                m3u8DownloadTask.download(url , taskListener , fileName)
            }
        }
        return Result.success()
    }

    private fun addVideoToGallery(fileName: String){
        val filePath = M3U8Util.getSaveFileDir(fileName)
        val mp4Path = File("$filePath.mp4")
        val values = ContentValues()
        if(Build.VERSION.SDK_INT >= 29){
            val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            values.put(MediaStore.Video.Media.TITLE , fileName)
            values.put(MediaStore.Video.Media.DISPLAY_NAME , fileName)
            values.put(MediaStore.Video.Media.DESCRIPTION , "")
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            values.put(MediaStore.Video.Media.DATE_ADDED , System.currentTimeMillis())
            values.put(MediaStore.Video.Media.DATE_TAKEN , System.currentTimeMillis())
            values.put(MediaStore.Downloads.RELATIVE_PATH , Environment.DIRECTORY_DOWNLOADS)
            val contentResolver = applicationContext.contentResolver
            val url = contentResolver.insert(uri, values)
            M3U8Log.d("Add Video To Gallery ,uri: $uri  , url:$url")
        }else {
            values.put(MediaStore.Video.Media.TITLE , fileName)
            values.put(MediaStore.Video.Media.DISPLAY_NAME , fileName)
            values.put(MediaStore.Video.Media.DESCRIPTION , "")
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            values.put(MediaStore.Video.Media.DATE_ADDED , System.currentTimeMillis())
            values.put(MediaStore.Video.Media.DATE_TAKEN , System.currentTimeMillis())
            values.put(MediaStore.Video.Media.DATA , mp4Path.absolutePath)
            val contentResolver = applicationContext.contentResolver
            contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI , values)
        }
    }

    override fun onStopped() {
        m3u8DownloadTask.stop()
        M3U8Log.d("$id Task Stopped")
    }

}


