package com.jsac.sync.data.remote.dto

data class FormDetailResponse(
    val status: String,
    val form: FormDetail
)

data class FormDetail(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val created_at: String,
    val fields: List<FormField>
)

data class FormField(
    val id: String,
    val name: String,
    val type: String,  // text, email, dropdown, textarea, number, date
    val required: Boolean,
    val placeholder: String? = null,
    val options: List<String>? = null  // For dropdown types
)