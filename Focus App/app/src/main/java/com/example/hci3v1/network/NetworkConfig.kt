package com.example.hci3v1.network

import okhttp3.*
import java.io.IOException

object NetworkConfig {
    private const val ESP_IP = "http://192.168.158.202"
    private val client = OkHttpClient()

    fun sendRequest(endpoint: String, params: String = "", callback: (Boolean) -> Unit) {
        val url = "$ESP_IP/$endpoint?$params"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }

    // Add new method to check proximity
    fun checkProximity(callback: (Int) -> Unit) {
        val request = Request.Builder()
            .url("$ESP_IP/get_rssi")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(0)
            }

            override fun onResponse(call: Call, response: Response) {
                val rssi = response.body?.string()?.toIntOrNull() ?: 0
                callback(rssi)
            }
        })
    }
}
