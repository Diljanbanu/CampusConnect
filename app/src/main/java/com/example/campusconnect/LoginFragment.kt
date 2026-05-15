package com.example.campusconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.FirebaseDatabase

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.btnLogin.setOnClickListener {
            loginOrRegisterUser()
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordSelectionFragment)
        }
    }

    private fun loginOrRegisterUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        if (!email.endsWith("@rku.ac.in")) {
            Toast.makeText(requireContext(), "Only RK University emails are allowed (@rku.ac.in)", Toast.LENGTH_LONG).show()
            return
        }

        binding.btnLogin.isEnabled = false
        
        // Try to Sign In first
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    handleSuccessfulLogin()
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthInvalidUserException) {
                        // User does not exist, try to Register
                        registerNewUser(email, password)
                    } else {
                        Toast.makeText(requireContext(), "Login Failed: ${exception?.message}", Toast.LENGTH_SHORT).show()
                        binding.btnLogin.isEnabled = true
                    }
                }
            }
    }

    private fun registerNewUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val userId = firebaseUser?.uid
                    
                    // Send Email Verification
                    firebaseUser?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                Toast.makeText(requireContext(), "New account created. Verification email sent to $email.", Toast.LENGTH_LONG).show()
                            }
                        }

                    // Create a minimal user profile in Realtime Database
                    // Using "RKU Student" as default name since we don't have it from the UI
                    val user = User(uid = userId, fullName = "RKU Student", email = email)

                    if (userId != null) {
                        database.reference.child("Users").child(userId).setValue(user)
                            .addOnSuccessListener {
                                val sharedPref = requireActivity().getSharedPreferences("CampusConnectPrefs", Context.MODE_PRIVATE)
                                sharedPref.edit().putBoolean("isFirstRun", false).apply()
                                
                                // Send professional welcome email
                                EmailSender.sendWelcomeEmail(email, "RKU Student")

                                // Redirect or ask to verify
                                Toast.makeText(requireContext(), "Registration successful. Please verify email and login.", Toast.LENGTH_LONG).show()
                                auth.signOut()
                                binding.btnLogin.isEnabled = true
                            }
                    }
                } else {
                    Toast.makeText(requireContext(), "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    binding.btnLogin.isEnabled = true
                }
            }
    }

    private fun handleSuccessfulLogin() {
        val user = auth.currentUser
        if (user != null) {
            if (user.isEmailVerified) {
                Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            } else {
                Toast.makeText(requireContext(), "Please verify your email first.", Toast.LENGTH_LONG).show()
                auth.signOut()
                binding.btnLogin.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
