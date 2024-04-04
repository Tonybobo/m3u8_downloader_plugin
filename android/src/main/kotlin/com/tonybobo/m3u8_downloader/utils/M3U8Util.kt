package com.tonybobo.m3u8_downloader.utils

import com.tonybobo.m3u8_downloader.bean.M3U8
import com.tonybobo.m3u8_downloader.bean.M3U8Ts
import com.tonybobo.m3u8_downloader.downloader.M3U8DownloadConfig
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.URL

object M3U8Util {
    private const val KB:Float = 1024F
    private const val MB:Float = 1024* KB
    private const val GB:Float = 1024*MB

    fun parseIndex(url:String):M3U8{
     val baseUrl = URL(url)
     val streams = BufferedReader(InputStreamReader(baseUrl.openStream()))

     val m3u8 = M3U8()
     m3u8.basesUrl = url

     var seconds = 0.0F
     var line = ""

     streams.useLines { lines ->
         lines.forEach loop@ {
             line = it
             if (line.startsWith('#')) {
                 if (line.startsWith("#EXTINF:")) {
                     line = line.drop(line.indexOf(":")+ 1)
                     if (line.endsWith(",")) {
                         line = line.dropLast(1)
                     }
                     seconds = line.toFloat()
                 } else if (line.startsWith("#EXT-X-KEY:")) {
                     line = line.split("EXT-X-KEY:")[1]
                     val arr: List<String> = line.split(",")
                     for (s in arr) {
                         if (s.contains("=")) {
                             val k = s.split("=")[0]
                             var v = s.split("=")[1]
                             if (k == "URI") {
                                 v = v.replace("\"", "")
                                 v = v.replace("'", "")
                                 val keyReader =
                                     BufferedReader(InputStreamReader(URL(baseUrl, v).openStream()))
                                 m3u8.key = (keyReader.readLine())
                                 M3U8Log.d("M3U8 key:" + m3u8.key)
                                 keyReader.close()
                             } else if (k == "IV") {
                                 m3u8.iv = v
                                 M3U8Log.d("M3U8 IV: $v")
                             }
                         }
                     }
                 }
                 return@loop
             }
             if (line.endsWith("m3u8")) {
                 return parseIndex(URL(baseUrl, line).toString())
             }
             m3u8.tsList.add(M3U8Ts(line, 0, seconds))
             seconds = 0.0F
         }
     }
    streams.close()
    return  m3u8;
 }

    fun clearDir(dir:File): Boolean {
        if(dir.exists()) {
            if(dir.isFile){
                return dir.delete()
            }else if(dir.isDirectory){
                val files = dir.listFiles()
                if(files != null && files.size > 0){
                    for (file in files){
                        clearDir(file)
                    }
                }
                return dir.delete()
            }
        }
        return true
    }

    fun formatFileSize(size:Long):String{
        return if(size>= GB){
            String.format("%.1f GB", size/GB)
        }else if(size >= MB){
            val value = size/MB
            val format = if (value > 100) "%.0f MB" else "%.1f MB"
            String.format(format, value)
        }else if(size>= KB){
            val value = size/MB
            val format = if (value > 100) "%.0f KB" else "%.1f KB"
            String.format(format, value)
        }else {
            String.format("%d B", size)
        }
    }

    fun createLocalM3u8(fileName: String , m3u8: M3U8){
        createLocalM3u8(fileName , m3u8 , null)
    }

    fun createLocalM3u8(fileName:String , m3u8:M3U8 , keyPath:String?){
        M3U8Log.d("CreateLocalM3u8: $fileName")
        val bfw = BufferedWriter(FileWriter(fileName , false))
        bfw.write("#EXTM3U\n")
        bfw.write("#EXT-X-VERSION:3\n")
        bfw.write("#EXT-X-MEDIA-SEQUENCE:0\n")
        bfw.write("#EXT-X-TARGETDURATION:13\n")
        if(keyPath != null) bfw.write("EXT-X-KEY:METHOD=AES-128,URI=\"$keyPath\"\n")
        for(m3u8Ts in m3u8.tsList){
            bfw.write("#EXTINF:" +m3u8Ts.seconds + ",\n")
            bfw.write(m3u8Ts.obtainEncodeTsFileName())
            bfw.newLine()
        }
        bfw.write("#EXT-X-ENDLIST")
        bfw.flush()
        bfw.close()
    }

    fun readFile(fileName: String):ByteArray{
        val file = File(fileName)
        val inputStream = FileInputStream(file)
        val length = inputStream.available()
        val buffer = ByteArray(length)
        inputStream.read(buffer)
        inputStream.close()
        return buffer
    }

    fun saveFile(bytes: ByteArray , fileName: String){
        val file = File(fileName)
        val outputStream = FileOutputStream(file)
        outputStream.write(bytes)
        outputStream.flush()
        outputStream.close()
    }

    fun saveFile(text:String , fileName:String){
        val file = File(fileName)
        val out = BufferedWriter(FileWriter(file))
        out.write(text);
        out.flush()
        out.close()
    }

    fun getSaveFileDir(url:String):String{
        return M3U8DownloadConfig.getSaveDir()+File.separator + EncryptUtil.md5Encode(url)
    }

}