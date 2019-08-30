package com.codinghub.apps.rygister.viewmodel

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.codinghub.apps.rygister.app.Injection
import com.codinghub.apps.rygister.model.AppPrefs
import com.codinghub.apps.rygister.model.error.Either
import com.codinghub.apps.rygister.model.qrcode.QRCodeRequest
import com.codinghub.apps.rygister.model.qrcode.QRCodeResponse
import com.codinghub.apps.rygister.model.register.RegisterRequest
import com.codinghub.apps.rygister.model.register.RegisterResponse

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Injection.provideRepository()

    fun saveUIMode(mode: String) {
        return AppPrefs.saveUIMode(mode)
    }

    fun getUIMode() = AppPrefs.getIUMode()

    fun checkQRCode(qrcode: String): LiveData<Either<QRCodeResponse>>  {
        val request = QRCodeRequest(qrcode)

        return repository.checkQRCode(request)
    }

    fun register(address: String?, country: String?, dob: String?, email: String?, facebook_id: String?,
                 gender: String?, line_id: String?, manual: String?, name: String, nation: String?, person_id: String,
                 picture: String, province: String?, qrcode: String, regtype: String, tel: String?, company: String): LiveData<Either<RegisterResponse>> {

        val request = RegisterRequest(address, country, dob, email, facebook_id, gender, line_id, manual, name, nation, person_id, picture, province, qrcode, regtype, tel, company)

        return repository.register(request)

    }

    fun modifyImageOrientation(activity: Activity, bitmap: Bitmap, uri: Uri): Bitmap {
        return repository.modifyImageOrientation(activity, bitmap, uri)
    }
}