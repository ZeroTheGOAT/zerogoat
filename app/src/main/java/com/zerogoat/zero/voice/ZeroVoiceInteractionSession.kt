package com.zerogoat.zero.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import com.zerogoat.zero.MainActivity

class ZeroVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        
        // Launch Zero main activity to handle voice
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("start_voice", true)
        }
        context.startActivity(intent)
        
        // Hide overlay so the activity shows immediately
        hide()
    }
}
