package com.codinghub.apps.rygister.repository

import androidx.lifecycle.LiveData
import com.codinghub.apps.rygister.model.compareface.CompareFaceRequest
import com.codinghub.apps.rygister.model.compareface.CompareFaceResponse
import com.codinghub.apps.rygister.model.compareface.TrainFaceRequest
import com.codinghub.apps.rygister.model.compareface.TrainFaceResponse
import com.codinghub.apps.rygister.model.error.Either
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface RygisterApi {

    @Headers("Accept: application/json")
    @POST("faceapi/train")
    fun trainFace(@Body body: TrainFaceRequest): Call<TrainFaceResponse>

    @Headers("Accept: application/json")
    @POST("faceapi/compare")
    fun compareFace(@Body body: CompareFaceRequest): Call<CompareFaceResponse>
}