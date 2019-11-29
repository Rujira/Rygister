package com.codinghub.apps.rygister.model.compareface

data class Face(val created_date: String,
                val status: String,
                val person_id: String,
                val id: Int,
                val image: String,
                val face_image_id: String,
                val name: String)