package com.codinghub.apps.rygister.viewmodel

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.codinghub.apps.rygister.app.Injection
import com.codinghub.apps.rygister.model.AppPrefs
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

    fun register(image: String, name: String, visiteeName: String, cardNumber: String): LiveData<Either<RegisterResponse>> {

//        val json = Json {
//
//                "face_image_content" to "Non"
//                "person_information" to Json {
//                    "name" to name
//                    "visitee_name" to visiteeName
//                    "visit_time_type" to 1
//                    "visit_start_timestamp" to 0
//                }
//                "tag_id_list" to JSONArray().apply {  }
//                "dept_id" to AppPrefs.getDept()
//                "card_numbers" to JSONArray().apply {
//                    cardNumber
//                }
//        }



        //Log.d("MAINVIEWMODEL", json.toString())

        var personInformation = PersonInformation(name, visiteeName, 1,0)
        var dept_id = AppPrefs.getDept().toString()
        var card_numbers = listOf(cardNumber)

        var tag_id_list = listOf<String>()
        val visitorData = VisitorData(image, personInformation, tag_id_list, dept_id, card_numbers)


        val request = RegisterRequest(listOf(visitorData))
        return repository.register(request)
    }

    fun modifyImageOrientation(activity: Activity, bitmap: Bitmap, uri: Uri): Bitmap {
        return repository.modifyImageOrientation(activity, bitmap, uri)
    }
}

class Json() {

    private val json = JSONObject()

    constructor(init: Json.() -> Unit) : this() {
        this.init()
    }

    infix fun String.to(value: Json) {
        json.put(this, value.json)
    }

    infix fun <T> String.to(value: T) {
        json.put(this, value)
    }

    override fun toString(): String {
        return json.toString()
    }
}