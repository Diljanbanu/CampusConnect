package com.example.campusconnect

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.databinding.FragmentChangePasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private var isLengthValid = false
    private var isUpperValid = false
    private var isNumberValid = false
    private var isSpecialValid = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setupPasswordValidation()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }
        
        binding.btnUpdatePassword.setOnClickListener {
            updatePassword()
        }
    }

    private fun setupPasswordValidation() {
        binding.etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()
                validatePassword(password)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkFormValidity()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun validatePassword(password: String) {
        isLengthValid = password.length >= 8
        isUpperValid = password.any { it.isUpperCase() }
        isNumberValid = password.any { it.isDigit() }
        isSpecialValid = password.any { !it.isLetterOrDigit() }

        updateValidationUI()
        checkFormValidity()
    }

    private fun updateValidationUI() {
        val green = ContextCompat.getColor(requireContext(), R.color.online_green)
        val grey = ContextCompat.getColor(requireContext(), R.color.gray_text)

        binding.ivCheckLength.apply {
            imageTintList = android.content.res.ColorStateList.valueOf(if (isLengthValid) green else grey)
        }
        binding.ivCheckUpper.apply {
            imageTintList = android.content.res.ColorStateList.valueOf(if (isUpperValid) green else grey)
        }
        binding.ivCheckNumber.apply {
            imageTintList = android.content.res.ColorStateList.valueOf(if (isNumberValid) green else grey)
        }
        binding.ivCheckSpecial.apply {
            imageTintList = android.content.res.ColorStateList.valueOf(if (isSpecialValid) green else grey)
        }
    }

    private fun checkFormValidity() {
        val allValid = isLengthValid && isUpperValid && isNumberValid && isSpecialValid
        val passwordsMatch = binding.etNewPassword.text.toString() == binding.etConfirmPassword.text.toString()
        val currentPopulated = !binding.etCurrentPassword.text.isNullOrBlank()

        if (allValid && passwordsMatch && currentPopulated) {
            binding.btnUpdatePassword.isEnabled = true
            binding.btnUpdatePassword.backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primaryRed)
            )
        } else {
            binding.btnUpdatePassword.isEnabled = false
            binding.btnUpdatePassword.backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primaryRedLight)
            )
        }
    }

    private fun updatePassword() {
        val user = auth.currentUser
        val currentPassword = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()

        if (user == null || user.email == null) return

        binding.btnUpdatePassword.isEnabled = false
        binding.btnUpdatePassword.text = "Updating..."

        // Re-authenticate user first
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Now update password
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        Toast.makeText(requireContext(), "Password Updated Successfully", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(requireContext(), "Error: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                        binding.btnUpdatePassword.isEnabled = true
                        binding.btnUpdatePassword.text = "Update Password"
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Authentication Failed: Incorrect current password", Toast.LENGTH_SHORT).show()
                binding.btnUpdatePassword.isEnabled = true
                binding.btnUpdatePassword.text = "Update Password"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
