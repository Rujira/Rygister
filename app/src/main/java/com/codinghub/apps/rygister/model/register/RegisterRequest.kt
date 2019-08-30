package com.codinghub.apps.rygister.model.register

data class RegisterRequest(val address: String?,
                           val country: String?,
                           val dob: String?,
                           val email: String?,
                           val facebook_id: String?,
                           val gender: String?,
                           val line_id: String?,
                           val manual: String?,
                           val name: String,
                           val nation: String?,
                           val person_id: String,
                           val picture: String,
                           val province: String?,
                           val qrcode: String,
                           val regtype: String,
                           val tel: String?,
                           val company: String?)
