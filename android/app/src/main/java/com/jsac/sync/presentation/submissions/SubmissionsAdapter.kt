package com.jsac.sync.presentation.submissions

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.jsac.sync.R
import com.jsac.sync.data.local.db.entity.FormSubmissionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for displaying form submissions as Material cards.
 *
 * Status color/label logic lives entirely in SubmissionStatusUi so this
 * adapter and SubmissionDetailFragment can never show a different color
 * for the same status.
 */
class SubmissionsAdapter(
    private val onSubmissionClick: (Int) -> Unit,
    private val onSyncClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : ListAdapter<FormSubmissionEntity, SubmissionsAdapter.SubmissionViewHolder>(
    SubmissionDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_submission, parent, false)

        return SubmissionViewHolder(view, onSubmissionClick, onSyncClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: SubmissionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun submitList(list: List<FormSubmissionEntity>?) {
        Log.d("SubmissionsAdapter", "submitList called with ${list?.size ?: 0} items")
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
        private val chipStatus: Chip = itemView.findViewById(R.id.chipStatus)
        private val tvRetryCount: TextView = itemView.findViewById(R.id.tvRetryCount)
        private val btnSync: MaterialButton = itemView.findViewById(R.id.btnSync)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())

        fun bind(submission: FormSubmissionEntity) {
            val submissionId = submission.id
            val context = itemView.context

            // ============================================
            // FORM NAME
            // ============================================
            // FormSubmissionEntity only has form_id (not form_name)
            tvFormName.text = submission.form_id
            tvDate.text = dateFormat.format(Date(submission.created_at))

            // ============================================
            // STATUS CHIP — single source of truth
            // ============================================

            val statusInfo = SubmissionStatusUi.of(submission.sync_status)
            chipStatus.text = statusInfo.label
            chipStatus.chipBackgroundColor = ContextCompat.getColorStateList(
                context, statusInfo.backgroundColorRes
            )
            chipStatus.setTextColor(ContextCompat.getColor(context, statusInfo.foregroundColorRes))

            // ============================================
            // RETRY BADGE
            // ============================================

            if (submission.retry_count > 0) {
                tvRetryCount.text = "↻ ${submission.retry_count}"
                tvRetryCount.visibility = View.VISIBLE
            } else {
                tvRetryCount.visibility = View.GONE
            }

            // ============================================
            // SYNC BUTTON
            // ============================================

            val synced = SubmissionStatusUi.isSynced(submission.sync_status)
            btnSync.isEnabled = !synced
            btnSync.alpha = if (synced) 0.6f else 1f
            btnSync.text = if (synced) {
                context.getString(R.string.status_synced)
            } else {
                context.getString(R.string.btn_sync)
            }

            btnSync.icon = if (synced) {
                null
            } else {
                ContextCompat.getDrawable(context, R.drawable.ic_sync)
            }

            // ============================================
            // CLICK LISTENERS
            // ============================================

            itemView.setOnClickListener { onSubmissionClick(submissionId) }
            btnSync.setOnClickListener { onSyncClick(submissionId) }
            btnDelete.setOnClickListener { onDeleteClick(submissionId) }
        }
    }

    class SubmissionDiffCallback : DiffUtil.ItemCallback<FormSubmissionEntity>() {
        override fun areItemsTheSame(
            oldItem: FormSubmissionEntity,
            newItem: FormSubmissionEntity
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: FormSubmissionEntity,
            newItem: FormSubmissionEntity
        ): Boolean = oldItem == newItem
    }
}