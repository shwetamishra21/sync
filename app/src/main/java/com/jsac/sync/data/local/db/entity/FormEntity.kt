package com.jsac.sync.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Room entity for storing form metadata locally
 *
 * WHY: Allows offline access to list of available forms
 * Reduces API calls when user returns to dashboard
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

    // Metadata
    val cached_at: Long = System.currentTimeMillis(),  // When we cached it
    val is_downloaded: Boolean = false  // Full form with fields cached locally
)