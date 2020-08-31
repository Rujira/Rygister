package com.codinghub.apps.rygister.model.register

import com.codinghub.apps.rygister.viewmodel.Json
import com.google.gson.JsonObject

data class RegisterRequest(val visitor_list : List<VisitorData>)
