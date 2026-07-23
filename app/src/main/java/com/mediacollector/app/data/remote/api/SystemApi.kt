package com.mediacollector.app.data.remote.api

import com.mediacollector.app.data.remote.dto.*
import retrofit2.http.*

/** 系统检测 API */
interface SystemApi {
    @GET("api/v1/system/check")
    suspend fun check(): ApiResponse<SystemCheck>
}
