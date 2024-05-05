package com.tonybobo.m3u8_downloader

import android.content.Context
import android.os.Handler
import androidx.work.Data
import com.tonybobo.m3u8_downloader.downloader.DownloadStatus
import com.tonybobo.m3u8_downloader.downloader.M3U8DownloadConfig
import com.tonybobo.m3u8_downloader.utils.M3U8Log
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
            M3U8Log.d("Initializer Dispatcher")
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
                M3U8Log.d("Initialize Background Isolate")


                val callbackHandle: Long =  M3U8DownloadConfig.getCallbackHandle()

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

    fun sendUpdateProcessEvent(id: UUID , status: DownloadStatus , fileSize:String, progress:Float  , inputData : Data , applicationContext: Context){
        val args: MutableList<Any> = ArrayList()
        val callbackHandle: Long = inputData.getLong(ARG_CALLBACK_HANDLE, 0)
        args.add(callbackHandle)
        args.add(id.toString())
        args.add((progress*100).toInt())
        args.add(status.ordinal)
        args.add(fileSize)
        synchronized(isolateStarted){
            if(!isolateStarted.get()){
                M3U8Log.d("isolateStarted : $isolateStarted")
                isolateQueue.add(args)
            }else {
                Handler(applicationContext.mainLooper).post{
                    backgroundChannel?.invokeMethod("", args)
                }
            }
        }
    }
}