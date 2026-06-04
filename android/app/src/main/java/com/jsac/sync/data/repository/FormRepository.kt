package com.jsac.sync.data.repository

import android.util.Log
import com.jsac.sync.data.local.db.dao.FormDao
import com.jsac.sync.data.local.db.entity.FormEntity
import com.jsac.sync.data.local.db.entity.FormFieldEntity
import com.jsac.sync.data.remote.api.FormApi
import com.jsac.sync.data.remote.dto.FormDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * FormRepository - UPDATED WITH OFFLINE-FIRST PATTERN
 *
 * Strategy:
 * 1. Check local cache first
 * 2. Return cached data immediately
 * 3. Fetch from API in background
 * 4. Update local cache
 * 5. Emit updated data
 *
 * Benefits:
 * - Instant UI response (local cache first)
 * - Works offline
 * - Automatic sync when online
 * - Reduces API calls
 */
class FormRepository @Inject constructor(
    private val api: FormApi,
    private val dao: FormDao
) {

    /**
     * Get list of forms - OFFLINE-FIRST
     *
     * Logic:
     * 1. Emit cached forms immediately (if available)
     * 2. Fetch fresh data from API
     * 3. Update cache
     * 4. Emit updated list
     */
    fun getFormsList(): Flow<Result<List<FormEntity>>> = flow {
        try {
            Log.d("FormRepository", "📦 Checking local cache for forms...")

            // 1. Check cache first
            var cachedForms: List<FormEntity> = emptyList()
            dao.getAllForms().collect { forms ->
                if (forms.isNotEmpty()) {
                    cachedForms = forms
                    Log.d("FormRepository", "✅ Cached forms found: ${forms.size}")
                    // Emit cached data immediately
                    emit(Result.success(forms))
                }
            }

            // 2. Fetch fresh data from API
            Log.d("FormRepository", "🌐 Fetching forms from API...")
            val response = api.getFormsList()

            if (response.isSuccessful && response.body() != null) {
                val formList = response.body()!!.forms
                Log.d("FormRepository", "✅ API returned ${formList.size} forms")

                // 3. Convert and cache
                val entities = formList.map { form ->
                    FormEntity(
                        id = form.id,
                        name = form.name,
                        description = form.description,
                        version = form.version,
                        created_at = form.created_at,
                        field_count = form.field_count,
                        cached_at = System.currentTimeMillis(),
                        is_downloaded = false
                    )
                }

                dao.insertForms(entities)
                Log.d("FormRepository", "💾 Forms cached locally")

                // 4. Emit updated list (if different from cache)
                if (entities != cachedForms) {
                    emit(Result.success(entities))
                }

            } else {
                Log.e("FormRepository", "❌ API error: ${response.code()}")
                // If API fails, emit cached data or error
                if (cachedForms.isNotEmpty()) {
                    emit(Result.success(cachedForms))
                } else {
                    emit(Result.failure(Exception("API error: ${response.code()}")))
                }
            }

        } catch (e: Exception) {
            Log.e("FormRepository", "❌ Exception: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Get form detail with fields - OFFLINE-FIRST
     *
     * Downloads complete form definition including all fields
     * Marks form as "is_downloaded = true" for offline access
     */
    fun getFormDetail(formId: String): Flow<Result<FormDetail>> = flow {
        try {
            Log.d("FormRepository", "🔍 Fetching form detail for: $formId")

            // Check if already downloaded
            val isCached = dao.isFormCached(formId) > 0
            if (isCached) {
                Log.d("FormRepository", "✅ Form already cached: $formId")
            }

            // Fetch from API
            val response = api.getFormDetail(formId)

            if (response.isSuccessful && response.body() != null) {
                val formDetail = response.body()!!.form
                Log.d("FormRepository", "✅ Got form detail with ${formDetail.fields.size} fields")

                // Cache the complete form
                cacheFormDetail(formDetail)

                emit(Result.success(formDetail))

            } else {
                Log.e("FormRepository", "❌ API error: ${response.code()}")
                emit(Result.failure(Exception("API error: ${response.code()}")))
            }

        } catch (e: Exception) {
            Log.e("FormRepository", "❌ Exception: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Cache complete form definition to local database
     */
    private suspend fun cacheFormDetail(form: FormDetail) {
        try {
            val formEntity = FormEntity(
                id = form.id,
                name = form.name,
                description = form.description,
                version = form.version,
                created_at = form.created_at,
                field_count = form.fields.size,
                cached_at = System.currentTimeMillis(),
                is_downloaded = true  // Mark as fully downloaded
            )
            dao.insertForm(formEntity)

            // Cache all fields
            val fieldEntities = form.fields.mapIndexed { index, field ->
                FormFieldEntity(
                    form_id = form.id,
                    field_id = field.id,
                    name = field.name,
                    type = field.type,
                    required = field.required,
                    placeholder = field.placeholder,
                    field_order = index,
                    options_json = if (field.options != null) {
                        field.options.joinToString(",")
                    } else null,
                    cached_at = System.currentTimeMillis()
                )
            }

            dao.insertFields(fieldEntities)
            Log.d("FormRepository", "💾 Form detail cached: ${form.fields.size} fields")

        } catch (e: Exception) {
            Log.e("FormRepository", "❌ Caching error: ${e.message}", e)
        }
    }

    /**
     * Get cached form fields (for offline access)
     */
    fun getFormFieldsOffline(formId: String): Flow<List<FormFieldEntity>> = flow {
        dao.getFormFields(formId).collect { fields ->
            Log.d("FormRepository", "📖 Retrieved ${fields.size} fields for form: $formId")
            emit(fields)
        }
    }

    /**
     * Refresh forms from API (manual sync)
     */
    suspend fun refreshFormsList(): Result<List<FormEntity>> {
        return try {
            Log.d("FormRepository", "🔄 Manual refresh of forms list")

            val response = api.getFormsList()
            if (response.isSuccessful && response.body() != null) {
                val formList = response.body()!!.forms

                val entities = formList.map { form ->
                    FormEntity(
                        id = form.id,
                        name = form.name,
                        description = form.description,
                        version = form.version,
                        created_at = form.created_at,
                        field_count = form.field_count,
                        cached_at = System.currentTimeMillis(),
                        is_downloaded = false
                    )
                }

                dao.deleteAllForms()  // Clear old data
                dao.insertForms(entities)
                Log.d("FormRepository", "✅ Forms refreshed: ${entities.size} forms")

                Result.success(entities)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }

        } catch (e: Exception) {
            Log.e("FormRepository", "❌ Refresh error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Clear all cached forms
     */
    suspend fun clearCache() {
        try {
            dao.deleteAllForms()
            Log.d("FormRepository", "🗑️ Cleared form cache")
        } catch (e: Exception) {
            Log.e("FormRepository", "❌ Error clearing cache: ${e.message}", e)
        }
    }

    /**
     * Get sync status for forms
     */
    fun getFormCount(): Flow<Int> = flow {
        dao.getFormCount().collect { count ->
            emit(count)
        }
    }
}