package com.codinghub.apps.rygister.model.register

data class RegisterResponse(val result: List<ResultData>,
                            val rtn: Int,
                            val message: String)