package com.tonybobo.m3u8_downloader

import android.content.ComponentName
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import java.lang.NullPointerException
import java.util.concurrent.Executors

class M3u8DownloaderInitializer : ContentProvider() {

    companion object {
        private const val TAG = "DownloaderInitializer"
        private const val DEFAULT_MAX_CONCURRENT_TASKS = 3
    }
    override fun onCreate(): Boolean {
        val context = requireNotNull(this.context){
            "Cannot find context from the provider"
        }
        val maximumConcurrentTask = getMaxConcurrentTaskMetadata(context)
        WorkManager.initialize(context, Configuration.Builder().setExecutor(Executors.newFixedThreadPool(maximumConcurrentTask)).build())
        return true
    }

    override fun query(
        uri: Uri,
        strings: Array<String>?,
        s: String?,
        strings1: Array<String>?,
        s1: String?
    ): Nothing?= null

    override fun getType(p0: Uri): Nothing? = null

    override fun insert(p0: Uri, p1: ContentValues?): Uri?  = null

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?) = 0

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?) = 0

    private fun getMaxConcurrentTaskMetadata(context: Context): Int {
        try {
            val providerInfo = context.packageManager.getProviderInfo(
                ComponentName(context , "com.tonybobo.m3u8downloader.M3u8DownloaderInitializer" ),
                PackageManager.GET_META_DATA
            )
            val bundle = providerInfo.metaData
            val max = bundle.getInt(
                "com.tonybobo.m3u8downloader.MAX_CONCURRENT_TASKS",
                DEFAULT_MAX_CONCURRENT_TASKS
            )
            Log.d(TAG , "MAX_CONCURRENT_TASKS = $max")
            return max
        }catch (e:NameNotFoundException){
            Log.e(TAG , "NameNotFoundException" + e.message)
        }catch (e:NullPointerException){
            Log.e(TAG , "NullPointerException" + e.message)
        }

        return DEFAULT_MAX_CONCURRENT_TASKS
    }
}