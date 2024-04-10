package com.tonybobo.m3u8_downloader.downloader

data class DownloadTask(
    var primaryId: Int,
    var taskId: String,
    var status: DownloadStatus,
    var url: String?,
    var progress: Int,
    var filename: String?,
    var lastTs: String?,
    var timeCreated: Long,
)
