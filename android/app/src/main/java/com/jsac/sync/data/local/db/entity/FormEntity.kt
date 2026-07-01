package com.jsac.sync.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing form metadata locally.
 *
 * Stores the complete UI configuration so forms can be rendered offline.
 */
@Entity(tableName = "forms")
data class FormEntity(

    @PrimaryKey
    val id: String,

    val name: String,
    val description: String,
    val version: String,
    val created_at: String,
    val field_count: Int,

    // ===========================
    // Dynamic UI Configuration
    // ===========================


    // ===========================
    // Cache Metadata
    // ===========================

    val cached_at: Long = System.currentTimeMillis(),

    val is_downloaded: Boolean = false
)