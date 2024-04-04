package com.tonybobo.m3u8_downloader.downloader

import android.content.Context
import android.os.Handler
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tonybobo.m3u8_downloader.FlutterBackgroundExecutor
import com.tonybobo.m3u8_downloader.bean.M3U8
import com.tonybobo.m3u8_downloader.bean.M3U8Task
import com.tonybobo.m3u8_downloader.db.TaskDao
import com.tonybobo.m3u8_downloader.db.TaskDbHelper
import com.tonybobo.m3u8_downloader.listeners.OnTaskDownloadListener
import com.tonybobo.m3u8_downloader.utils.M3U8Log

class DownloadWorker(context: Context , params: WorkerParameters ) : Worker(context , params)  {
    private var dbHelper: TaskDbHelper?= null
    private var taskDao: TaskDao?=null
    private var lastProgress = 0
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
            M3U8Log.d("onStartDownload: $curTs/$totalTs")
            if (totalTs > 0) downloadProgress = 1.0f * curTs/totalTs
        }

        override fun onDownloadItem(itemFileSize: Long, totalTs: Int, curTs: Int) {
            // Plan to execute flutter background executor. But it may send a lot of message as TS files are normally small;
            // Currently it will be used as a monitor and to update progress
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
            taskDao!!.updateTask(id.toString() , DownloadStatus.COMPLETED , downloadProgress.toInt())
            M3U8Log.d("Success : TaskId: $id , FileName : $fileName ")
        }

        override fun onError(error: Throwable) {
            error.printStackTrace()
            dbHelper = TaskDbHelper.getInstance(context)
            taskDao = TaskDao(dbHelper!!)
            taskDao!!.updateTask(id.toString(), DownloadStatus.FAILED , downloadProgress.toInt())
            M3U8Log.e("onError: ${error.message}")
        }

    }

    companion object{
        const val ARG_URL = "url"
        const val ARG_FILE_NAME = "file_name"
        const val ARG_CALLBACK_HANDLE = "callback_handle"
        const val ARG_DEBUG = "debug"
        const val ARG_STEP = "step"

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
            m3u8DownloadTask.download(url , taskListener)
        }
        return Result.success()
    }

    // Background process update UI Main Thread


    override fun onStopped() {
       val context : Context = applicationContext
       dbHelper = TaskDbHelper.getInstance(context)
        taskDao = TaskDao(dbHelper!!)
        val task = taskDao?.loadTask(id.toString())
        if(task != null && task.status == DownloadStatus.ENQUEUED){
            taskDao?.updateTask(id.toString() , DownloadStatus.CANCELED , lastProgress)
        }
    }

}


