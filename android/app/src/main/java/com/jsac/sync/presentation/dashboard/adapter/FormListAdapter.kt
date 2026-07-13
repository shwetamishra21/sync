package com.jsac.sync.presentation.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jsac.sync.R
import com.jsac.sync.data.local.db.entity.FormEntity

class FormListAdapter(
    private val onFormClick: (formId: String, formName: String) -> Unit
) : ListAdapter<FormEntity, FormListAdapter.FormViewHolder>(FormDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FormViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard_form, parent, false)
        return FormViewHolder(view, onFormClick)
    }

    override fun onBindViewHolder(
        holder: FormViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    class FormViewHolder(
        itemView: View,
        private val onFormClick: (formId: String, formName: String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvName: TextView = itemView.findViewById(R.id.tvFormName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvFormDescription)
        private val tvFieldCount: TextView = itemView.findViewById(R.id.tvFieldCount)
        private val container: View = itemView.findViewById(R.id.itemContainer)

        fun bind(form: FormEntity) {
            tvName.text = form.name

            tvDescription.text = form.description.takeIf { it.isNotBlank() }
                ?: "Government workflow form"

            tvFieldCount.text = "${form.field_count} fields"

            container.setOnClickListener {
                onFormClick(form.id, form.name)
            }
        }
    }

    class FormDiffCallback : DiffUtil.ItemCallback<FormEntity>() {
        override fun areItemsTheSame(old: FormEntity, new: FormEntity): Boolean {
            return old.id == new.id
        }

        override fun areContentsTheSame(old: FormEntity, new: FormEntity): Boolean {
            return old == new
        }
    }
}