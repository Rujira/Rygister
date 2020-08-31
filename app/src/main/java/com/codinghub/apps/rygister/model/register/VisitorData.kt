package com.codinghub.apps.rygister.model.register

data class VisitorData(val face_image_content: String,
                       val person_information: PersonInformation,
                       val tag_id_list: List<String>,
                       val dept_id : String,
                       val card_number : List<String>)