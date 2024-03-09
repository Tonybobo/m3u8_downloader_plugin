package com.tonybobo.m3u8_downloader.bean

data class M3U8(val basesUrl:String, val dirPath:String , val localPath:String , val key:String , val iv:String ,val tsList: MutableList<M3U8Ts> = ArrayList<M3U8Ts>()){

    fun addTs(ts: M3U8Ts){
        tsList.add(ts)
    }
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
