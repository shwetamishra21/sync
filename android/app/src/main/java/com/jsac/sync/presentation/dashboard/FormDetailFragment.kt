package com.jsac.sync.presentation.dashboard

import android.os.Bundle
import android.util.Log
import android.view.View
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.FileProvider
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.net.Uri
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jsac.sync.R
import com.jsac.sync.data.remote.dto.FormDetail
import com.jsac.sync.data.remote.dto.FormField
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.jsac.sync.utils.LocationHelper

/**
 * Fragment for displaying and filling a form
 *
 * Features:
 * - Dynamically creates form fields based on form definition
 * - Supports: text, email, textarea, dropdown, number, date fields
 * - Validates required fields
 * - Offline-first submission (saves locally, syncs later)
 * - Shows submission status to user
 */
@AndroidEntryPoint
class FormDetailFragment : Fragment(R.layout.fragment_form_detail) {

    private val viewModel: FormDetailViewModel by viewModels()

    private lateinit var formId: String
    private lateinit var formName: String
    private val mediaUploadViewModel: MediaUploadViewModel by viewModels()

    // UI Components
    private lateinit var tvFormTitle: TextView
    private lateinit var tvFormDescription: TextView
    private lateinit var containerForm: LinearLayout
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private var currentForm: FormDetail? = null

