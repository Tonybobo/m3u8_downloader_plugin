package com.tonybobo.m3u8_downloader.bean

import android.webkit.URLUtil
import com.tonybobo.m3u8_downloader.utils.EncryptUtil
import java.net.URL

data class M3U8Ts(val url:String, var fileSize:Long= 0, val seconds : Float) : Comparable<M3U8Ts>{

    fun obtainEncodeTsFileName(): String {
        val lastValue = if (url.contains("/") ) url.substringAfterLast("/") else url
        return EncryptUtil.md5Encode(lastValue).plus(".ts")
    }
    fun obtainFullUrl(hostUrl:String?): URL {
         if(URLUtil.isValidUrl(url)){
             return URL(url)
         }
        val host = URL(hostUrl)
        return URL(host,url)
    }

    override fun compareTo(other: M3U8Ts): Int {
        return url.compareTo(other.url)
    }

}