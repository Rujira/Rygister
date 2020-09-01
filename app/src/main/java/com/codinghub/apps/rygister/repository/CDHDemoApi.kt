package com.codinghub.apps.rygister.repository

import com.codinghub.apps.rygister.model.compareface.CompareFaceRequest
import com.codinghub.apps.rygister.model.compareface.CompareFaceResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface CDHDemoApi {

    @Headers("Accept: application/json")
    @POST("compare")
    fun compareFace(@Body body: CompareFaceRequest): Call<CompareFaceResponse>

}