    private var pendingGpsFieldId: String? = null
    private var pendingMediaFieldId: String? = null
    private var pendingCameraFieldId: String? = null
    private var capturedPhotoUri: Uri? = null

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                pendingGpsFieldId?.let {
                    captureLocation(it)
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    private val mediaPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                pendingMediaFieldId?.let { fieldId ->

                    viewModel.updateFieldValue(
                        fieldId,
                        uri.toString()
                    )

                    Toast.makeText(
                        requireContext(),
                        "Photo selected",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                pendingCameraFieldId?.let { fieldId ->

                    val imageFile = createImageFile()

                    capturedPhotoUri =
                        FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.provider",
                            imageFile
                        )

                    cameraLauncher.launch(capturedPhotoUri)
                }

            } else {

                Toast.makeText(
                    requireContext(),
                    "Camera permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (success && capturedPhotoUri != null) {

                pendingCameraFieldId?.let { fieldId ->

                    val imagePath =
                        capturedPhotoUri?.path ?: ""

                    viewModel.updateFieldValue(
                        fieldId,
                        imagePath
                    )

                    Log.d(
                        "FormDetailFragment",
                        "📸 Image path = $imagePath"
                    )

                    Toast.makeText(
                        requireContext(),
                        "Photo captured",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("FormDetailFragment", "🎬 FormDetailFragment created")

        // Get arguments
        formId = arguments?.getString("form_id") ?: ""
        formName = arguments?.getString("form_name") ?: "Form"

        // Find views
        tvFormTitle = view.findViewById(R.id.tvFormTitle)
        tvFormDescription = view.findViewById(R.id.tvFormDescription)
        containerForm = view.findViewById(R.id.containerForm)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)

        // Set title
        tvFormTitle.text = formName

        // Load form
        viewModel.loadForm(formId)

        // Observe UI state
        observeFormState()

        // Observe submit state
        observeSubmitState()

        // Handle submit button
        btnSubmit.setOnClickListener {
            if (currentForm != null) {
                viewModel.submitForm(formId, currentForm!!)
            }
        }
    }

    private fun observeFormState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is FormDetailViewModel.UiState.Loading -> {
                        showLoading()
                    }

                    is FormDetailViewModel.UiState.Success -> {
                        hideLoading()
                        currentForm = state.form
                        displayForm(state.form)
                    }

                    is FormDetailViewModel.UiState.Error -> {
                        hideLoading()
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun observeSubmitState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.submitState.collect { state ->
                when (state) {
                    is FormDetailViewModel.SubmitState.Idle -> {
                        btnSubmit.isEnabled = true
                        btnSubmit.text = "Submit Form"
                    }

                    is FormDetailViewModel.SubmitState.Submitting -> {
                        btnSubmit.isEnabled = false
                        btnSubmit.text = "Submitting..."
                    }

                    is FormDetailViewModel.SubmitState.Success -> {
                        Toast.makeText(
                            requireContext(),
                            "✅ Form submitted successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        Log.d("FormDetailFragment", "✅ Submission ID: ${state.submissionId}")

                        // Navigate back to dashboard
                        findNavController().popBackStack()
                    }

                    is FormDetailViewModel.SubmitState.Error -> {
                        Toast.makeText(
                            requireContext(),
                            "❌ ${state.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        btnSubmit.isEnabled = true
                        btnSubmit.text = "Submit Form"

                        Log.e("FormDetailFragment", "❌ Submission error: ${state.message}")
                    }
                }
            }
        }
    }

    private fun displayForm(form: FormDetail) {
        Log.d("FormDetailFragment", "📋 Displaying form with ${form.fields.size} fields")

        tvFormDescription.text = form.description
        containerForm.removeAllViews()

        // Create form fields dynamically
        for (field in form.fields) {
            createFormField(field)
        }
    }

    private fun createFormField(field: FormField) {
        val fieldContainer = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
            orientation = LinearLayout.VERTICAL
        }

        // Field label
        val label = TextView(requireContext()).apply {
            text = field.name + if (field.required) " *" else ""
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        fieldContainer.addView(label)

        // Create appropriate input based on field type
        when (field.type) {
            "text", "number" -> {
                var inputType = when (field.type) {
                    "number" -> android.text.InputType.TYPE_CLASS_NUMBER
                    else -> android.text.InputType.TYPE_CLASS_TEXT
                }

                val editText = EditText(requireContext()).apply {
                    hint = field.placeholder ?: "Enter ${field.name}"
                    inputType = inputType
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(12, 12, 12, 12)
                }

                editText.setOnTextChangedListener { text ->
                    viewModel.updateFieldValue(field.id, text.toString())
                }

                fieldContainer.addView(editText)
            }

            "email" -> {
                val editText = EditText(requireContext()).apply {
                    hint = field.placeholder ?: "Enter email"
                    inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(12, 12, 12, 12)
                }

                editText.setOnTextChangedListener { text ->
                    viewModel.updateFieldValue(field.id, text.toString())
                }

                fieldContainer.addView(editText)
            }

            "textarea" -> {
                val editText = EditText(requireContext()).apply {
                    hint = field.placeholder ?: "Enter ${field.name}"
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    minLines = 4
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(12, 12, 12, 12)
                }

                editText.setOnTextChangedListener { text ->
                    viewModel.updateFieldValue(field.id, text.toString())
                }

                fieldContainer.addView(editText)
            }

            "dropdown" -> {
                val spinner = Spinner(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                if (!field.options.isNullOrEmpty()) {
                    val adapter = android.widget.ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        field.options
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter

                    spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                            val selected = field.options[position]
                            viewModel.updateFieldValue(field.id, selected)
                        }

                        override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                    }
                }

                fieldContainer.addView(spinner)
            }

            "date" -> {
                val editText = EditText(requireContext()).apply {
                    hint = "YYYY-MM-DD"
                    inputType = android.text.InputType.TYPE_CLASS_DATETIME
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(12, 12, 12, 12)
                }

                editText.setOnTextChangedListener { text ->
                    viewModel.updateFieldValue(field.id, text.toString())
                }

                fieldContainer.addView(editText)
            }
            "media" -> {

                val button = Button(requireContext()).apply {
                    text = "Select Photo"

                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                button.setOnClickListener {

                    pendingMediaFieldId = field.id

                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Select Image")
                        .setItems(
                            arrayOf(
                                "Camera",
                                "Gallery"
                            )
                        ) { _, which ->

                            when (which) {

                                0 -> {

                                    pendingCameraFieldId = field.id

                                    if (
                                        ContextCompat.checkSelfPermission(
                                            requireContext(),
                                            Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {

                                        val imageFile = createImageFile()

                                        capturedPhotoUri =
                                            FileProvider.getUriForFile(
                                                requireContext(),
                                                "${requireContext().packageName}.provider",
                                                imageFile
                                            )

                                        cameraLauncher.launch(capturedPhotoUri)

                                    } else {

                                        cameraPermissionLauncher.launch(
                                            Manifest.permission.CAMERA
                                        )
                                    }
                                }

                                1 -> {
                                    mediaPickerLauncher.launch("image/*")
                                }
                            }
                        }
                        .show()
                }

                fieldContainer.addView(button)
            }

            "gps" -> {

                val button = Button(requireContext()).apply {
                    text = "Capture Location"

                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                button.setOnClickListener {

                    if (
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {

                        captureLocation(field.id)

                    } else {

                        pendingGpsFieldId = field.id

                        locationPermissionLauncher.launch(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    }
                }

                fieldContainer.addView(button)
            }
        }

        containerForm.addView(fieldContainer)
    }
    private fun createImageFile(): File {

        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        val imageDir = File(
            requireContext().cacheDir,
            "images"
        )

        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }

        return File(
            imageDir,
            "IMG_${timeStamp}.jpg"
        )
    }

    private fun captureLocation(fieldId: String) {

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val fusedClient =
                    LocationServices.getFusedLocationProviderClient(
                        requireContext()
                    )

                val locationHelper =
                    LocationHelper(
                        requireContext(),
                        fusedClient
                    )

                val location =
                    locationHelper.getCurrentLocation()

                if (location != null) {

                    val value =
                        "${location.latitude},${location.longitude}"

                    viewModel.updateFieldValue(
                        fieldId,
                        value
                    )

                    Toast.makeText(
                        requireContext(),
                        "Location captured",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Unable to get location",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        containerForm.visibility = View.GONE
        btnSubmit.visibility = View.GONE
        tvError.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        containerForm.visibility = View.VISIBLE
        btnSubmit.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        tvError.visibility = View.VISIBLE
        tvError.text = "❌ Error: $message"
        containerForm.visibility = View.GONE
        btnSubmit.visibility = View.GONE
    }
}

// ============================================
// EXTENSION FUNCTION
// ============================================

private fun EditText.setOnTextChangedListener(callback: (String) -> Unit) {
    this.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            callback(s?.toString() ?: "")
        }
        override fun afterTextChanged(s: android.text.Editable?) {}
    })
}