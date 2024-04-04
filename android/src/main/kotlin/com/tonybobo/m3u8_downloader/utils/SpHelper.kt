package com.tonybobo.m3u8_downloader.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SpHelper {
    companion object{
        private const val NULL_KEY = "NULL_KEY"
        private const val TAG_NAME = "M3U8PreferenceHelper"

        private var PREFERENCES: SharedPreferences? = null

        fun init(context: Context){
            PREFERENCES = context.getSharedPreferences(TAG_NAME , Context.MODE_PRIVATE)
        }

        private fun newEditor(): SharedPreferences.Editor {
            return PREFERENCES!!.edit()
        }

        private fun checkKeyNotNull(key :String?):String{
           if(key == null){
               Log.e(NULL_KEY , "Key is null !!!")
               return NULL_KEY
           }
           return key
        }

        fun putBoolean(key: String , value: Boolean){
           newEditor().putBoolean(checkKeyNotNull(key), value).apply()
        }

        fun getBoolean(key:String , value: Boolean):Boolean{
            return PREFERENCES!!.getBoolean(checkKeyNotNull(key) , value)
        }

        fun putInt(key: String , value: Int){
            newEditor().putInt(checkKeyNotNull(key) , value).apply()
        }

        fun getInt(key: String , value: Int): Int {
            return PREFERENCES!!.getInt(checkKeyNotNull(key) , value)
        }

        fun putLong(key: String , value: Long){
            newEditor().putLong(checkKeyNotNull(key) , value).apply()
        }

        fun getLong(key: String , value: Long):Long{
            return PREFERENCES!!.getLong(checkKeyNotNull(key) , value)
        }

        fun putFloat(key: String , value: Float){
           newEditor().putFloat(checkKeyNotNull(key) , value).apply()
        }

        fun getFloat(key: String , value: Float):Float{
            return PREFERENCES!!.getFloat(checkKeyNotNull(key) , value)
        }

        fun putString(key: String , value: String){
            newEditor().putString(checkKeyNotNull(key), value).apply()
        }

        fun getString(key: String , value: String ):String?{
            return PREFERENCES!!.getString(checkKeyNotNull(key) , value)
        }


        fun putStringSet(key: String , value: MutableSet<String>){
            newEditor().putStringSet(checkKeyNotNull(key), value).apply()
        }

        fun getStringSet(key: String , value: MutableSet<String> ): MutableSet<String>? {
            return PREFERENCES!!.getStringSet(checkKeyNotNull(key) , value)
        }

        fun remove(key: String){
            newEditor().remove(key).apply()
        }

        fun clear(){
            newEditor().clear().commit()
        }
    }
}