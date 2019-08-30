package com.codinghub.apps.rygister.model.qrcode

data class QRCodeData(val gate: String,
                      val id: Int,
                      val qrcode: String,
                      val regtype: String,
                      val seat_no: Int,
                      val zone: Int)