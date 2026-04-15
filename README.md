# StreamFetch - Advanced Android Media Downloader

StreamFetch is a highly capable, completely serverless media downloader for Android natively powered by `yt-dlp` and `FFmpeg`. Engineered with precision, it runs the entire extraction, parsing, and remuxing lifecycle directly on your device CPU, requiring zero backend web servers.

## Features

* **Serverless Native Extraction**: Utilitzes `youtubedl-android` to natively execute Python processes on the handset.
* **Auto-Update Engine**: Bypasses the YouTube '403 Forbidden' and 'SABR' strict limitations. The headless application shell reaches out to the `yt-dlp` nightly branch at launch to download hotfixes and patch extraction protocols silently in the background.
* **Intelligent Telemetry Bypassing**: When accepting direct intent sharing from primary media apps like YouTube, it actively scrubs query tracking parameters (like `?si=`) to prevent backend algorithm flagging and throttling.
* **Format Merging Engine**: 
  * Leveraging an embedded FFmpeg binary, StreamFetch supports *Auto-Merge Extraction*. This enables the capability to bypass DASH segregation by parsing ultra-high-resolution streams (like 1080p, 4K) without sound, parsing the highest quality independent audio stream, and merging them locally into an `MP4`.
* **Raw Codec Extraction**: Disabling the auto-merge engine allows absolute granular control, retrieving raw separated codec streams like `.m4a`, `.ogg`, `.webm`, and `.mkv`.
* **Built for One UI**: Features a bespoke **Kinetic Brutalist Theme** specifically designed for modern large-screen hardware (like the Samsung Galaxy S25 Ultra). Employs ergonomic "One-Hand Operation" UX logic dragging all core interactive elements, tabs, and action boxes directly into the lower 25% thumb-reach boundary.

## Technical Stack & Architecture

* **Language**: Kotlin 
* **UI Structure**: Native XML View Binding with dynamic programmatic dataset parsing.
* **Storage**: Interfaces natively with the Android Storage Access Framework (SAF) pushing completely decoded final artifacts cleanly into your public system `Downloads/` directory for fast MediaStore scanning.
* **Dependencies**: 
    ```kotlin
    implementation("com.github.yausername.youtubedl-android:library:0.16.0")
    implementation("com.github.yausername.youtubedl-android:ffmpeg:0.16.0")
    ```

## Installation & Build

Because StreamFetch processes media directly via embedded core binaries natively to bypass backend dependency and subscription limitations, you must grant it core dependencies:

1. Clone or download the repository.
2. Ensure you have the `jitpack.io` Maven repository defined in your `settings.gradle.kts`.
3. Perform a clean Gradle Sync.
4. Launch to your device. 
*Note: Due to the presence of embedded `ffmpeg` binaries, your APK size will reflect the weight of native CPU architecture libraries (arm64-v8a).*
