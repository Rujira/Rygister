package com.codinghub.apps.rygister.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.codinghub.apps.rygister.R
import com.codinghub.apps.rygister.model.AppPrefs
import com.codinghub.apps.rygister.model.UIMode
import com.codinghub.apps.rygister.model.error.ApiError
import com.codinghub.apps.rygister.model.error.Either
import com.codinghub.apps.rygister.model.error.Status
import com.codinghub.apps.rygister.model.login.LoginResponse
import com.codinghub.apps.rygister.model.register.RegisterResponse
import com.codinghub.apps.rygister.thcard.SmartCardDevice
import com.codinghub.apps.rygister.thcard.ThaiSmartCard
import com.codinghub.apps.rygister.viewmodel.MainViewModel
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.choose_image_sheet.view.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mainViewModel: MainViewModel
    internal val TAG = MainActivity::class.java.simpleName

    var task = MyTask()
    private val CARD_READ = 1
    private val CARD_EMPTY = 2
    private val CARD_DETACHED = 3
    private var card_status = CARD_EMPTY

    var smartCardReader: SmartCardDevice? = null
    private lateinit var receiver: BroadcastReceiver

    private var usbDeviceIsAttached = false

    private var mManual: String = ""
    private var fullName: String = ""
    private var visiteeName: String = ""
    private var cardNumber: String = ""

    var cardTapCount: Int = 0

    private var isTakePhoto : Boolean = false

    private var image_uri: Uri? = null
    private var exif_data: Uri? = null

    private lateinit var snapImage: Bitmap

    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001
    private val IMAGE_GALLERY_CODE = 1002


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        supportActionBar!!.elevation = 0.0f

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        reset()
        configureUI()
        updateConnection()
        updateUI()
        performLoginRequest()
        checkCardReaderAttached()
        if (task.status != AsyncTask.Status.RUNNING) {
            task.execute()
        }

        faceImageView.setOnClickListener {
            showChooseImageDialog()
        }

        fullnameTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                fullName = fullnameTextView.text.toString()
                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        visiteeTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                visiteeName = visiteeTextView.text.toString()
                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

