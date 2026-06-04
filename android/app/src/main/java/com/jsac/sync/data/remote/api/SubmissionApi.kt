package com.jsac.sync.data.remote.api

import com.jsac.sync.data.remote.dto.FormSubmissionRequest
import com.jsac.sync.data.remote.dto.FormSubmissionResponse
import com.jsac.sync.data.remote.dto.FormSubmissionDetailResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * API interface for form submission endpoints
 *
 * Handles:
 * - Submitting completed forms
 * - Retrieving submission details
 * - Batch operations
 */
interface SubmissionApi {

    /**
     * Submit a completed form to the backend
     *
     * @param request Form data to submit
     * @return Server-generated submission ID
     */
    @POST("forms/submit")
    suspend fun submitForm(
        @Body request: FormSubmissionRequest
    ): Response<FormSubmissionResponse>

    /**
     * Get details of a previously submitted form
     *
     * @param submissionId Server submission ID
     * @return Form submission details
     */
    @GET("submissions/{submission_id}")
    suspend fun getSubmission(
        @Path("submission_id") submissionId: String
    ): Response<FormSubmissionDetailResponse>

    /**
     * Get all submissions for a specific form (with pagination)
     *
     * @param formId Form ID to filter by
     * @param page Page number (0-indexed)
     * @param limit Results per page
     * @return List of submissions
     */
    @GET("forms/{form_id}/submissions")
    suspend fun getFormSubmissions(
        @Path("form_id") formId: String
    ): Response<FormSubmissionDetailResponse>
}