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

    // Backend controlled UI configuration
    val theme: ThemeConfig = ThemeConfig(),
    val layout: LayoutConfig = LayoutConfig(),
    val branding: BrandingConfig = BrandingConfig(),

    val fields: List<FormField>
)

data class ThemeConfig(

    val primaryColor: String = "#1976D2",

    val secondaryColor: String = "#03A9F4",

    val backgroundColor: String = "#FFFFFF",

    val surfaceColor: String = "#FFFFFF",

    val buttonColor: String = "#1976D2",

    val buttonTextColor: String = "#FFFFFF",

    val textColor: String = "#212121",

    val cornerRadius: Int = 12
)

data class LayoutConfig(

    val columns: Int = 1,

    val spacing: Int = 16,

    val fieldStyle: String = "outlined",

    val labelPosition: String = "top",

    val cardPadding: Int = 16,

    val sectionSpacing: Int = 24,

    val showDividers: Boolean = true
)

data class BrandingConfig(
    val logo: String = "",
    val banner: String = "",
    val organizationName: String = "",
    val titleAlignment: String = "center"
)

data class ValidationConfig(
    val min: Int? = null,
    val max: Int? = null,

    val minLength: Int? = null,
    val maxLength: Int? = null,

    val regex: String? = null,

    val allowedExtensions: List<String>? = null,
    val maxImageSizeMB: Int? = null
)

data class VisibleIfConfig(
    val field: String,
    val equals: String
)

// Enable/Disable configuration
data class EnabledIfConfig(
    val field: String,
    val equals: String
)

data class FormField(
    val id: String,
    val name: String,
    val type: String,
    val required: Boolean,
    val placeholder: String? = null,
    val options: List<String>? = null,

    val validation: ValidationConfig? = null,

    val visible_if: VisibleIfConfig? = null,

    val enabled_if: EnabledIfConfig? = null,

    // Backend-driven default value
    val default_value: String? = null
)