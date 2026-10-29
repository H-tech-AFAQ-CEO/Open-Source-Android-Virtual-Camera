
package com.virtualcamera

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TargetAppPickerActivity : AppCompatActivity() {

    private lateinit var appList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(15, 23, 42)
        window.navigationBarColor = Color.rgb(15, 23, 42)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 20, 16, 16)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }

        val title = TextView(this).apply {
            text = "SELECT TARGET APP"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 20)
        }

        appList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this)
        scroll.addView(appList)

        root.addView(title)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
        loadApps()
    }

    @Suppress("DEPRECATION")
    private fun loadApps() {
        val pm = packageManager

        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(0)
            )
        } else {
            pm.getInstalledApplications(0)
        }

        val launchableApps = apps.mapNotNull { appInfo ->
            val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                ?: return@mapNotNull null

            Triple(
                pm.getApplicationLabel(appInfo).toString(),
                appInfo.packageName,
                launchIntent
            )
        }.sortedBy { it.first.lowercase() }

        if (launchableApps.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No launchable apps found"
                setTextColor(Color.WHITE)
                textSize = 16f
                gravity = Gravity.CENTER
            }
            appList.addView(empty)
            return
        }

        launchableApps.forEach { (appName, packageName, _) ->
            val button = Button(this).apply {
                text = "$appName\n$packageName"
                isAllCaps = false

                setOnClickListener {
                    val result = Intent().apply {
                        putExtra("selected_package", packageName)
                        putExtra("selected_app_name", appName)
                    }

                    setResult(Activity.RESULT_OK, result)
                    finish()
                }

                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4, 0, 4)
                }
            }

            appList.addView(button)
        }
    }
}
