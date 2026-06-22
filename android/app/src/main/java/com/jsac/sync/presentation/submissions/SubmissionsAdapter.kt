package com.jsac.sync.presentation.submissions

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jsac.sync.R
import com.jsac.sync.data.local.db.entity.FormSubmissionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ✅ VERIFIED: RecyclerView adapter for displaying form submissions
 *
 * Changes:
 * 1. Added detailed logging for debugging
 * 2. Verified submitList() properly updates items
 * 3. Ensured DiffUtil is calculating differences correctly
 *
 * Shows:
 * - Form name
 * - Creation date
 * - Color-coded status badge (Pending, Syncing, Synced, Failed)
 * - Retry count (if any)
 * - Sync and Delete action buttons
 */
class SubmissionsAdapter(
    private val onSubmissionClick: (Int) -> Unit,
    private val onSyncClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : ListAdapter<FormSubmissionEntity, SubmissionsAdapter.SubmissionViewHolder>(
    SubmissionDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
        Log.d("SubmissionsAdapter", "🔧 Creating new ViewHolder")

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_submission, parent, false)

        return SubmissionViewHolder(
            view,
            onSubmissionClick,
            onSyncClick,
            onDeleteClick
        )
    }

    override fun onBindViewHolder(holder: SubmissionViewHolder, position: Int) {
        Log.d("SubmissionsAdapter", "📌 Binding item at position: $position")
        holder.bind(getItem(position))
    }

    /**
     * ✅ OVERRIDE: Track list changes for debugging
     */
    override fun submitList(list: List<FormSubmissionEntity>?) {
        Log.d("SubmissionsAdapter", "📝 submitList called with ${list?.size ?: 0} items")
        list?.forEachIndexed { idx, submission ->
            Log.d("SubmissionsAdapter", "   [$idx] ID=${submission.id}, Form=${submission.form_id}, Status=${submission.sync_status}")
        }
        super.submitList(list)
    }

    class SubmissionViewHolder(
        itemView: View,
        private val onSubmissionClick: (Int) -> Unit,
        private val onSyncClick: (Int) -> Unit,
        private val onDeleteClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvFormName: TextView = itemView.findViewById(R.id.tvFormName)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvRetryCount: TextView = itemView.findViewById(R.id.tvRetryCount)
        private val btnSync: Button = itemView.findViewById(R.id.btnSync)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        fun bind(submission: FormSubmissionEntity) {
            val submissionId = submission.id

            Log.d("SubmissionsAdapter", "🔗 Binding submission #$submissionId")

            // ============================================
            // FORM NAME
            // ============================================

            tvFormName.text = "📋 Form: ${submission.form_id}"

            // ============================================
            // DATE
            // ============================================

            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val createdDate = dateFormat.format(Date(submission.created_at))
            tvDate.text = "📅 $createdDate"

            // ============================================
            // STATUS BADGE (COLOR-CODED)
            // ============================================

            val (statusText, statusColor) = when (submission.sync_status) {
                "PENDING" -> "⏳ Pending" to Color.parseColor("#FFC107")
                "SYNCING" -> "🔄 Syncing" to Color.parseColor("#2196F3")
                "SYNCED" -> "✅ Synced" to Color.parseColor("#4CAF50")
                "FAILED" -> "❌ Failed" to Color.parseColor("#F44336")
                else -> submission.sync_status to Color.GRAY
            }

            tvStatus.text = statusText
            tvStatus.setTextColor(statusColor)

            // ============================================
            // RETRY COUNT
            // ============================================

            if (submission.retry_count > 0) {
                tvRetryCount.text = "🔁 Retries: ${submission.retry_count}"
                tvRetryCount.visibility = View.VISIBLE
            } else {
                tvRetryCount.visibility = View.GONE
            }

            // ============================================
            // CLICK LISTENER (NAVIGATE TO DETAIL)
            // ============================================

            itemView.setOnClickListener {
                Log.d("SubmissionsAdapter", "👆 Item clicked: $submissionId")
                onSubmissionClick(submissionId)
            }

            // ============================================
            // SYNC BUTTON
            // ============================================

            btnSync.setOnClickListener {
                Log.d("SubmissionsAdapter", "🔄 Sync clicked for submission: $submissionId")
                onSyncClick(submissionId)
            }

            // Disable if already synced
            btnSync.isEnabled = submission.sync_status != "SYNCED"
            btnSync.text = if (submission.sync_status == "SYNCED") "✅ Synced" else "🔄 Sync"

            // ============================================
            // DELETE BUTTON
            // ============================================

            btnDelete.setOnClickListener {
                Log.d("SubmissionsAdapter", "🗑️ Delete clicked for submission: $submissionId")
                onDeleteClick(submissionId)
            }
        }
    }

    class SubmissionDiffCallback : DiffUtil.ItemCallback<FormSubmissionEntity>() {
        override fun areItemsTheSame(
            oldItem: FormSubmissionEntity,
            newItem: FormSubmissionEntity
        ): Boolean {
            val same = oldItem.id == newItem.id
            if (!same) {
                Log.d("SubmissionsAdapter", "   Different items: ${oldItem.id} vs ${newItem.id}")
            }
            return same
        }

        override fun areContentsTheSame(
            oldItem: FormSubmissionEntity,
            newItem: FormSubmissionEntity
        ): Boolean {
            val same = oldItem == newItem
            if (!same) {
                Log.d("SubmissionsAdapter", "   Content changed for item #${oldItem.id}")
                Log.d("SubmissionsAdapter", "      Old status: ${oldItem.sync_status} → New status: ${newItem.sync_status}")
            }
            return same
        }
    }
}