//        cardNumberTextView.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                cardNumber = cardNumberTextView.text.toString()
//                updateUI()
//            }
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
//        })

        setupDropdown()

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

    private fun setupDropdown() {
        val items = listOf("985859", "979502", "981348", "980676", "960263", "961169", "960867", "960565", "967939", "968227", "967651", "969088", "968802", "963294", "963896", "964193", "964480", "948605", "949478", "949770")
        val adapter = ArrayAdapter<String>(applicationContext, R.layout.dropdown_menu_pop_item, items)
        cardNumberDropdown.setAdapter(adapter)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_connect -> {
                performLoginRequest()
                true
            }

            R.id.action_settings -> {
                showSettings()
                true
            }


            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettings() {

        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = this.layoutInflater.inflate(R.layout.dialog_settings, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(true)

        dialogBuilder.setTitle("Settings")

        val serviceURLTextView = dialogView.findViewById<EditText>(R.id.serviceURLEditText)
        val usernameTextView = dialogView.findViewById<EditText>(R.id.usernameEditText)
        val passwordTextView = dialogView.findViewById<EditText>(R.id.passwordEditText)
        val deptTextView = dialogView.findViewById<EditText>(R.id.departmentEditText)

        serviceURLTextView.text = Editable.Factory.getInstance().newEditable(mainViewModel.getServiceURL())
        usernameTextView.text = Editable.Factory.getInstance().newEditable(mainViewModel.getUsername())
        passwordTextView.text = Editable.Factory.getInstance().newEditable(mainViewModel.getPassword())
        deptTextView.text = Editable.Factory.getInstance().newEditable(mainViewModel.getDept())

        dialogBuilder.setPositiveButton("Save Change") {_, _->

            mainViewModel.saveServiceURL(serviceURLTextView.text.toString())
            mainViewModel.saveUsername(usernameTextView.text.toString())
            mainViewModel.savePassword(passwordTextView.text.toString())
            mainViewModel.saveDept(deptTextView.text.toString())

        }

        dialogBuilder.setNegativeButton("Close") { _, _->
            //pass
        }

        val dialog = dialogBuilder.create()
        dialog.show()

        val dismissButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        dismissButton.setBackgroundColor(Color.TRANSPARENT)
        dismissButton.setTextColor(ContextCompat.getColor(this.applicationContext, R.color.colorLightGray))

    }


    private fun performLoginRequest() {

        val username = mainViewModel.getUsername()
        val password = mainViewModel.getPassword()

        mainViewModel.performLoginRequest(username!!, password!!).observe(this, Observer<Either<LoginResponse>> { either ->
            if (either?.status == Status.SUCCESS && either.data != null) {
                if (either.data.rtn == 0) {
                    Log.d(TAG, "${either.data.result}")
                    mainViewModel.saveToken(either.data.result.web_authorization)
                    mainViewModel.saveBackendToken(either.data.result.backend_authorization)
                    Toast.makeText(this, "Successful Login", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to Login ${either.data.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (either?.error == ApiError.LOGIN) {
                    Toast.makeText(this, "Could not connect to server!", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun configureUI() {

        Log.d(TAG, "Mode : ${mainViewModel.getUIMode()}")
        when(mainViewModel.getUIMode()) {
            UIMode.KIOSK.mode -> {
                mManual = UIMode.KIOSK.mode
                Toast.makeText(this,"Kiosk Mode", Toast.LENGTH_SHORT).show()
                fullnameTextView.isEnabled = false
                faceImageView.isEnabled = false

            }

            UIMode.STAFF.mode -> {
                mManual = UIMode.STAFF.mode
                Toast.makeText(this,"Staff Mode", Toast.LENGTH_SHORT).show()
                fullnameTextView.isEnabled = true
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

                        fullnameTextView.setText(info.NameTH)
                        faceImageView.setImageBitmap(thaiSmartCard.personalPicture)

                        updateUI()
                        isTakePhoto = true

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

    private fun updateUI() {
        if(fullnameTextView.text!!.isNotEmpty() && visiteeTextView.text!!.isNotEmpty() && cardNumberDropdown.text!!.isNotEmpty() && isTakePhoto) {
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
        visiteeTextView.setText("")
        cardNumberDropdown.setText("")

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

     //   Log.d(TAG, "MQR : ${mQRCode}")

        val image = getImageBase64(faceImageView)

        val registDialog: android.app.AlertDialog? = SpotsDialog.Builder()
            .setContext(this)
            .setMessage("Registering...")
            .setCancelable(false)
            .build()
            .apply {
                show()
            }


        mainViewModel.register(image, fullName, visiteeName, cardNumber).observe(this, Observer<Either<RegisterResponse>> { either ->
            if (either?.status == Status.SUCCESS && either.data != null) {
                if (either.data.rtn == 0) {
                    if (either.data.result.first().rtn == 0) {
                        Toast.makeText(this, "Register Successful ${either.data.result.first().message}", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this, "Failed to Register ${either.data.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (either?.error == ApiError.REGISTER) {
                    Toast.makeText(this, "Could not connect to server.", Toast.LENGTH_SHORT).show()
                }
            }
            registDialog?.dismiss()
        })

//        mainViewModel.register(mAddress, mCountry, mDob, mEmail, mFacebook_id, mGender, mLine_id, mManual, mName, mNation,
//            mPersonID, mPicture, mProvince, mQRCode, mRegType, mTel, mCompany).observe(this, Observer { either ->
//            if (either?.status == Status.SUCCESS && either.data != null) {
//                Log.d(TAG, "${either.data}")
//                if (either.data.ret == 0) {
//                    Toast.makeText(this, "Register Successful", Toast.LENGTH_SHORT).show()
//
//                   // reset()
//
//                } else {
//                    Toast.makeText(this, either.data.msg, Toast.LENGTH_SHORT).show()
//                }
//                registDialog?.dismiss()
//
//            } else {
//                if (either?.error == ApiError.REGISTER) {
//                    Toast.makeText(this, "Could connect to server.", Toast.LENGTH_SHORT).show()
//                }
//                registDialog?.dismiss()
//            }
//        })
    }


}

