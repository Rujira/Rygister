package com.codinghub.apps.rygister.app

import android.app.Application
import android.content.Context

class RygisterApplication: Application() {
    companion object {
        private lateinit var instance: RygisterApplication

        fun getAppContext(): Context = instance.applicationContext

    }

    override fun onCreate() {
        instance = this
        super.onCreate()
    }

}