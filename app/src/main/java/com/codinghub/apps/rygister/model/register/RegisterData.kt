package com.codinghub.apps.rygister.model.register

data class RegisterData(val created_date: String,
                        val email: String?,
                        val facebook_id: String?,
                        val gate: String?,
                        val id: String,
                        val image: String,
                        val line_id: String?,
                        val name: String,
                        val person_id: String,
                        val qrcode: String,
                        val regtype: String,
                        val seat_no: String?,
                        val tel: String?,
                        val zone: String)
