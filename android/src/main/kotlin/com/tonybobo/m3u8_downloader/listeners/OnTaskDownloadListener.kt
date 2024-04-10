package com.tonybobo.m3u8_downloader.listeners

import com.tonybobo.m3u8_downloader.bean.M3U8

interface OnTaskDownloadListener {
    fun onStart()

    fun onStartDownload(m3u8: M3U8 , curTs:Int)

    fun onDownloadItem(itemFileSize:Long , totalTs: Int, curTs: Int , lastTsFile : String)

    fun onProgress(curLength:Long)

    fun onConvert()

    fun onSuccess(m3U8: M3U8)

    fun onError(error:Throwable)

    fun onStop()
}