package com.tonybobo.m3u8_downloader

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.OnScanCompletedListener
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.os.IResultReceiver._Parcel
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.tonybobo.m3u8_downloader.db.TaskDao
import com.tonybobo.m3u8_downloader.db.TaskDbHelper
import com.tonybobo.m3u8_downloader.downloader.DownloadStatus
import com.tonybobo.m3u8_downloader.downloader.DownloadWorker
import com.tonybobo.m3u8_downloader.downloader.M3U8DownloadConfig
import com.tonybobo.m3u8_downloader.utils.M3U8Log
import com.tonybobo.m3u8_downloader.utils.M3U8Util

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

import kotlin.random.Random


class M3u8DownloaderPlugin: MethodChannel.MethodCallHandler , FlutterPlugin {
    private var flutterChannel: MethodChannel? = null
    private var taskDao : TaskDao? = null
    private var context: Context?= null
    private var callbackHandle: Long = 0
    private var step = 0
    private var initializationLock = Any()

    companion object{
        private const val CHANNEL = "com.tonybobo.m3u8_downloader"
        private const val TAG = "m3u8_download_task"
    }

    private fun onAttachedToEngine(applicationContext: Context? , messenger: BinaryMessenger){
        synchronized(initializationLock){
             if(flutterChannel != null){
                 return
             }
            context = applicationContext
            flutterChannel = MethodChannel(messenger, CHANNEL)
            flutterChannel?.setMethodCallHandler(this)
            val dbHelper: TaskDbHelper = TaskDbHelper.getInstance(context)
            taskDao = TaskDao(dbHelper)
            M3U8DownloadConfig.build(context!!)
        }
    }
  

    override fun onAttachedToEngine( flutterPluginBinding: FlutterPluginBinding) {
        onAttachedToEngine(flutterPluginBinding.applicationContext , flutterPluginBinding.binaryMessenger)
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        context = null
        flutterChannel?.setMethodCallHandler(null)
        flutterChannel = null
    }

    private fun requireContext() = requireNotNull(context)

    private fun buildRequest(
        url:String?,
        filename: String?,
        lastProgress: Float = 0.0f,
    ): WorkRequest {
        return OneTimeWorkRequest.Builder(DownloadWorker::class.java)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresStorageNotLow(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED )
                    .build()
            )
            .addTag(TAG)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10 , TimeUnit.SECONDS)
            .setInputData(
                Data.Builder()
                    .putString(DownloadWorker.ARG_URL , url)
                    .putString(DownloadWorker.ARG_FILE_NAME , filename)
                    .putLong(DownloadWorker.ARG_CALLBACK_HANDLE, callbackHandle)
                    .putInt(DownloadWorker.ARG_STEP , step)
                    .putBoolean(DownloadWorker.ARG_DEBUG , M3U8DownloadConfig.getDebugMode())
                    .putFloat(DownloadWorker.ARG_LAST_PROGRESS , lastProgress)
                    .build()
            )
            .build()
    }

   override fun onMethodCall( call: MethodCall,  result: Result) {
        when (call.method) {
            "getRandomNumber" -> {
                val rand = Random.nextInt(100)
                result.success(rand)
            }
            "initialize" -> initialize(call, result)
            "config" -> config(call , result)
            "loadTasks" -> loadTasks(result)
            "loadTasksWithRawQuery" -> loadTasksWithRawQuery(call , result)
            "registerCallback" -> registerCallback(call, result)
            "enqueue" -> enqueue(call, result)
            "pause" -> pause(call , result)
            "resume" -> resume(call, result)
//            "retry" -> retry(call ,result)
//            "open" -> open(call , result)
            "remove" -> remove(call ,result)
            else -> result.notImplemented()
        }
    }

    private fun initialize(call: MethodCall , result: Result){
        val args = call.arguments as List<*>
        val callbackHandle = args[0].toString().toLong()
        val debugMode = args[1].toString().toInt()
        M3U8Log.d("background callback handle: $callbackHandle , debug: $debugMode")
        M3U8DownloadConfig.setDebugMode(debugMode == 1)
        M3U8DownloadConfig.setCallbackHandle(callbackHandle)

        result.success(null)
    }

    private fun config(call: MethodCall , result: Result){
        val saveDir = call.requireArgument<String>("saveDir")
        val connTime = call.requireArgument<Int>("connTimeout")
        val readTime = call.requireArgument<Int>("readTimeout")

        M3U8DownloadConfig.setReadTimeout(readTime)
        M3U8DownloadConfig.setConnTimeout(connTime)
        M3U8DownloadConfig.setSaveDir(saveDir)

        result.success(true)
    }

    private fun <T> MethodCall.requireArgument(key: String): T = requireNotNull(argument(key)){
        "Required key '$key' was null"
    }

    private fun loadTasks(result: Result){
        val tasks = taskDao!!.loadAllTasks()
        val array: MutableList<Map<*,*>> = ArrayList()
        for (task in tasks) {
            val item: MutableMap<String , Any?> = HashMap()
            item["task_id"] = task.taskId
            item["status"] = task.status.ordinal
            item["url"] = task.url
            item["progress"] = task.progress
            item["file_name"] = task.filename
            item["time_created"] = task.timeCreated
            array.add(item)
        }
       result.success(array)
    }
    private fun loadTasksWithRawQuery(call: MethodCall , result: Result){
        val query:String = call.requireArgument("query")
        val tasks = taskDao!!.loadTasksWithRawQuery(query)
        val array: MutableList<Map<*,*>> = ArrayList()
        for (task in tasks) {
            val item: MutableMap<String , Any?> = HashMap()
            item["task_id"] = task.taskId
            item["status"] = task.status.ordinal
            item["url"] = task.url
            item["progress"] = task.progress
            item["file_name"] = task.filename
            item["time_created"] = task.timeCreated
            array.add(item)
        }
        result.success(array)
    }

    private fun registerCallback(call: MethodCall , result: Result){
        val args = call.arguments as List<*>
        callbackHandle = args[0].toString().toLong()
        step = args[1].toString().toInt()
        M3U8Log.d("RegisterCallbackHandle: $callbackHandle , step:$step")
        result.success(null)
    }

    private fun enqueue(call: MethodCall, result: Result){
        val url: String = call.requireArgument("url")
        val filename: String = call.requireArgument("filename")
        val request: WorkRequest = buildRequest(
            url,
            filename,
        )
        WorkManager.getInstance(requireContext()).enqueue(request)
        val taskId:String = request.id.toString()
        result.success(taskId)
        taskDao!!.insertOrUpdateNewTask(
            taskId,
            DownloadStatus.ENQUEUED,
            url,
            0,
            filename,
        )
    }

    private fun pause(call: MethodCall , result: Result){
        val taskId:String = call.requireArgument("task_id")
        taskDao!!.updateTask(taskId , DownloadStatus.PAUSED )
        WorkManager.getInstance(requireContext()).cancelWorkById(UUID.fromString(taskId))
        result.success(null)
    }

    private fun resume(call: MethodCall , result: Result){
       val taskId:String = call.requireArgument("task_id")
        val task = taskDao!!.loadTask(taskId)
        if(task != null){
           if(task.status == DownloadStatus.PAUSED){
               val request: WorkRequest = buildRequest(
                       task.url,
                       task.filename,
                       task.progress.toFloat()
                   )
                   val newTaskId:String = request.id.toString()
                   result.success(newTaskId)
                   taskDao!!.updateTask(taskId,newTaskId,DownloadStatus.RUNNING)
                   WorkManager.getInstance(requireContext()).enqueue(request)
               }else{
                   taskDao!!.updateTask(taskId,DownloadStatus.FAILED)
                   result.error(
                       taskId,
                       "Task is not re-summable ",
                       null
                   )
               }
           }else{
               result.error(taskId , "TaskId cannot be found" , null)
           }
        }

