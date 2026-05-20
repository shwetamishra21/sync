package com.jsac.sync.presentation.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.jsac.sync.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val etUsername =
            view.findViewById<EditText>(R.id.etUsername)

        val etPassword =
            view.findViewById<EditText>(R.id.etPassword)

        val btnLogin =
            view.findViewById<Button>(R.id.btnLogin)

        val btnGoToRegister =
            view.findViewById<Button>(R.id.btnGoToRegister)

        btnLogin.setOnClickListener {

            val username =
                etUsername.text.toString()

            val password =
                etPassword.text.toString()

            viewModel.login(
                username,
                password,

                onSuccess = {

                    Toast.makeText(
                        requireContext(),
                        "Login Successful",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(
                        R.id.action_login_to_home
                    )
                },

                onError = { error ->

                    Toast.makeText(
                        requireContext(),
                        error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        btnGoToRegister.setOnClickListener {

            findNavController().navigate(
                R.id.action_login_to_register
            )
        }
    }
}