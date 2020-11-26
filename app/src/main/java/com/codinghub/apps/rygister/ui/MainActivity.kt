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
import android.graphics.Matrix
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
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.codinghub.apps.rygister.R
import com.codinghub.apps.rygister.faceutillity.camera.CameraSourcePreview
import com.codinghub.apps.rygister.faceutillity.camera.GraphicOverlay
import com.codinghub.apps.rygister.faceutillity.facedetection.GraphicFaceTracker
import com.codinghub.apps.rygister.faceutillity.facedetection.GraphicFaceTrackerFactory
import com.codinghub.apps.rygister.model.AppPrefs
import com.codinghub.apps.rygister.model.DeptResponse.DeptResponse
import com.codinghub.apps.rygister.model.UIMode
import com.codinghub.apps.rygister.model.cardlist.CardList
import com.codinghub.apps.rygister.model.cardlist.CardNumber
import com.codinghub.apps.rygister.model.compareface.CompareFaceResponse
import com.codinghub.apps.rygister.model.error.ApiError
import com.codinghub.apps.rygister.model.error.Either
import com.codinghub.apps.rygister.model.error.Status
import com.codinghub.apps.rygister.model.login.LoginResponse
import com.codinghub.apps.rygister.model.register.RegisterResponse
import com.codinghub.apps.rygister.thcard.SmartCardDevice
import com.codinghub.apps.rygister.thcard.ThaiSmartCard
import com.codinghub.apps.rygister.utilities.SafeClickListener
import com.codinghub.apps.rygister.utilities.Exif
import com.codinghub.apps.rygister.viewmodel.MainViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonParseException
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.departmentDropdown
import kotlinx.android.synthetic.main.choose_image_sheet.view.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

class MainActivity : AppCompatActivity(), GraphicFaceTracker.GraphicFaceTrackerListener, GraphicFaceTracker.GraphicFaceTrackerDismissListener {

    private val listener: OnFaceSnappedListener? = null

    interface OnFaceSnappedListener {
        fun faceSnapped()
    }

    private val dismissListener: OnDialogFaceDismissListener? = null
    interface OnDialogFaceDismissListener {
        fun dismissDialog()
    }

    private val MAX_PREVIEW_WIDTH = 1024
    private val MAX_PREVIEW_HEIGHT = 1024
    private var mCameraSource: CameraSource? = null
    private var mPreview: CameraSourcePreview? = null
    private var mGraphicOverlay: GraphicOverlay? = null

    private var picture1: String = ""
    private var pictureCard: String = ""
    private var picture2: String = ""

    private var isTakingPhoto: Boolean = false

    private lateinit var faceDialog: AlertDialog
    private lateinit var loadingDialog: AlertDialog
    private lateinit var compareResultDialog: AlertDialog

    private lateinit var mainViewModel: MainViewModel
    internal val TAG = MainActivity::class.java.simpleName

    var task = MyTask()
    private val CARD_READ = 1
    private val CARD_EMPTY = 2
    private val CARD_DETACHED = 3
    private var card_status = CARD_EMPTY

    private var isAlreadyReadDataFromCard = false

    var smartCardReader: SmartCardDevice? = null
    private lateinit var receiver: BroadcastReceiver

    private var usbDeviceIsAttached = false

    private var mManual: String = ""

    private var cardType: String = ""
    private var fullName: String = ""
    private var cidNumber: String = ""
    private var visiteeName: String = ""
    private var floorNumber: String = ""
    private var department: String = ""

    private lateinit var cardObject: CardNumber
    private var cardNumber: String = ""

    var cardTapCount: Int = 0

    private var isTakePhoto : Boolean = false

    private var image_uri: Uri? = null
    private var exif_data: Uri? = null

    private lateinit var comparedPersonImage: Bitmap
    private lateinit var snapImage: Bitmap

    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001
    private val CARD_CAPTURE_CODE = 1011
    private val IMAGE_GALLERY_CODE = 1002

    companion object {
        private val TAG = "FaceTracker"
        private val RC_HANDLE_GMS = 9001
        private val RC_HANDLE_CAMERA_PERM = 2
    }

    private var deptIDList = mutableListOf<String>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        supportActionBar!!.elevation = 0.0f

        contentView.setOnTouchListener { view , _ ->
            val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }

