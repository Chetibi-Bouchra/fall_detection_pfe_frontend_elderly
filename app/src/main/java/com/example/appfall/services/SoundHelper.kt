package com.example.appfall.services

import android.content.Context
import android.media.MediaPlayer

class SoundHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun playSound(soundResource: Int) {
        mediaPlayer = MediaPlayer.create(context, soundResource)
        mediaPlayer?.start()
    }

    fun stopSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
