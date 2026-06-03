package com.jsac.sync.presentation.dashboard


import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jsac.sync.R

/**
 * Placeholder for Part 5: Form Detail Fragment
 *
 * This will display the complete form with all fields
 * and allow users to fill and submit the form
 */
class FormDetailFragment : Fragment(R.layout.fragment_form_detail) {

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val formId = arguments?.getString("form_id")
        val formName = arguments?.getString("form_name")

        val tvPlaceholder = view.findViewById<TextView>(R.id.tvPlaceholder)
        tvPlaceholder.text = "Form Detail Coming in Part 5\n\nForm: $formName\nID: $formId"
    }
}