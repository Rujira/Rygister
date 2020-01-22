package com.codinghub.apps.rygister.faceutility.facedetection

import com.codinghub.apps.rygister.faceutility.camera.GraphicOverlay
import com.codinghub.apps.rygister.ui.MainActivity
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face

class GraphicFaceTrackerFactory(internal var overlay: GraphicOverlay, var mainActivity: MainActivity) : MultiProcessor.Factory<Face> {

    override fun create(face: Face): Tracker<Face> {
        return GraphicFaceTracker(overlay, mainActivity, mainActivity)
    }
}