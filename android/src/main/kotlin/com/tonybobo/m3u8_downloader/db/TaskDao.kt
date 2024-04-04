package com.tonybobo.m3u8_downloader.db

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.tonybobo.m3u8_downloader.downloader.DownloadStatus
import com.tonybobo.m3u8_downloader.downloader.DownloadTask
import com.tonybobo.m3u8_downloader.utils.M3U8Log
import java.lang.Exception

class TaskDao(private val dbHelper: TaskDbHelper){
    private val projection = arrayOf(
        BaseColumns._ID,
        TaskEntry.COLUMN_NAME_TASK_ID,
        TaskEntry.COLUMN_NAME_PROGRESS,
        TaskEntry.COLUMN_NAME_STATUS,
        TaskEntry.COLUMN_NAME_URL,
        TaskEntry.COLUMN_NAME_FILE_NAME,
        TaskEntry.COLUMN_NAME_TIME_CREATED,
    )

    fun insertOrUpdateNewTask (
        taskId: String?,
        status: DownloadStatus,
        url : String,
        progress: Int,
        fileName: String,
    ){
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put(TaskEntry.COLUMN_NAME_TASK_ID , taskId)
        values.put(TaskEntry.COLUMN_NAME_STATUS , status.ordinal)
        values.put(TaskEntry.COLUMN_NAME_PROGRESS , progress)
        values.put(TaskEntry.COLUMN_NAME_URL , url)
        values.put(TaskEntry.COLUMN_NAME_FILE_NAME , fileName)
        values.put(TaskEntry.COLUMN_NAME_TIME_CREATED , System.currentTimeMillis())

        db.beginTransaction()
        try {
           db.insertWithOnConflict(
               TaskEntry.TABLE_NAME,
               null,
               values,
               SQLiteDatabase.CONFLICT_REPLACE
           )
            db.setTransactionSuccessful()
        }catch (e: Exception){
            e.printStackTrace()
        }finally {
            db.endTransaction()
        }
    }

    fun loadAllTasks():List<DownloadTask> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TaskEntry.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            null,
            null,
        )

        val result: MutableList<DownloadTask> = ArrayList()
        while (cursor.moveToNext()){
          result.add(parseCursor(cursor))
        }
        cursor.close()
        return result
    }

    fun loadTasksWithRawQuery(query: String?) : List<DownloadTask> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(query, null)
        val result: MutableList<DownloadTask> = ArrayList()
        while (cursor.moveToNext()){
            result.add(parseCursor(cursor))
        }
        cursor.close()
        return result
    }

    fun loadTask(taskId: String?):DownloadTask? {
        val db = dbHelper.readableDatabase
        var result: DownloadTask? = null
        val cursor = db.query(
            TaskEntry.TABLE_NAME,
            projection,
            TaskEntry.COLUMN_NAME_TASK_ID + " = ? ",
            arrayOf(taskId),
            null,
            null,
            BaseColumns._ID + " DESC",
            "1"
        )
        while (cursor.moveToNext()){
            result = parseCursor(cursor)
        }
        cursor.close()
        return result
    }

    fun updateTask(taskId: String , status: DownloadStatus , progress: Int){
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put(TaskEntry.COLUMN_NAME_STATUS , status.ordinal)
        values.put(TaskEntry.COLUMN_NAME_PROGRESS, progress)
        db.beginTransaction()
        try {
           db.update(
               TaskEntry.TABLE_NAME,
               values,
               TaskEntry.COLUMN_NAME_TASK_ID + " = ? ",
               arrayOf(taskId)
           )
            db.setTransactionSuccessful()
        }catch (e: Exception){
            e.printStackTrace()
        }finally {
            db.endTransaction()
        }
    }

    fun updateTask(currentTaskId: String, newTaskId:String , status: DownloadStatus , progress: Int ){
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put(TaskEntry.COLUMN_NAME_TASK_ID , newTaskId)
        values.put(TaskEntry.COLUMN_NAME_STATUS , status.ordinal)
        values.put(TaskEntry.COLUMN_NAME_TIME_CREATED , System.currentTimeMillis())
        values.put(TaskEntry.COLUMN_NAME_PROGRESS, progress)
        db.beginTransaction()
        try {
            db.update(
                TaskEntry.TABLE_NAME,
                values,
                TaskEntry.COLUMN_NAME_TASK_ID + " = ? ",
                arrayOf(currentTaskId)
            )
            db.setTransactionSuccessful()
        }catch (e: Exception){
            e.printStackTrace()
        }finally {
            db.endTransaction()
        }
    }


//    fun updateTask(taskId: String){
//        val db = dbHelper.writableDatabase
//        val values = ContentValues()
//        db.beginTransaction()
//        try {
//            db.update(
//                TaskEntry.TABLE_NAME,
//                values,
//                TaskEntry.COLUMN_NAME_TASK_ID + " = ? ",
//                arrayOf(taskId)
//            )
//            db.setTransactionSuccessful()
//        }catch (e: Exception){
//            e.printStackTrace()
//        }finally {
//            db.endTransaction()
//        }
//    }

    fun deleteTask(taskId: String){
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(
                TaskEntry.TABLE_NAME,
                TaskEntry.COLUMN_NAME_TASK_ID + " = ? ",
                arrayOf(taskId)
            )
            db.setTransactionSuccessful()
        }catch (e:Exception){
            e.printStackTrace()
        }finally {
            db.endTransaction()
        }
    }


    private fun parseCursor(cursor: Cursor): DownloadTask {
        val primaryId = cursor.getInt(cursor.getColumnIndexOrThrow(BaseColumns._ID))
        val taskId = cursor.getString(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_TASK_ID))
        val status = cursor.getInt(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_STATUS))
        val url = cursor.getString(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_URL))
        val progress = cursor.getInt(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_PROGRESS))
        val filename = cursor.getString(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_FILE_NAME))
        val timeCreated = cursor.getLong(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_TIME_CREATED))

        return DownloadTask(
            primaryId,
            taskId,
            DownloadStatus.values()[status],
            url,
            progress,
            filename,
            timeCreated,
        )
    }


}