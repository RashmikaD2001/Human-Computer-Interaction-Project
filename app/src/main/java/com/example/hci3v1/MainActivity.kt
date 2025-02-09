package com.example.hci3v1

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.hci3v1.network.NetworkConfig
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

class MainActivity : AppCompatActivity() {
    private val scoreViewModel: ScoreViewModel by viewModels()
    private var isBlocking = false
    private lateinit var radarChart: RadarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scoreTextView: TextView = findViewById(R.id.tv_score)
        val btnStartBlocker: Button = findViewById(R.id.btn_start_blocker)
        radarChart = findViewById(R.id.radarChart)

        scoreViewModel.score.observe(this, Observer { score ->
            scoreTextView.text = "Score: $score"
        })

        btnStartBlocker.setOnClickListener {
            isBlocking = !isBlocking
            btnStartBlocker.text = if (isBlocking) "Stop Blocking" else "Start Blocking"

            // Send broadcast to toggle blocking mode
            val intent = Intent("com.example.hci3v1.TOGGLE_BLOCKING")
            sendBroadcast(intent)

            if (isBlocking) {
                scoreViewModel.increaseScore(10)
                Toast.makeText(this, "Social Media Blocking Started", Toast.LENGTH_SHORT).show()
                NetworkConfig.sendRequest("start") {}
            } else {
                scoreViewModel.score.value?.let { score ->
                    NetworkConfig.sendRequest("stop", "score=$score") {}
                }
                Toast.makeText(this, "Focus session stopped", Toast.LENGTH_SHORT).show()
            }
        }

        // Check for usage access and load chart
        if (!hasUsageAccess()) {
            Toast.makeText(this, "Please grant usage access", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            val usageData = getAppUsageData()
            setupRadarChart(usageData)
        }
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getAppUsageData(): List<Pair<String, Float>> {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000 // Last 24 hours

        val targetApps = mapOf(
            "com.instagram.android" to "Instagram",
            "com.google.android.youtube" to "YouTube",
            "com.android.chrome" to "Chrome",
            "com.linkedin.android" to "LinkedIn",
            "com.netflix.mediaclient" to "Netflix",
            "com.whatsapp" to "WhatsApp",
            "com.facebook.katana" to "Facebook",
            "com.spotify.music" to "Spotify"
        )

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (usageStats.isNullOrEmpty()) {
            Log.e("AppUsage", "No usage stats data available")
            return emptyList()
        }

        val appUsageData = mutableListOf<Pair<String, Float>>()

        usageStats.forEach { usageStat ->
            if (targetApps.containsKey(usageStat.packageName)) {
                try {
                    val appName = targetApps[usageStat.packageName] ?: return@forEach
                    val usageTime = usageStat.totalTimeInForeground / 1000f // Convert to seconds

                    if (usageTime > 0) {
                        appUsageData.add(appName to usageTime)
                    }
                } catch (e: Exception) {
                    Log.e("AppUsage", "Error processing package ${usageStat.packageName}: ${e.message}")
                }
            }
        }

        targetApps.values.forEach { appName ->
            if (!appUsageData.any { it.first == appName }) {
                appUsageData.add(appName to 0f)
            }
        }

        Log.d("AppUsage", "Usage data retrieved: $appUsageData")
        return appUsageData
    }

    private fun setupRadarChart(usageData: List<Pair<String, Float>>) {
        if (usageData.isEmpty()) {
            Toast.makeText(this, "No app usage data available", Toast.LENGTH_SHORT).show()
            return
        }

        val entries = usageData.mapIndexed { index, data ->
            RadarEntry(data.second / 3600f) // Convert seconds to hours
        }

        val labels = usageData.map { it.first }

        val dataSet = RadarDataSet(entries, "App Usage (hours)").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.teal_200)
            fillColor = ContextCompat.getColor(this@MainActivity, R.color.teal_200)
            fillAlpha = 150
            setDrawFilled(true)
            lineWidth = 2f
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1f", value)
                }
            }
        }

        radarChart.apply {
            data = RadarData(dataSet)
            description.isEnabled = false

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                textSize = 12f
                textColor = ContextCompat.getColor(this@MainActivity, R.color.black)
                position = XAxis.XAxisPosition.TOP
            }

            yAxis.apply {
                setDrawLabels(true)
                axisMinimum = 0f
                labelCount = 5
                textSize = 10f
                textColor = ContextCompat.getColor(this@MainActivity, R.color.black)
            }

            legend.apply {
                textSize = 12f
                isEnabled = true
            }

            webColor = ContextCompat.getColor(this@MainActivity, R.color.gray)
            webLineWidth = 1f
            webAlpha = 100

            invalidate()
        }
    }
}