        mainContainer.setOnTouchListener { view , _ ->
            val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }

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

        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {

        } else {
            requestCameraPermission()
        }

        faceImageView.setOnClickListener {
          //  showChooseImageDialog()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || ActivityCompat.checkSelfPermission(this.applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permission, PERMISSION_CODE)
                } else {
                    openCamera(IMAGE_CAPTURE_CODE)

                }

            } else {
                openCamera(IMAGE_CAPTURE_CODE)

            }
        }

        cardImageView.setOnClickListener {
            //  showChooseImageDialog()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || ActivityCompat.checkSelfPermission(this.applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permission, PERMISSION_CODE)
                } else {
                    openCamera(CARD_CAPTURE_CODE)

                }

            } else {
                openCamera(CARD_CAPTURE_CODE)

            }
        }

        cardTypeDropdown.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                cardType = cardTypeDropdown.text.toString()
                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        fullnameTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                fullName = fullnameTextView.text.toString()
                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        cidTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                cidNumber = cidTextView.text.toString()
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

        floorDropdown.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                floorNumber = floorDropdown.text.toString()
                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        departmentDropdown.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                department = departmentDropdown.text.toString()
                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

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
            if (mainViewModel.getFaceCompareMode()) {
                showFaceDialog()
            }  else {
                register(false)
            }
           //
        }

        //test
        generateCardNumber(mainViewModel.getCurrentCardNumberPosition())

    }

    private fun setupDropdown() {
        val items = listOf("บัตรประชาชน", "ใบขับขี่", "บัตรพนักงาน / ข้าราชการ", "Passport", "ไม่มีบัตร")
        val adapter = ArrayAdapter<String>(applicationContext, R.layout.dropdown_menu_pop_item, items)
        cardTypeDropdown.setAdapter(adapter)

        val floorItems = mutableListOf<String>()
        floorItems.clear()
        for (i in 1..28) {
            floorItems.add("${i}F")
        }
        val floorAdapter = ArrayAdapter<String>(applicationContext, R.layout.dropdown_menu_pop_item, floorItems)
        floorDropdown.setAdapter(floorAdapter)

        val deptItems = listOf<String>("สินเชื่อรถยนต์", "ศูนย์ลูกค้า SME", "กรุงศรี Corporate", "กรุงศรี เอ็กซ์คลูซีฟ", "วางแผนการเงิน", "บริการบัตรเครดิต", "สินเชื่อบ้าน / บุคคล")
        val deptAdapter = ArrayAdapter<String>(applicationContext, R.layout.dropdown_menu_pop_item, deptItems)
        departmentDropdown.setAdapter(deptAdapter)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun showSettings() {

        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = this.layoutInflater.inflate(R.layout.dialog_settings, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(true)

        dialogBuilder.setTitle("Settings")

        val serviceURLTextView = dialogView.findViewById<EditText>(R.id.serviceURLEditText)
        val usernameTextView = dialogView.findViewById<EditText>(R.id.usernameEditText)
        val passwordTextView = dialogView.findViewById<EditText>(R.id.passwordEditText)
        val departmentDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.departmentDropdown)
        val branchDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.branchDropdown)
        var cardNumberPositionTextView = dialogView.findViewById<EditText>(R.id.cardNumberPositionEditText)

        Log.d(TAG, "Count ${deptIDList.size}")

        val faceCompareSwitch = dialogView.findViewById<Switch>(R.id.faceCompareSwitch)

        faceCompareSwitch.isChecked = mainViewModel.getFaceCompareMode()

        serviceURLTextView.text = Editable.Factory.getInstance().newEditable(mainViewModel.getServiceURL())
        usernameTextView.text = Editable.Factory.getInstance().newEditable(mainViewModel.getUsername())
        passwordTextView.text = Editable.Factory.getInstance().newEditable(mainViewModel.getPassword())
        departmentDropdown.setText(mainViewModel.getDept())
        branchDropdown.setText(mainViewModel.getBranch())
        cardNumberPositionTextView.text = Editable.Factory.getInstance().newEditable(mainViewModel.getCurrentCardNumberPosition()
            .toString())


        val adapter = ArrayAdapter(applicationContext, R.layout.dropdown_menu_pop_item, deptIDList)
        departmentDropdown.setAdapter(adapter)

        val branchItems = listOf("card_number_n", "card_number_rama3")
        val branchAdapter = ArrayAdapter(applicationContext, R.layout.dropdown_menu_pop_item, branchItems)
        branchDropdown.setAdapter(branchAdapter)

        faceCompareSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mainViewModel.saveFaceCompareMode(true)
            } else {
                mainViewModel.saveFaceCompareMode(false)
            }
        }
        dialogBuilder.setPositiveButton("Save Change") {_, _->

            mainViewModel.saveServiceURL(serviceURLTextView.text.toString())
            mainViewModel.saveUsername(usernameTextView.text.toString())
            mainViewModel.savePassword(passwordTextView.text.toString())
            mainViewModel.saveDept(departmentDropdown.text.toString())
            mainViewModel.saveBranch(branchDropdown.text.toString())
            mainViewModel.saveCurrentCardNumberPosition(cardNumberPositionTextView.text.toString().toInt())

            reset()

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

        deptIDList.clear()
        mainViewModel.performLoginRequest(username!!, password!!).observe(this, Observer<Either<LoginResponse>> { either ->
            if (either?.status == Status.SUCCESS && either.data != null) {
                if (either.data.rtn == 0) {
                    Log.d(TAG, "${either.data.result}")
                    mainViewModel.saveToken(either.data.result.web_authorization)
                    mainViewModel.saveBackendToken(either.data.result.backend_authorization)
                    getDeptID()
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

    private fun getDeptID() {

        mainViewModel.getDeptID().observe(this, Observer<Either<DeptResponse>>{either ->
            if (either?.status == Status.SUCCESS && either.data != null) {
                if (either.data.rtn == 0) {
                    Log.d(TAG, "${either.data.result}")
                    if (either.data.result.first().children.isNotEmpty()) {
                        for (departmentID in either.data.result.first().children) {
                            deptIDList.add(departmentID.key)
                        }
                    }

                    Toast.makeText(this, "Successful Get Department", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to Get Department ${either.data.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (either?.error == ApiError.LOGIN) {
                    Toast.makeText(this, "Could not connect to server for department!", Toast.LENGTH_SHORT).show()
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
//                fullnameTextView.isEnabled = false
//                faceImageView.isEnabled = false

            }

            UIMode.STAFF.mode -> {
                mManual = UIMode.STAFF.mode
                Toast.makeText(this,"Staff Mode", Toast.LENGTH_SHORT).show()
//                fullnameTextView.isEnabled = true
//                faceImageView.isEnabled = true

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
                if (!isAlreadyReadDataFromCard) {
                if (card_status != CARD_READ) {

                    val info = thaiSmartCard.personalInformation

                    if (info != null) {

                        if (cardTypeDropdown.text.toString() == "บัตรประชาชน") {

                            fullnameTextView.setText(info.NameTH)
                            cidTextView.setText(info.PersonalID)

                            comparedPersonImage = thaiSmartCard.personalPicture
                            faceImageView.setImageBitmap(comparedPersonImage)

                            isTakePhoto = true

                            val resizedBitmap = resizeBitmap(
                                comparedPersonImage,
                                comparedPersonImage.width * 2,
                                comparedPersonImage.height * 2
                            )

                            val stream = ByteArrayOutputStream()
                            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                            val byteArray = stream.toByteArray()

                            val base64String: String

                            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                                Log.d(TAG, "Android O")
                                base64String = Base64.getEncoder().encodeToString(byteArray)

                            } else {
                                Log.d(TAG, "Android Other")
                                base64String = android.util.Base64.encodeToString(
                                    byteArray,
                                    android.util.Base64.NO_WRAP
                                )
                            }

                            picture1 = base64String
                            isAlreadyReadDataFromCard = true
                            updateUI()
                        }


                        card_status = CARD_READ

                    }
                }
                } else {
                    card_status = CARD_EMPTY

                }

            } else {
                card_status = CARD_DETACHED
                isAlreadyReadDataFromCard = false
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
        if (mCameraSource != null) {
            mCameraSource?.release()
        }
    }

    override fun onResume() {
     //   reset()
        updateConnection()
        updateUI()
        if (task.status != AsyncTask.Status.RUNNING) {
            task.execute()
        }
        super.onResume()
        mPreview?.stop()
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
                    openCamera(IMAGE_CAPTURE_CODE)
                } else {
                    // Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            RC_HANDLE_CAMERA_PERM -> {
                Log.d(TAG, "Got unexpected permission result: " + requestCode)
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                return
            }

            else -> {
                /* nothing to do in here */
            }
        }

    }

    private fun updateUI() {
        if(cardTypeDropdown.text!!.isNotEmpty() &&
            fullnameTextView.text!!.isNotEmpty() &&
            visiteeTextView.text!!.isNotEmpty() &&
            isTakePhoto) {
            enableRegisterButton()
        } else {
            disableRegisterButton()
        }

        when (cardTypeDropdown.text.toString()) {
            "บัตรประชาชน" -> {
                cidTextInputLayout.hint = "หมายเลขบัตรประชาชน"
                cidTextInputLayout.visibility = View.VISIBLE
                cardImageView.visibility = View.VISIBLE
                textView9.visibility = View.VISIBLE
            }
            "ใบขับขี่" -> {
                cidTextInputLayout.hint = "หมายเลขใบขับขี่"
                cidTextInputLayout.visibility = View.VISIBLE
                cardImageView.visibility = View.VISIBLE
                textView9.visibility = View.VISIBLE
            }
            "บัตรพนักงาน / ข้าราชการ" -> {
                cidTextInputLayout.hint = "หมายเลขบัตรพนักงาน / ข้าราชการ"
                cidTextInputLayout.visibility = View.VISIBLE
                cardImageView.visibility = View.VISIBLE
                textView9.visibility = View.VISIBLE
            }
            "Passport" -> {
                cidTextInputLayout.hint = "หมายเลข Passport"
                cidTextInputLayout.visibility = View.VISIBLE
                cardImageView.visibility = View.VISIBLE
                textView9.visibility = View.VISIBLE
            }
            else -> {
                cidTextInputLayout.visibility = View.GONE
                cardImageView.visibility = View.GONE
                textView9.visibility = View.GONE

            }
        }

        generateCardNumber(mainViewModel.getCurrentCardNumberPosition())
    }

    private fun disableRegisterButton() {
        registerButton.isEnabled = false
    }

    private fun enableRegisterButton() {
        registerButton.isEnabled = true
    }

    private fun reset() {
        isAlreadyReadDataFromCard = false
        isTakePhoto = false
        faceImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_face_image))
        cardImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_card))

        cardTypeDropdown.setText("")
        fullnameTextView.setText("")
        cidTextView.setText("")
        visiteeTextView.setText("")
        floorDropdown.setText("")
        departmentDropdown.setText("")

        picture1 = ""
        picture2 = ""
        pictureCard = ""

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
                    openCamera(IMAGE_CAPTURE_CODE)
                    dialog.dismiss()
                }

            } else {
                openCamera(IMAGE_CAPTURE_CODE)
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

    private fun openCamera(captureCode: Int) {

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, captureCode)

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if ((requestCode == IMAGE_CAPTURE_CODE || requestCode == CARD_CAPTURE_CODE) && resultCode == Activity.RESULT_OK) {
            isTakePhoto = true

            val ins : InputStream? = contentResolver.openInputStream(image_uri)
            val takedPersonImage : Bitmap? = BitmapFactory.decodeStream(ins)

            ins?.close()

            if (takedPersonImage != null) {
                if (requestCode == 1001) {
                    faceImageView.setImageBitmap(mainViewModel.modifyImageOrientation(this ,takedPersonImage, image_uri!!))
                    comparedPersonImage = mainViewModel.modifyImageOrientation(this, takedPersonImage, image_uri!!)
                    picture1 = getImageBase64(faceImageView)
                } else {
                    cardImageView.setImageBitmap(mainViewModel.modifyImageOrientation(this ,takedPersonImage, image_uri!!))
                    pictureCard = getImageBase64(cardImageView)
                }

                //contentResolver.delete(image_uri!!, null, null)
            }
        }

        else if (requestCode == IMAGE_GALLERY_CODE && resultCode == Activity.RESULT_OK) {

            isTakePhoto = true

            exif_data = data?.data!!
            val ins: InputStream? = contentResolver.openInputStream(exif_data)
            val takedPersonImage  = BitmapFactory.decodeStream(ins)

            faceImageView.setImageBitmap(mainViewModel.modifyImageOrientation(this ,takedPersonImage, exif_data!!))
            comparedPersonImage = mainViewModel.modifyImageOrientation(this ,takedPersonImage, exif_data!!)
            picture1 = getImageBase64(faceImageView)
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

    override fun faceSnapped() {
        listener?.faceSnapped()
        Log.d(TAG, "Snapped")
        captureImage()
    }

    private fun captureImage() {
        Log.d(TAG, "Capture")

        if (!isTakingPhoto) {
            isTakingPhoto = true
            mCameraSource?.takePicture(null, CameraSource.PictureCallback { data ->
                Log.d(TAG, "PictureCallback")

                val orientation = Exif.getOrientation(data)
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)

                snapImage = when (orientation) {
                    90 -> rotateImage(bitmap, 90f)
                    180 -> rotateImage(bitmap, 180f)
                    270 -> rotateImage(bitmap, 270f)
                    else -> rotateImage(bitmap, 0f)
                }

                val resizedBitmap = resizeBitmap(snapImage, snapImage.width / 2, snapImage.height / 2)

                val stream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val byteArray = stream.toByteArray()

                //  var base64String: String

                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                    Log.d(TAG, "Android O")
                    picture2 = Base64.getEncoder().encodeToString(byteArray)

                } else {
                    Log.d(TAG, "Android Other")
                    picture2 = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
                }

                compareFace()
            })
        }
    }

    private fun showLoadingDialogWith(message: String){

        loadingDialog = SpotsDialog.Builder()
            .setContext(this)
            .setMessage(message)
            .setCancelable(false)
            .build()
            .apply {
                show()
            }
    }

    private fun rotateImage(bitmap: Bitmap, angle: Float): Bitmap {
        val mat : Matrix? = Matrix()
        mat?.postRotate(angle)

        return Bitmap.createBitmap(bitmap , 0, 0, bitmap.width, bitmap.height, mat, true)
    }

    private fun dismissLoadingDialog() {
        loadingDialog.dismiss()
    }

    private fun compareFace() {

        if (faceDialog.isShowing) {
            faceDialog.dismiss()
        }

      //  picture1 = getImageBase64(faceImageView)

        showLoadingDialogWith("กำลังเปรียบเทียบใบหน้า")

        mainViewModel.compareFace(picture1, picture2)
            .observe(this, Observer<Either<CompareFaceResponse>> { either ->
                if (either?.status == Status.SUCCESS && either.data != null) {
                    if (either.data.ret == 0) {
                        // compareTextView.text = either.data.similarity.toString()
                        //Toast.makeText(this, "Similarity : ${either.data.similarity}", Toast.LENGTH_SHORT).show()
                        showCompareResultDialog(either.data)
                    } else {
                        // emptyStudentLayout.visibility = View.VISIBLE
                        Toast.makeText(this, "Error comparing face.", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    if (either?.error == ApiError.COMPARE_FACE) {
                        Toast.makeText(this, "Error comparing face.", Toast.LENGTH_SHORT).show()
                    }
                }
                dismissLoadingDialog()
            })
    }

    override fun dismissDialog() {
        dismissListener?.dismissDialog()
    }

    private fun requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission")
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions,
                RC_HANDLE_CAMERA_PERM
            )
            return
        }

        val thisActivity = this
        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(thisActivity, permissions,
                RC_HANDLE_CAMERA_PERM
            )
        }

        Toast.makeText(this,R.string.permission_camera_rationale, Toast.LENGTH_SHORT).show()

        mGraphicOverlay?.let {
            Snackbar.make(
                it, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show()
        }
    }

    private fun showFaceDialog() {

        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = this.layoutInflater.inflate(R.layout.dialog_face, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(true)

        val mPreview = dialogView.findViewById<View>(R.id.preview) as CameraSourcePreview
        val mGraphicOverlay = dialogView.findViewById<View>(R.id.faceOverlay) as GraphicOverlay
        val autoSnapButton = dialogView.findViewById<Button>(R.id.autoSnapButton)
        val compareButton = dialogView.findViewById<Button>(R.id.compareButton)

        if (AppPrefs.getAutoSnapMode()) {
            autoSnapButton.visibility = View.VISIBLE
            compareButton.visibility = View.GONE
        } else {
            autoSnapButton.visibility = View.GONE
            compareButton.visibility = View.VISIBLE
        }

        compareButton.setSafeOnClickListener {
            val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            if (rc == PackageManager.PERMISSION_GRANTED) {
                faceSnapped()
            }
        }

        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {

            val context = applicationContext
            val detector = FaceDetector.Builder(context)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
                .setMode(FaceDetector.ACCURATE_MODE)
                .build()
            detector.setProcessor(
                MultiProcessor.Builder(mGraphicOverlay?.let {
                    GraphicFaceTrackerFactory(
                        it, this
                    )
                }).build())

            if (!detector.isOperational) {
                Log.w(TAG, "Face detector dependencies are not yet available.")
            }

            mCameraSource = CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(60.0f)
                .setAutoFocusEnabled(true)
                .build()

            val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                applicationContext)
            if (code != ConnectionResult.SUCCESS) {
                val dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
                dlg.show()
            }

            if (mCameraSource != null) {
                try {
                    mGraphicOverlay?.let { mPreview!!.start(mCameraSource!!, it) }
                } catch (e: IOException) {
                    Log.e(TAG, "Unable to start camera source.", e)
                    mCameraSource!!.release()
                    mCameraSource = null
                }
            }

        } else {
            requestCameraPermission()
        }

        faceDialog = dialogBuilder.create()
        faceDialog.show()

        faceDialog.setOnDismissListener {
            mPreview.stop()

            if (mCameraSource != null) {
                mCameraSource!!.release()
            }

            isTakingPhoto = false
        }

    }

    private fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    private fun showCompareResultDialog(faceResponse : CompareFaceResponse) {

        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = this.layoutInflater.inflate(R.layout.dialog_compare, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(true)

        val imageView1 = dialogView.findViewById<ImageView>(R.id.compareImageView1)
        val imageView2 = dialogView.findViewById<ImageView>(R.id.compareImageView2)
        val similarityTextView = dialogView.findViewById<TextView>(R.id.similarityTextView)
        val matchImageView = dialogView.findViewById<ImageView>(R.id.matchImageView)
        val descTextView = dialogView.findViewById<TextView>(R.id.descTextView)
        val registerButton = dialogView.findViewById<Button>(R.id.registerButton)

        imageView1.setImageBitmap(comparedPersonImage)
        imageView2.setImageBitmap(snapImage)

        val df = DecimalFormat("##.##")
        df.roundingMode = RoundingMode.CEILING
        similarityTextView.text = df.format(faceResponse.similarity)

        if (faceResponse.similarity >= AppPrefs.getSimilarity()) {
            //Match
            matchImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_match))
            descTextView.text = "This face is similar to the face on the card."
            registerButton.isEnabled = true
        } else {
            matchImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mismatch))
            descTextView.text = "This face is NOT similar to the face on the card."
            registerButton.isEnabled = false
        }

        registerButton.setOnClickListener {
            //addNewPerson()
            register(true)
        }

        compareResultDialog = dialogBuilder.create()
        compareResultDialog.show()

        compareResultDialog.setOnDismissListener {
            fullName = ""
            picture1 = ""
            picture2 = ""
            reset()
        }
    }


    private fun register(isFaceCompare: Boolean) {

        val image = getImageBase64(faceImageView)

        cardNumber = cardObject.uuid.toString()

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
                        Toast.makeText(this, "Register Successful ${either.data.result.first().message}", Toast.LENGTH_LONG).show()

                        if (mainViewModel.getCurrentCardNumberPosition() < 250) {
                            mainViewModel.saveCurrentCardNumberPosition(mainViewModel.getCurrentCardNumberPosition() + 1)
                            reset()
                        }
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
            if (isFaceCompare) {
                compareResultDialog?.dismiss()
            }
        })
    }

    private fun generateCardNumber(position: Int) {

        val json: String?
        val inputStream: InputStream = this.assets.open("${mainViewModel.getBranch()}.json")
        json = inputStream.bufferedReader().use { it.readText() }

        try {
            val jsonArray = Gson().fromJson(json, CardList::class.java)
            val allCards = mutableListOf<CardNumber>()
            for (province in jsonArray.number_items) {
                allCards.add(province)
            }
            cardObject = allCards[position]

            runningNumberTextView.text = "${cardObject.card_number} - ${cardObject.uuid}"

        } catch (e : JsonParseException) {

        }
    }
}

