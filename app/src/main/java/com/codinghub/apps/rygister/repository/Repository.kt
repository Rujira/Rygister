package com.codinghub.apps.rygister.repository

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import com.codinghub.apps.rygister.model.compareface.CompareFaceRequest
import com.codinghub.apps.rygister.model.compareface.CompareFaceResponse
import com.codinghub.apps.rygister.model.compareface.TrainFaceRequest
import com.codinghub.apps.rygister.model.compareface.TrainFaceResponse
import com.codinghub.apps.rygister.model.error.Either

interface Repository {

    fun trainFace(request: TrainFaceRequest): LiveData<Either<TrainFaceResponse>>
    fun compareFaces(request: CompareFaceRequest): LiveData<Either<CompareFaceResponse>>
    fun modifyImageOrientation(activity: Activity, bitmap: Bitmap, uri: Uri): Bitmap

}