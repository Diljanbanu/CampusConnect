package com.example.campusconnect.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.R
import com.example.campusconnect.controller.AuthController
import com.example.campusconnect.databinding.FragmentLoginBinding
import com.example.campusconnect.util.EmailSender

class LoginFragment : Fragment(), AuthController.AuthListener {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var controller: AuthController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller = AuthController(this)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!email.endsWith("@rku.ac.in")) {
                Toast.makeText(requireContext(), "Only RK University emails are allowed (@rku.ac.in)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            binding.btnLogin.isEnabled = false
            controller.loginOrRegister(email, password)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordSelectionFragment)
        }
    }

    override fun onAuthSuccess() {
        Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
    }

    override fun onAuthFailure(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        binding.btnLogin.isEnabled = true
    }

    override fun onRegistrationSuccess(message: String) {
        val email = binding.etEmail.text.toString()
        EmailSender.sendWelcomeEmail(email, "RKU Student")
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        binding.btnLogin.isEnabled = true
    }

    override fun onEmailVerificationSent(email: String) {
        Toast.makeText(requireContext(), "Verification email sent to $email.", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
