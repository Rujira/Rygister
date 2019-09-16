package com.codinghub.apps.rygister.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.codinghub.apps.rygister.R
import com.codinghub.apps.rygister.model.UIMode
import com.codinghub.apps.rygister.model.error.ApiError
import com.codinghub.apps.rygister.model.error.Status
import com.codinghub.apps.rygister.thcard.SmartCardDevice
import com.codinghub.apps.rygister.thcard.ThaiSmartCard
import com.codinghub.apps.rygister.viewmodel.MainViewModel
import com.google.zxing.Result
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.choose_image_sheet.view.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mainViewModel: MainViewModel

    var task = MyTask()
    private val CARD_READ = 1
    private val CARD_EMPTY = 2
    private val CARD_DETACHED = 3
    private var card_status = CARD_EMPTY

    var smartCardReader: SmartCardDevice? = null
    private lateinit var receiver: BroadcastReceiver

    private var usbDeviceIsAttached = false

    internal val TAG = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        supportActionBar!!.elevation = 0.0f

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        reset()
        updateConnection()
        updateUI()
        checkCardReaderAttached()
        if (task.status != AsyncTask.Status.RUNNING) {
            task.execute()
        }

        ageTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        similarityTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })


        resetButton.setOnClickListener {
            reset()
        }

        registerButton.setOnClickListener {
            showCamera()
        }


    }

    private fun showCamera() {
        
//        val dialogBuilder = AlertDialog.Builder(this)
//        val dialogView = this.layoutInflater.inflate(R.layout.fragment_camera, null)
//        dialogBuilder.setView(dialogView)
//        dialogBuilder.setCancelable(true)
//
//        val cameraDialog = dialogBuilder.create()
//        cameraDialog.show()
    }


    private fun updateConnection() {
        // Register USB Broadcast for Detecting USB
        Log.d(TAG, "updateConnection()")
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                when (intent?.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> handleUsbAttached()
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> handleUsbDetached()
                }
            }
        }
        this.registerReceiver(receiver, filter)
    }

    fun handleUsbAttached() {

        usbDeviceIsAttached = true
        checkCardReaderAttached()
    }

    fun handleUsbDetached() {

        usbDeviceIsAttached = false
        card_status = CARD_EMPTY
        smartCardReader = null
    }

    private fun checkCardReaderAttached() {
        Log.d(TAG, "checkCardReaderAttached()")
        if (smartCardReader == null) {
            smartCardReader = SmartCardDevice.getSmartCardDevice(
                this.applicationContext,
                object : SmartCardDevice.SmartCardDeviceEvent {

                    override fun OnReady(device: SmartCardDevice) {

                        if (task.status != AsyncTask.Status.RUNNING) {
                            task.execute()
                        }
                    }

                    override fun OnDetached(device: SmartCardDevice) {
                        Log.d(TAG, "KeyCard Detached" + device.deviceProductName)

                    }
                }
            )
        }
    }

    fun readDataFromCardReader() {

        if (smartCardReader != null) {

            val thaiSmartCard = ThaiSmartCard(smartCardReader)
            if (thaiSmartCard.isInserted) {
                if (card_status != CARD_READ) {

                    val info = thaiSmartCard.personalInformation

                    if (info != null) {
                        similarityTextView.setText(info.PersonalID)
                        ageTextView.setText(info.NameTH)
                        faceImageView.setImageBitmap(thaiSmartCard.personalPicture)

                        updateUI()

                        card_status = CARD_READ

                    } else {

                        card_status = CARD_EMPTY
                    }

                }
            } else {

                card_status = CARD_DETACHED
            }
        } else {
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class MyTask : AsyncTask<String, Int, String>() {

        override fun onPreExecute() {
            Log.d(TAG,"MyTask onPreExecute")
            //checkCardReaderAttached()
        }

        override fun doInBackground(vararg params: String): String {
            var i =0
            while (i != -1) {
                Thread.sleep(1000)
                i++
                if (usbDeviceIsAttached) {

                    runOnUiThread {
                        readDataFromCardReader()
                    }
                }
            }
            return ""
        }

        override fun onPostExecute(result: String) {
            Log.d(TAG, "Post Ex ${result}")
        }
    }



    override fun onStart() {
      //  mScannerView.startCamera()
        doRequestPermission()
        super.onStart()
    }

    override fun onDestroy() {
        task.cancel(false)
        super.onDestroy()
    }

    override fun onResume() {
     //   reset()
        updateConnection()
        updateUI()
        if (task.status != AsyncTask.Status.RUNNING) {
            task.execute()
        }
        super.onResume()
    }

    private fun doRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            100 -> {
               // initScannerView()
            }
        }
    }

    private fun updateUI() {
        if(ageTextView.text!!.isNotEmpty() && similarityTextView.text!!.isNotEmpty()) {
            enableRegisterButton()
        } else {
            enableRegisterButton()
        }
    }

    private fun disableRegisterButton() {
        registerButton.isEnabled = false
    }

    private fun enableRegisterButton() {
        registerButton.isEnabled = true
    }

    private fun reset() {

        faceImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_face_image))
        ageTextView.setText("")
        similarityTextView.setText("")


        updateUI()
    }


    private fun getImageBase64(image: ImageView): String {

        val bitmap = (image.drawable as BitmapDrawable).bitmap

       // val resizedBitmap = resizeBitmap(bitmap,800,1066)

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val byteArray = stream.toByteArray()

        var base64String: String

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            Log.d(TAG, "Android O")
            base64String = Base64.getEncoder().encodeToString(byteArray)

        } else {
            Log.d(TAG, "Android Other")
            base64String = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
        }
        return base64String
    }

    private fun resizeBitmap(bitmap: Bitmap, width:Int, height:Int): Bitmap {

        return Bitmap.createScaledBitmap(
            bitmap,
            width,
            height,
            false
        )
    }



//    private fun showMapDialog(regType: String) {
//
//        Log.d(TAG, "showMapDialog")
//        val dialogBuilder = AlertDialog.Builder(this)
//        val dialogView = this.layoutInflater.inflate(R.layout.dialog_map_result, null)
//        dialogBuilder.setView(dialogView)
//        dialogBuilder.setCancelable(true)
//
//        val mapImageView = dialogView.findViewById<ImageView>(R.id.mapImageView)
//        val mapCloseButton = dialogView.findViewById<Button>(R.id.mapCloseButton)
//
//        when(regType) {
//            "VVIP" -> {
//                Log.d(TAG, "MAP VVIP")
//                mapImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.vvip_map))
//            }
//            "VIP" -> {
//                Log.d(TAG, "MAP VIP")
//                mapImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.vip_map))
//            }
//            "REGULAR" -> {
//                Log.d(TAG, "MAP REGULAR")
//                mapImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.regular_map))
//            }
//            "PRESS" -> {
//                Log.d(TAG, "MAP PRESS")
//                mapImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.press_map))
//            }
//            else -> {
//                Log.d(TAG, "MAP ELSE")
//                mapImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.vvip_map))
//            }
//        }
//
//        mapCloseButton.setOnClickListener {
//            mapDialog.dismiss()
//        }
//
//        mapImageView.setOnClickListener {
//            mapDialog.dismiss()
//        }
//
//        mapDialog = dialogBuilder.create()
//        mapDialog.show()
//
//        mapDialog.setOnDismissListener {
//            reset()
//        }
//    }

}

