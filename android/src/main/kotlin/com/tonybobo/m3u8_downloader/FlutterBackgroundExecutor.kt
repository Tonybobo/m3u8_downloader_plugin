package com.tonybobo.m3u8_downloader

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import androidx.work.Data
import com.tonybobo.m3u8_downloader.downloader.DownloadStatus
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.FlutterCallbackInformation
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class FlutterBackgroundExecutor : MethodChannel.MethodCallHandler {
    companion object{
        private var backgroundFlutterEngine:FlutterEngine? = null
        private val isolateStarted = AtomicBoolean(false)
        private val isolateQueue = ArrayDeque<List<Any>>()
        private const val ARG_CALLBACK_HANDLE = "callback_handle"
    }
    private var backgroundChannel: MethodChannel?= null
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if(call.method.equals("didInitializeDispatcher")){
            synchronized(isolateStarted){
                while(!isolateQueue.isEmpty()){
                    backgroundChannel?.invokeMethod("", isolateQueue.removeFirst())
                }
                isolateStarted.set(true)
                result.success(null)
            }
        }else{
            result.notImplemented()
        }
    }

    fun startBackgroundIsolate(context: Context){
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

               backgroundFlutterEngine = FlutterEngine(context , null , false)

                val flutterCallback : FlutterCallbackInformation?= FlutterCallbackInformation.lookupCallbackInformation(callbackHandle)

                val appBundlePath : String = FlutterInjector.instance().flutterLoader().findAppBundlePath()
                val assets = context.assets

                backgroundFlutterEngine?.dartExecutor?.executeDartCallback(
                    DartExecutor.DartCallback(
                        assets,
                        appBundlePath,
                        flutterCallback!!
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

    fun sendUpdateProcessEvent(id: UUID , status: DownloadStatus , progress:Float  , inputData : Data , applicationContext: Context){
        val args: MutableList<Any> = ArrayList()
        val callbackHandle: Long = inputData.getLong(ARG_CALLBACK_HANDLE, 0)
        args.add(callbackHandle)
        args.add(id.toString())
        args.add(progress)
        args.add(status.ordinal)
        args.add(progress)
        synchronized(isolateStarted){
            if(!isolateStarted.get()){
                isolateQueue.add(args)
            }else {
                Handler(applicationContext.mainLooper).post{
                    backgroundChannel?.invokeMethod("", args)
                }
            }
        }
    }
}