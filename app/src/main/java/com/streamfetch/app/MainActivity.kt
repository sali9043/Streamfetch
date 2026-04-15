package com.streamfetch.app

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btnTabDownloader: Button
    private lateinit var btnTabRecent: Button
    private lateinit var layoutDownloaderList: View
    private lateinit var layoutRecent: View

    private lateinit var etUrl: EditText
    private lateinit var cbAutoMerge: CheckBox
    private lateinit var btnAnalyze: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var llFormats: LinearLayout
    private lateinit var llRecentFiles: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnTabDownloader = findViewById(R.id.btnTabDownloader)
        btnTabRecent = findViewById(R.id.btnTabRecent)
        layoutDownloaderList = findViewById(R.id.layoutDownloaderList)
        layoutRecent = findViewById(R.id.layoutRecent)

        etUrl = findViewById(R.id.etUrl)
        cbAutoMerge = findViewById(R.id.cbAutoMerge)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        llFormats = findViewById(R.id.llFormats)
        llRecentFiles = findViewById(R.id.llRecentFiles)

        // Handle Share Intent
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            handleSharedText(intent.getStringExtra(Intent.EXTRA_TEXT))
        }

        btnTabDownloader.setOnClickListener {
            layoutDownloaderList.visibility = View.VISIBLE
            layoutRecent.visibility = View.GONE
            btnTabDownloader.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF000000.toInt())
            btnTabRecent.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFF3F3F3.toInt())
            btnTabRecent.setTextColor(0xFF474747.toInt())
            btnTabDownloader.setTextColor(0xFFFFFFFF.toInt())
        }

        btnTabRecent.setOnClickListener {
            layoutDownloaderList.visibility = View.GONE
            layoutRecent.visibility = View.VISIBLE
            btnTabRecent.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF000000.toInt())
            btnTabDownloader.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFF3F3F3.toInt())
            btnTabDownloader.setTextColor(0xFF474747.toInt())
            btnTabRecent.setTextColor(0xFFFFFFFF.toInt())
            loadRecentDownloads()
        }

        btnAnalyze.setOnClickListener {
            var url = etUrl.text.toString().trim()
            url = url.replace(Regex("[?&]si=[a-zA-Z0-9_-]+"), "")
            etUrl.setText(url)

            if (url.isNotEmpty()) {
                analyzeUrl(url)
            } else {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSharedText(sharedText: String?) {
        if (sharedText != null) {
            val urlRegex = Regex("(https?://[^\\s]+)")
            val match = urlRegex.find(sharedText)
            var url = match?.value ?: sharedText
            url = url.replace(Regex("[?&]si=[a-zA-Z0-9_-]+"), "")
            etUrl.setText(url)
            
            // Auto analyze instantly upon receiving a shared intent
            analyzeUrl(url)
        }
    }

    private fun loadRecentDownloads() {
        llRecentFiles.removeAllViews()
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir.exists()) {
            val files = downloadsDir.listFiles { _, name -> 
                name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".m4a") || name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".mkv")
            }
            if (files != null && files.isNotEmpty()) {
                files.sortByDescending { it.lastModified() }
                
                val tvHeader = TextView(this)
                tvHeader.text = "Recent Media in /Downloads:"
                tvHeader.setTextColor(0xFF1B1B1B.toInt())
                tvHeader.setPadding(0, 0, 0, 24)
                tvHeader.textSize = 18f
                tvHeader.setTypeface(null, android.graphics.Typeface.BOLD)
                llRecentFiles.addView(tvHeader)

                for (f in files.take(30)) {
                    val tv = TextView(this)
                    val mbSize = String.format("%.2f", f.length() / (1024.0 * 1024.0))
                    tv.text = "• ${f.name}\n  Size: $mbSize MB"
                    tv.setTextColor(0xFF474747.toInt())
                    tv.setPadding(0, 16, 0, 16)
                    llRecentFiles.addView(tv)
                }
            } else {
                val tv = TextView(this)
                tv.text = "No compatible media files found in Downloads."
                tv.setTextColor(0xFF888888.toInt())
                llRecentFiles.addView(tv)
            }
        }
    }

    private fun analyzeUrl(url: String) {
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Executing yt-dlp..."
        btnAnalyze.isEnabled = false
        llFormats.removeAllViews()

        val wantsMerge = cbAutoMerge.isChecked

        Thread {
            try {
                // Get JSON info
                val request = YoutubeDLRequest(url)
                request.addOption("-J")
                request.addOption("--no-update")

                val response = YoutubeDL.getInstance().execute(request, "StreamFetch", null)
                val jsonOutput = response.out
                
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnAnalyze.isEnabled = true
                    tvStatus.text = "Extraction Success! Select format below:"
                    
                    if (jsonOutput != null) {
                        try {
                            val json = JSONObject(jsonOutput)
                            val formats = json.optJSONArray("formats")
                            if (formats != null) {
                                
                                if (wantsMerge) {
                                    renderMergedUI(url, formats)
                                } else {
                                    renderRawFormatsUI(url, formats)
                                }
                                
                            }
                        } catch(e: Exception) {
                            Log.e("StreamFetch", "JSON Parse error", e)
                            tvStatus.text = "Failed parsing extracted data."
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StreamFetch", "Extraction failed", e)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnAnalyze.isEnabled = true
                    tvStatus.text = "Error processing URL: ${e.message}"
                }
            }
        }.start()
    }

    private fun renderMergedUI(url: String, formats: org.json.JSONArray) {
        val resolutions = mutableSetOf<Int>()
        var hasAudio = false
        
        for (i in 0 until formats.length()) {
            val format = formats.getJSONObject(i)
            val vcodec = format.optString("vcodec", "none")
            val acodec = format.optString("acodec", "none")
            val height = format.optInt("height", 0)

            if (vcodec != "none" && height > 0) {
                resolutions.add(height)
            }
            if (acodec != "none" && acodec != "null") {
                hasAudio = true
            }
        }

        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (60 * resources.displayMetrics.density).toInt()
        )
        btnParams.setMargins(0, 0, 0, (12 * resources.displayMetrics.density).toInt())

        if (hasAudio) {
            val btnAudio = Button(this@MainActivity)
            btnAudio.text = "DOWNLOAD AUDIO ONLY - BEST"
            btnAudio.layoutParams = btnParams
            btnAudio.setBackgroundResource(R.drawable.bg_rounded_button)
            btnAudio.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF327a32.toInt())
            btnAudio.setTextColor(0xFFFFFFFF.toInt())
            btnAudio.setOnClickListener {
                downloadFormat(url, "bestaudio", wantsMerge = true)
            }
            llFormats.addView(btnAudio)
        }

        val sortedRes = resolutions.sortedDescending()
        for (res in sortedRes) {
            val btnVideo = Button(this@MainActivity)
            btnVideo.text = "DOWNLOAD VIDEO ${res}P (MERGED)"
            btnVideo.layoutParams = btnParams
            btnVideo.setBackgroundResource(R.drawable.bg_rounded_button)
            btnVideo.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF000000.toInt())
            btnVideo.setTextColor(0xFFFFFFFF.toInt())
            btnVideo.setOnClickListener {
                downloadFormat(url, "bestvideo[height<=${res}]+bestaudio/best", wantsMerge = true)
            }
            llFormats.addView(btnVideo)
        }
    }

    private fun renderRawFormatsUI(url: String, formats: org.json.JSONArray) {
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (60 * resources.displayMetrics.density).toInt()
        )
        btnParams.setMargins(0, 0, 0, (12 * resources.displayMetrics.density).toInt())

        for (i in 0 until formats.length()) {
            val format = formats.getJSONObject(i)
            val vcodec = format.optString("vcodec", "none")
            val acodec = format.optString("acodec", "none")
            val ext = format.optString("ext", "unknown")
            val height = format.optInt("height", 0)
            val formatId = format.optString("format_id")
            val note = format.optString("format_note", "")
            
            val filesize = format.optLong("filesize", 0L).let { 
                if (it == 0L) format.optLong("filesize_approx", 0L) else it 
            }
            val sizeStr = if (filesize > 0) " - ${String.format("%.1f MB", filesize / (1024.0 * 1024.0))}" else ""

            val isVideo = vcodec != "none" 
            val isAudio = acodec != "none" && acodec != "null"

            if (isVideo && !isAudio) {
                val btn = Button(this@MainActivity)
                btn.text = "VIDEO ONLY: ${height}P ($ext) - $note$sizeStr"
                btn.layoutParams = btnParams
                btn.setBackgroundResource(R.drawable.bg_rounded_button)
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF444444.toInt())
                btn.setTextColor(0xFFFFFFFF.toInt())
                btn.setOnClickListener { downloadFormat(url, formatId, false) }
                llFormats.addView(btn)
            } else if (!isVideo && isAudio) {
                val btn = Button(this@MainActivity)
                btn.text = "AUDIO ONLY: ($ext) - $note$sizeStr"
                btn.layoutParams = btnParams
                btn.setBackgroundResource(R.drawable.bg_rounded_button)
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF327a32.toInt())
                btn.setTextColor(0xFFFFFFFF.toInt())
                btn.setOnClickListener { downloadFormat(url, formatId, false) }
                llFormats.addView(btn)
            } else if (isVideo && isAudio) {
                val btn = Button(this@MainActivity)
                btn.text = "COMBINED: ${height}P ($ext)$sizeStr"
                btn.layoutParams = btnParams
                btn.setBackgroundResource(R.drawable.bg_rounded_button)
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF5500.toInt())
                btn.setTextColor(0xFFFFFFFF.toInt())
                btn.setOnClickListener { downloadFormat(url, formatId, false) }
                llFormats.addView(btn)
            }
        }
    }


    private fun downloadFormat(url: String, formatSelector: String, wantsMerge: Boolean) {
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Initializing download..."
        
        Thread {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                
                val request = YoutubeDLRequest(url)
                request.addOption("-f", formatSelector)
                request.addOption("--no-update")

                if (wantsMerge) {
                    request.addOption("--merge-output-format", "mp4")
                }

                request.addOption("-o", "${downloadsDir.absolutePath}/%(title)s.%(ext)s")

                YoutubeDL.getInstance().execute(request, "DownloadTask") { progress, etaInSeconds, line ->
                    runOnUiThread {
                        tvStatus.text = "Downloading... $progress% (ETA: ${etaInSeconds}s)"
                    }
                }
                
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    tvStatus.text = "Download Complete! Saved to Downloads folder."
                    Toast.makeText(this@MainActivity, "Saved securely to phone native storage.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("StreamFetch", "Download failed", e)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    tvStatus.text = "Download Error: ${e.message}"
                }
            }
        }.start()
    }
}
