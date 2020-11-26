package com.codinghub.apps.rygister.viewmodel

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.text.Editable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.codinghub.apps.rygister.app.Injection
import com.codinghub.apps.rygister.model.AppPrefs
import com.codinghub.apps.rygister.model.DeptResponse.DeptResponse
import com.codinghub.apps.rygister.model.compareface.CompareFaceRequest
import com.codinghub.apps.rygister.model.compareface.CompareFaceResponse
import com.codinghub.apps.rygister.model.error.Either
import com.codinghub.apps.rygister.model.login.LoginRequest
import com.codinghub.apps.rygister.model.login.LoginResponse
import com.codinghub.apps.rygister.model.qrcode.QRCodeRequest
import com.codinghub.apps.rygister.model.qrcode.QRCodeResponse
import com.codinghub.apps.rygister.model.register.PersonInformation
import com.codinghub.apps.rygister.model.register.RegisterRequest
import com.codinghub.apps.rygister.model.register.RegisterResponse
import com.codinghub.apps.rygister.model.register.VisitorData
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Injection.provideRepository()

    fun performLoginRequest(username: String, password: String) : LiveData<Either<LoginResponse>> {
        val request = LoginRequest(username, password)
        return repository.rygisterLogin(request)
    }

    fun saveUIMode(mode: String) {
        return AppPrefs.saveUIMode(mode)
    }
    fun getUIMode() = AppPrefs.getIUMode()

    fun saveServiceURL(serviceUrl: String) {
        return AppPrefs.saveServiceURL(serviceUrl)
    }
    fun getServiceURL() = AppPrefs.getServiceURL()

    fun saveUsername(username: String) {
        return AppPrefs.saveUsername(username)
    }
    fun getUsername() = AppPrefs.getHeaderUserName()

    fun savePassword(password: String){
        return AppPrefs.savePassword(password)
    }
    fun getPassword() = AppPrefs.getHeaderPassword()

    fun saveDept(dept: String) {
        return AppPrefs.saveDept(dept)
    }
    fun getDept() = AppPrefs.getDept()


    fun saveToken(token: String) {
        return AppPrefs.saveToken(token)
    }
    fun getToken() = AppPrefs.getToken()

    fun saveBackendToken(backendToken: String) {
        return AppPrefs.saveBackendToken(backendToken)
    }
    fun getBackendToken() = AppPrefs.getBackendToken()

    fun getDeptID() : LiveData<Either<DeptResponse>> {
        return repository.getDeptID()
    }

    fun register(image: String, name: String, visiteeName: String, cardNumber: String): LiveData<Either<RegisterResponse>> {

        val personInformation = PersonInformation(name, visiteeName, 1,0)
        val dept_id = AppPrefs.getDept().toString()
        val card_numbers = listOf(cardNumber)

        val tag_id_list = listOf<String>()
        val visitorData = VisitorData(image, personInformation, tag_id_list, dept_id, card_numbers)

        val request = RegisterRequest(listOf(visitorData))
        return repository.register(request)
    }

    fun modifyImageOrientation(activity: Activity, bitmap: Bitmap, uri: Uri): Bitmap {
        return repository.modifyImageOrientation(activity, bitmap, uri)
    }

    fun compareFace(picture1: String, picture2: String): LiveData<Either<CompareFaceResponse>> {
        val request = CompareFaceRequest(picture1, picture2)

        return repository.compareFaces(request)
    }

    fun saveFaceCompareMode(isEnable: Boolean) {
        AppPrefs.saveFaceCompareMode(isEnable)
    }

    fun getFaceCompareMode() = AppPrefs.getFaceCompareMode()

    fun saveBranch(name: String) {
        AppPrefs.saveCardNumberBranch(name)
    }
    fun getBranch() = AppPrefs.getCardNumberBranch()

    fun saveCurrentCardNumberPosition(position: Int) {
        AppPrefs.saveCurrentCardNumberPosition(position)
    }
    fun getCurrentCardNumberPosition() = AppPrefs.getCurrentCardNumberPosition()
}

