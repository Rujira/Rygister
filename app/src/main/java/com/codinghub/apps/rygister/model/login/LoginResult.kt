package com.codinghub.apps.rygister.model.login

data class LoginResult(val web_authorization: String,
                       val backend_authorization: String,
                       val times_total: Int,
                       val times_left: Int,
                       val locked_time_min: Int)
