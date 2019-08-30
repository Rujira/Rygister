package com.codinghub.apps.rygister.model

import android.preference.PreferenceManager
import android.provider.Contacts
import com.codinghub.apps.rygister.app.RygisterApplication
import com.google.gson.Gson

object AppPrefs {

    private const val KEY_SERVICE_URL = "KEY_SERVICE_URL"
    private const val KEY_HEADER_USERNAME = "KEY_HEADER_USERNAME"
    private const val KEY_HEADER_PASSWORD = "KEY_HEADER_PASSWORD"
    private const val KEY_UI_MODE = "KEY_UI_MODE"

    private fun sharedPrefs() = PreferenceManager.getDefaultSharedPreferences(RygisterApplication.getAppContext())

    fun getServiceURL(): String? = sharedPrefs().getString(KEY_SERVICE_URL, "http://27.254.41.62:8050")
    fun getHeaderUserName(): String? = sharedPrefs().getString(KEY_HEADER_USERNAME, "deepregister")
    fun getHeaderPassword(): String? = sharedPrefs().getString(KEY_HEADER_PASSWORD, "dr1234")

    fun saveUIMode(mode: String) {
        sharedPrefs().edit().putString(KEY_UI_MODE, mode).apply()
    }

    fun getIUMode(): String? = sharedPrefs().getString(KEY_UI_MODE, UIMode.KIOSK.mode)

}