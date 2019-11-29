package com.codinghub.apps.rygister.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.codinghub.apps.rygister.app.Injection
import com.codinghub.apps.rygister.model.preferences.AppPrefs

class SettingsViewModel(application: Application) : AndroidViewModel(application) {


    fun saveAutoSnapMode(isEnable: Boolean) {
        AppPrefs.saveAutoSnapMode(isEnable)
    }

    fun saveShowBoundingBoxState(isShowBoundingBox: Boolean) {
        AppPrefs.saveShowBoundingBoxState(isShowBoundingBox)
    }

    fun saveSnapDistance(distance: Int) {
        AppPrefs.saveSnapDistance(distance)
    }

    fun saveSimilarity(similarity: Int) {
        AppPrefs.saveSimilarity(similarity)
    }
}