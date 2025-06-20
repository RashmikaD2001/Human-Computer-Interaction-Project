package com.example.hci3v1.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.core.content.ContextCompat
import com.example.hci3v1.R

class RadarChartHelper(private val context: Context, private val radarChart: RadarChart) {

    fun setupRadarChart() {
        val usageData = getAppUsageData()
        if (usageData.isEmpty()) {
            Toast.makeText(context, "No app usage data available", Toast.LENGTH_SHORT).show()
            return
        }

        val entries = usageData.map { RadarEntry(it.second / 3600f) }
        val labels = usageData.map { it.first }

        val dataSet = RadarDataSet(entries, "App Usage (hours)").apply {
            color = ContextCompat.getColor(context, R.color.teal_200)
            fillColor = ContextCompat.getColor(context, R.color.teal_200)
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
                textColor = ContextCompat.getColor(context, R.color.black)
                position = XAxis.XAxisPosition.TOP
            }

            yAxis.apply {
                setDrawLabels(true)
                axisMinimum = 0f
                labelCount = 5
                textSize = 10f
                textColor = ContextCompat.getColor(context, R.color.black)
            }

            legend.apply {
                textSize = 12f
                isEnabled = true
            }

            webColor = ContextCompat.getColor(context, R.color.gray)
            webLineWidth = 1f
            webAlpha = 100

            invalidate()
        }
    }

    private fun getAppUsageData(): List<Pair<String, Float>> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
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
}
