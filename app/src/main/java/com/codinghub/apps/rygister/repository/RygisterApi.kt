package com.codinghub.apps.rygister.repository

import com.codinghub.apps.rygister.model.qrcode.QRCodeRequest
import com.codinghub.apps.rygister.model.qrcode.QRCodeResponse
import com.codinghub.apps.rygister.model.register.RegisterRequest
import com.codinghub.apps.rygister.model.register.RegisterResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface RygisterApi {

    @Headers("Accept: application/json")
    @POST("check_qrcode")
    fun checkQRCode(@Body body: QRCodeRequest): Call<QRCodeResponse>

    @Headers("Accept: application/json")
    @POST("register")
    fun register(@Body body: RegisterRequest): Call<RegisterResponse>

}