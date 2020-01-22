package com.codinghub.apps.rygister.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.hardware.usb.UsbManager
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.app.AlertDialog
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.Observer
import com.codinghub.apps.rygister.R
import com.codinghub.apps.rygister.faceutility.camera.CameraSourcePreview
import com.codinghub.apps.rygister.faceutility.camera.GraphicOverlay
import com.codinghub.apps.rygister.faceutility.facedetection.GraphicFaceTracker
import com.codinghub.apps.rygister.faceutility.facedetection.GraphicFaceTrackerFactory
import com.codinghub.apps.rygister.model.compareface.CompareFaceResponse
import com.codinghub.apps.rygister.model.compareface.TrainFaceResponse
import com.codinghub.apps.rygister.model.error.ApiError
import com.codinghub.apps.rygister.model.error.Either
import com.codinghub.apps.rygister.model.error.Status
import com.codinghub.apps.rygister.model.preferences.AppPrefs
import com.codinghub.apps.rygister.thcard.SmartCardDevice
import com.codinghub.apps.rygister.thcard.ThaiSmartCard
import com.codinghub.apps.rygister.utilities.Exif
import com.codinghub.apps.rygister.utilities.SafeClickListener
import com.codinghub.apps.rygister.viewmodel.MainViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.material.snackbar.Snackbar
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_compare.view.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), GraphicFaceTracker.GraphicFaceTrackerListener, GraphicFaceTracker.GraphicFaceTrackerDismissListener {

    private val listener: OnFaceSnappedListener? = null

    interface OnFaceSnappedListener {
        fun faceSnapped()
    }

    private val dismissListener: OnDialogFaceDismissListener? = null
    interface OnDialogFaceDismissListener {
        fun dismissDialog()
    }

    private lateinit var mainViewModel: MainViewModel

    private val MAX_PREVIEW_WIDTH = 1024
    private val MAX_PREVIEW_HEIGHT = 1024

    private var mCameraSource: CameraSource? = null
    private var mPreview: CameraSourcePreview? = null
    private var mGraphicOverlay: GraphicOverlay? = null

    private lateinit var personImage: Bitmap
    private lateinit var snapImage: Bitmap

    private var isTakingPhoto: Boolean = false

    private var picture1: String = ""
    private var picture2: String = ""

    private var personName: String = ""
    private var personID: String = ""

    private lateinit var faceDialog: AlertDialog
    private lateinit var loadingDialog: AlertDialog
    private lateinit var compareResultDialog: AlertDialog

    var task = MyTask()
    private val CARD_READ = 1
    private val CARD_EMPTY = 2
    private val CARD_DETACHED = 3
    private var card_status = CARD_EMPTY

    var smartCardReader: SmartCardDevice? = null

    private lateinit var receiver: BroadcastReceiver

    private var usbDeviceIsAttached = false

    internal val TAG = MainActivity::class.java.simpleName

    companion object {
        private val TAG = "FaceTracker"
        private val RC_HANDLE_GMS = 9001
        private val RC_HANDLE_CAMERA_PERM = 2
    }

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

        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {

        } else {
            requestCameraPermission()
        }


        testButton.visibility = View.INVISIBLE
        testButton.setOnClickListener {
            showFaceDialog()
        }

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
        when (item.itemId) {
            R.id.action_settings -> {
                gotoSettings()
                return true
            }
//            R.id.action_add_new_face -> {
//                gotoAddNewFace()
//                return true
//            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun gotoSettings() {
        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
        startActivity(intent)
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

                        personID = info.PersonalID
                        personName = info.NameTH

                        personImage = thaiSmartCard.personalPicture
                        val stream = ByteArrayOutputStream()
                        thaiSmartCard.personalPicture.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        val byteArray = stream.toByteArray()

                        var base64String: String

                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                            Log.d(TAG, "Android O")
                            base64String = Base64.getEncoder().encodeToString(byteArray)

                        } else {
                            Log.d(TAG, "Android Other")
                            base64String = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
                        }

                        picture1 = base64String

                        showFaceDialog()

                        card_status = CARD_READ

                    } else {

                        card_status = CARD_EMPTY
                    }
                }

            } else {

                card_status = CARD_DETACHED
            }
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

    private fun updateUI() {

    }


    private fun reset() {

        updateUI()
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

    private fun rotateImage(bitmap: Bitmap, angle: Float): Bitmap {
        val mat : Matrix? = Matrix()
        mat?.postRotate(angle)

        return Bitmap.createBitmap(bitmap , 0, 0, bitmap.width, bitmap.height, mat, true)
    }

    private fun resizeBitmap(bitmap: Bitmap, width:Int, height:Int): Bitmap {

        return Bitmap.createScaledBitmap(
            bitmap,
            width,
            height,
            false
        )
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

    private fun dismissLoadingDialog() {
        loadingDialog.dismiss()
    }

    private fun compareFace() {

        if (faceDialog.isShowing) {
            faceDialog.dismiss()
        }

        showLoadingDialogWith("Identifying Face...")

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
                    if (either?.error == ApiError.COMPARE) {
                        Toast.makeText(this, "Error comparing face.", Toast.LENGTH_SHORT).show()
                    }
                }
                dismissLoadingDialog()
            })
    }



    override fun dismissDialog() {
        dismissListener?.dismissDialog()

//        if (matchDialog.isShowing) {
//            matchDialog.dismiss()
//        }
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


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode)
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.size != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source")
            //createCameraSource(CameraSource.CAMERA_FACING_BACK)

            return
        }

        Log.e(
            TAG, "Permission not granted: results len = " + grantResults.size +
                    " Result code = " + if (grantResults.size > 0) grantResults[0] else "(empty)")

        val listener = DialogInterface.OnClickListener { dialog, id -> finish() }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Face Tracker sample")
            .setMessage(R.string.no_camera_permission)
            .setPositiveButton(R.string.ok, listener)
            .show()
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

        imageView1.setImageBitmap(personImage)
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
            addNewPerson()
        }

        compareResultDialog = dialogBuilder.create()
        compareResultDialog.show()

        compareResultDialog.setOnDismissListener {
            personName = ""
            personID = ""
            picture1 = ""
            picture2 = ""
        }
    }

    private fun addNewPerson() {


        mainViewModel.trainFace(picture1, personName, personID).observe(this, Observer<Either<TrainFaceResponse>> {either ->
            if (either?.status == Status.SUCCESS && either.data != null) {
                if (either.data.ret == 0) {

                    Toast.makeText(this,"Train face complete", Toast.LENGTH_LONG).show()


                } else {

                    Toast.makeText(this,"Error training face.", Toast.LENGTH_SHORT).show()
                }

            } else {
                if (either?.error == ApiError.TRAIN) {
                    Toast.makeText(this,"Error training face.", Toast.LENGTH_SHORT).show()
                }

            }
            if (compareResultDialog.isShowing) {
                compareResultDialog.dismiss()
            }
        })

    }

}

