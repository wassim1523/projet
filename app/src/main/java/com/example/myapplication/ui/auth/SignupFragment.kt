package com.example.myapplication.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.auth.SessionManager
import com.example.myapplication.data.local.auth.UserEntity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class SignupFragment : Fragment(R.layout.fragment_signup) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etEmail = view.findViewById<EditText>(R.id.etSignupEmail)
        val etPassword = view.findViewById<EditText>(R.id.etSignupPassword)
        val btnSignup = view.findViewById<MaterialButton>(R.id.btnSignup)

        val db = AppDatabase.getInstance(requireContext())
        val userDao = db.userDao()
        val sessionManager = SessionManager(requireContext())

        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val existingUser = userDao.getUserByEmail(email)

                if (existingUser != null) {
                    Toast.makeText(requireContext(), "Email already exists", Toast.LENGTH_SHORT).show()
                } else {
                    val newUser = UserEntity(
                        username = name,
                        email = email,
                        password = password
                    )

                    val userId = userDao.insertUser(newUser).toInt()

                    sessionManager.saveLoginSession(
                        userId = userId,
                        name = name,
                        email = email
                    )

                    Toast.makeText(requireContext(), "Account created successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_signupFragment_to_homeFragment)
                }
            }
        }
    }
}