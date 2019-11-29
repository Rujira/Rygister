package com.codinghub.apps.rygister.repository

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.codinghub.apps.rygister.app.Injection
import com.codinghub.apps.rygister.model.compareface.CompareFaceRequest
import com.codinghub.apps.rygister.model.compareface.CompareFaceResponse
import com.codinghub.apps.rygister.model.compareface.TrainFaceRequest
import com.codinghub.apps.rygister.model.compareface.TrainFaceResponse
import com.codinghub.apps.rygister.model.error.ApiError
import com.codinghub.apps.rygister.model.error.Either
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object RemoteRepository : Repository {

    private val api = Injection.provideRygisterApi()

    override fun compareFaces(request: CompareFaceRequest): LiveData<Either<CompareFaceResponse>> {
        val liveData = MutableLiveData<Either<CompareFaceResponse>>()
        api.compareFace(request).enqueue(object : Callback<CompareFaceResponse>{

            override fun onResponse(call: Call<CompareFaceResponse>, response: Response<CompareFaceResponse>) {
                if(response != null && response.isSuccessful) {
                    liveData.value = Either.success(response.body())
                } else {
                    liveData.value = Either.error(ApiError.COMPARE, null)
                }
            }

            override fun onFailure(call: Call<CompareFaceResponse>, t: Throwable) {
                liveData.value = Either.error(ApiError.COMPARE, null)
            }
        })
        return liveData
    }

    override fun trainFace(request: TrainFaceRequest): LiveData<Either<TrainFaceResponse>> {
        val liveData = MutableLiveData<Either<TrainFaceResponse>>()
        api.trainFace(request).enqueue(object : Callback<TrainFaceResponse>{

            override fun onResponse(call: Call<TrainFaceResponse>, response: Response<TrainFaceResponse>) {
                if(response != null && response.isSuccessful) {
                    liveData.value = Either.success(response.body())
                } else {
                    liveData.value = Either.error(ApiError.TRAIN, null)
                }
            }

            override fun onFailure(call: Call<TrainFaceResponse>, t: Throwable) {
                liveData.value = Either.error(ApiError.TRAIN, null)
            }
        })
        return liveData
    }

    @SuppressLint("Recycle")
    override fun modifyImageOrientation(activity: Activity, bitmap: Bitmap, uri: Uri): Bitmap {

        val columns = arrayOf(MediaStore.MediaColumns.DATA)
        val c = activity.contentResolver.query(uri, columns, null, null, null)
        if (c == null) {
            Log.d("modifyOrientation", "Could not get cursor")
            return bitmap
        }

        c.moveToFirst()
        Log.d("modifyOrientation", c.getColumnName(0))
        val str = c.getString(0)
        if (str == null) {
            Log.d("modifyOrientation", "Could not get exif")
            return bitmap
        }
        Log.d("modifyOrientation", "get cursor");
        val exifInterface = ExifInterface(c.getString(0)!!)
        val exifR : Int = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        val orientation : Float =
            when (exifR) {
                ExifInterface.ORIENTATION_ROTATE_90 ->  90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

        val mat : Matrix? = Matrix()
        mat?.postRotate(orientation)
        return Bitmap.createBitmap(bitmap as Bitmap, 0, 0, bitmap?.width as Int,
            bitmap.height as Int, mat, true)
    }
}
