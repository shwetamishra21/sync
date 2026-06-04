package com.jsac.sync.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request DTO for submitting a form to the backend
 */
data class FormSubmissionRequest(
    @SerializedName("form_id")
    val formId: String,

    @SerializedName("form_data")
    val formData: Map<String, Any>,  // JSON object with all field values

    @SerializedName("submitted_at")
    val submittedAt: Long = System.currentTimeMillis(),

    @SerializedName("gps_location")
    val gpsLocation: Map<String, Double>? = null  // {"lat": 25.5941, "lng": 85.1376}
)

/**
 * Response DTO when form is successfully submitted
 */
data class FormSubmissionResponse(
    @SerializedName("status")
    val status: String,  // "success" or "error"

    @SerializedName("submission_id")
    val submissionId: String,  // Server-generated ID

    @SerializedName("message")
    val message: String = "",

    @SerializedName("submitted_at")
    val submittedAt: Long? = null
)

/**
 * Model for passing submission data between layers
 */
data class FormSubmissionModel(
    val id: Int = 0,
    val formId: String,
    val formData: Map<String, Any>,
    val syncStatus: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0
)

/**
 * Response for getting submission details
 */
data class FormSubmissionDetailResponse(
    val status: String,
    val submission: FormSubmissionDetail
)

data class FormSubmissionDetail(
    val id: String,
    val form_id: String,
    val form_data: Map<String, Any>,
    val submitted_at: Long,
    val synced_at: Long?
)

/**
 * Batch submission response
 */
data class BatchSubmissionResponse(
    val status: String,
    val created: Int,
    val failed: Int,
    val message: String
)