package com.tonybobo.m3u8_downloader.bean

data class M3U8(
    var basesUrl:String = "",
    var dirPath:String ="",
    var localPath:String = "",
    var key:String ="", var iv:String = "",
    val tsList: MutableList<M3U8Ts> = ArrayList()
){

    fun getTotalFileSize():Long{
        var fileSize : Long = 0
        for(m3u8Ts in tsList){
            fileSize += m3u8Ts.fileSize
        }
        return fileSize
    }

    fun getTotalTime():Long{
        var totalTime:Long = 0
        for(m3u8Ts in tsList){
            totalTime += m3u8Ts.seconds.toInt()
        }
        return totalTime
    }

}
