package com.codinghub.apps.rygister.faceutility.facedetection

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.codinghub.apps.rygister.faceutility.camera.FaceTrackingListener
import com.codinghub.apps.rygister.faceutility.camera.GraphicOverlay
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.face.Face

internal class FaceGraphic(overlay: GraphicOverlay) : GraphicOverlay.Graphic(overlay) {
    private val mFacePositionPaint: Paint = Paint()
    private val mIdPaint: Paint
    private val mBoxPaint: Paint
    @Volatile
    private var mFace: Face? = null
    private var mFaceId: Int = 0
    private val mFaceHappiness: Float = 0.toFloat()

    init {

//        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.size
//        val selectedColor = COLOR_CHOICES[mCurrentColorIndex]
        mFacePositionPaint.color = Color.GREEN
        mIdPaint = Paint()
        mIdPaint.color = Color.GREEN
        mIdPaint.textSize = ID_TEXT_SIZE
        mBoxPaint = Paint()
        mBoxPaint.color = Color.GREEN
        mBoxPaint.style = Paint.Style.STROKE
        mBoxPaint.strokeWidth = BOX_STROKE_WIDTH
    }

    fun setBoxColor(color: Int) {
        mBoxPaint.color = color
    }

    fun setId(id: Int) {
        mFaceId = id
    }

    fun updateFace(face: Face) {

        //Log.d("FaceGraphic", "On Update Face")
        mFace = face
        postInvalidate()
        logFaceData(mFace, object : FaceTrackingListener {
            override fun onFaceLeftMove() {
                Log.d("FaceGraphic", "onFaceLeftMove")
            }

            override fun onFaceRightMove() {
                Log.d("FaceGraphic", "onFaceRightMove")
            }

            override fun onFaceUpMove() {
                Log.d("FaceGraphic", "onFaceUpMove")
            }

            override fun onFaceDownMove() {
                Log.d("FaceGraphic", "onFaceDownMove")
            }

            override fun onGoodSmile() {
                Log.d("FaceGraphic", "onGoodSmile")
            }

            override fun onEyeCloseError() {
                Log.d("FaceGraphic", "onEyeCloseError")
            }

            override fun onMouthOpenError() {
                Log.d("FaceGraphic", "onMouthOpenError")
            }

            override fun onMultipleFaceError() {
                Log.d("FaceGraphic", "onMultipleFaceError")
            }
        })
    }

    override fun draw(canvas: Canvas) {
        val face = mFace ?: return

//        val x = translateX(face.position.x + face.width / 2)
//        val y = translateY(face.position.y + face.height / 2)
//        Log.e("Y", "" + y)
//        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint)
//        canvas.drawText("id: " + mFaceId, x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint)
//       // canvas.drawText("happiness: " + String.format("%.2f", face.getIsSmilingProbability()), x - ID_X_OFFSET, y - ID_Y_OFFSET, mIdPaint)
//        canvas.drawText("right eye: " + String.format("%.2f", face.getIsRightEyeOpenProbability()), x + ID_X_OFFSET * 2, y + ID_Y_OFFSET * 2, mIdPaint)
//        canvas.drawText("left eye: " + String.format("%.2f", face.getIsLeftEyeOpenProbability()), x - ID_X_OFFSET * 2, y - ID_Y_OFFSET * 2, mIdPaint)
//        val xOffset = scaleX(face.width / 2.0f)
//        val yOffset = scaleY(face.height / 2.0f)
//        val left = x - xOffset
//        val top = y - yOffset
//        val right = x + xOffset
//        val bottom = y + yOffset

//        Log.e("Right", "" + right)
//        Log.e("Left", "" + left)
//        Log.e("Top", "" + top)
//        Log.e("Bottom", "" + bottom)
//        canvas.drawRect(left, top, right, bottom, mBoxPaint)


        val x = translateX(face.position.x + face.width / 2)
        val y = translateY(face.position.y + face.height / 2)
      //  canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint)
       // canvas.drawText("id: +${mFaceId}", x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint)


//        canvas.drawText("happiness: ${String.format("%.2f", face.isSmilingProbability)}", x + ID_X_OFFSET * 3, y - 2 * ID_Y_OFFSET, mIdPaint)
//        canvas.drawText("right eye: ${String.format("%.2f", face.isRightEyeOpenProbability)}", x - ID_X_OFFSET, y, mIdPaint)
//        canvas.drawText("left eye: ${String.format("%.2f", face.isLeftEyeOpenProbability)}", x + ID_X_OFFSET * 6, y, mIdPaint)

        val xOffset = scaleX(face.width / 2.0f)
        val yOffset = scaleY(face.height / 2.0f)
        val left = x - xOffset
        val top = y - yOffset
        val right = x + xOffset
        val bottom = y + yOffset
        canvas.drawRect(left, top, right, bottom, mBoxPaint)

//        val EulerY = mFace!!.eulerY
//        val EulerZ = mFace!!.eulerZ
//        canvas.drawText("Euler Y: " + String.format("%.2f", EulerY), x - ID_X_OFFSET, y - ID_Y_OFFSET, mIdPaint)
//        canvas.drawText("Euler Z: " + String.format("%.2f", EulerZ), x + ID_X_OFFSET * 2, y + ID_Y_OFFSET * 2, mIdPaint)
//        } else {
//            canvas.drawText(
//                "left eye: ${String.format("%.2f", face.leftEyeOpenProbability)}",
//                x - ID_X_OFFSET,
//                y,
//                idPaint
//            )
//            canvas.drawText(
//                "right eye: ${String.format("%.2f", face.rightEyeOpenProbability)}",
//                x + ID_X_OFFSET * 6,
//                y,
//                idPaint
//            )
//        }


    }

    private fun logFaceData(mFaces: Face?, listener: FaceTrackingListener) {
        val smilingProbability: Float
        val leftEyeOpenProbability: Float
        val rightEyeOpenProbability: Float
        val eulerY: Float
        val eulerZ: Float

        smilingProbability = mFaces!!.isSmilingProbability
        leftEyeOpenProbability = mFaces.isLeftEyeOpenProbability
        rightEyeOpenProbability = mFaces.isRightEyeOpenProbability
        eulerY = mFaces.eulerY
        eulerZ = mFaces.eulerZ
//        Log.e( "Tuts+ Face Detection", "Smiling: " + smilingProbability )
//        Log.e( "Tuts+ Face Detection", "Left eye open: " + leftEyeOpenProbability )
//        Log.e( "Tuts+ Face Detection", "Right eye open: " + rightEyeOpenProbability )
//        Log.e( "Tuts+ Face Detection", "Euler Y: " + eulerY )
//        Log.e("Tuts+ Face Detection", "Euler Z: " + eulerZ)

    }

    companion object {
        private val FACE_POSITION_RADIUS = 10.0f
        private val ID_TEXT_SIZE = 40.0f
        private val ID_Y_OFFSET = 50.0f
        private val ID_X_OFFSET = -50.0f
        private val BOX_STROKE_WIDTH = 5.0f
        private val COLOR_CHOICES = intArrayOf(Color.YELLOW)
        private var mCurrentColorIndex = 0
    }
}