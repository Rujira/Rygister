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
    private const val KEY_CARD_POSITION = "KEY_CARD_POSITION"
    private const val KEY_BRANCH_NAME = "KEY_BRANCH_NAME"

    private const val KEY_TOKEN = "KEY_TOKEN"
    private const val KEY_BACKEND_TOKEN = "KEY_BACKEND_TOKEN"

    private fun sharedPrefs() = PreferenceManager.getDefaultSharedPreferences(RygisterApplication.getAppContext())

    fun saveServiceURL(url: String) {
        sharedPrefs().edit().putString(KEY_SERVICE_URL, url).apply()
    }
    fun getServiceURL(): String? = sharedPrefs().getString(KEY_SERVICE_URL, "https://10.50.9.1:11181")

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
    fun getDept(): String? = sharedPrefs().getString(KEY_DEPT_ID, "5f4de80ec1fa78000179b49f")

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

    //CDH API
    private const val KEY_CDH_SERVICE_URL = "KEY_CDH_SERVICE_URL"
    private const val KEY_API_KEY = "KEY_API_KEY"
    private const val KEY_AUTO_SNAP_MODE = "KEY_AUTO_SNAP_MODE"
    private const val KEY_IS_SHOW_BOUNDING_BOX = "KEY_IS_SHOW_BOUNDING_BOX"
    private const val KEY_DISTANCE = "KEY_DISTANCE"
    private const val KEY_SIMILARITY = "KEY_SIMILARITY"
    private const val KEY_CDH_USERNAME = "KEY_CDH_USERNAME"
    private const val KEY_CDH_PASSWORD = "KEY_CDH_PASSWORD"
    private const val KEY_FACE_COMPARE_MODE = "KEY_FACE_COMPARE_MODE"
    fun getCDHServiceURL(): String? = sharedPrefs().getString(KEY_CDH_SERVICE_URL, "http://27.254.41.62:8041/") // http://103.208.27.9:8041  //http://27.254.41.62:8050/
    fun getCDHUserName(): String? = sharedPrefs().getString(KEY_CDH_USERNAME, "DEMO")
    fun getCDHPassword(): String? = sharedPrefs().getString(KEY_CDH_PASSWORD, "Demon2499")


    fun getApiKey(): String? = sharedPrefs().getString(KEY_API_KEY, "ZH7mDMvggNGGaZ1q3ERhQqPSuYrafq5v")


    fun saveAutoSnapMode(isEnable: Boolean) {
        sharedPrefs().edit().putBoolean(KEY_AUTO_SNAP_MODE, isEnable).apply()
    }

    fun getAutoSnapMode(): Boolean = sharedPrefs().getBoolean(KEY_AUTO_SNAP_MODE, false)

    fun saveShowBoundingBoxState(isShowBoundingBox: Boolean) {
        sharedPrefs().edit().putBoolean(KEY_IS_SHOW_BOUNDING_BOX, isShowBoundingBox).apply()
    }

    fun getShowBoundingBoxState(): Boolean = sharedPrefs().getBoolean(KEY_IS_SHOW_BOUNDING_BOX, true)

    fun saveFaceCompareMode(isEnable: Boolean) {
        sharedPrefs().edit().putBoolean(KEY_FACE_COMPARE_MODE, isEnable).apply()
    }
    fun getFaceCompareMode(): Boolean = sharedPrefs().getBoolean(KEY_FACE_COMPARE_MODE, false)


    fun saveSnapDistance(distance: Int) {
        sharedPrefs().edit().putInt(KEY_DISTANCE, distance).apply()
    }

    fun getSnapDistance(): Int = sharedPrefs().getInt(KEY_DISTANCE, 50000)

    fun saveSimilarity(similarity: Int) {
        sharedPrefs().edit().putInt(KEY_SIMILARITY, similarity).apply()
    }

    fun getSimilarity(): Int = sharedPrefs().getInt(KEY_SIMILARITY, 90)

    fun saveCardNumberBranch(name: String) {
        sharedPrefs().edit().putString(KEY_BRANCH_NAME, name).apply()
    }

    fun getCardNumberBranch(): String? = sharedPrefs().getString(KEY_BRANCH_NAME, "card_number_n")

    fun saveCurrentCardNumberPosition(position: Int) {
        if (position < 250) {
            sharedPrefs().edit().putInt(KEY_CARD_POSITION, position).apply()
        }
    }

    fun getCurrentCardNumberPosition(): Int = sharedPrefs().getInt(KEY_CARD_POSITION, 0)

}