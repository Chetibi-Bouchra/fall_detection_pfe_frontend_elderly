package com.example.appfall.services


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class SensorResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val result = intent?.getStringExtra("result") ?: "unknown"
        if (result == "fall") {
            Toast.makeText(context, "Fall detected!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "No fall detected", Toast.LENGTH_LONG).show()
        }
    }
}
