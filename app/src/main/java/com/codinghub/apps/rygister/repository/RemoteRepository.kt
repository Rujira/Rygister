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
import com.codinghub.apps.rygister.model.DeptResponse.DeptResponse
import com.codinghub.apps.rygister.model.compareface.CompareFaceRequest
import com.codinghub.apps.rygister.model.compareface.CompareFaceResponse
import com.codinghub.apps.rygister.model.error.ApiError
import com.codinghub.apps.rygister.model.error.Either
import com.codinghub.apps.rygister.model.login.LoginRequest
import com.codinghub.apps.rygister.model.login.LoginResponse
import com.codinghub.apps.rygister.model.qrcode.QRCodeRequest
import com.codinghub.apps.rygister.model.qrcode.QRCodeResponse
import com.codinghub.apps.rygister.model.register.RegisterRequest
import com.codinghub.apps.rygister.model.register.RegisterResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object RemoteRepository : Repository {


    private val api = Injection.provideRygisterApi()

    override fun rygisterLogin(request: LoginRequest): LiveData<Either<LoginResponse>> {

        val loginAPI = Injection.provideLoginAPI()
        val liveData = MutableLiveData<Either<LoginResponse>>()
        loginAPI.rygisterLogin(request).enqueue(object : Callback<LoginResponse> {

            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    liveData.value = Either.success(response.body())
                } else {
                    liveData.value = Either.error(ApiError.LOGIN, null)
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                liveData.value = Either.error(ApiError.LOGIN, null)
            }
        })
        return liveData
    }

    override fun checkQRCode(request: QRCodeRequest): LiveData<Either<QRCodeResponse>> {

        val liveData = MutableLiveData<Either<QRCodeResponse>>()
        api.checkQRCode(request).enqueue(object : Callback<QRCodeResponse> {

            override fun onResponse(call: Call<QRCodeResponse>, response: Response<QRCodeResponse>) {
                if(response.isSuccessful) {
                    liveData.value = Either.success(response.body())
                } else {
                    liveData.value = Either.error(ApiError.QRCODE, null)
                }
            }

            override fun onFailure(call: Call<QRCodeResponse>, t: Throwable) {
                liveData.value = Either.error(ApiError.QRCODE, null)
            }
        })
        return liveData
    }

    override fun getDeptID(): LiveData<Either<DeptResponse>> {

        val liveData = MutableLiveData<Either<DeptResponse>>()
        api.getDeptID().enqueue(object : Callback<DeptResponse>{
            override fun onResponse(call: Call<DeptResponse>, response: Response<DeptResponse>) {
                if (response.isSuccessful) {
                    liveData.value = Either.success(response.body())
                } else {
                    liveData.value = Either.error(ApiError.GETDEPT, null)
                }
            }

            override fun onFailure(call: Call<DeptResponse>, t: Throwable) {
                liveData.value = Either.error(ApiError.GETDEPT, null)
            }
        })
        return liveData
    }

    override fun register(request: RegisterRequest): LiveData<Either<RegisterResponse>> {

        val liveData = MutableLiveData<Either<RegisterResponse>>()
        api.register(request).enqueue(object : Callback<RegisterResponse> {

            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if(response.isSuccessful) {
                    liveData.value = Either.success(response.body())
                } else {
                    liveData.value = Either.error(ApiError.REGISTER, null)
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                liveData.value = Either.error(ApiError.REGISTER, null)
            }
        })
        return liveData
    }

    override fun compareFaces(request: CompareFaceRequest): LiveData<Either<CompareFaceResponse>> {

        val cdhAPI = Injection.provideCDHDemoApi()
        val liveData = MutableLiveData<Either<CompareFaceResponse>>()
        cdhAPI.compareFace(request).enqueue(object : Callback<CompareFaceResponse>{

            override fun onResponse(call: Call<CompareFaceResponse>, response: Response<CompareFaceResponse>) {
                if(response.isSuccessful) {
                    liveData.value = Either.success(response.body())
                } else {
                    liveData.value = Either.error(ApiError.COMPARE_FACE, null)
                }
            }

            override fun onFailure(call: Call<CompareFaceResponse>, t: Throwable) {
                liveData.value = Either.error(ApiError.COMPARE_FACE, null)
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
