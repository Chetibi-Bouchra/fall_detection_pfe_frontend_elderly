package com.example.appfall.views.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.input.key.Key.Companion.Window
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.example.appfall.R
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.databinding.ActivityMainBinding
import com.example.appfall.ml.CnnLstmExp1Allclasses
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.appfall.services.SoundHelper
import com.example.appfall.utils.PermissionHelper
import com.example.appfall.viewModels.ContactsViewModel
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

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var userDao: UserDao
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var soundHelper: SoundHelper

    private val windowSize = 200
    private val accX = mutableListOf<Float>()
    private val accY = mutableListOf<Float>()
    private val accZ = mutableListOf<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PermissionHelper.requestAllPermissions(this)


        // Initialize ViewModel
        contactsViewModel = ViewModelProvider(this)[ContactsViewModel::class.java]

        // Initialize SoundHelper
        soundHelper = SoundHelper(this)

        userDao = AppDatabase.getInstance(this).userDao()

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

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        } ?: run {
            Log.e("MainActivity", "Accelerometer sensor not available")
        }

        // Initialize TextToSpeech if needed
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionHelper.ALL_PERMISSIONS_REQUEST_CODE -> {
                // Find the first denied permission
                val deniedPermissionIndex = grantResults.indexOfFirst { it != PackageManager.PERMISSION_GRANTED }

                if (deniedPermissionIndex != -1) {
                    // At least one permission is denied
                    Toast.makeText(this, "Permissions not granted. Exiting app.", Toast.LENGTH_LONG).show()
                    finish() // Close the activity
                } else {
                    // All permissions granted
                    Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


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

    private fun playIgnoreSound() {
        // Implement sound playback logic here
        soundHelper.playSound(R.raw.notification_sound)
    }

    private fun launchAlertActivity() {
        val intent = Intent(this, AlertActivity::class.java)
        startActivity(intent)
    }

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
            Log.e("MainActivity", "Model inference error: ${e.message}")
            throw e
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            accX.add(event.values[0])
            accY.add(event.values[1])
            accZ.add(event.values[2])

            if (accX.size >= windowSize) {
                // Assume Window is a correct data structure or replace it
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
                Log.d("MainActivity", "Calculated features: $features")

                // Calculate feature matrix
                val featureMatrix = calculateFeatureMatrix(normalizedWindow, features, windowSize)
                Log.d("MainActivity", "Feature Matrix: ${featureMatrix.joinToString { it.joinToString(", ") }}")

                // Verify the feature matrix shape
                if (featureMatrix.size == 200 && featureMatrix[0].size == 58) {
                    // Run model inference
                    try {
                        val modelOutput = runModelInference(featureMatrix)
                        Log.d("MainActivity", "Model Output: ${modelOutput.joinToString(", ")}")
                        handleModelOutput(modelOutput)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Model inference error: ${e.message}")
                    }
                } else {
                    Log.e("MainActivity", "Feature matrix has incorrect shape: ${featureMatrix.size}x${featureMatrix[0].size}")
                }

                // Clear lists for next window
                accX.clear()
                accY.clear()
                accZ.clear()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not used in this example
    }

    private fun handleModelOutput(modelOutput: FloatArray) {
        val classes = arrayOf("BSC", "CHU", "CSI", "CSO", "FKL", "FOL", "JOG", "JUM", "LYI", "SCH", "SDL", "SIT", "STD", "STN", "STU", "WAL")
        val maxIndex = modelOutput.indices.maxByOrNull { modelOutput[it] } ?: -1
        val result = if (maxIndex != -1) classes[maxIndex] else "unknown"

        Log.d("MainActivity", "Predicted activity: $result")

        if (result in arrayOf("FOL", "FKL", "BSC", "SDL")) {
            launchAlertActivity()
        } else {
            Log.d("MainActivity", "Predicted activity: $result")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
    data class Window(val accX: List<Float>, val accY: List<Float>, val accZ: List<Float>)

    private fun runModelInference(featureMatrix: Array<Array<Float>>): FloatArray {
        // Implementation of model inference
        TODO()
    }
}
