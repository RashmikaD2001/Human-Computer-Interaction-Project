package com.example.hci3v1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BlockingStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // This class is just for manifest declaration
        // Actual receiver is implemented in MyAccessibilityService
    }
}