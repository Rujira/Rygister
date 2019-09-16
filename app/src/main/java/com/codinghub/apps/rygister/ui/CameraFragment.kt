package com.codinghub.apps.rygister.ui


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders

import com.codinghub.apps.rygister.R
import kotlinx.android.synthetic.main.fragment_camera.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.io.ByteArrayOutputStream
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class CameraFragment : Fragment() {

    private val MAX_PREVIEW_WIDTH = 1280
    private val MAX_PREVIEW_HEIGHT = 720

    // private val MAX_PREVIEW_WIDTH = 1280
    // private val MAX_PREVIEW_HEIGHT = 720
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    private lateinit var identifyButton: Button

    private lateinit var cameraDevice: CameraDevice
    private val deviceStateCallback = object : CameraDevice.StateCallback(){
        override fun onOpened(camera: CameraDevice?) {
            Log.d(TAG, "camera device opened")
            if (camera != null) {
                cameraDevice = camera
                previewSession()
            }
        }

        override fun onDisconnected(camera: CameraDevice?) {
            Log.d(TAG, "camera device disconnected")
            camera?.close()
        }

        override fun onError(camera: CameraDevice?, error: Int) {
            Log.d(TAG, "camera device error")
            this@CameraFragment.activity?.finish()
        }
    }
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    private val cameraManager by lazy {
        activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private fun previewSession() {

        val displayMetrics = DisplayMetrics()

        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)

        var width = displayMetrics.widthPixels
        var height = displayMetrics.heightPixels -

                Log.d(TAG, width.toString())
        Log.d(TAG, height.toString())

        val surfaceTexture = previewTextureView.surfaceTexture
        surfaceTexture.setDefaultBufferSize(previewTextureView.width, previewTextureView.height)
        val surface = Surface(surfaceTexture)

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)

        cameraDevice.createCaptureSession(
            Arrays.asList(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "create capture session failed!")
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    if (session != null) {
                        captureSession = session
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                    }
                }
            }, null)
    }

    private fun closeCamera() {
        if (this::captureSession.isInitialized) {
            captureSession.close()
        }

        if (this::cameraDevice.isInitialized) {
            cameraDevice.close()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera2 Kotlin").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun <T> cameraCharacteristics(cameraId: String, key: CameraCharacteristics.Key<T>) : T {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        return when (key) {
            CameraCharacteristics.LENS_FACING -> characteristics.get(key)
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP -> characteristics.get(key)
            else -> throw IllegalArgumentException("Key not recognized")
        }
    }

    private fun cameraId(lens: Int): String {
        var deviceId = listOf<String>()
        try {
            val cameraIdList = cameraManager.cameraIdList
            deviceId = cameraIdList.filter { lens == cameraCharacteristics(it, CameraCharacteristics.LENS_FACING) }

        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
        return deviceId[0]
    }

    @SuppressLint("MissingPermission")
    private fun connectCamera() {
        val deviceId = cameraId(CameraCharacteristics.LENS_FACING_BACK)

        Log.d(TAG, "deviceId: $deviceId")
        try {
            cameraManager.openCamera(deviceId, deviceStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            Log.e(TAG, "Open camera device interrupted while opened")
        }
    }

    companion object {
        const val REQUEST_CAMERA_PERMISSION = 100
        private val TAG = CameraFragment::class.qualifiedName
        @JvmStatic fun newInstance() = CameraFragment()
    }

    private val surfaceListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "textureSurface width: $width height: $height" )
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @AfterPermissionGranted(REQUEST_CAMERA_PERMISSION)
    private fun checkCameraPermission() {
        if (EasyPermissions.hasPermissions(activity!!, Manifest.permission.CAMERA)) {
            Log.d(TAG, "App has camera permission.")
            connectCamera()
        } else {
            EasyPermissions.requestPermissions(activity!!,
                getString(R.string.camera_request_rationale), REQUEST_CAMERA_PERMISSION,
                Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (previewTextureView.isAvailable) {
            openCamera()
        } else {
            previewTextureView.surfaceTextureListener = surfaceListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()

    }

    private fun openCamera() {
        checkCameraPermission()
    }



    private fun convertBitmapToBase64String(bitmap: Bitmap): String {
        var base64String: String
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val byteArray = stream.toByteArray()

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            base64String = Base64.getEncoder().encodeToString(byteArray)
        } else {
            base64String = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
        }

        return base64String
    }



}
