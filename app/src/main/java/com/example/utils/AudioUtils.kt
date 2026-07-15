package com.example.utils

import android.media.AudioManager
import android.media.ToneGenerator

object AudioUtils {
    fun playSuccessBeep() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playErrorBeep() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            // Soft buzzer tone for failure or duplication warning
            toneGen.startTone(ToneGenerator.TONE_SUP_ERROR, 250)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
