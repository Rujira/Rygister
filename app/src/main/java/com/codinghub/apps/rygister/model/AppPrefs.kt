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
    private const val KEY_DEPT_ID = "KEY_DEPT_ID"

    private const val KEY_TOKEN = "KEY_TOKEN"
    private const val KEY_BACKEND_TOKEN = "KEY_BACKEND_TOKEN"

    private fun sharedPrefs() = PreferenceManager.getDefaultSharedPreferences(RygisterApplication.getAppContext())

    fun saveServiceURL(url: String) {
        sharedPrefs().edit().putString(KEY_SERVICE_URL, url).apply()
    }
    fun getServiceURL(): String? = sharedPrefs().getString(KEY_SERVICE_URL, "https://peerawat.dyndns.biz:18882")

    fun saveUsername(username: String) {
        sharedPrefs().edit().putString(KEY_HEADER_USERNAME, username).apply()
    }
    fun getHeaderUserName(): String? = sharedPrefs().getString(KEY_HEADER_USERNAME, "SuperAdmin")

    fun savePassword(password: String) {
        sharedPrefs().edit().putString(KEY_HEADER_PASSWORD, password).apply()
    }
    fun getHeaderPassword(): String? = sharedPrefs().getString(KEY_HEADER_PASSWORD, "1364ca313dd288a31f96694118891866")

    fun saveDept(dept: String) {
        sharedPrefs().edit().putString(KEY_DEPT_ID, dept).apply()
    }
    fun getDept(): String? = sharedPrefs().getString(KEY_DEPT_ID, "5f48924df8629600011b11ac")

    fun saveUIMode(mode: String) {
        sharedPrefs().edit().putString(KEY_UI_MODE, mode).apply()
    }
    fun getIUMode(): String? = sharedPrefs().getString(KEY_UI_MODE, UIMode.STAFF.mode)

    fun saveToken(token: String) {
        sharedPrefs().edit().putString(KEY_TOKEN, token).apply()
    }
    fun getToken(): String? = sharedPrefs().getString(KEY_TOKEN, "")

    fun saveBackendToken(backendToken: String) {
        sharedPrefs().edit().putString(KEY_BACKEND_TOKEN, backendToken).apply()
    }
    fun getBackendToken(): String? = sharedPrefs().getString(KEY_BACKEND_TOKEN, "")





}