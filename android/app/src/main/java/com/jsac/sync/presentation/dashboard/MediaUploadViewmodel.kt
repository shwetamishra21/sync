package com.jsac.sync.presentation.dashboard

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsac.sync.data.repository.FormSubmissionRepository
import com.jsac.sync.utils.ImageCompression
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for media capture and upload
 * Handles:
 * - Photo capture from camera
 * - File selection from gallery
 * - Image compression
 * - Upload to server (Part 5)
 */
@HiltViewModel
class MediaUploadViewModel @Inject constructor(
    private val submissionRepository: FormSubmissionRepository
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Capturing : UiState()
        data class Preview(val imagePath: String, val fileName: String, val fileSize: Double) : UiState()
        object Uploading : UiState()
        data class Success(val mediaId: Int) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentSubmissionId: Int = 0
    private var currentFieldId: String = ""

    // ============================================
    // INITIALIZATION
    // ============================================

    fun setSubmissionContext(submissionId: Int, fieldId: String) {
        currentSubmissionId = submissionId
        currentFieldId = fieldId
        Log.d("MediaUploadViewModel", "📸 Context set - Submission: $submissionId, Field: $fieldId")
    }

    // ============================================
    // PHOTO CAPTURE & PREVIEW
    // ============================================

    /**
     * Process captured photo from camera
     */
    fun onPhotoCaptured(context: Context, bitmap: Bitmap) {
        Log.d("MediaUploadViewModel", "📷 Photo captured: ${bitmap.width}x${bitmap.height}")

        viewModelScope.launch {
            _uiState.value = UiState.Capturing

            try {
                // Compress image
                val compressionResult = ImageCompression.compressAndSave(
                    context,
                    bitmap,
                    maxWidth = 1280,
                    maxHeight = 1280,
                    quality = 85
                )

                compressionResult.onSuccess { imagePath ->
                    val fileSize = ImageCompression.getFileSizeMB(imagePath)
                    val fileName = "photo_${System.currentTimeMillis()}.jpg"

                    Log.d("MediaUploadViewModel", "✅ Compressed: ${fileSize}MB")

                    _uiState.value = UiState.Preview(imagePath, fileName, fileSize)

                }.onFailure { error ->
                    Log.e("MediaUploadViewModel", "❌ Compression failed: ${error.message}")
                    _uiState.value = UiState.Error(error.message ?: "Compression failed")
                }

            } catch (e: Exception) {
                Log.e("MediaUploadViewModel", "❌ Error: ${e.message}", e)
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Process selected file from gallery
     */
    fun onFileSelected(context: Context, filePath: String, fileName: String) {
        Log.d("MediaUploadViewModel", "📁 File selected: $fileName")

        viewModelScope.launch {
            _uiState.value = UiState.Capturing

            try {
                // Load and compress if it's an image
                val bitmap = ImageCompression.loadBitmap(filePath)

                if (bitmap != null) {
                    onPhotoCaptured(context, bitmap)
                } else {
                    // Non-image file, use as-is
                    val fileSize = ImageCompression.getFileSizeMB(filePath)
                    _uiState.value = UiState.Preview(filePath, fileName, fileSize)
                }

            } catch (e: Exception) {
                Log.e("MediaUploadViewModel", "❌ Error: ${e.message}", e)
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ============================================
    // SAVE MEDIA TO SUBMISSION
    // ============================================

    /**
     * Save media file to submission
     * Marks as "LOCAL" status (to be uploaded later in Part 5)
     */
    fun saveMediaToSubmission(imagePath: String, fileName: String, fileSize: Double) {
        Log.d("MediaUploadViewModel", "💾 Saving media: $fileName")

        viewModelScope.launch {
            _uiState.value = UiState.Uploading

            try {
                if (currentSubmissionId == 0) {
                    throw Exception("No submission context set")
                }

                val result = submissionRepository.addMediaFile(
                    submissionId = currentSubmissionId,
                    fieldId = currentFieldId,
                    localPath = imagePath,
                    fileName = fileName,
                    fileSize = (fileSize * 1024 * 1024).toLong(), // Convert MB to bytes
                    fileType = "image/jpeg"
                )

                result.onSuccess { mediaId ->
                    Log.d("MediaUploadViewModel", "✅ Media saved - ID: $mediaId")
                    _uiState.value = UiState.Success(mediaId)

                }.onFailure { error ->
                    Log.e("MediaUploadViewModel", "❌ Save failed: ${error.message}")
                    _uiState.value = UiState.Error(error.message ?: "Save failed")
                }

            } catch (e: Exception) {
                Log.e("MediaUploadViewModel", "❌ Error: ${e.message}", e)
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ============================================
    // STATE MANAGEMENT
    // ============================================

    /**
     * Reset to idle state
     */
    fun resetState() {
        _uiState.value = UiState.Idle
    }

    /**
     * Discard current capture/selection
     */
    fun discard() {
        Log.d("MediaUploadViewModel", "🗑️ Discarding media")
        _uiState.value = UiState.Idle
    }

    // ============================================
    // VALIDATION
    // ============================================

    /**
     * Validate file before save
     */
    fun validateFile(fileSize: Double): Pair<Boolean, String> {
        val maxSizeMB = 10.0

        return when {
            fileSize > maxSizeMB -> Pair(false, "File too large (max ${maxSizeMB}MB)")
            fileSize <= 0 -> Pair(false, "File is empty")
            else -> Pair(true, "")
        }
    }
}