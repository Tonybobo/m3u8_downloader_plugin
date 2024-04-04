package com.tonybobo.m3u8_downloader.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class TaskDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context , DATABASE_NAME , null , DATABASE_VERSION){
    companion object{
        const val DATABASE_VERSION = 4
        const val DATABASE_NAME = "download_tasks.db"
        private var instance: TaskDbHelper? = null
        private const val SQL_CREATE_ENTRIES = (
                "CREATE TABLE " + TaskEntry.TABLE_NAME + " (" +
                  BaseColumns._ID + " INTEGER PRIMARY KEY," +
                  TaskEntry.COLUMN_NAME_TASK_ID + " VARCHAR(256)," +
                  TaskEntry.COLUMN_NAME_STATUS + " INTEGER DEFAULT 0," +
                  TaskEntry.COLUMN_NAME_PROGRESS + " INTEGER DEFAULT 0," +
                  TaskEntry.COLUMN_NAME_URL + " TEXT," +
                  TaskEntry.COLUMN_NAME_FILE_NAME + " TEXT," +
                  TaskEntry.COLUMN_NAME_TIME_CREATED + " INTEGER DEFAULT 0" +
                  ")"
                )
        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${TaskEntry.TABLE_NAME}"

        fun getInstance(ctx: Context?): TaskDbHelper{
            if(instance == null){
                instance = TaskDbHelper(ctx!!.applicationContext)
            }
            return instance as TaskDbHelper
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase,oldVersion:  Int, newVersion: Int) {
            db.execSQL(SQL_DELETE_ENTRIES)
            onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

}