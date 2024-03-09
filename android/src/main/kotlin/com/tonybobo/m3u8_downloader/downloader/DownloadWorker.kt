package com.tonybobo.m3u8_downloader.downloader

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tonybobo.m3u8_downloader.M3u8DownloaderPlugin
import com.tonybobo.m3u8_downloader.R
import com.tonybobo.m3u8_downloader.db.TaskDao
import com.tonybobo.m3u8_downloader.db.TaskDbHelper
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.FlutterCallbackInformation
import java.io.File
import java.lang.Exception
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class DownloadWorker(context: Context , params: WorkerParameters ) : Worker(context , params) , MethodChannel.MethodCallHandler {
    private var backgroundChannel : MethodChannel?= null
    private var dbHelper: TaskDbHelper?= null
    private var taskDao: TaskDao?=null
    private var showNotification = false
    private var clickToOpenDownloadedFile = false
    private var debug = false
    private var ignoreSsl = false
    private var lastProgress = 0
    private var primaryId = 0
    private var msgStarted:String?= null
    private var msgInProgress: String?= null
    private var msgCancel:String?= null
    private var msgFailed:String?= null
    private var msgPaused:String?= null
    private var msgComplete:String?= null
    private var lastCallUpdateNotification:Long = 0
    private var step = 0
    private var saveInPublicStorage = false
    private val notificationIconRes: Int
        get() {
            try {
                val applicationInfo:ApplicationInfo = applicationContext.packageManager
                    .getApplicationInfo(
                        applicationContext.packageName,
                        PackageManager.GET_META_DATA
                    )

                val appIconResId: Int = applicationInfo.icon
                return applicationInfo.metaData.getInt(
                    "com.tonybobo.m3u8Download.NOTIFICATION_ICON",
                    appIconResId
                )
            }catch (e:PackageManager.NameNotFoundException){
                e.printStackTrace()
            }
            return 0
        }

    companion object{
        const val ARG_URL = "url"
        const val ARG_FILE_NAME = "file_name"
        const val ARG_SAVED_DIR = "saved_file"
        const val ARG_HEADERS =  "headers"
        const val ARG_IS_RESUME = "is_resume"
        const val ARG_TIMEOUT = "timeout"
        const val ARG_SHOW_NOTIFICATION = "show_notification"
        const val ARG_OPEN_FILE_FROM_NOTIFICATION = "open_file_from_notification"
        const val ARG_CALLBACK_HANDLE = "callback_handle"
        const val ARG_DEBUG = "debug"
        const val ARG_STEP = "step"
        const val ARG_SAVE_IN_PUBLIC_STORAGE = "save_in_public_storage"
        const val ARG_IGNORESSL = "ignoreSsl"
        private val TAG = DownloadWorker::class.java.simpleName
        private const val BUFFER_SIZE = 4096
        private const val CHANNEL_ID = "M3U8_DOWNLOADER_NOTIFICATION"
        private val isolateStarted = AtomicBoolean(false)
        private val isolateQueue = ArrayDeque<List<Any>>()
        private var backgroundFlutterEngine: FlutterEngine? = null
        val DO_NOT_VERIFY = HostnameVerifier {_ , _ -> true}

        private fun trustAllHosts(){
            val tag = "trustAllHosts"

            val trustManagers: Array<TrustManager> = arrayOf(
                @SuppressLint("CustomX509TrustManager")
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                        Log.i(tag , "checkClientTrusted")
                    }

                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                        Log.i(tag , "checkServerTrusted")
                    }

                    override fun getAcceptedIssuers(): Array<out X509Certificate> = emptyArray()
                }
            )

            try {
                val sslContent: SSLContext = SSLContext.getInstance("TLS")
                sslContent.init(null , trustManagers , SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContent.socketFactory)
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    init {
        Handler(context.mainLooper).post {
            startBackgroundIsolate(context)
        }
    }

    // Background process update UI Main Thread
    private fun startBackgroundIsolate(context: Context){
        synchronized(isolateStarted){
            if(backgroundFlutterEngine == null){
                val pref: SharedPreferences = context.getSharedPreferences(
                    M3u8DownloaderPlugin.SHARED_PREFERENCES_KEY,
                    Context.MODE_PRIVATE
                )
                val callbackHandle: Long = pref.getLong(
                    M3u8DownloaderPlugin.CALLBACK_DISPATCHER_HANDLE_KEY,
                    0
                )

                backgroundFlutterEngine = FlutterEngine(applicationContext , null , false)

                val flutterCallback : FlutterCallbackInformation?= FlutterCallbackInformation.lookupCallbackInformation(callbackHandle)

                if(flutterCallback == null){
                    log("Fatal: failed to find callback")
                    return
                }

                val appBundlePath : String = FlutterInjector.instance().flutterLoader().findAppBundlePath()
                val assets = applicationContext.assets

                backgroundFlutterEngine?.dartExecutor?.executeDartCallback(
                    DartExecutor.DartCallback(
                        assets,
                        appBundlePath,
                        flutterCallback
                    )
                )
            }
        }
        backgroundChannel = MethodChannel(
            backgroundFlutterEngine!!.dartExecutor,
            "com.tonybobo.m3u8Downloader_background"
        )
        backgroundChannel?.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall , result: MethodChannel.Result){
        if(call.method.equals("didInitializeDispatcher")){
            synchronized(isolateStarted){
                while(!isolateQueue.isEmpty()){
                    backgroundChannel?.invokeMethod("", isolateQueue.remove())
                }
                isolateStarted.set(true)
                result.success(null)
            }
        }else{
            result.notImplemented()
        }
    }

    override fun onStopped() {
       val context : Context = applicationContext
       dbHelper = TaskDbHelper.getInstance(context)
        taskDao = TaskDao(dbHelper!!)
        val url: String? =inputData.getString(ARG_URL)
        val fileName: String?= inputData.getString(ARG_FILE_NAME)
        val task = taskDao?.loadTask(id.toString())
        if(task != null && task.status == DownloadStatus.ENQUEUED){
            updateNotification(context, fileName?:url , DownloadStatus.CANCELED , -1 , null , true)
            taskDao?.updateTask(id.toString() , DownloadStatus.CANCELED , lastProgress)
        }
    }

    private fun setUpNotification(context: Context){
        if(!showNotification) return

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            val res = applicationContext.resources
            val channelName: String = res.getString(R.string.m3u8_downloader_notification_channel_name)
            val channelDescription: String = res.getString(R.string.m3u8_downloader_notification_channel_description)
            val importance: Int = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID , channelName, importance)
            channel.description = channelDescription
            channel.setSound(null , null)

            val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendUpdateProcessEvent(status: DownloadStatus , progress: Int){
        val args: MutableList<Any> = ArrayList()
        val callbackHandle : Long = inputData.getLong(ARG_CALLBACK_HANDLE , 0)
        args.add(callbackHandle)
        args.add(id.toString())
        args.add(status.ordinal)
        args.add(progress)
        synchronized(isolateStarted){
            if(!isolateStarted.get()){
                isolateQueue.add(args)
            }else{
                Handler(applicationContext.mainLooper).post{
                    backgroundChannel?.invokeMethod("", args)
                }
            }
        }
    }

    private fun updateNotification(context: Context , title: String? , status: DownloadStatus , progress: Int , intent: PendingIntent? , finalize: Boolean){
        sendUpdateProcessEvent(status, progress)
        if(showNotification){
            val builder  = NotificationCompat.Builder(context , CHANNEL_ID).setContentTitle(title)
                .setContentIntent(intent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
            when (status){
               DownloadStatus.RUNNING -> {
                   if(progress <= 0){
                       builder.setContentText(msgStarted)
                           .setProgress(0 , 0 , false)
                       builder.setOngoing(false)
                           .setSmallIcon(notificationIconRes)
                   }else if(progress < 100){
                       builder.setContentText(msgInProgress)
                           .setProgress(100 , progress , false)
                       builder.setOngoing(true)
                           .setSmallIcon(android.R.drawable.stat_sys_download)
                   }else{
                       builder.setContentText(msgComplete)
                           .setProgress(0 , 0 , false)
                       builder.setOngoing(false)
                           .setSmallIcon(android.R.drawable.stat_sys_download_done)
                   }
               }

                DownloadStatus.CANCELED -> {
                    builder.setContentText(msgCancel)
                        .setProgress(0 , 0 , false)
                    builder.setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                }

                DownloadStatus.FAILED -> {
                    builder.setContentText(msgFailed)
                        .setProgress(0 , 0 , false)
                    builder.setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                }

                DownloadStatus.PAUSED -> {
                    builder.setContentText(msgPaused)
                        .setProgress(0 , 0 , false)
                    builder.setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                }

                DownloadStatus.COMPLETED -> {
                    builder.setContentText(msgComplete)
                        .setProgress(0 , 0 , false)
                    builder.setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                }

                else -> {
                    builder.setProgress(0 , 0 , false)
                    builder.setOngoing(false).setSmallIcon(notificationIconRes)
                }
            }

            if(System.currentTimeMillis() - lastCallUpdateNotification < 1000){
                if(finalize){
                    log("Update Too frequently!")
                    try {
                        Thread.sleep(1000)
                    }catch (e: InterruptedException){
                        e.printStackTrace()
                    }
                }else {
                    log("Update too frequently. Please enable finalize")
                    return
                }
            }
            log("Update Notification: {notificationId: $primaryId , title : $title , status: $status , progress: $progress}")
            NotificationManagerCompat.from(context).notify(primaryId , builder.build())
            lastCallUpdateNotification = System.currentTimeMillis()
        }
    }


    private fun log(message: String){
        if(debug){
            Log.d(TAG , message)
        }
    }

    private fun logError(message:String){
        if(debug){
            Log.d(TAG, message)
        }
    }

    override fun doWork(): Result {
        TODO("Not yet implemented")
    }

}