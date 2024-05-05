package com.tonybobo.m3u8_downloader.utils

import android.text.TextUtils
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


object EncryptUtil {
    private const val ENCODING:String = "UTF-8"

    private fun String.md5():String{
        val md = MessageDigest.getInstance("MD5")
        md.update(this.toByteArray())
        return BigInteger(1 , md.digest()).toString(16)
    }

    fun md5Encode(str: String): String {
        try {
            return str.md5()
        }catch (e: NoSuchAlgorithmException){
            e.printStackTrace()
        }
        return str
    }

    fun decryptTs(bytes: ByteArray , key : String , iv :String): ByteArray{
        if(TextUtils.isEmpty(key)){
            return bytes
        }
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        var ivByte = ByteArray(16)
        if(!TextUtils.isEmpty(iv)){
            ivByte = if(iv.startsWith("0x")){
                parseHexStr2Byte(iv.substring(2))
            }else{
                iv.toByteArray()
            }
            if(ivByte.size != 16){
                ivByte = ByteArray(16)
            }
        }
        val keySpec = SecretKeySpec(key.toByteArray(charset(ENCODING)), "AES")
        val paramSpec = IvParameterSpec(ivByte)
        cipher.init(Cipher.DECRYPT_MODE, keySpec,paramSpec)
        return cipher.doFinal(bytes)

    }

    private fun parseHexStr2Byte(hexStr: String): ByteArray{
        check(hexStr.length % 2 == 0){
            "Must have even length"
        }
        return hexStr.chunked(2).map { it.toInt(16).toByte()}.toByteArray()
    }


}