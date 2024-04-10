package com.tonybobo.m3u8_downloader.db

import android.provider.BaseColumns

object TaskEntry : BaseColumns {
    const val TABLE_NAME = "task"
    const val COLUMN_NAME_TASK_ID = "task_id"
    const val COLUMN_NAME_STATUS = "status"
    const val COLUMN_NAME_PROGRESS = "progress"
    const val COLUMN_NAME_URL = "url"
    const val COLUMN_NAME_FILE_NAME= "file_name"
    const val COLUMN_NAME_TIME_CREATED= "time_created"
    const val COLUMN_NAME_LAST_TS = "last_ts"
}

