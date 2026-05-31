package com.jsac.sync.data.remote.dto

data class FormListResponse(
    val status: String,
    val forms: List<FormMetadata>,
    val count: Int
)

data class FormMetadata(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val created_at: String,
    val field_count: Int
)