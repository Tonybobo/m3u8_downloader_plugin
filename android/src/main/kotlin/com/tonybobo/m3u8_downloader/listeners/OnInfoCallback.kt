package com.tonybobo.m3u8_downloader.listeners

import com.tonybobo.m3u8_downloader.bean.M3U8

interface OnInfoCallback {
     fun success(m3u8:M3U8)
} 