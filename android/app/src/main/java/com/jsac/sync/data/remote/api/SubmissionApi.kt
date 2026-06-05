package com.jsac.sync.data.remote.api

import com.jsac.sync.data.remote.dto.SubmitFormRequest
import com.jsac.sync.data.remote.dto.SubmitFormResponse
import com.jsac.sync.data.remote.dto.SubmissionDetailResponse
import com.jsac.sync.data.remote.dto.SubmissionsListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit API interface for form submission endpoints
 * All endpoints require JWT authentication via AuthInterceptor
 */
interface SubmissionApi {

    /**
     * Submit a completed form to the server
     *
     * POST /forms/submit
     * Headers: Authorization: Bearer <JWT_TOKEN>
     *
     * Response: SubmitFormResponse (201 Created)
     */
    @POST("forms/submit")
    suspend fun submitForm(
        @Body request: SubmitFormRequest
    ): Response<SubmitFormResponse>

    /**
     * Get details of a specific submission
     *
     * GET /submissions/{submission_id}
     * Headers: Authorization: Bearer <JWT_TOKEN>
     *
     * Response: SubmissionDetailResponse (200 OK)
     */
    @GET("submissions/{submission_id}")
    suspend fun getSubmission(
        @Path("submission_id") submissionId: String
    ): Response<SubmissionDetailResponse>

    /**
     * Get all submissions for a specific form
     *
     * GET /forms/{form_id}/submissions
     * Headers: Authorization: Bearer <JWT_TOKEN>
     *
     * Response: SubmissionsListResponse (200 OK)
     */
    @GET("forms/{form_id}/submissions")
    suspend fun getSubmissions(
        @Path("form_id") formId: String
    ): Response<SubmissionsListResponse>
}