package com.codinghub.apps.rygister.faceutility.camera

interface FaceTrackingListener {
    fun onFaceLeftMove()
    fun onFaceRightMove()
    fun onFaceUpMove()
    fun onFaceDownMove()
    fun onGoodSmile()
    fun onEyeCloseError()
    fun onMouthOpenError()
    fun onMultipleFaceError()

}