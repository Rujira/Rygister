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

class MainActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    private lateinit var mainViewModel: MainViewModel

    private lateinit var mScannerView: ZXingScannerView

    private lateinit var qrCodeDialog: AlertDialog
    
    private lateinit var mapDialog: AlertDialog

    var task = MyTask()
    private val CARD_READ = 1
    private val CARD_EMPTY = 2
    private val CARD_DETACHED = 3
    private var card_status = CARD_EMPTY

    var smartCardReader: SmartCardDevice? = null
    private lateinit var receiver: BroadcastReceiver

    private var usbDeviceIsAttached = false

    internal val TAG = MainActivity::class.java.simpleName

    var cardTapCount: Int = 0

    private var isTakePhoto : Boolean = false

    private var image_uri: Uri? = null
    private var exif_data: Uri? = null

    private lateinit var snapImage: Bitmap

    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001
    private val IMAGE_GALLERY_CODE = 1002

    private var mAddress: String = ""
    private var mCountry: String = ""
    private var mDob: String = ""
    private var mEmail: String = ""
    private var mFacebook_id: String = ""
    private var mGender: String = ""
    private var mLine_id: String = ""
    private var mManual: String = ""
    private var mName: String = ""
    private var mNation: String = ""
    private var mPersonID: String = ""
    private var mPicture: String = ""
    private var mProvince: String = ""
    private var mQRCode: String = ""
    private var mRegType: String = ""
    private var mTel: String = ""
    private var mCompany: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        supportActionBar!!.elevation = 0.0f

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        reset()
        configureUI()
        updateConnection()
        updateUI()
        checkCardReaderAttached()
        if (task.status != AsyncTask.Status.RUNNING) {
            task.execute()
        }

        faceImageView.setOnClickListener {
            showChooseImageDialog()
        }

        fullnameTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mName = fullnameTextView.text.toString()
                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        cidTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mPersonID = cidTextView.text.toString()
                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })


        codeTextView.addTextChangedListener(object  : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mQRCode = codeTextView.text.toString()
                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        telTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mTel = telTextView.text.toString()
                updateUI()

            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        emailTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mEmail = emailTextView.text.toString()
                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        companyTextView.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                mCompany = companyTextView.text.toString()
                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        codeTextInputLayout.setEndIconOnClickListener {
            Log.d(TAG, "Clicked")
            showQRCodeDialog()
        }

        resetButton.setOnClickListener {
            reset()
        }

        mainContainer.setOnClickListener {
            Log.d(TAG, "Touch ${cardTapCount}")
            cardTapCount += 1
            if (cardTapCount == 10) {
                cardTapCount = 0

                Log.d(TAG, "Touch 10" )

                if(mainViewModel.getUIMode() == UIMode.KIOSK.mode) {
                    Log.d(TAG, "Switch to STAFF" )
                    mainViewModel.saveUIMode(UIMode.STAFF.mode)
                    configureUI()
                } else if(mainViewModel.getUIMode() == UIMode.STAFF.mode) {
                    Log.d(TAG, "Switch to KIOSK" )
                    mainViewModel.saveUIMode(UIMode.KIOSK.mode)
                    configureUI()
                }
            }
        }

        registerButton.setOnClickListener {
            register()
        }

    }

    private fun configureUI() {

        Log.d(TAG, "Mode : ${mainViewModel.getUIMode()}")
        when(mainViewModel.getUIMode()) {
            UIMode.KIOSK.mode -> {
                mManual = UIMode.KIOSK.mode
                Toast.makeText(this,"Kiosk Mode", Toast.LENGTH_SHORT).show()
                fullnameTextView.isEnabled = false
                cidTextView.isEnabled = false
                faceImageView.isEnabled = false

            }

            UIMode.STAFF.mode -> {
                mManual = UIMode.STAFF.mode
                Toast.makeText(this,"Staff Mode", Toast.LENGTH_SHORT).show()
                fullnameTextView.isEnabled = true
                cidTextView.isEnabled = true
                faceImageView.isEnabled = true

            }
        }
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
                        cidTextView.setText(info.PersonalID)
                        fullnameTextView.setText(info.NameTH)
                        faceImageView.setImageBitmap(thaiSmartCard.personalPicture)
                        showQRCodeDialog()
                        updateUI()
                        isTakePhoto = true

                        mDob = info.BirthDate
                        mGender = info.Gender
                        mAddress = info.Address

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

    private fun showQRCodeDialog() {

        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = this.layoutInflater.inflate(R.layout.dialog_qr_code, null)
        dialogBuilder.setView(dialogView)

        val speakerButton = dialogView.findViewById<Button>(R.id.speakerButton)

        if (mainViewModel.getUIMode() == UIMode.KIOSK.mode) {

            speakerButton.visibility = View.GONE
        } else {
            speakerButton.visibility = View.VISIBLE
        }


        val cameraLayout = dialogView.findViewById<FrameLayout>(R.id.frame_layout_camera)
        mScannerView = ZXingScannerView(this)
        mScannerView.setAutoFocus(true)
        mScannerView.setResultHandler(this)
        mScannerView.startCamera(1)
        cameraLayout.addView(mScannerView)

        qrCodeDialog = dialogBuilder.create()
        qrCodeDialog.show()

        qrCodeDialog.setOnDismissListener {
            Log.d(TAG, "Dismiss Stop Camera")
            mScannerView.stopCamera()
        }

        speakerButton.setOnClickListener {

            mRegType = "SPEAKER"

            codeTextView.setText(mRegType)
            regTypeTextView.text = mRegType
            zoneTextView.text =  "Zone -"

            qrCodeDialog.dismiss()
            updateUI()
        }
    }


    override fun onStart() {
      //  mScannerView.startCamera()
        doRequestPermission()
        super.onStart()
    }

    override fun onPause() {
        //mScannerView.stopCamera()

        super.onPause()
    }

    override fun onDestroy() {
        task.cancel(false)
        super.onDestroy()
    }

    override fun onResume() {
     //   reset()
        configureUI()
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
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    // Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                /* nothing to do in here */
            }
        }
    }

    override fun handleResult(rawResult: Result?) {
        Log.d(TAG, "rawResult : ${rawResult.toString()}")
       // codeTextView.setText(rawResult?.text)

        val qrDialog: android.app.AlertDialog? = SpotsDialog.Builder()
            .setContext(this)
            .setMessage("Checking for QR Code...")
            .setCancelable(false)
            .build()
            .apply {
                show()
            }

        mainViewModel.checkQRCode(rawResult.toString()).observe(this, Observer { either ->
            if (either?.status == Status.SUCCESS && either.data != null) {

                Log.d(TAG, "${either.data}")

                if (either.data.ret == 0) {

                    codeTextView.setText(rawResult?.text)
                    regTypeTextView.text = either.data.data.regtype
                    zoneTextView.text =  "Zone ${either.data.data.zone}"

                    mQRCode = rawResult.toString()
                    mRegType = either.data.data.regtype


                } else {
                    codeTextView.setText("")
                    regTypeTextView.text = ""
                    zoneTextView.text =  ""

                    mQRCode = ""
                    mRegType = ""
                    Toast.makeText( this, "Could not found Code",Toast.LENGTH_SHORT).show()
                }

                qrDialog?.dismiss()

            } else {
                if (either?.error == ApiError.QRCODE) {
                    Toast.makeText(this, "Could connect to server.", Toast.LENGTH_SHORT).show()
                }

                codeTextView.setText("")
                regTypeTextView.text = ""
                zoneTextView.text =  ""

                mQRCode = ""
                mRegType = ""

                qrDialog?.dismiss()
            }
        })

        qrCodeDialog.dismiss()
        updateUI()

    }

    private fun updateUI() {
        if(fullnameTextView.text!!.isNotEmpty() && cidTextView.text!!.isNotEmpty() && codeTextView.text!!.isNotEmpty() && isTakePhoto) {
            enableRegisterButton()
        } else {
            disableRegisterButton()
        }
    }

    private fun disableRegisterButton() {
        registerButton.isEnabled = false
    }

    private fun enableRegisterButton() {
        registerButton.isEnabled = true
    }

    private fun reset() {

        isTakePhoto = false
        faceImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_face_image))
        fullnameTextView.setText("")
        cidTextView.setText("")
        codeTextView.setText("")
        regTypeTextView.text = ""
        zoneTextView.text =  ""
        telTextView.setText("")
        emailTextView.setText("")
        companyTextView.setText("")

        mAddress = ""
        mCountry = ""
        mDob = ""
        mEmail = ""
        mFacebook_id = ""
        mGender = ""
        mLine_id = ""
        mManual = ""
        mName = ""
        mNation = ""
        mPersonID = ""
        mPicture = ""
        mProvince = ""
        mQRCode = ""
        mRegType = ""
        mTel = ""
        mCompany = ""

        updateUI()
    }

    private fun showChooseImageDialog() {

        val dialogBuilder = android.app.AlertDialog.Builder(this)
        val dialogView = this.layoutInflater.inflate(R.layout.choose_image_sheet, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(true)
        val dialog = dialogBuilder.create()

        dialogView.textViewCamera.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || ActivityCompat.checkSelfPermission(this.applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permission, PERMISSION_CODE)
                } else {
                    openCamera()
                    dialog.dismiss()
                }

            } else {
                openCamera()
                dialog.dismiss()
            }

        }
        dialogView.textViewGallery.setOnClickListener {
            openGallery()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_PICK
        startActivityForResult(intent, IMAGE_GALLERY_CODE)
    }

    private fun openCamera() {

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            isTakePhoto = true

            val ins : InputStream? = contentResolver.openInputStream(image_uri)
            val snapImage : Bitmap? = BitmapFactory.decodeStream(ins)
            ins?.close()

            if (snapImage != null) {
                faceImageView.setImageBitmap(mainViewModel.modifyImageOrientation(this ,snapImage, image_uri!!))

            }
        }

        else if (requestCode == IMAGE_GALLERY_CODE && resultCode == Activity.RESULT_OK) {

            isTakePhoto = true

            exif_data = data?.data!!
            val ins: InputStream? = contentResolver.openInputStream(exif_data)
            snapImage = BitmapFactory.decodeStream(ins)

            faceImageView.setImageBitmap(mainViewModel.modifyImageOrientation(this ,snapImage, exif_data!!))
        }

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

    private fun register() {

        Log.d(TAG, "MQR : ${mQRCode}")

        mPicture = getImageBase64(faceImageView)

        val registDialog: android.app.AlertDialog? = SpotsDialog.Builder()
            .setContext(this)
            .setMessage("Registering...")
            .setCancelable(false)
            .build()
            .apply {
                show()
            }

        mainViewModel.register(mAddress, mCountry, mDob, mEmail, mFacebook_id, mGender, mLine_id, mManual, mName, mNation,
            mPersonID, mPicture, mProvince, mQRCode, mRegType, mTel, mCompany).observe(this, Observer { either ->
            if (either?.status == Status.SUCCESS && either.data != null) {
                Log.d(TAG, "${either.data}")
                if (either.data.ret == 0) {
                    Toast.makeText(this, "Register Successful", Toast.LENGTH_SHORT).show()
                    showMapDialog(either.data.data.regtype.toUpperCase())
                   // reset()

                } else {
                    Toast.makeText(this, either.data.msg, Toast.LENGTH_SHORT).show()
                }
                registDialog?.dismiss()

            } else {
                if (either?.error == ApiError.REGISTER) {
                    Toast.makeText(this, "Could connect to server.", Toast.LENGTH_SHORT).show()
                }
                registDialog?.dismiss()
            }
        })
    }


    private fun showMapDialog(regType: String) {

        Log.d(TAG, "showMapDialog")
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = this.layoutInflater.inflate(R.layout.dialog_map_result, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(true)

        val mapImageView = dialogView.findViewById<ImageView>(R.id.mapImageView)
        val mapCloseButton = dialogView.findViewById<Button>(R.id.mapCloseButton)
        
        when(regType) {
            "VVIP" -> {
                Log.d(TAG, "MAP VVIP")
                mapImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.vvip_map))
            }
            "VIP" -> {
                Log.d(TAG, "MAP VIP")
                mapImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.vip_map))
            }
            "REGULAR" -> {
                Log.d(TAG, "MAP REGULAR")
                mapImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.regular_map))
            }
            "PRESS" -> {
                Log.d(TAG, "MAP PRESS")
                mapImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.press_map))
            }
            else -> {
                Log.d(TAG, "MAP ELSE")
                mapImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.vvip_map))
            }
        }

        mapCloseButton.setOnClickListener {
            mapDialog.dismiss()
        }

        mapImageView.setOnClickListener {
            mapDialog.dismiss()
        }
        
        mapDialog = dialogBuilder.create()
        mapDialog.show()

        mapDialog.setOnDismissListener {
            reset()
        }
    }

}

