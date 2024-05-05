package com.tonybobo.m3u8_downloader.downloader

import android.content.Context
import android.media.MediaScannerConnection
import android.text.TextUtils
import com.tonybobo.m3u8_downloader.listeners.OnInfoCallback
import com.tonybobo.m3u8_downloader.bean.M3U8
import com.tonybobo.m3u8_downloader.listeners.OnTaskDownloadListener
import com.tonybobo.m3u8_downloader.utils.EncryptUtil
import com.tonybobo.m3u8_downloader.utils.M3U8Log
import com.tonybobo.m3u8_downloader.utils.M3U8Util
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.util.Timer
import java.util.TimerTask
import kotlin.Exception

class M3U8DownloadTask(context: Context) {
    companion object{
        const val M3U8_KEY_NAME = "key.key"
        private const val BUFFER_SIZE = 4096
    }

    private var ctx = context
    private var saveDir = ""
    private var currentM3u8:M3U8? = null
    private var timer: Timer? = Timer()
    var isRunning = false
    private var curTs = 0
    private var curLength: Long = 0
    private var totalTs:Int = 0
    private var itemFileSize:Long = 0
    private var onTaskListener : OnTaskDownloadListener? = null
    private var connTimeout:Int = 0
    private var readTimeOut: Int = 0
    private var lastTs:String = ""

    init {
        connTimeout = M3U8DownloadConfig.getConnTimeout()
        readTimeOut = M3U8DownloadConfig.getReadTimeout()
    }

    fun download(url:String , onTaskDownloadListener: OnTaskDownloadListener , fileName:String){
        saveDir = M3U8Util.getSaveFileDir(fileName)
        onTaskListener = onTaskDownloadListener
        onTaskListener!!.onStart()
        val file = File("$fileName.mp4")
        if(file.exists()){
            if(timer != null){
                timer!!.cancel()
            }
            return
        }
        if(!isRunning){
            getM3U8Info(url , object : OnInfoCallback {
                override fun success(m3u8: M3U8) {
                    start(m3u8)
                }
            }  )
        }else {
            handlerError(Throwable("Task is  running"))
        }
    }

    private fun getM3U8Info(url: String , callback: OnInfoCallback){
        try {
            val m3u8 = M3U8Util.parseIndex(url)
            callback.success(m3u8)
        }catch (e:Exception){
            onTaskListener!!.onError(e)
        }
    }

    private fun start(m3U8: M3U8){
        currentM3u8 = m3U8
        onTaskListener!!.onStartDownload(currentM3u8!! , curTs  )
        M3U8Log.d("start download, save dir: $saveDir")

        try {
            downloadTsList()
            if(isRunning){
                currentM3u8!!.dirPath = saveDir
                convertMp4()
                if(timer != null){
                    timer!!.cancel()
                }
                onTaskListener!!.onSuccess(currentM3u8!!)
                isRunning = false
            }
        }catch (_:InterruptedIOException){

        }catch (e:IOException){
            handlerError(e)
        }catch (e:Exception){
            handlerError(e)
        }
    }

    private fun downloadTsList(){
        val dir = File(saveDir)
        if(!dir.exists()){
            dir.mkdirs()
        }
        if(!TextUtils.isEmpty(currentM3u8!!.key)){
            try {
                M3U8Util.saveFile(currentM3u8!!.key , saveDir + File.separator + M3U8_KEY_NAME)
            }catch (e: IOException){
                handlerError(e)
            }
        }
        totalTs = currentM3u8!!.tsList.size
        isRunning = true
        if(timer != null){
            timer!!.cancel()
        }
        timer = Timer()
        timer!!.schedule( object : TimerTask(){
           override fun run(){
               onTaskListener!!.onProgress(curLength)
           }
        } , 0 , 1500)

        val basePath = currentM3u8!!.basesUrl
        for(ts in currentM3u8!!.tsList){
            val file =  File(dir.absolutePath + File.separator + ts.obtainEncodeTsFileName())
            if(!isRunning) break

            if(!file.exists()){
                var readFinished = false
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                val url = ts.obtainFullUrl(basePath)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = if (connTimeout < 20000) 20000 else connTimeout
                conn.readTimeout = if(readTimeOut < 20000) 20000 else connTimeout
                val outputStream = file.outputStream()
                val inputStream = conn.inputStream

                try {
                    while(inputStream.read(buffer).also { bytesRead = it } != -1){
                        curLength += bytesRead.toLong()
                       outputStream.write(buffer , 0 , bytesRead)
                    }
                    readFinished = true

                }catch (e: MalformedURLException){
                   handlerError(e)
                }catch (e: IOException){
                   handlerError(e)
                } finally {
                    if (!readFinished && file.exists())
                        file.delete()
                    inputStream?.close()
                    outputStream.close()
                    conn.disconnect()
                }
                onTaskListener!!.onDownloadItem(curLength , totalTs , curTs++)
                lastTs = ts.url
            }else{
                curTs++
                M3U8Log.d("${ts.url} exists")
            }
            itemFileSize = file.length()
            ts.fileSize = itemFileSize
        }
    }

    private fun convertMp4(){
        val dir = File(saveDir)
        val mp4FilePath = "$saveDir.mp4"
        var mp4File:File? = null
        var outStream: FileOutputStream? = null
        var inputStream: InputStream? = null
        onTaskListener!!.onConvert()

        try {
            mp4File = File(mp4FilePath)
            if(mp4File.exists()){
                mp4File.delete()
            }
            outStream = FileOutputStream(mp4File)
            var bytes = ByteArray(BUFFER_SIZE)
            for(ts in currentM3u8!!.tsList){

                val file = try {
                    File(dir.absolutePath + File.separator + ts.obtainEncodeTsFileName())
                }catch (e:Exception){
                    File(dir.absolutePath+ File.separator + ts.url)
                }

                if(!file.exists()){
                    continue
                }
                try {
                    inputStream = FileInputStream(file)
                    if(!TextUtils.isEmpty(currentM3u8!!.key)){
                        val available = inputStream.available()
                        if(bytes.size < available){
                            bytes = ByteArray(available)
                        }
                        inputStream.read(bytes)
                        outStream.write(EncryptUtil.decryptTs(bytes , currentM3u8!!.key , currentM3u8!!.iv))

                    }else{
                        inputStream.use { stream -> stream.copyTo(outStream) }
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                    handlerError(e)
                }finally {
                    inputStream?.close()
                }
            }

            currentM3u8!!.localPath = mp4FilePath
            M3U8Util.clearDir(dir)
            MediaScannerConnection.scanFile(ctx , arrayOf(mp4FilePath), arrayOf("video/mp4"), null)

        }catch (e:FileNotFoundException){
            e.printStackTrace()
            handlerError(e)
        }catch (e:IOException){
            e.printStackTrace()
            handlerError(e)
        }catch (e:Exception){
            e.printStackTrace()
            handlerError(e)
        }finally {
            if(mp4File != null && mp4File.exists() && mp4File.length().toInt() == 0 ){
                mp4File.delete()
            }
            outStream?.flush()
            outStream?.close()
        }
    }

    private fun handlerError(e:Throwable){
       e.printStackTrace()
       onTaskListener!!.onError(e)
    }


    fun stop(){
        if(timer != null) {
            timer!!.cancel()
            timer = null
        }
        isRunning = false
        onTaskListener!!.onStop()
    }

}