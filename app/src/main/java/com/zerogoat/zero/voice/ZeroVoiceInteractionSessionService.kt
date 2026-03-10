package com.zerogoat.zero.voice

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class ZeroVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return ZeroVoiceInteractionSession(this)
    }
}
