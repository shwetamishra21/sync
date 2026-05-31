package com.jsac.sync.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for form fields
 *
 * WHY: Offline-first design - cache full form definition locally
 */
@Entity(
    tableName = "form_fields",
    foreignKeys = [
        ForeignKey(
            entity = FormEntity::class,
            parentColumns = ["id"],
            childColumns = ["form_id"],
            onDelete = ForeignKey.CASCADE  // Delete fields when form is deleted
        )
    ],
    indices = [
        Index("form_id")  // Speed up queries by form_id
    ]
)
data class FormFieldEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val form_id: String,
    val field_id: String,
    val name: String,
    val type: String,  // text, email, dropdown, textarea, number, date
    val required: Boolean,
    val placeholder: String? = null,
    val field_order: Int = 0,  // Display order

    // Dropdown/select options as JSON string
    val options_json: String? = null,  // ["Option1", "Option2"]

    val cached_at: Long = System.currentTimeMillis()
)