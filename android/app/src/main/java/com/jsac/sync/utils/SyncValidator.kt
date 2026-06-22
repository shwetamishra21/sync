package com.jsac.sync.utils

import android.util.Log
import com.jsac.sync.data.local.db.entity.FormSubmissionEntity

/**
 * Validates submissions before attempting sync
 * Helps diagnose why sync might be failing
 *
 * Use this to check submission data integrity
 */
object SyncValidator {

    /**
     * Validate submission before sync
     *
     * @param submission The submission to validate
     * @return Pair of (isValid, errorMessage)
     */
    fun validateSubmission(submission: FormSubmissionEntity): Pair<Boolean, String> {
        Log.d("SyncValidator", "🔍 Validating submission #${submission.id}")

        // Check ID
        if (submission.id <= 0) {
            val msg = "Invalid submission ID: ${submission.id}"
            Log.e("SyncValidator", "   ❌ $msg")
            return Pair(false, msg)
        }

        // Check form ID
        if (submission.form_id.isBlank()) {
            val msg = "Form ID is blank"
            Log.e("SyncValidator", "   ❌ $msg")
            return Pair(false, msg)
        }

        Log.d("SyncValidator", "   ✅ Form ID: ${submission.form_id}")

        // Check form data
        if (submission.form_data.isBlank()) {
            val msg = "Form data is empty"
            Log.e("SyncValidator", "   ❌ $msg")
            return Pair(false, msg)
        }

        Log.d("SyncValidator", "   ✅ Form data length: ${submission.form_data.length} chars")

        // Validate JSON structure
        if (!submission.form_data.startsWith("{")) {
            val msg = "Form data is not valid JSON (doesn't start with {)"
            Log.e("SyncValidator", "   ❌ $msg")
            Log.e("SyncValidator", "   First 50 chars: ${submission.form_data.take(50)}")
            return Pair(false, msg)
        }

        if (!submission.form_data.endsWith("}")) {
            val msg = "Form data is not valid JSON (doesn't end with })"
            Log.e("SyncValidator", "   ❌ $msg")
            Log.e("SyncValidator", "   Last 50 chars: ${submission.form_data.takeLast(50)}")
            return Pair(false, msg)
        }

        Log.d("SyncValidator", "   ✅ Form data is valid JSON structure")

        // Check sync status
        val validStatuses = listOf("PENDING", "SYNCING", "SYNCED", "FAILED")
        if (submission.sync_status !in validStatuses) {
            val msg = "Invalid sync status: ${submission.sync_status}"
            Log.e("SyncValidator", "   ❌ $msg")
            Log.e("SyncValidator", "   Valid statuses: ${validStatuses.joinToString(", ")}")
            return Pair(false, msg)
        }

        Log.d("SyncValidator", "   ✅ Sync status: ${submission.sync_status}")

        // Check timestamps
        if (submission.created_at <= 0) {
            val msg = "Invalid created_at timestamp: ${submission.created_at}"
            Log.e("SyncValidator", "   ❌ $msg")
            return Pair(false, msg)
        }

        Log.d("SyncValidator", "   ✅ Created at: ${submission.created_at} (${formatTime(submission.created_at)})")

        // Check updated_at
        if (submission.updated_at <= 0) {
            val msg = "Invalid updated_at timestamp: ${submission.updated_at}"
            Log.e("SyncValidator", "   ❌ $msg")
            return Pair(false, msg)
        }

        Log.d("SyncValidator", "   ✅ Updated at: ${submission.updated_at} (${formatTime(submission.updated_at)})")

        // Warn about old submissions
        val ageMs = System.currentTimeMillis() - submission.created_at
        if (ageMs > 7 * 24 * 60 * 60 * 1000) {
            Log.w("SyncValidator", "   ⚠️ Submission is ${ageMs / (24 * 60 * 60 * 1000)} days old")
        }

        Log.d("SyncValidator", "✅ Submission validation PASSED")
        return Pair(true, "")
    }

    /**
     * Validate multiple submissions
     */
    fun validateSubmissions(submissions: List<FormSubmissionEntity>): Map<Int, Pair<Boolean, String>> {
        Log.d("SyncValidator", "🔍 Validating ${submissions.size} submission(s)")

        val results = mutableMapOf<Int, Pair<Boolean, String>>()

        for (submission in submissions) {
            val (isValid, msg) = validateSubmission(submission)
            results[submission.id] = Pair(isValid, msg)
        }

        val validCount = results.count { it.value.first }
        Log.d("SyncValidator", "✅ Validation complete: $validCount/${submissions.size} valid")

        return results
    }

    /**
     * Get validation summary for diagnostics
     */
    fun getSummary(submission: FormSubmissionEntity): String {
        return buildString {
            append("Submission #${submission.id}: ")
            append("Form=${submission.form_id}, ")
            append("Status=${submission.sync_status}, ")
            append("DataSize=${submission.form_data.length} bytes, ")
            append("Age=${(System.currentTimeMillis() - submission.created_at) / 1000}s")
        }
    }

    private fun formatTime(timeMs: Long): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timeMs))
    }
}