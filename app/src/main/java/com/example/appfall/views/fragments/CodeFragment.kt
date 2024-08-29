package com.example.appfall.views.fragments

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.appfall.R
import com.example.appfall.databinding.FragmentCodeBinding
import com.example.appfall.websockets.WebSocketManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap
import android.os.Handler
import android.telephony.SmsManager
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.appfall.adapters.ContactsAdapter
import com.example.appfall.services.NetworkHelper
import com.example.appfall.viewModels.ContactsViewModel
import com.example.appfall.viewModels.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class CodeFragment : Fragment() {
    private var _binding: FragmentCodeBinding? = null
    private val binding get() = _binding!!
    private var nbMessages: Int = 0
    private lateinit var token: String
    private lateinit var phone: String
    private val viewModel: UserViewModel by viewModels()
    private lateinit var networkHelper: NetworkHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkHelper = NetworkHelper(requireContext())
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (networkHelper.isInternetAvailable()) {
            // Show progress bar and hide QR code initially
            binding.progressBar.visibility = View.VISIBLE
            binding.qrCodeImageView.visibility = View.GONE

            viewModel.getLocalUser()
            observeLocalUser()

            WebSocketManager.receivedMessage.observe(viewLifecycleOwner) { message ->
                println("WebSocket View: $message")
                if (nbMessages == 0) {
                    showConfirmationDialog(message)
                    nbMessages += 1
                } else {
                    showEndDialog(message)
                    nbMessages -= 1
                }
            }
        } else {
            binding.noNetworkLayout.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.contentLayout.visibility = View.GONE
        }


    }

    private fun observeLocalUser() {
        viewModel.localUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                phone = user.phone
                token = user.token

                val codeLength = 6
                val randomCode = generateRandomCode(codeLength)
                println("randomCode $randomCode")
                println("phone $phone")
                println("token $token")
                val secretCode = "$phone:$randomCode"
                val width = 1100
                val height = 1100

                // Generate QR code asynchronously
                lifecycleScope.launch {
                    val qrCodeBitmap = withContext(Dispatchers.IO) {
                        generateQRCode(secretCode, width, height)
                    }

                    // Update UI on the main thread
                    binding.qrCodeImageView.setImageBitmap(qrCodeBitmap)
                    binding.progressBar.visibility = View.GONE
                    binding.qrCodeImageView.visibility = View.VISIBLE

                    // Connect WebSocket and send message
                    WebSocketManager.connectWebSocket()
                    WebSocketManager.sendMessage("register:$token:$secretCode")
                }
            }
        }
    }

    private fun generateRandomCode(length: Int): String {
        val charPool: List<Char> = ('0'..'9').toList()
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun showConfirmationDialog(message: String) {
        val dialogView = layoutInflater.inflate(R.layout.pop_up_confirmation, null)
        dialogView.findViewById<TextView>(R.id.tvMessage).text = message

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.card)

        dialog.show()

        dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            WebSocketManager.sendMessage("yes")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            WebSocketManager.sendMessage("no")
            dialog.dismiss()
        }
    }

    private fun showEndDialog(message: String) {
        val dialogView = layoutInflater.inflate(R.layout.pop_up, null)
        dialogView.findViewById<TextView>(R.id.tvMessage).text = message

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.card)

        dialog.show()

        val handler = Handler()
        handler.postDelayed({
            dialog.dismiss()
        }, 3000)
    }

    private fun generateQRCode(content: String, width: Int, height: Int): Bitmap? {
        try {
            val hints: MutableMap<EncodeHintType, Any> = EnumMap(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else -0x1)
                }
            }
            return bmp
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val piSent: PendingIntent? = PendingIntent.getBroadcast(requireContext(), 0, Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE)
            val piDelivered: PendingIntent? = PendingIntent.getBroadcast(requireContext(), 0, Intent("SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE)

            // Divide the message into parts if it's too long
            val parts = smsManager.divideMessage(message)

            // Send each part of the message
            for (part in parts) {
                smsManager.sendTextMessage(phoneNumber, null, part, piSent, piDelivered)
            }

            Toast.makeText(requireContext(), "SMS sent successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to send SMS.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            println("Phone error: $e")
        }
    }
}
