package com.ies2k.tcsdapps.scanin.thcard

import android.graphics.Bitmap
import java.io.Serializable

class IDCard {

    var cid = ""
    var thaiName = ""
    var engName = ""
    var gender = ""
    var dob = ""
    var address = ""
    var issueExp = ""
    lateinit var photo: Bitmap
    var result = ""
    var errMsg = ""


}