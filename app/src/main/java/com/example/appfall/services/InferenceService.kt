package com.example.appfall.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.appfall.ml.CnnLstmExp1Allclasses
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis
import org.apache.commons.math3.stat.descriptive.moment.Skewness
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.apache.commons.math3.stat.descriptive.rank.Median
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt
import android.speech.tts.TextToSpeech
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.appfall.R
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.viewModels.ContactsViewModel
import com.example.appfall.viewModels.FallsViewModel
import com.example.appfall.viewModels.UserViewModel
import com.example.appfall.data.models.ConnectedSupervisor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.lifecycle.Observer
import com.example.appfall.data.daoModels.FallDaoModel
import com.example.appfall.data.models.FallWithoutID
import com.example.appfall.data.models.Place
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.views.activities.AlertActivity
import com.example.appfall.views.activities.MainActivity
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.properties.Delegates

class InferenceService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var locationHelper: LocationHelper
    private var accelerometer: Sensor? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var contactsObserver: Observer<List<ConnectedSupervisor>?>? = null

    private lateinit var smsHelper: SmsHelper
    private lateinit var userDao: UserDao
    private lateinit var fallsViewModel: FallsViewModel
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var networkHelper: NetworkHelper
    private lateinit var soundHelper: SoundHelper
    private var latitude by Delegates.notNull<Double>()
    private var longitude by Delegates.notNull<Double>()


    private val windowSize = 200
    private val accX = mutableListOf<Float>()
    private val accY = mutableListOf<Float>()
    private val accZ = mutableListOf<Float>()

    private val CHANNEL_ID = "NotificationChannel"
    private val CHANNEL_ID2 = "InferenceServiceChannel"
    private val NOTIFICATION_ID = 1
    private val NOTIFICATION_ID2 = 2

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        smsHelper = SmsHelper(this)
        locationHelper = LocationHelper(this)
        userDao = AppDatabase.getInstance(this).userDao()
        fallsViewModel = FallsViewModel(application)
        contactsViewModel = ContactsViewModel(application)
        networkHelper = NetworkHelper(this)
        soundHelper = SoundHelper(this)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID2)
            .setContentTitle("Détection de chutes")
            .setContentText("La détection de chutes est activée")
            .setSmallIcon(R.drawable.ic_falls)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID2, notification)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        } ?: run {
            Log.e("SensorService", "Accelerometer sensor not available")
        }

        requestLocationUpdates()

    }

    private fun requestLocationUpdates() {
        locationHelper.startLocationUpdates { location ->
             latitude = location.latitude
             longitude = location.longitude
            Log.d("LocationUpdate2", "Latitude: $latitude, Longitude: $longitude")
        }
    }

    private fun playIgnoreSound() {
        soundHelper.playSound(R.raw.notification_sound)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Inference Service Channel",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for Inference Service"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Restart service if it's killed by the system
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            accX.add(event.values[0])
            accY.add(event.values[1])
            accZ.add(event.values[2])

            if (accX.size >= windowSize) {
                val window = Window(accX.toList(), accY.toList(), accZ.toList())

                // Normalize the window
                val meanX = 0.22124304f
                val meanY = 5.88213382f
                val meanZ = 0.70410258f
                val stdX = 3.96606625f
                val stdY = 7.09657627f
                val stdZ = 3.95054001f
                val normalizedWindow = normalizeWindow(window, meanX, meanY, meanZ, stdX, stdY, stdZ)

                // Calculate features
                val features = calculateFeatures(normalizedWindow)
                Log.d("SensorService", "Calculated features: $features")

                // Calculate feature matrix
                val featureMatrix = calculateFeatureMatrix(normalizedWindow, features, windowSize)
                Log.d("SensorService", "Feature Matrix: ${featureMatrix.joinToString { it.joinToString(", ") }}")

                // Verify the feature matrix shape
                if (featureMatrix.size == windowSize && featureMatrix[0].size == features.size + 3) {
                    // Run model inference
                    try {
                        val modelOutput = runModelInference(featureMatrix)
                        Log.d("SensorService", "Model Output: ${modelOutput.joinToString(", ")}")
                        handleModelOutput(modelOutput)
                    } catch (e: Exception) {
                        Log.e("SensorService", "Model inference error: ${e.message}")
                    }
                } else {
                    Log.e("SensorService", "Feature matrix has incorrect shape: ${featureMatrix.size}x${featureMatrix[0].size}")
                }

                // Clear lists for next window
                accX.clear()
                accY.clear()
                accZ.clear()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleModelOutput(modelOutput: FloatArray) {
        val classes = arrayOf("BSC", "CHU", "CSI", "CSO", "FKL", "FOL", "JOG", "JUM", "LYI", "SCH", "SDL", "SIT", "STD", "STN", "STU", "WAL")
        val maxIndex = modelOutput.indices.maxByOrNull { modelOutput[it] } ?: -1
        val result = if (maxIndex != -1) classes[maxIndex] else "unknown"

        Log.d("SensorService", "Predicted activity: $result")

        // Determine if the result is a fall
        if (result in arrayOf("FOL", "FKL", "BSC", "SDL")) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val isAppForeground = activityManager.runningAppProcesses.any { it.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }

            if (isAppForeground) {
                val alertIntent = Intent(this, AlertActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(alertIntent)
            } else {

                Log.d("SensorService", "App is in the foreground")
                addFall("active")
                sendNotification("Une chute a été détectée")
                playIgnoreSound()
                Log.d("SensorService", "Sending notification")

                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Chute détectée")
                    .setContentText("Une chute a été détectée. Cliquez pour ouvrir.")
                    .setSmallIcon(R.drawable.ic_fall)
                    .setFullScreenIntent(pendingIntent, true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

                val notificationManager = NotificationManagerCompat.from(this)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    return
                }
                notificationManager.notify(NOTIFICATION_ID, notification)



            }
        } else {

        }

        // Prepare the local broadcast intent
        val intent = Intent("SENSOR_RESULT_ACTION").apply {
            putExtra("result", if (result in arrayOf("FOL", "FKL", "BSC", "SDL")) "fall" else "nonfall")
            putExtra("timer", "timerValue") // Add the timer value here
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addFall(status: String) {
        Log.d("SensorService", "Adding fall with status: $status")
        if (networkHelper.isInternetAvailable()) {
            addFallToDatabase(status)
        } else {
            addFallOffline(status)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addFallToDatabase(status: String) {
        val fall = FallWithoutID(
            place = Place(
                latitude = latitude,
                longitude = longitude
            ),
            status = status,
            dateTime = getCurrentDateTimeFormatted()
        )
        Log.d("dateTime",fall.dateTime)
        fallsViewModel.addFall(fall)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addFallOffline(status: String) {
        val fall = FallDaoModel(
            id = System.currentTimeMillis().toString(),
            latitude = 0.0,
            longitude = 0.0,
            status = status,
            datetime = getCurrentDateTimeFormatted()
        )

        fallsViewModel.addFallOffline(fall)

        /*lifecycleScope.launch(Dispatchers.IO) {
            val fallsDao = AppDatabase.getInstance(this@AlertActivity).fallDao()
            Log.d("OfflineFalls", fallsDao.getAllFalls().toString())
        }*/

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentDateTimeFormatted(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        val instant = Instant.ofEpochMilli(System.currentTimeMillis())
        val offsetDateTime = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)
        return offsetDateTime.format(formatter)
    }

    private fun sendNotification(message: String) {
        if (networkHelper.isInternetAvailable()) {
            sendPushNotification(message)
        } else {
            sendSMS(message)
        }
    }

    private fun sendPushNotification(message: String) {
        coroutineScope.launch {
            val user = userDao.getUser()
            val notification = user?.let {
                com.example.appfall.data.models.Notification(
                    topic = it.phone,
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
        coroutineScope.launch {
            contactsViewModel.getContacts()
            
            val observer = Observer<List<ConnectedSupervisor>?> { contacts ->
                contacts?.forEach { contact ->
                    smsHelper.sendSMS(contact.phone, message)
                }
            }

            // Observe contacts list
            contactsViewModel.observeContactsList().observeForever(observer)

            // If you need to remove the observer later, you can call:
            // contactsViewModel.observeContactsList().removeObserver(observer)
        }

    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not used in this example
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Log.d("SensorService", "Service destroyed and resources released")
        contactsObserver?.let {
            contactsViewModel.observeContactsList().removeObserver(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Return null as we are not using binding in this example
        return null
    }

    data class Window(val accX: List<Float>, val accY: List<Float>, val accZ: List<Float>)

    private fun normalizeWindow(window: Window, meanX: Float, meanY: Float, meanZ: Float, stdX: Float, stdY: Float, stdZ: Float): Window {
        val normalizedAccX = window.accX.map { (it - meanX) / stdX }.map { if (it.isNaN()) 0f else it }
        val normalizedAccY = window.accY.map { (it - meanY) / stdY }.map { if (it.isNaN()) 0f else it }
        val normalizedAccZ = window.accZ.map { (it - meanZ) / stdZ }.map { if (it.isNaN()) 0f else it }
        return Window(normalizedAccX, normalizedAccY, normalizedAccZ)
    }

    private fun calculateFeatures(window: Window): Map<String, Float> {
        val features = mutableMapOf<String, Float>()

        val magnitude = window.accX.zip(window.accY).zip(window.accZ) { (x, y), z ->
            sqrt(x * x + y * y + z * z)
        }.map { if (it.isNaN()) 0f else it }

        val theta = window.accY.zip(window.accX) { y, x ->
            atan2(y, x)
        }.map { if (it.isNaN()) 0f else it }

        val axes = listOf("accX", "accY", "accZ")

        // Calculate per-axis features
        for (axis in axes) {
            val data = when (axis) {
                "accX" -> window.accX
                "accY" -> window.accY
                "accZ" -> window.accZ
                else -> throw IllegalArgumentException("Unknown axis: $axis")
            }
            val absData = data.map { abs(it) }.map { if (it.isNaN()) 0f else it }

            features["${axis}_mean"] = data.average().toFloat()
            features["${axis}_abs_mean"] = absData.average().toFloat()
            features["${axis}_median"] = safeEvaluate(Median(), data)
            features["${axis}_abs_median"] = safeEvaluate(Median(), absData)
            features["${axis}_std"] = safeEvaluate(StandardDeviation(), data)
            features["${axis}_abs_std"] = safeEvaluate(StandardDeviation(), absData)
            features["${axis}_skew"] = safeEvaluate(Skewness(), data)
            features["${axis}_abs_skew"] = safeEvaluate(Skewness(), absData)
            features["${axis}_kurtosis"] = safeEvaluate(Kurtosis(), data)
            features["${axis}_abs_kurtosis"] = safeEvaluate(Kurtosis(), absData)
            features["${axis}_min"] = data.minOrNull() ?: 0f
            features["${axis}_abs_min"] = absData.minOrNull() ?: 0f
            features["${axis}_max"] = data.maxOrNull() ?: 0f
            features["${axis}_abs_max"] = absData.maxOrNull() ?: 0f
        }

        // Features for magnitude and theta
        features["magnitude_mean"] = magnitude.average().toFloat()
        features["magnitude_std"] = safeEvaluate(StandardDeviation(), magnitude)
        features["theta_mean"] = theta.average().toFloat()
        features["theta_std"] = safeEvaluate(StandardDeviation(), theta)

        // Features only for theta
        features["theta_skew"] = safeEvaluate(Skewness(), theta)
        features["theta_kurtosis"] = safeEvaluate(Kurtosis(), theta)

        // Features only for magnitude
        features["magnitude_min"] = magnitude.minOrNull() ?: 0f
        features["magnitude_max"] = magnitude.maxOrNull() ?: 0f
        features["zero_crossing_rate"] = magnitude.zipWithNext().count { it.first * it.second < 0 }.toFloat() / magnitude.size
        features["diff_min_max"] = (magnitude.maxOrNull() ?: 0f) - (magnitude.minOrNull() ?: 0f)

        val time = window.accX.indices.map { it.toFloat() }
        val slope = calculateSlope(time, magnitude)
        features["slope"] = if (slope.isNaN()) 0f else slope
        features["abs_slope"] = abs(features["slope"] ?: 0f)
        features["avg_acc_rate"] = magnitude.zipWithNext { a, b -> abs(b - a) }.average().toFloat()

        return features
    }

    private fun safeEvaluate(stat: Any, data: List<Float>): Float {
        return try {
            when (stat) {
                is Median -> stat.evaluate(data.map { it.toDouble() }.toDoubleArray()).toFloat()
                is StandardDeviation -> if (data.size > 1) stat.evaluate(data.map { it.toDouble() }.toDoubleArray()).toFloat() else 0f
                is Skewness -> if (data.size > 1) stat.evaluate(data.map { it.toDouble() }.toDoubleArray()).toFloat() else 0f
                is Kurtosis -> if (data.size > 1) stat.evaluate(data.map { it.toDouble() }.toDoubleArray()).toFloat() else 0f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    private fun calculateSlope(time: List<Float>, axis: List<Float>): Float {
        val validTime = time.map { if (it.isNaN()) 0f else it }
        val validAxis = axis.map { if (it.isNaN()) 0f else it }

        val meanX = validTime.average().toFloat()
        val meanY = validAxis.average().toFloat()

        val numerator = validTime.zip(validAxis).sumOf { (x, y) -> (x - meanX) * (y - meanY).toDouble() }
        val denominator = validTime.sumOf { x -> ((x - meanX).pow(2)).toDouble() }

        return if (denominator != 0.0) (numerator / denominator).toFloat() else 0f
    }

    private fun calculateFeatureMatrix(window: Window, features: Map<String, Float>, windowSize: Int = 200): Array<FloatArray> {
        val featureMatrix = Array(windowSize) { FloatArray(features.size + 3) }

        // Fill first three columns with absolute values of the data points
        for (i in 0 until windowSize) {
            featureMatrix[i][0] = abs(window.accX[i])
            featureMatrix[i][1] = abs(window.accY[i])
            featureMatrix[i][2] = abs(window.accZ[i])
        }

        // Repeat the features for each row
        features.values.forEachIndexed { index, value ->
            for (i in 0 until windowSize) {
                featureMatrix[i][index + 3] = value
            }
        }

        return featureMatrix
    }

    private fun runModelInference(featureMatrix: Array<FloatArray>): FloatArray {
        try {
            val model = CnnLstmExp1Allclasses.newInstance(this)

            // Prepare the input buffer
            val byteBuffer = ByteBuffer.allocateDirect(4 * windowSize * featureMatrix[0].size)
            byteBuffer.order(ByteOrder.nativeOrder())
            for (i in featureMatrix.indices) {
                for (j in featureMatrix[i].indices) {
                    byteBuffer.putFloat(featureMatrix[i][j])
                }
            }

            // Creates inputs for reference
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, windowSize, featureMatrix[0].size), DataType.FLOAT32)
            inputFeature0.loadBuffer(byteBuffer)

            // Runs model inference and gets result
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            // Releases model resources if no longer used
            model.close()

            return outputFeature0.floatArray
        } catch (e: Exception) {
            Log.e("SensorService", "Model inference error: ${e.message}")
            throw e
        }
    }
}
