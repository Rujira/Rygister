package com.codinghub.apps.rygister.repository

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import com.codinghub.apps.rygister.model.error.Either
import com.codinghub.apps.rygister.model.qrcode.QRCodeRequest
import com.codinghub.apps.rygister.model.qrcode.QRCodeResponse
import com.codinghub.apps.rygister.model.register.RegisterRequest
import com.codinghub.apps.rygister.model.register.RegisterResponse

interface Repository {

    fun register(request: RegisterRequest): LiveData<Either<RegisterResponse>>
    fun checkQRCode(request: QRCodeRequest): LiveData<Either<QRCodeResponse>>
    fun modifyImageOrientation(activity: Activity, bitmap: Bitmap, uri: Uri): Bitmap

}