package com.example.appfall.views.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.appfall.R
import com.example.appfall.data.models.Fall
import com.example.appfall.data.models.FallWithoutID
import com.example.appfall.data.models.Place
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.databinding.ActivityAlertBinding
import com.example.appfall.services.LocationHelper
import com.example.appfall.services.NotificationHelper
import com.example.appfall.services.SmsHelper
import com.example.appfall.services.SoundHelper
import com.example.appfall.viewModels.FallsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertBinding
    private lateinit var timer: CountDownTimer
    private lateinit var locationHelper: LocationHelper
    private lateinit var smsHelper: SmsHelper
    private lateinit var soundHelper: SoundHelper
    private var timeLeftInMillis: Long = 30000 // 30 seconds in milliseconds
    private lateinit var userDao: UserDao
    private lateinit var fallsViewModel: FallsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        fallsViewModel = ViewModelProvider(this).get(FallsViewModel::class.java)

        userDao = AppDatabase.getInstance(this).userDao()
        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationHelper = LocationHelper(this)
        smsHelper = SmsHelper(this)
        soundHelper = SoundHelper(this)
        playIgnoreSound()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.buttonIgnore.setOnClickListener {
            stopIgnoreSound()
            lifecycleScope.launch(Dispatchers.IO) {
                userDao.updateInDangerStatus(false)
            }
            addFallToDatabase("false")
            finish()
        }

        binding.buttonSave.setOnClickListener {
            handleFallEvent("Sauvée")

        }

        binding.buttonFalseAlert.setOnClickListener {
            handleFallEvent("Fausse alerte")
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
                // Show the other buttons when the timer finishes
                binding.buttonIgnore.visibility = View.GONE
                binding.buttonSave.visibility = View.VISIBLE
                binding.buttonFalseAlert.visibility = View.VISIBLE
                getContactsAndSendSMS("Fall detected! Immediate assistance needed.")
                addFallToDatabase("active")
            }
        }.start()
    }

    private fun updateTimer() {
        val secondsLeft = (timeLeftInMillis / 1000).toInt()
        binding.progressTimer.progress = secondsLeft
        binding.timerText.text = secondsLeft.toString()
    }

    private fun handleFallEvent(status: String) {
        getContactsAndSendSMS(status)
        addFallToDatabase(status)
        lifecycleScope.launch(Dispatchers.IO) {
            userDao.updateInDangerStatus(false)
        }
        restartModel()
    }

    private fun getContactsAndSendSMS(message: String) {
        val contacts = getContacts()
        for (contact in contacts) {
            smsHelper.sendSMS(contact.phoneNumber, message)
        }
    }

    private fun getContacts(): List<Contact> {
        // Implement your logic to get the contacts
        // Here, I assume you have a Contact data class with a phoneNumber property
        return listOf(
            //Contact("0555529412")
            // Add more contacts as needed
        )
    }

    private fun addFallToDatabase(status: String) {
        val fall = FallWithoutID(
            place = Place(
                latitude = 0.0,
                longitude = 0.0
            ),
            status = status,
            dateTime = System.currentTimeMillis().toString()
        )
        fallsViewModel.addFall(fall)
    }

    private fun restartModel() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close the AlertActivity
    }

    private fun playIgnoreSound() {
        soundHelper.playSound(R.raw.notification_sound)
    }

    private fun stopIgnoreSound() {
        soundHelper.stopSound() // Assuming stopSound method exists in SoundHelper
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Handle permission granted
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

    private fun sendTestNotification() {
        val message = "Votre application a détecté une chute"
        val notificationIntent = Intent(this, NotificationHelper::class.java).apply {
            putExtra("title", "Notification de chute")
            putExtra("message", message)
        }
        startService(notificationIntent)
    }

    data class Contact(val phoneNumber: String)
}
