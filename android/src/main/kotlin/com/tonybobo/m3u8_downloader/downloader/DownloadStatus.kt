package com.tonybobo.m3u8_downloader.downloader

enum class DownloadStatus {
    UNDEFINED,
    ENQUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELED,
    PAUSED
}