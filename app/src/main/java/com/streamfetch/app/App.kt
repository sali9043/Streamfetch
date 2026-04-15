package com.streamfetch.app

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Log.d("StreamFetch", "YoutubeDL & FFmpeg initialized successfully.")
            
            // Auto update yt-dlp to latest nightly build in background
            Thread {
                try {
                    val newVersion = YoutubeDL.getInstance().updateYoutubeDL(this, YoutubeDL.UpdateChannel.NIGHTLY)
                    Log.d("StreamFetch", "Updated yt-dlp to version: $newVersion")
                } catch (e: Exception) {
                    Log.e("StreamFetch", "Failed to update yt-dlp engine", e)
                }
            }.start()
            
        } catch (e: Exception) {
            Log.e("StreamFetch", "failed to initialize youtubedl-android or ffmpeg", e)
        }
    }
}
