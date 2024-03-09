package com.tonybobo.m3u8_downloader.utils

import android.util.Log

object M3U8Log {
    private const val TAG = "M3U8Log"
    private const val PREFIX = "===== "

    fun d(message: String) {
        Log.d(TAG , PREFIX + message)
    }

    fun e(message: String) {
       Log.e(TAG , PREFIX + message)
    }
}