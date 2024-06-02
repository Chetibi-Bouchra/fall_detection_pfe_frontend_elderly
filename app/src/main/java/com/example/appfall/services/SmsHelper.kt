package com.example.appfall.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.widget.Toast

class SmsHelper(private val context: Context) {

    fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val piSent: PendingIntent = PendingIntent.getBroadcast(context, 0, Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE)
            val piDelivered: PendingIntent = PendingIntent.getBroadcast(context, 0, Intent("SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE)

            // Divide the message into parts if it's too long
            val parts = smsManager.divideMessage(message)

            // Send each part of the message
            for (part in parts) {
                smsManager.sendTextMessage(phoneNumber, null, part, piSent, piDelivered)
            }

            Toast.makeText(context, "SMS sent successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send SMS.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            println("Phone error: $e")
        }
    }
}