//    private fun retry(call: MethodCall, result: Result){
//      val taskId: String = call.requireArgument("task_id")
//        val task = taskDao!!.loadTask(taskId)
//        val requiresStorageNotLow: Boolean = call.requireArgument("requires_storage_not_low")
//        val timeout:Int = call.requireArgument("timeout")
//        if(task != null){
//            if(task.status == DownloadStatus.FAILED || task.status == DownloadStatus.CANCELED){
//                val request: WorkRequest = buildRequest(
//                    task.url,
//                    task.savedDir,
//                    task.filename,
//                    task.headers,
//                    task.showNotification,
//                    task.openFileFromNotification,
//                    false,
//                    requiresStorageNotLow,
//                    task.saveInPublicStorage,
//                    timeout,
//                    allowCellular = task.allowCellular
//                )
//                val newTaskId:String = request.id.toString()
//                result.success(newTaskId)
//                sendUpdateProgress(newTaskId, DownloadStatus.RUNNING , task.progress)
//                taskDao!!.updateTask(taskId,newTaskId,DownloadStatus.RUNNING,task.progress,false)
//                WorkManager.getInstance(requireContext()).enqueue(request)
//            }else{
//                result.error(invalidStatus, "only failed and canceled task" , null)
//            }
//        }else{
//            result.error(invalidTaskId , "not found task corresponding to given task id" , null)
//        }
//    }

//    private fun open(call: MethodCall , result: Result){
//       val taskId:String = call.requireArgument("task_id")
//        val task = taskDao!!.loadTask(taskId)
//        if(task == null){
//            result.error(invalidTaskId, "not found task with id $taskId", null)
//            return
//        }
//
//        if(task.status != DownloadStatus.COMPLETED){
//            result.error(invalidStatus , "Only Completed Task can be opened" , null)
//            return
//        }
//
//        val savedDir = task.savedDir
//        val filename = task.filename
//        val saveFilePath = savedDir + File.separator + filename
//        val intent: Intent? = IntentUtils.validatedFileIntent(requireContext(),saveFilePath,task.mimeType)
//        if(intent != null){
//          requireContext().startActivity(intent)
//          result.success(true)
//        }else{
//            result.success(false)
//        }
//
//    }

    private fun remove(call: MethodCall, result: Result){
        val taskId:String = call.requireArgument("task_id")
        val task = taskDao!!.loadTask(taskId)
        if(task != null){
            if(task.status == DownloadStatus.ENQUEUED || task.status == DownloadStatus.RUNNING){
                WorkManager.getInstance(requireContext()).cancelWorkById(UUID.fromString(taskId))
            }
            val filename = task.filename
            val fileDir = M3U8Util.getSaveFileDir(filename!!)
            M3U8Util.clearDir(File(fileDir))
            val mp4File = File("$fileDir.mp4")
            if(mp4File.exists()){
                M3U8Log.d("Deleting MP4")
                deleteVideoInMediaStore(mp4File)
            }
            taskDao!!.deleteTask(taskId)
//            NotificationManagerCompat.from(requireContext()).cancel(task.primaryId)
            result.success(null)
        }else{
            result.error(taskId, "not found task corresponding with given Task Id", null)
        }
    }

    private fun deleteVideoInMediaStore(file: File){
        MediaScannerConnection.scanFile(context , arrayOf<String>(file.absolutePath) , arrayOf("video/mp4")
        ) { _, uri ->
            if (uri != null) {
                requireContext().contentResolver.delete(uri, null, null)
            }
        }
    }
}

