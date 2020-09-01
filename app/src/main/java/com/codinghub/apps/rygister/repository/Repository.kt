package com.codinghub.apps.rygister.repository

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import com.codinghub.apps.rygister.model.DeptResponse.DeptResponse
import com.codinghub.apps.rygister.model.compareface.CompareFaceRequest
import com.codinghub.apps.rygister.model.compareface.CompareFaceResponse
import com.codinghub.apps.rygister.model.error.Either
import com.codinghub.apps.rygister.model.login.LoginRequest
import com.codinghub.apps.rygister.model.login.LoginResponse
import com.codinghub.apps.rygister.model.qrcode.QRCodeRequest
import com.codinghub.apps.rygister.model.qrcode.QRCodeResponse
import com.codinghub.apps.rygister.model.register.RegisterRequest
import com.codinghub.apps.rygister.model.register.RegisterResponse

interface Repository {

    //login function
    fun rygisterLogin(request: LoginRequest): LiveData<Either<LoginResponse>>
    fun getDeptID(): LiveData<Either<DeptResponse>>

    fun register(request: RegisterRequest): LiveData<Either<RegisterResponse>>
    fun checkQRCode(request: QRCodeRequest): LiveData<Either<QRCodeResponse>>
    fun modifyImageOrientation(activity: Activity, bitmap: Bitmap, uri: Uri): Bitmap

    fun compareFaces(request: CompareFaceRequest): LiveData<Either<CompareFaceResponse>>

}