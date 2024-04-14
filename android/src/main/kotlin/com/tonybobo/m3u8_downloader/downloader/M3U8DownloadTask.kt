package com.tonybobo.m3u8_downloader.downloader

import android.content.Context
import android.media.MediaScannerConnection
import android.text.TextUtils
import com.tonybobo.m3u8_donwloader.listeners.OnInfoCallback
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
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class M3U8DownloadTask(context: Context) {
    companion object{
        const val LOCAL_FILE = "local.m3u8"
        const val M3U8_KEY_NAME = "key.key"
        private const val BUFFER_SIZE = 4096
    }

    private var ctx = context
    private var saveDir = ""
    private var currentM3u8:M3U8? = null
    private var timer: Timer? = Timer()
    var isRunning = false
    private val curTs:AtomicInteger = AtomicInteger(0)
    private val curLength: AtomicLong = AtomicLong(0)
    @Volatile
    private var totalTs:Int = 0
    @Volatile
    private var itemFileSize:Long = 0
    private var onTaskListener : OnTaskDownloadListener? = null
    private var connTimeout:Int = 0
    private var readTimeOut: Int = 0
    private var lastTs:String = ""

    init {
        connTimeout = M3U8DownloadConfig.getConnTimeout()
        readTimeOut = M3U8DownloadConfig.getReadTimeout()
    }

    fun resume(url: String , onTaskDownloadListener: OnTaskDownloadListener , fileName: String , lastTs:String){
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
        if(!isRunning) {
            getM3U8Info(url, object : OnInfoCallback {
                override fun success(m3u8: M3U8) {
                    removeDownloadedTs(m3u8 , lastTs)
                    start(m3u8)
                }
            }, lastTs)
        }else{
            handlerError(Throwable("Task is  running"))
        }
    }

    private fun removeDownloadedTs(m3U8: M3U8 , lastTs: String){
        val index = m3U8.tsList.indexOfFirst { it.url == lastTs }
        M3U8Log.d("Found index : $index")
        if(index != -1) m3U8.tsList = m3U8.tsList.drop(index).toMutableList()
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
            } , null)
        }else {
            handlerError(Throwable("Task is  running"))
        }
    }

    private fun getM3U8Info(url: String , callback: OnInfoCallback , lastTs: String?){
        try {
            if(lastTs != null){
                val m3u8 = M3U8Util.parseLocalM3u8("$saveDir/$LOCAL_FILE", url)
                M3U8Log.d("Read local M3U8")
                callback.success(m3u8)
            }else{
                val m3u8 = M3U8Util.parseIndex(url)
                MediaScannerConnection.scanFile(ctx , arrayOf("$saveDir/$LOCAL_FILE") ,
                    arrayOf("audio/x-mpegurl"), null)
                M3U8Util.createLocalM3u8(saveDir , m3u8 , if(TextUtils.isEmpty(m3u8.key)) null else m3u8.key)
                callback.success(m3u8)
            }
        } catch (e: Exception) {
            handlerError(e)
        }
    }

    private fun start(m3U8: M3U8){
        currentM3u8 = m3U8
        onTaskListener!!.onStartDownload(currentM3u8!! , curTs.get()  )
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
        curTs.set(0)
        curLength.set(0)
        isRunning = true
        if(timer != null){
            timer!!.cancel()
        }
        timer = Timer()
        timer!!.schedule( object : TimerTask(){
           override fun run(){
               onTaskListener!!.onProgress(curLength.get())
           }
        } , 0 , 1500)

        val basePath = currentM3u8!!.basesUrl
        for(ts in currentM3u8!!.tsList){
            val file =  File(dir.absolutePath + File.separator + ts.obtainEncodeTsFileName())

            if(!file.exists()){
                var readFinished = false
                try {
                    val url = ts.obtainFullUrl(basePath)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = if (connTimeout < 2000) 2000 else connTimeout
                    conn.readTimeout = if(readTimeOut < 2000) 2000 else connTimeout

                    if(conn.responseCode == 200)
                        conn.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) }}
                    readFinished = true
                    conn.disconnect()

                }catch (e: MalformedURLException){
                   handlerError(e)
                }catch (e: IOException){
                   handlerError(e)
                }finally {
                   if(!readFinished && file.exists())
                       file.delete()
                }
                curLength.set(curLength.get() + file.length())
                onTaskListener!!.onDownloadItem(itemFileSize , totalTs , curTs.get())
                lastTs = ts.url
            }
            itemFileSize = file.length()
            ts.fileSize = itemFileSize
            curTs.incrementAndGet()
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
            MediaScannerConnection.scanFile(ctx , arrayOf(mp4FilePath), arrayOf("video/mp4"), null)
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
            }

            currentM3u8!!.localPath = mp4FilePath
            M3U8Util.clearDir(dir)

            onTaskListener!!.onSuccess(currentM3u8!!)
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
            inputStream?.close()
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
        onTaskListener!!.onStop(lastTs)
    }

}