package com.codinghub.apps.rygister.utilities

import android.os.SystemClock
import android.view.View

class SafeClickListener(private var defaultInterval: Int = 1000,
                        private var onSafeClick:(View) -> Unit) : View.OnClickListener {

    private var lastTimeClicked: Long = 0

    override fun onClick(v: View) {
        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            return
        }
        lastTimeClicked = SystemClock.elapsedRealtime()
        onSafeClick(v)
    }

}