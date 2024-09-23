package com.example.appfall.views.activities

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.example.appfall.services.InferenceService
import com.example.appfall.R
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.databinding.ActivityMainBinding
import com.example.appfall.services.LocationHelper
import com.example.appfall.services.NetworkHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.appfall.services.SoundHelper
import com.example.appfall.utils.PermissionHelper
import com.example.appfall.viewModels.ContactsViewModel
import com.example.appfall.viewModels.FallsViewModel
import com.example.appfall.viewModels.UserViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var userDao: UserDao
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var soundHelper: SoundHelper
    private lateinit var fallsViewModel: FallsViewModel
    private lateinit var networkHelper: NetworkHelper
    private lateinit var locationHelper: LocationHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //PermissionHelper.requestAllPermissions(this)
        networkHelper = NetworkHelper(this)
        locationHelper = LocationHelper(this)

        LocalBroadcastManager.getInstance(this).registerReceiver(sensorResultReceiver, IntentFilter("SENSOR_RESULT_ACTION"))

        // Start the service
        Intent(this, InferenceService::class.java).also { intent ->
            startService(intent)
        }

        // Initialize ViewModel
        contactsViewModel = ViewModelProvider(this)[ContactsViewModel::class.java]
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        fallsViewModel = ViewModelProvider(this)[FallsViewModel::class.java]

        // Initialize SoundHelper
        soundHelper = SoundHelper(this)

        userDao = AppDatabase.getInstance(this).userDao()

        if (networkHelper.isInternetAvailable()) {
            locationHelper.getLastLocation { location ->
                if (location != null) {
                    Log.d("MainActivity", "Location received: $location")
                    fallsViewModel.addFallsFromOfflineToDb(location.longitude, location.latitude)
                } else {
                    Log.e("MainActivity", "Location not available, falls cannot be sent.")
                }
            }
        }

        //observeInDanger()

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.btm_nav)
        val navController = Navigation.findNavController(this, R.id.host_fragment)

        NavigationUI.setupWithNavController(bottomNavigation, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.contactsFragment -> updateHeaderTitle("Contacts")
                R.id.codeFragment -> updateHeaderTitle("QR Code")
            }
        }

        binding.settingsHeader.setOnClickListener {
            startActivity(Intent(this, ParametersActivity::class.java))
        }
    }

    private fun observeInDanger() {
        Log.d("UserFallInDanger","testtss")
        userViewModel.inDanger.observe(this) { isInDanger ->
            Log.d("UserFallInDanger", isInDanger.toString())
            if (isInDanger) {
                Log.d("UserFallInDangerTrue", "inside the condition if")
                stopFallDetectionService()
                sendTestNotification()
                launchAlertActivity()
            } else {
                startFallDetectionService()
            }
        }
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        when (requestCode) {
//            PermissionHelper.ALL_PERMISSIONS_REQUEST_CODE -> {
//                // Find the first denied permission
//                val deniedPermissionIndex = grantResults.indexOfFirst { it != PackageManager.PERMISSION_GRANTED }
//
//                if (deniedPermissionIndex != -1) {
//                    // At least one permission is denied
//                    Toast.makeText(this, "Permissions not granted. Exiting app.", Toast.LENGTH_LONG).show()
//                    finish() // Close the activity
//                } else {
//                    // All permissions granted
//                    Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }


    private fun updateHeaderTitle(title: String) {
        binding.textHeader.text = title
    }

    private fun sendTestNotification() {
        val title = "Chute détectée"
        val message = "Votre application a détecté une chute"

        val notificationBuilder = NotificationCompat.Builder(this, "fall_detection_channel")
            .setSmallIcon(R.drawable.ic_fall)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        } else {
            notificationManager.notify(123, notificationBuilder.build())
        }
    }


    private fun startFallDetectionService() {
        Intent(this, InferenceService::class.java).also { intent ->
            startService(intent)
        }
    }


    private fun stopFallDetectionService() {
        Intent(this, InferenceService::class.java).also { intent ->
            stopService(intent)
        }
    }

    private fun launchAlertActivity() {
        val intent = Intent(this, AlertActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorResultReceiver)

        // Optionally stop the service if needed
        Intent(this, InferenceService::class.java).also { intent ->
            stopService(intent)
        }
    }

    private val sensorResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val result = intent?.getStringExtra("result") ?: "unknown"
            val timer = intent?.getStringExtra("timer") ?: "unknown"
            Log.d("FallResult", result)
            lifecycleScope.launch(Dispatchers.IO) {
                if (result == "fall") {
                    userViewModel.updateInDangerStatus(true)
                    Log.d("UserFallInfoTrue", userDao.getUser()?.inDanger.toString())
                    Log.d("Timer", timer)
                } else {
                    userViewModel.updateInDangerStatus(false)
                    Log.d("MainActivity", "Predicted activity: $result")
                    Log.d("Timer", timer)
                }
            }
        }
    }


}
