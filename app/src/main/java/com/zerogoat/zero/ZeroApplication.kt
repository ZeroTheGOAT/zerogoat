package com.zerogoat.zero

import android.app.Application

class ZeroApplication : Application() {

    companion object {
        lateinit var instance: ZeroApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
