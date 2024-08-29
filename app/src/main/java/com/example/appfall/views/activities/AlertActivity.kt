package com.example.appfall.views.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.appfall.R
import com.example.appfall.data.models.Fall
import com.example.appfall.data.models.FallWithoutID
import com.example.appfall.data.models.Notification
import com.example.appfall.data.models.Place
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.databinding.ActivityAlertBinding
import com.example.appfall.services.InferenceService
import com.example.appfall.services.LocationHelper
import com.example.appfall.services.NetworkHelper
import com.example.appfall.services.NotificationHelper
import com.example.appfall.services.SmsHelper
import com.example.appfall.services.SoundHelper
import com.example.appfall.viewModels.ContactsViewModel
import com.example.appfall.viewModels.FallsViewModel
import com.example.appfall.viewModels.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class AlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertBinding
    private lateinit var timer: CountDownTimer
    private lateinit var locationHelper: LocationHelper
    private lateinit var smsHelper: SmsHelper
    private lateinit var soundHelper: SoundHelper
    private var timeLeftInMillis: Long = 5000 // 30 seconds in milliseconds
    private lateinit var userDao: UserDao
    private lateinit var fallsViewModel: FallsViewModel
    private lateinit var fallId: String
    private lateinit var userViewModel: UserViewModel
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var networkHelper: NetworkHelper

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        fallsViewModel = ViewModelProvider(this)[FallsViewModel::class.java]
        contactsViewModel = ViewModelProvider(this)[ContactsViewModel::class.java]
        networkHelper = NetworkHelper(this)

        userDao = AppDatabase.getInstance(this).userDao()
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

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
                userViewModel.updateInDangerStatus(false)
                Log.d("UserFallInfoAlert", userDao.getUser()?.inDanger.toString())
            }
            addFallToDatabase("false")
            restartModel()
        }

        binding.buttonSave.setOnClickListener {
            handleFallEvent("rescued")

        }

        binding.buttonFalseAlert.setOnClickListener {
            handleFallEvent("false")
        }

        startTimer()

        observeAddedFall()
    }


    private fun startTimer() {
        binding.progressTimer.max = (timeLeftInMillis / 1000).toInt()

        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimer()
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onFinish() {
                binding.progressTimer.progress = 0
                // Show the other buttons when the timer finishes
                binding.buttonIgnore.visibility = View.GONE
                binding.buttonSave.visibility = View.VISIBLE
                binding.buttonFalseAlert.visibility = View.VISIBLE
                sendNotification("Une chute a été détectée")
                lifecycleScope.launch(Dispatchers.IO) {
                    addFallToDatabase("active")
                }
            }
        }.start()
    }

    private fun updateTimer() {
        val secondsLeft = (timeLeftInMillis / 1000).toInt()
        binding.progressTimer.progress = secondsLeft
        binding.timerText.text = secondsLeft.toString()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleFallEvent(status: String) {
        sendNotification(status)
        updateFallStatus(status)
        lifecycleScope.launch(Dispatchers.IO) {
            userViewModel.updateInDangerStatus(false)
            Log.d("UserFallInfoAlertFalse", userDao.getUser()?.inDanger.toString())
        }
        restartModel()
    }

    private fun sendNotification(message: String) {
        if (networkHelper.isInternetAvailable()) {
            sendPushNotification(message)
        } else {
            sendSMS(message)
        }
    }

    private fun sendPushNotification(message: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val user = userDao.getUser()
            val notification = user?.let {
                Notification(
                    //topic = it.phone,
                    topic = "news",
                    title = "Notification de chute",
                    message = message
                )
            }
            if (notification != null) {
                withContext(Dispatchers.Main) {
                    fallsViewModel.sendNotification(notification)
                }
            }
        }

    }

    private fun sendSMS(message: String) {
        contactsViewModel.observeContactsList().observe(this) { contacts ->
            contacts.forEach { contact ->
                smsHelper.sendSMS(contact.phone, message)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentDateTimeFormatted(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        val instant = Instant.ofEpochMilli(System.currentTimeMillis())
        val offsetDateTime = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)
        return offsetDateTime.format(formatter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addFallToDatabase(status: String) {
        val fall = FallWithoutID(
            place = Place(
                latitude = 0.0,
                longitude = 0.0
            ),
            status = status,
            dateTime = getCurrentDateTimeFormatted()
        )
        Log.d("dateTime",fall.dateTime)
        fallsViewModel.addFall(fall)
    }

    private fun observeAddedFall() {
        fallsViewModel.addFallResponse.observe(this, Observer { response ->
            response?.let {
                fallId =  it.data._id
            }
        })
    }

    private fun updateFallStatus(status: String) {
        fallsViewModel.updateFallStatus(fallId, status)
    }

    private fun restartModel() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
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

    data class Contact(val phoneNumber: String)
}
