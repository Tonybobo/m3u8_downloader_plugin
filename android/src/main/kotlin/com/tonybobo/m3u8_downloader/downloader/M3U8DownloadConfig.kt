package com.tonybobo.m3u8_downloader.downloader

import android.content.Context
import android.os.Environment
import com.tonybobo.m3u8_downloader.utils.SpHelper
import java.io.File

object M3U8DownloadConfig {
        private const val TAG_SAVE_DIR = "TAG_SAVE_DIR_M3U8"
        private const val TAG_DEBUG = "TAG_DEBUG_M3U8"
        private const val TAG_CONN_TIMEOUT = "TAG_CONN_TIMEOUT_M3U8"
        private const val TAG_READ_TIMEOUT = "TAG_READ_TIMEOUT_M3U8"
        private const val CALLBACK_DISPATCHER_HANDLE_KEY="callback_dispatcher_handle_key"


        fun build(context: Context){
            SpHelper.init(context)
        }

        fun getSaveDir():String?{
            return SpHelper.getString(TAG_SAVE_DIR , Environment.getExternalStorageDirectory().path + File.separator + "M3U8Downloader")
        }
        fun setSaveDir(saveDir:String){
            SpHelper.putString(TAG_SAVE_DIR , saveDir)
        }


        fun setConnTimeout(connTimeout:Int){
            SpHelper.putInt(TAG_CONN_TIMEOUT , connTimeout)
        }

        fun getConnTimeout():Int{
            return SpHelper.getInt(TAG_CONN_TIMEOUT,10*1000)
        }

        fun setReadTimeout(readTimeout:Int){
            SpHelper.putInt(TAG_READ_TIMEOUT , readTimeout)
        }

        fun getReadTimeout():Int{
            return SpHelper.getInt(TAG_READ_TIMEOUT , 30*60*1000)
        }

        fun setDebugMode(debug: Boolean){
            SpHelper.putBoolean(TAG_DEBUG, debug)
        }

        fun getDebugMode():Boolean{
            return SpHelper.getBoolean(TAG_DEBUG , false)
        }

       fun setCallbackHandle(handle: Long){
           SpHelper.putLong(CALLBACK_DISPATCHER_HANDLE_KEY , handle)
       }

       fun getCallbackHandle():Long {
           return SpHelper.getLong(CALLBACK_DISPATCHER_HANDLE_KEY  , 0)
       }
}





