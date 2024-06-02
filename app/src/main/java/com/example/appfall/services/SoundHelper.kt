package com.example.appfall.services

import android.content.Context
import android.media.MediaPlayer
import android.media.AudioAttributes

class SoundHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun playSound(resourceId: Int) {
        // If there's already a MediaPlayer instance, release it
        mediaPlayer?.release()

        // Create a new MediaPlayer instance with the specified sound resource
        mediaPlayer = MediaPlayer.create(context, resourceId).apply {
            setOnCompletionListener {
                // Release the MediaPlayer once the sound has finished playing
                release()
            }
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            start()
        }
    }

    fun stopSound() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
    }
}
