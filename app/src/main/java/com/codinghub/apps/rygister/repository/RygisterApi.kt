package com.codinghub.apps.rygister.repository

import com.codinghub.apps.rygister.model.DeptResponse.DeptResponse
import com.codinghub.apps.rygister.model.login.LoginRequest
import com.codinghub.apps.rygister.model.login.LoginResponse
import com.codinghub.apps.rygister.model.qrcode.QRCodeRequest
import com.codinghub.apps.rygister.model.qrcode.QRCodeResponse
import com.codinghub.apps.rygister.model.register.RegisterRequest
import com.codinghub.apps.rygister.model.register.RegisterResponse
import retrofit2.Call
import retrofit2.http.*

interface RygisterApi {

//    @FormUrlEncoded
//    @POST("login")
//    fun rygisterLogin(@Field("username") username: String, @Field("password") password: String): Call<LoginResponse>

    @Headers("Accept: application/json")
    @POST("/park/website/login")
    fun rygisterLogin(@Body body: LoginRequest): Call<LoginResponse>

    @GET("/park/website/simpleDept")
    fun getDeptID(): Call<DeptResponse>

    @Headers("Accept: application/json")
    @POST("check_qrcode")
    fun checkQRCode(@Body body: QRCodeRequest): Call<QRCodeResponse>

    @Headers("Accept: application/json")
    @POST("/park/website/visitors")
    fun register(@Body body: RegisterRequest): Call<RegisterResponse>

}