package com.jsac.sync.data.remote.dto

import com.google.gson.annotations.SerializedName
import java. util. UUID

/**
 * Data Transfer Objects for form submissions
 * Used for API communication with backend
 */

// ============================================
// REQUEST MODELS (To Server)
// ============================================

/**
 * Request body for submitting a form
 */
data class SubmitFormRequest(
    @SerializedName("form_id")
    val formId: String,

    @SerializedName("form_data")
    val formData: Map<String, String>,

    @SerializedName("submitted_at")
    val submittedAt: Long,

    @SerializedName("gps_location")
    val gpsLocation: GpsLocation? = null,

    @SerializedName("idempotency_key")
    val idempotencyKey: String = generateIdempotencyKey()  // ✅ ADD
)

private fun generateIdempotencyKey(): String {
    // Format: "<local_submission_id>_<timestamp>_<hash>"
    // Ensures each submission has unique key, deterministic for retries
    return UUID.randomUUID().toString()
}

data class GpsLocation(
    @SerializedName("lat")
    val latitude: Double,

    @SerializedName("lng")
    val longitude: Double
)

// ============================================
// RESPONSE MODELS (From Server)
// ============================================

/**
 * Response from submitting a form
 */
data class SubmitFormResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("submission_id")
    val submissionId: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("submitted_at")
    val submittedAt: Long
)

/**
 * Response from getting a single submission
 */
data class SubmissionDetailResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("submission")
    val submission: SubmissionDetail
)

data class SubmissionDetail(
    @SerializedName("id")
    val id: String,

    @SerializedName("form_id")
    val formId: String,

    @SerializedName("form_data")
    val formData: Map<String, String>,

    @SerializedName("submitted_at")
    val submittedAt: Long,

    @SerializedName("synced_at")
    val syncedAt: Long?
)

/**
 * Response from getting all submissions for a form
 */
data class SubmissionsListResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("submissions")
    val submissions: List<SubmissionDetail>,

    @SerializedName("count")
    val count: Int
)