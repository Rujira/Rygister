package com.codinghub.apps.rygister.model.DeptResponse

data class DeptResult(val value: String,
                      val key: String,
                      val title: String,
                      val children: List<Children>)