package com.jsac.sync.data.remote.dto


data class HealthResponse(
    val status: String,
    val message: String,
    val version: String
)