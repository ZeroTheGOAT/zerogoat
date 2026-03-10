package com.zerogoat.zero.voice

import android.service.voice.VoiceInteractionService
import android.util.Log

class ZeroVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        Log.i("ZeroVoice", "VoiceInteractionService ready")
    }
}
