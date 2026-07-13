package com.jsac.sync.presentation.dashboard

import android.os.Bundle
import android.util.Log
import android.view.View
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Color
import com.jsac.sync.data.remote.dto.ThemeConfig
import com.jsac.sync.data.remote.dto.LayoutConfig
import com.jsac.sync.data.remote.dto.BrandingConfig
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
 * - Conditional visibility based on field values
 * - Dynamic enable/disable based on field values
 * - Backend-driven default values
 * - Synchronized theme, layout, and branding from backend
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
    private val fieldContainers = mutableMapOf<String, View>()

    // ✅ Map to store input widgets for enable/disable control
    private val fieldInputs = mutableMapOf<String, View>()

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

                    refreshDynamicRules()

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

                    refreshDynamicRules()

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

            currentForm?.let { form ->

                val (isValid, message) = viewModel.validateForm(form)

                if (isValid) {

                    viewModel.submitForm(
                        formId,
                        form
                    )

                } else {

                    Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    private fun styleEditText(
        editText: EditText,
        field: FormField
    ) {
        editText.apply {

            setTextColor(
                Color.parseColor(
                    currentForm?.theme?.textColor ?: "#212121"
                )
            )

            setHintTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_hint
                )
            )

            textSize = 16f

            hint = field.placeholder ?: ""

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            setPadding(
                resources.getDimensionPixelSize(R.dimen.space_md),
                resources.getDimensionPixelSize(R.dimen.space_md),
                resources.getDimensionPixelSize(R.dimen.space_md),
                resources.getDimensionPixelSize(R.dimen.space_md)
            )

            // Optional: Set background drawable if exists
            try {
                setBackgroundResource(R.drawable.bg_edittext)
                background?.setTint(
                    Color.parseColor(
                        currentForm?.theme?.primaryColor ?: "#1976D2"
                    )
                )
            } catch (e: Exception) {
                // Use default if drawable doesn't exist
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

        currentForm = form

        applyTheme(form.theme)
        applyLayout(form.layout)
        applyBranding(form.branding)

        tvFormDescription.text = form.description

        containerForm.removeAllViews()

        for (field in form.fields) {
            createFormField(field)
        }

        // Call refreshDynamicRules after rendering all fields
        refreshDynamicRules()
    }

    private fun createFormField(field: FormField) {
        val fieldContainer = createFieldContainer()

        val label = createFieldLabel(field)

        fieldContainer.addView(label)

        // Create appropriate input based on field type
        when (field.type) {
            "text", "number" -> {

                val inputType = when (field.type) {
                    "number" -> android.text.InputType.TYPE_CLASS_NUMBER
                    else -> android.text.InputType.TYPE_CLASS_TEXT
                }

                val editText = EditText(requireContext()).apply {

                    setText(field.default_value ?: "")

                    this.inputType = inputType
                }

                styleEditText(
                    editText,
                    field
                )

                registerDefaultValue(field)

                registerTextWatcher(
                    field,
                    editText
                )

                fieldContainer.addView(editText)
            }

            "email" -> {

                val editText = EditText(requireContext()).apply {

                    inputType =
                        android.text.InputType.TYPE_CLASS_TEXT or
                                android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

                    setText(field.default_value ?: "")
                }

                styleEditText(
                    editText,
                    field
                )

                editText.hint =
                    field.placeholder ?: "Enter email address"

                registerDefaultValue(field)

                registerTextWatcher(
                    field,
                    editText
                )

                fieldContainer.addView(editText)
            }

            "textarea" -> {

                val editText = EditText(requireContext()).apply {

                    inputType =
                        android.text.InputType.TYPE_CLASS_TEXT or
                                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

                    minLines = 4

                    maxLines = 8

                    gravity = android.view.Gravity.TOP

                    setText(field.default_value ?: "")
                }

                styleEditText(
                    editText,
                    field
                )

                registerDefaultValue(field)

                registerTextWatcher(
                    field,
                    editText
                )

                fieldContainer.addView(editText)
            }

            "dropdown" -> {

                val spinner = Spinner(requireContext()).apply {

                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    minimumHeight = 140

                    setPadding(
                        resources.getDimensionPixelSize(R.dimen.space_md),
                        resources.getDimensionPixelSize(R.dimen.space_sm),
                        resources.getDimensionPixelSize(R.dimen.space_md),
                        resources.getDimensionPixelSize(R.dimen.space_sm)
                    )
                }

                registerSpinner(
                    field,
                    spinner
                )

                fieldContainer.addView(spinner)
            }

            "date" -> {

                val editText = EditText(requireContext()).apply {

                    inputType =
                        android.text.InputType.TYPE_CLASS_DATETIME

                    isFocusable = true

                    isClickable = true

                    setText(field.default_value ?: "")
                }

                styleEditText(
                    editText,
                    field
                )

                editText.hint =
                    field.placeholder ?: "YYYY-MM-DD"

                registerDefaultValue(field)

                registerTextWatcher(
                    field,
                    editText
                )

                fieldContainer.addView(editText)
            }

            "media" -> {

                val button = Button(requireContext()).apply {

                    text = "Select Photo"

                    styleActionButton(this)
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

                                        val imageFile =
                                            createImageFile()

                                        capturedPhotoUri =
                                            FileProvider.getUriForFile(
                                                requireContext(),
                                                "${requireContext().packageName}.provider",
                                                imageFile
                                            )

                                        cameraLauncher.launch(
                                            capturedPhotoUri
                                        )

                                    } else {

                                        cameraPermissionLauncher.launch(
                                            Manifest.permission.CAMERA
                                        )
                                    }
                                }

                                1 -> {

                                    mediaPickerLauncher.launch(
                                        "image/*"
                                    )
                                }
                            }
                        }
                        .show()
                }

                fieldInputs[field.id] = button

                fieldContainer.addView(button)
            }

            "gps" -> {

                val button = Button(requireContext()).apply {

                    text = "Capture Location"

                    styleActionButton(this)
                }

                button.setOnClickListener {

                    if (
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {

                        captureLocation(field.id)

                    } else {

                        pendingGpsFieldId = field.id

                        locationPermissionLauncher.launch(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    }
                }

                fieldInputs[field.id] = button

                fieldContainer.addView(button)
            }
        }
        fieldContainers[field.id] = fieldContainer

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
    private fun createFieldContainer(): LinearLayout {
        return LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val layout = currentForm?.layout

                setMargins(
                    0,
                    layout?.sectionSpacing ?: 24,
                    0,
                    layout?.spacing ?: 16
                )
            }

            orientation = LinearLayout.VERTICAL
        }
    }

    private fun createFieldLabel(
        field: FormField
    ): TextView {

        return TextView(requireContext()).apply {

            text =
                if (field.required)
                    "${field.name} *"
                else
                    field.name

            textSize = 15f

            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD
            )

            setTextColor(
                Color.parseColor(
                    currentForm?.theme?.textColor ?: "#212121"
                )
            )

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    bottomMargin =
                        currentForm?.layout?.spacing
                            ?: 16
                }
        }
    }

    private fun styleActionButton(
        button: Button
    ) {

        button.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        button.minimumHeight =
            resources.getDimensionPixelSize(
                R.dimen.button_height
            )

        button.isAllCaps = false

        button.setPadding(
            resources.getDimensionPixelSize(R.dimen.space_md),
            resources.getDimensionPixelSize(R.dimen.space_md),
            resources.getDimensionPixelSize(R.dimen.space_md),
            resources.getDimensionPixelSize(R.dimen.space_md)
        )

        button.setBackgroundColor(
            Color.parseColor(
                currentForm?.theme?.buttonColor ?: "#1976D2"
            )
        )

        button.setTextColor(
            Color.parseColor(
                currentForm?.theme?.buttonTextColor ?: "#FFFFFF"
            )
        )
    }

    private fun registerDefaultValue(
        field: FormField
    ) {
        field.default_value?.let {
            viewModel.updateFieldValue(
                field.id,
                it
            )
        }
    }

    private fun registerTextWatcher(
        field: FormField,
        editText: EditText
    ) {

        editText.setOnTextChangedListener {

            viewModel.updateFieldValue(
                field.id,
                it
            )

            refreshDynamicRules()
        }

        fieldInputs[field.id] = editText
    }

    private fun registerSpinner(
        field: FormField,
        spinner: Spinner
    ) {

        if (field.options.isNullOrEmpty()) return

        val adapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            field.options
        ).apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        spinner.adapter = adapter

        field.default_value?.let { defaultValue ->

            val index = field.options.indexOf(defaultValue)

            if (index >= 0) {

                spinner.setSelection(index)

                viewModel.updateFieldValue(
                    field.id,
                    defaultValue
                )
            }
        }

        spinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    viewModel.updateFieldValue(
                        field.id,
                        field.options[position]
                    )

                    refreshDynamicRules()
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }

        fieldInputs[field.id] = spinner
    }
    private fun showToast(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showLongToast(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun updateField(
        fieldId: String,
        value: String
    ) {
        viewModel.updateFieldValue(
            fieldId,
            value
        )

        refreshDynamicRules()
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

                    refreshDynamicRules()

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

    private fun applyTheme(theme: ThemeConfig) {

        try {

            tvFormTitle.setTextColor(
                Color.parseColor(theme.textColor)
            )

            tvFormDescription.setTextColor(
                Color.parseColor(theme.textColor)
            )

            btnSubmit.setBackgroundColor(
                Color.parseColor(theme.buttonColor)
            )

            btnSubmit.setTextColor(
                Color.parseColor(theme.buttonTextColor)
            )

            requireView().setBackgroundColor(
                Color.parseColor(theme.backgroundColor)
            )

        } catch (e: Exception) {

            Log.e(
                "Theme",
                "Theme parse error: ${e.message}"
            )

        }
    }

    private fun applyLayout(layout: LayoutConfig) {

        containerForm.orientation =
            if (layout.columns == 1)
                LinearLayout.VERTICAL
            else
                LinearLayout.HORIZONTAL

        containerForm.setPadding(

            layout.cardPadding,

            layout.cardPadding,

            layout.cardPadding,

            layout.cardPadding

        )

    }

    private fun applyBranding(
        branding: BrandingConfig
    ) {

        tvFormTitle.text =

            if (branding.organizationName.isNotBlank())

                branding.organizationName

            else

                currentForm?.name ?: formName


        when (branding.titleAlignment) {

            "left" ->
                tvFormTitle.textAlignment =
                    View.TEXT_ALIGNMENT_VIEW_START

            "right" ->
                tvFormTitle.textAlignment =
                    View.TEXT_ALIGNMENT_VIEW_END

            else ->
                tvFormTitle.textAlignment =
                    View.TEXT_ALIGNMENT_CENTER

        }

    }

    // Update conditional visibility and enabled state together
    private fun refreshDynamicRules() {
        updateConditionalVisibility()
        updateConditionalEnabledState()
    }

    private fun updateConditionalVisibility() {

        val form = currentForm ?: return

        for (field in form.fields) {

            val container = fieldContainers[field.id] ?: continue

            val rule = field.visible_if

            if (rule == null) {

                container.visibility = View.VISIBLE
                continue
            }

            val currentValue = viewModel.getFieldValue(rule.field).trim()

            container.visibility =
                if (currentValue.equals(rule.equals.trim(), ignoreCase = true)) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    // Update enabled/disabled state based on enabled_if rules
    private fun updateConditionalEnabledState() {

        val form = currentForm ?: return

        for (field in form.fields) {

            val input = fieldInputs[field.id] ?: continue

            val rule = field.enabled_if

            if (rule == null) {

                input.isEnabled = true

                continue
            }

            val currentValue = viewModel
                .getFieldValue(rule.field)
                .trim()

            input.isEnabled =
                currentValue.equals(
                    rule.equals.trim(),
                    ignoreCase = true
                )
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        containerForm.visibility = View.GONE
        btnSubmit.visibility = View.GONE
        tvError.visibility = View.GONE
    }

    private fun hideLoading() {

        view?.findViewById<View>(
            R.id.loadingContainer
        )?.visibility = View.GONE

        containerForm.visibility = View.VISIBLE

        btnSubmit.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        tvError.visibility = View.VISIBLE
        tvError.text =
            "Unable to load the form.\n\n$message"
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