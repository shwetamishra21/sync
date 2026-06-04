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

class FormRepository @Inject constructor(
    private val api: FormApi,
    private val dao: FormDao
) {

    fun getFormsList(): Flow<Result<List<FormEntity>>> = flow {
        try {
            Log.d("FormRepository", "🌐 Fetching forms from API...")
            val response = api.getFormsList()

            if (response.isSuccessful && response.body() != null) {
                val formList = response.body()!!.forms

                Log.d("FormRepository", "✅ API returned ${formList.size} forms")

                val entities = formList.map { form ->
                    FormEntity(
                        id = form.id,
                        name = form.name,
                        description = form.description,
                        version = form.version,
                        created_at = form.created_at,
                        field_count = form.field_count,
                        cached_at = System.currentTimeMillis()
                    )
                }

                // Clear old cache and insert fresh data
                dao.deleteAllForms()
                dao.insertForms(entities)
                Log.d("FormRepository", "💾 Forms cached locally: ${entities.size} forms")

                emit(Result.success(entities))

            } else {
                Log.e("FormRepository", "❌ API error: ${response.code()}")
                Log.e("FormRepository", "❌ Error body: ${response.errorBody()?.string()}")
                emit(Result.failure(Exception("API error: ${response.code()}")))
            }

        } catch (e: Exception) {
            Log.e("FormRepository", "❌ Exception: ${e.message}", e)
            e.printStackTrace()

            // Fallback to cached data if API fails
            try {
                Log.d("FormRepository", "📦 Falling back to cached forms...")
                dao.getAllForms().collect { cachedForms ->
                    if (cachedForms.isNotEmpty()) {
                        Log.d("FormRepository", "✅ Using cached forms: ${cachedForms.size}")
                        emit(Result.success(cachedForms))
                    } else {
                        Log.d("FormRepository", "❌ No cached forms available, emitting original error")
                        emit(Result.failure(e))
                    }
                }
            } catch (cacheError: Exception) {
                Log.e("FormRepository", "❌ Cache error: ${cacheError.message}")
                emit(Result.failure(e))
            }
        }
    }

    fun getFormDetail(formId: String): Flow<Result<FormDetail>> = flow {
        try {
            Log.d("FormRepository", "🔍 Fetching form detail for: $formId")

            val response = api.getFormDetail(formId)

            if (response.isSuccessful && response.body() != null) {
                val formDetail = response.body()!!.form
                Log.d("FormRepository", "✅ Got form detail with ${formDetail.fields.size} fields")

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

    private suspend fun cacheFormDetail(form: FormDetail) {
        try {
            val formEntity = FormEntity(
                id = form.id,
                name = form.name,
                description = form.description,
                version = form.version,
                created_at = form.created_at,
                field_count = form.fields.size,
                is_downloaded = true
            )
            dao.insertForm(formEntity)

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
}