package com.codinghub.apps.rygister.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.codinghub.apps.rygister.app.Injection
import com.codinghub.apps.rygister.model.compareface.CompareFaceRequest
import com.codinghub.apps.rygister.model.compareface.CompareFaceResponse
import com.codinghub.apps.rygister.model.compareface.TrainFaceRequest
import com.codinghub.apps.rygister.model.compareface.TrainFaceResponse
import com.codinghub.apps.rygister.model.error.Either

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Injection.provideRepository()

    fun trainFace(picture: String, name: String, person_id: String): LiveData<Either<TrainFaceResponse>> {

        val request = TrainFaceRequest(picture, name, person_id)

        return repository.trainFace(request)
    }

    fun compareFace(picture1: String, picture2: String): LiveData<Either<CompareFaceResponse>> {
        val request = CompareFaceRequest(picture1, picture2)

        return repository.compareFaces(request)
    }


}