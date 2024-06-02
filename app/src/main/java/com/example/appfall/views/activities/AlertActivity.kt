package com.example.appfall.views.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.appfall.R
import com.example.appfall.databinding.ActivityAlertBinding
import com.example.appfall.services.LocationHelper
import com.example.appfall.services.NotificationHelper
import com.example.appfall.services.SmsHelper
import com.example.appfall.services.SoundHelper

class AlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertBinding
    private lateinit var timer: CountDownTimer
    private lateinit var locationHelper: LocationHelper
    private lateinit var smsHelper: SmsHelper
    private lateinit var soundHelper: SoundHelper
    private var timeLeftInMillis: Long = 30000 // 30 seconds in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationHelper = LocationHelper(this)
        smsHelper = SmsHelper(this)
        soundHelper = SoundHelper(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.buttonIgnore.setOnClickListener {
            playIgnoreSound()
            //getLocationAndSendSMS()
        }

        startTimer()
    }

    private fun startTimer() {
        binding.progressTimer.max = (timeLeftInMillis / 1000).toInt()

        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimer()
            }

            override fun onFinish() {
                binding.progressTimer.progress = 0
            }
        }.start()
    }

    private fun updateTimer() {
        val secondsLeft = (timeLeftInMillis / 1000).toInt()
        binding.progressTimer.progress = secondsLeft
        binding.timerText.text = secondsLeft.toString()
    }

    private fun sendTestNotification(latitude: Double, longitude: Double) {
        Toast.makeText(this, "Notification", Toast.LENGTH_SHORT).show()
        val notificationHelper = NotificationHelper(this)
        val message = "Votre application a détecté une chute à la latitude $latitude et longitude $longitude."
        notificationHelper.sendNotification("Notification de chute", message)
    }

    private fun getLocationAndSendSMS() {
        Toast.makeText(this, "Location", Toast.LENGTH_SHORT).show()
        locationHelper.getLastLocation { location ->
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                sendTestNotification(latitude, longitude)
                sendSMSMessage(latitude, longitude)
            }
        }
    }

    private fun sendSMSMessage(latitude: Double, longitude: Double) {
        Toast.makeText(this, "SMS", Toast.LENGTH_SHORT).show()
        val phoneNumber = "0555529412"
        val mapsLink = "https://maps.google.com/?q=$latitude,$longitude"
        val message = "Votre application a détecté une chute. Voir l'emplacement ici: $mapsLink"
        smsHelper.sendSMS(phoneNumber, message)
    }

    private fun playIgnoreSound() {
        soundHelper.playSound(R.raw.notification_sound)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocationAndSendSMS()
            } else {
                // Handle the case where the user denies the permission
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationHelper.startLocationUpdates { location ->
                // Handle location updates if necessary
            }
        }
    }

    override fun onPause() {
        super.onPause()
        locationHelper.stopLocationUpdates()
    }
}
