package com.example.appfall.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.example.appfall.R
import com.example.appfall.websockets.WebSocketManager

class QRScannerFragment: Fragment() {
    private lateinit var codeScanner: CodeScanner

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_qr_scanner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        WebSocketManager.connectWebSocket()

        val scannerView = view.findViewById<CodeScannerView>(R.id.scanner_view)
        val activity = requireActivity()
        codeScanner = CodeScanner(activity, scannerView)
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.ALL_FORMATS

        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false

        codeScanner.decodeCallback = DecodeCallback {
            activity.runOnUiThread {
                Toast.makeText(activity, it.text, Toast.LENGTH_LONG).show()
                WebSocketManager.sendMessage("connect:eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY2M2Q2MzM3YjkxNGE1YTM3ODJlY2YyYiIsImlhdCI6MTcxNTI5OTEyN30.mJjNgnchI5NibSx_8E3fjDJfPM9iCqaAz4HgDqc4qZA:maissa1:${it.text}")

                WebSocketManager.receivedMessage.observe(viewLifecycleOwner) { message ->
                    println("WebSocket View: $message")
                    if(message == "no"){
                        Toast.makeText(activity, message, Toast.LENGTH_LONG)
                        Log.d("received message", message)
                    }

                }

            }
        }

        codeScanner.errorCallback = ErrorCallback {
            activity.runOnUiThread {
                Toast.makeText(activity, "Camera initialization error: ${it.message}", Toast.LENGTH_LONG)
            }

        }

        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }


    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }
}