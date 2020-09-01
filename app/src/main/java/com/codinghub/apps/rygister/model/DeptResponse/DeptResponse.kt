package com.codinghub.apps.rygister.model.DeptResponse

data class DeptResponse(val result: List<DeptResult>,
                        val rtn: Int,
                        val message: String)