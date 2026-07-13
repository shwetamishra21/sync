package com.jsac.sync.presentation.submissions

import com.jsac.sync.R

/**
 * Single source of truth for how a sync_status string is presented in the UI.
 *
 * Both SubmissionsAdapter (list cards) and SubmissionDetailFragment (detail
 * screen) call SubmissionStatusUi.of(status) instead of each maintaining
 * their own `when (status) { "PENDING" -> Color.parseColor(...) }` block.
 * If a new status is added, or a color changes, it changes here once.
 */
object SubmissionStatusUi {

    enum class Kind { PENDING, SYNCING, SYNCED, FAILED, UNKNOWN }

    data class StatusInfo(
        val kind: Kind,
        val label: String,
        /** Chip background — a status_*_bg color from colors.xml */
        val backgroundColorRes: Int,
        /** Chip / text foreground — the matching status_*_fg color */
        val foregroundColorRes: Int
    )

    // Note: a style="@style/..." XML attribute only applies at inflation
    // time, so it can't be swapped per-item at bind() time. Instead every
    // Widget.Sync.Chip.* style and this mapping both pull from the same
    // status_*_bg / status_*_fg tokens in colors.xml, so XML-inflated chips
    // and runtime-colored chips can never drift out of sync with each other.
    fun of(rawStatus: String?): StatusInfo = when (rawStatus) {
        "PENDING" -> StatusInfo(Kind.PENDING, "Pending", R.color.status_pending_bg, R.color.status_pending_fg)
        "SYNCING" -> StatusInfo(Kind.SYNCING, "Syncing", R.color.status_syncing_bg, R.color.status_syncing_fg)
        "SYNCED" -> StatusInfo(Kind.SYNCED, "Synced", R.color.status_synced_bg, R.color.status_synced_fg)
        "FAILED" -> StatusInfo(Kind.FAILED, "Failed", R.color.status_failed_bg, R.color.status_failed_fg)
        else -> StatusInfo(Kind.UNKNOWN, rawStatus ?: "Unknown", R.color.md_surface_variant, R.color.md_on_surface_variant)
    }

    fun isSynced(rawStatus: String?): Boolean = rawStatus == "SYNCED"
}