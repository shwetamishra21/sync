package com.jsac.sync.presentation.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsac.sync.data.local.db.entity.FormEntity
import com.jsac.sync.data.remote.dto.FormDetail
import com.jsac.sync.data.repository.FormRepository
import com.jsac.sync.data.repository.FormSubmissionRepository
import com.jsac.sync.worker.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for form detail screen
 * Handles:
 * - Loading form details
 * - Managing form state (data, validation)
 * - Submitting forms (offline-first)
 * - ✅ FIXED: Triggers sync after form submission
 */
@HiltViewModel
class FormDetailViewModel @Inject constructor(
    private val formRepository: FormRepository,
    private val submissionRepository: FormSubmissionRepository,
    @ApplicationContext private val context: Context  // ✅ Added for SyncScheduler
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val form: FormDetail) : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class SubmitState {
        object Idle : SubmitState()
        object Submitting : SubmitState()
        data class Success(val submissionId: Int) : SubmitState()
        data class Error(val message: String) : SubmitState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    // Store form data as user fills it
    private val formDataMap = mutableMapOf<String, String>()

    init {
        Log.d("FormDetailViewModel", "🎬 Initializing FormDetailViewModel")
    }

    // ============================================
    // LOAD FORM
    // ============================================

    fun loadForm(formId: String) {
        Log.d("FormDetailViewModel", "📥 Loading form: $formId")

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            formRepository.getFormDetail(formId).collect { result ->
                result.onSuccess { formDetail ->
                    Log.d("FormDetailViewModel", "✅ Form loaded: ${formDetail.name}")
                    _uiState.value = UiState.Success(formDetail)

                }.onFailure { error ->
                    Log.e("FormDetailViewModel", "❌ Error: ${error.message}", error)
                    _uiState.value = UiState.Error(error.message ?: "Unknown error")
                }
            }
        }
    }

    // ============================================
    // FORM DATA MANAGEMENT
    // ============================================

    fun updateFieldValue(fieldId: String, value: String) {
        Log.d("FormDetailViewModel", "✏️ Updated $fieldId = $value")
        formDataMap[fieldId] = value
    }

    fun getFormData(): Map<String, String> {
        return formDataMap.toMap()
    }

    fun clearFormData() {
        formDataMap.clear()
    }

    // ============================================
    // VALIDATION
    // ============================================

    fun validateForm(form: FormDetail): Pair<Boolean, String> {
        Log.d("FormDetailViewModel", "🔍 Validating form")

        for (field in form.fields) {
            if (field.required) {
                val value = formDataMap[field.id]

                if (value.isNullOrBlank()) {
                    val errorMsg = "${field.name} is required"
                    Log.d("FormDetailViewModel", "❌ Validation failed: $errorMsg")
                    return Pair(false, errorMsg)
                }

                // Email validation
                if (field.type == "email") {
                    if (!isValidEmail(value)) {
                        val errorMsg = "${field.name} must be a valid email"
                        Log.d("FormDetailViewModel", "❌ Validation failed: $errorMsg")
                        return Pair(false, errorMsg)
                    }
                }
            }
        }

        Log.d("FormDetailViewModel", "✅ Form validation passed")
        return Pair(true, "")
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // ============================================
    // SUBMIT FORM
    // ============================================

    /**
     * Submit form (offline-first)
     * ✅ FIXED: Now triggers sync immediately after saving locally
     *
     * Flow:
     * 1. Validate form
     * 2. Save to local Room database (offline-first)
     * 3. ✅ Trigger WorkManager sync immediately
     * 4. Return success to user
     * 5. User sees form submitted message
     * 6. Background sync happens automatically
     */
    fun submitForm(formId: String, form: FormDetail) {
        Log.d("FormDetailViewModel", "🚀 SUBMIT CALLED - formId: $formId, fields: ${form.fields.size}")

        viewModelScope.launch {
            _submitState.value = SubmitState.Submitting

            try {
                val (isValid, errorMsg) = validateForm(form)
                if (!isValid) {
                    Log.d("FormDetailViewModel", "❌ Validation FAILED: $errorMsg")
                    _submitState.value = SubmitState.Error(errorMsg)
                    return@launch
                }

                val formData = getFormData()
                Log.d("FormDetailViewModel", "✅ Form data collected: ${formData.size} fields")
                formData.forEach { (k, v) -> Log.d("FormDetailViewModel", "   $k = $v") }

                Log.d("FormDetailViewModel", "📝 Calling submissionRepository.submitForm()...")
                val result = submissionRepository.submitForm(
                    formId = formId,
                    formData = formData,
                    gpsLocation = null
                )

                result.onSuccess { submissionId ->
                    Log.d("FormDetailViewModel", "✅✅✅ SUBMISSION SAVED TO ROOM - ID: $submissionId")
                    _submitState.value = SubmitState.Success(submissionId)
                    clearFormData()

                    try {
                        Log.d("FormDetailViewModel", "📤 Scheduling sync...")
                        SyncScheduler.scheduleSync(context)
                        Log.d("FormDetailViewModel", "✅ Sync scheduled")
                    } catch (e: Exception) {
                        Log.e("FormDetailViewModel", "⚠️ Error scheduling sync: ${e.message}", e)
                    }

                }.onFailure { error ->
                    Log.e("FormDetailViewModel", "❌❌❌ SUBMISSION SAVE FAILED: ${error.message}", error)
                    error.printStackTrace()
                    _submitState.value = SubmitState.Error(error.message ?: "Unknown error")
                }

            } catch (e: Exception) {
                Log.e("FormDetailViewModel", "❌ Exception: ${e.message}", e)
                _submitState.value = SubmitState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ============================================
    // STATE RESET
    // ============================================

    fun resetSubmitState() {
        _submitState.value = SubmitState.Idle
    }

    fun retrySubmit(formId: String, form: FormDetail) {
        resetSubmitState()
        submitForm(formId, form)
    }
}