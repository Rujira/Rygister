package com.codinghub.apps.rygister.faceutility.facedetection

import android.graphics.Color
import android.util.Log
import com.codinghub.apps.rygister.faceutility.camera.GraphicOverlay
import com.codinghub.apps.rygister.model.preferences.AppPrefs
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face

class GraphicFaceTracker internal constructor(private val mOverlay: GraphicOverlay,
                                              private val faceSnapListener: GraphicFaceTrackerListener,
                                              private val dismissListener: GraphicFaceTrackerDismissListener) : Tracker<Face>() {

    interface GraphicFaceTrackerListener {
        fun faceSnapped()
    }

    interface GraphicFaceTrackerDismissListener {
        fun dismissDialog()
    }

    private val mFaceGraphic: FaceGraphic = FaceGraphic(mOverlay)

    private val TAG = GraphicFaceTracker::class.java.simpleName

    var frameCount = 0

    var distance = AppPrefs.getSnapDistance()

    override fun onNewItem(faceId: Int, item: Face?) {
        mFaceGraphic.setId(faceId)
    }

    override fun onUpdate(p0: Detector.Detections<Face>?, face: Face?) {
        mOverlay.add(mFaceGraphic)
        face?.let {
            mFaceGraphic.updateFace(it)
            val boxSize = face.width * face.height

            //Log.d(TAG, "Size : ${boxSize}")

            // faceRecognitionActivity.updateBoxAreaTextView(boxSize.toInt())
            if (boxSize > distance) {

                if (AppPrefs.getShowBoundingBoxState()) {
                    mFaceGraphic.setBoxColor(Color.GREEN)
                } else {
                    mFaceGraphic.setBoxColor(Color.TRANSPARENT)
                }

                frameCount += 1

                if (frameCount == 20) {

                    if (AppPrefs.getAutoSnapMode()) {
                        faceSnapListener.faceSnapped()
                    }
                }

            } else {

                if (AppPrefs.getShowBoundingBoxState()) {
                    mFaceGraphic.setBoxColor(Color.YELLOW)
                } else {
                    mFaceGraphic.setBoxColor(Color.TRANSPARENT)
                }

                frameCount = 0
            }
        }
    }

    override fun onMissing(p0: Detector.Detections<Face>?) {
        Log.d(TAG, "onMissing")
        mOverlay.remove(mFaceGraphic)

        // dismissListener.dismissDialog()

    }

    override fun onDone() {
        mOverlay.remove(mFaceGraphic)
    }
}