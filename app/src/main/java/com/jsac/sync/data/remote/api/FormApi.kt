package com.jsac.sync.data.remote.api

import com.jsac.sync.data.remote.dto.FormDetailResponse
import com.jsac.sync.data.remote.dto.FormListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface FormApi {

    @GET("forms")
    suspend fun getFormsList(): Response<FormListResponse>

    @GET("forms/{form_id}")
    suspend fun getFormDetail(
        @Path("form_id") formId: String
    ): Response<FormDetailResponse>
}