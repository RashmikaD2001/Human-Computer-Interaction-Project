package com.example.hci3v1

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.hci3v1.network.NetworkConfig
import com.example.hci3v1.utils.RadarChartHelper
import com.github.mikephil.charting.charts.RadarChart

class          MainActivity : AppCompatActivity() {
    private val scoreViewModel: ScoreViewModel by viewModels()
    private var isBlocking = false
    private lateinit var radarChartHelper: RadarChartHelper

    override fun onCreate(savedInstanceState: Bundle?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scoreTextView: TextView = findViewById(R.id.tv_score)
        val btnStartBlocker: Button = findViewById(R.id.btn_start_blocker)
        val radarChart: RadarChart = findViewById(R.id.radarChart)

        radarChartHelper = RadarChartHelper(this, radarChart)

        scoreViewModel.score.observe(this, Observer { score ->
            var textToDisplay = "Score: $score"

            if(score > 100){
                textToDisplay += "\nYou are doing great!\uD83D\uDD25 Maintain your score.\nDon't get distract by social media.\nYou have \uD83C\uDF1F \uD83C\uDF1F \uD83C\uDF1F\uD83C\uDF1F\uD83C\uDF1F stars"
            }else if(score > 0){
                textToDisplay += "\nYou can increase your score\uD83D\uDE4C. Stay focus!\nDon' get distract by social media.\nYou have \uD83C\uDF1F\uD83C\uDF1F\uD83C\uDF1F stars"
            }else if(score > -100){
                textToDisplay += "\nYou have entered danger zone\uD83D\uDE31. You can still be better.\nDon't use social media.\nYou have \uD83D\uDEA9\uD83D\uDEA9\uD83D\uDEA9 flags"
            }else{
                textToDisplay += "You are in danger zone⛔⛔. You need to stop using social media.\nPlease don't use social media.\nYou have \uD83D\uDEA9\uD83D\uDEA9\uD83D\uDEA9\uD83D\uDEA9\uD83D\uDEA9 flags"
            }
            scoreTextView.text = textToDisplay
        })

        btnStartBlocker.setOnClickListener {
            isBlocking = !isBlocking
            btnStartBlocker.text = if (isBlocking) "Stop Blocking" else "Start Blocking"

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

        if (!hasUsageAccess()) {
            Toast.makeText(this, "Please grant usage access", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            radarChartHelper.setupRadarChart()
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
}
