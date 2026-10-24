
package com.virtualcamera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import android.view.ViewGroup
import android.graphics.Color
import android.view.Gravity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var statusText: TextView
    private lateinit var loopButton: Button

    private var selectedVideoUri: Uri? = null
    private var isLooping = true
    private var isStarted = false

    private val videoPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            selectedVideoUri = uri
            videoView.setVideoURI(uri)
            videoView.setOnPreparedListener { player ->
                player.isLooping = isLooping
                videoView.start()
                statusText.text = "Video ready"
            }
            statusText.text = "Loading video..."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(12, 18, 32)
        window.navigationBarColor = Color.rgb(12, 18, 32)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            gravity = Gravity.TOP
            setBackgroundColor(Color.rgb(12, 18, 32))
        }

        val title = TextView(this).apply {
            text = "VIRTUAL CAMERA"
            textSize = 25f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "Choose a video to begin"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(8, 20, 8, 20)
        }

        videoView = VideoView(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        val previewParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply {
            setMargins(0, 12, 0, 16)
        }

        val chooseButton = makeButton("SELECT VIDEO") {
            videoPicker.launch(arrayOf("video/*"))
        }

        loopButton = makeButton("LOOP: ON") {
            isLooping = !isLooping
            loopButton.text = if (isLooping) "LOOP: ON" else "LOOP: OFF"
            videoView.setOnPreparedListener { player ->
                player.isLooping = isLooping
                statusText.text = "Video ready"
            }
        }

        val startButton = makeButton("START SERVICE") {
            val uri = selectedVideoUri
            if (uri == null) {
                statusText.text = "Please select a video first"
                return@makeButton
            }

            val serviceIntent = Intent(
                this,
                VirtualCameraService::class.java
            ).apply {
                action = VirtualCameraService.ACTION_START
                putExtra(
                    VirtualCameraService.EXTRA_VIDEO_URI,
                    uri.toString()
                )
            }

            statusText.text =
                "Service request sent. Camera feed is not yet verified."

            try {
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                isStarted = true
            } catch (e: Exception) {
                statusText.text = "Could not start service: ${e.message}"
            }
        }

        val stopButton = makeButton("STOP SERVICE") {
            val stopIntent = Intent(
                this,
                VirtualCameraService::class.java
            ).apply {
                action = VirtualCameraService.ACTION_STOP
            }

            try {
                startService(stopIntent)
                videoView.pause()
                isStarted = false
                statusText.text = "Stop request sent"
            } catch (e: Exception) {
                statusText.text = "Could not stop service"
            }
        }

        root.addView(title)
        root.addView(statusText)
        root.addView(videoView, previewParams)
        root.addView(chooseButton)
        root.addView(loopButton)
        root.addView(startButton)
        root.addView(stopButton)

        setContentView(root)
    }

    private fun makeButton(
        label: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = 15f
            isAllCaps = false
            setOnClickListener { action() }

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 5, 0, 5)
            }
        }
    }

    override fun onDestroy() {
        if (::videoView.isInitialized) {
            videoView.stopPlayback()
        }
        super.onDestroy()
    }
}
