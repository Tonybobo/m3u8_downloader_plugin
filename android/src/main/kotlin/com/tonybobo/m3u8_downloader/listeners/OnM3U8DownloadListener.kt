package com.tonybobo.m3u8_downloader.listeners

import com.tonybobo.m3u8_downloader.bean.M3U8Task

interface OnM3U8DownloadListener {
    fun onDownloadPrepare(task: M3U8Task)

    fun onDownloadPending(task: M3U8Task)

    fun onDownloadProgress(task: M3U8Task)

    fun onDownloadItem(task: M3U8Task)

    fun onDownloadSuccess(task: M3U8Task , itemFileSize: Long , totalTs: Int, curTs:Int)

    fun onDownloadPause(task: M3U8Task)

    fun onConvert();

    fun onDownloadError(task: M3U8Task , error:Throwable)

    fun onStop(task: M3U8Task)
}