package com.tonybobo.m3u8_downloader.bean

import com.tonybobo.m3u8_downloader.downloader.DownloadStatus
import com.tonybobo.m3u8_downloader.utils.M3U8Util

data class M3U8Task(var url:String , var state: DownloadStatus = DownloadStatus.UNDEFINED , var speed:Long = 0 , var progress:Float = 0f , var m3u8:M3U8? = null){
    fun getFormatSpeed():String {
        if(speed == 0.toLong()) {
            return ""
        }
        return M3U8Util.formatFileSize(speed) + "/s"
    }

    fun getTotalSize():Long {
        if(m3u8 == null) return 0
        return m3u8!!.getTotalFileSize()
    }

    fun getFormatTotalSize():String{
        if(m3u8 == null) return ""
        val fileSize:Long = getTotalSize()
        if(fileSize.toInt() == 0) return ""
        return M3U8Util.formatFileSize(fileSize)
    }

    fun getFormatCurrentSize():String{
        if(m3u8 == null) return ""
        return M3U8Util.formatFileSize((progress * m3u8!!.getTotalFileSize()).toLong())
    }
}
