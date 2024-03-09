package com.tonybobo.m3u8_downloader.bean

import com.tonybobo.m3u8_downloader.utils.EncryptUtil
import java.net.URL

data class M3U8Ts(val url:String , val fileSize:Long , val seconds : Float) : Comparable<M3U8Ts>{

    fun obtainEncodeTsFileName(): String {
        return EncryptUtil.md5Encode(url).plus(".ts")
    }

    fun obtainFullUrl(hostUrl:String?): URL? {
        val host = URL(hostUrl)
        return URL(host,url)
    }

    override fun compareTo(other: M3U8Ts): Int {
        return url.compareTo(other.url)
    }

}