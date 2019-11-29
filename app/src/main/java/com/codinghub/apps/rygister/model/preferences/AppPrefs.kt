package com.codinghub.apps.rygister.model.preferences

import android.preference.PreferenceManager
import com.codinghub.apps.rygister.app.RygisterApplication

object AppPrefs {

    private const val KEY_SERVICE_URL = "KEY_SERVICE_URL"
    private const val KEY_HEADER_USERNAME = "KEY_HEADER_USERNAME"
    private const val KEY_HEADER_PASSWORD = "KEY_HEADER_PASSWORD"
    private const val KEY_AUTO_SNAP_MODE = "KEY_AUTO_SNAP_MODE"
    private const val KEY_IS_SHOW_BOUNDING_BOX = "KEY_IS_SHOW_BOUNDING_BOX"
    private const val KEY_DISTANCE = "KEY_DISTANCE"
    private const val KEY_SIMILARITY = "KEY_SIMILARITY"

    private fun sharedPrefs() = PreferenceManager.getDefaultSharedPreferences(RygisterApplication.getAppContext())

    fun getServiceURL(): String? = sharedPrefs().getString(
        KEY_SERVICE_URL, "http://203.150.199.181/") // http://103.208.27.9:8041  //http://27.254.41.62:8050/
    fun getHeaderUserName(): String? = sharedPrefs().getString(
        KEY_HEADER_USERNAME, "CodingHubDemo01")
    fun getHeaderPassword(): String? = sharedPrefs().getString(
        KEY_HEADER_PASSWORD, "CDHDemo")

    fun saveAutoSnapMode(isEnable: Boolean) {
        sharedPrefs().edit().putBoolean(KEY_AUTO_SNAP_MODE, isEnable).apply()
    }

    fun getAutoSnapMode(): Boolean = sharedPrefs().getBoolean(KEY_AUTO_SNAP_MODE, false)

    fun saveShowBoundingBoxState(isShowBoundingBox: Boolean) {
        sharedPrefs().edit().putBoolean(KEY_IS_SHOW_BOUNDING_BOX, isShowBoundingBox).apply()
    }

    fun getShowBoundingBoxState(): Boolean = sharedPrefs().getBoolean(KEY_IS_SHOW_BOUNDING_BOX, true)

    fun saveSnapDistance(distance: Int) {
        sharedPrefs().edit().putInt(KEY_DISTANCE, distance).apply()
    }

    fun getSnapDistance(): Int = sharedPrefs().getInt(KEY_DISTANCE, 50000)

    fun saveSimilarity(similarity: Int) {
        sharedPrefs().edit().putInt(KEY_SIMILARITY, similarity).apply()
    }

    fun getSimilarity(): Int = sharedPrefs().getInt(KEY_SIMILARITY, 90)

}