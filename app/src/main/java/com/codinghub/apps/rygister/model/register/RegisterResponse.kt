package com.codinghub.apps.rygister.model.register

data class RegisterResponse(val ret: Int,
                            val data: RegisterData,
                            val msg: String)