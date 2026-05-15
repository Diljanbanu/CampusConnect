package com.example.campusconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.databinding.FragmentForgotPasswordSelectionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ForgotPasswordSelectionFragment : Fragment() {

    private var _binding: FragmentForgotPasswordSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var selection = "email" // Default selection

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        updateUI()

        binding.cardEmail.setOnClickListener {
            selection = "email"
            updateUI()
        }

        binding.cardSms.setOnClickListener {
            selection = "sms"
            updateUI()
        }

        binding.btnSendOtp.setOnClickListener {
            if (selection == "email") {
                val email = binding.etIdentifierEmail.text.toString().trim()
                if (email.isEmpty()) {
                    binding.etIdentifierEmail.error = "Enter registered email"
                    return@setOnClickListener
                }
                verifyAndSendEmailOtp(email)
            } else {
                val phone = binding.etIdentifierPhone.text.toString().trim()
                if (phone.isEmpty()) {
                    binding.etIdentifierPhone.error = "Enter registered mobile number"
                    return@setOnClickListener
                }
                verifyAndSendSmsOtp(phone)
            }
        }
    }

    private fun updateUI() {
        val red = resources.getColor(R.color.primaryRed)
        val lightGray = resources.getColor(R.color.light_gray_bg)

        if (selection == "email") {
            binding.cardEmail.setStrokeColor(red)
            binding.cardEmail.strokeWidth = 6
            binding.cardSms.setStrokeColor(lightGray)
            binding.cardSms.strokeWidth = 2
            
            binding.tilEmail.visibility = View.VISIBLE
            binding.tilPhone.visibility = View.GONE
            binding.tvDescription.text = "Enter your registered email to receive a verification OTP"
        } else {
            binding.cardSms.setStrokeColor(red)
            binding.cardSms.strokeWidth = 6
            binding.cardEmail.setStrokeColor(lightGray)
            binding.cardEmail.strokeWidth = 2
            
            binding.tilPhone.visibility = View.VISIBLE
            binding.tilEmail.visibility = View.GONE
            binding.tvDescription.text = "Enter your registered mobile number to receive a verification OTP"
        }
    }

    private fun verifyAndSendEmailOtp(email: String) {
        database.reference.child("Users").orderByChild("email").equalTo(email)
            .get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val otp = (100000..999999).random().toString()
                    EmailSender.sendPasswordResetOtp(email, otp)
                    
                    val bundle = Bundle().apply {
                        putString("recoveryType", "email")
                        putString("identifier", email)
                        putString("otp", otp)
                    }
                    findNavController().navigate(R.id.action_forgotPasswordSelectionFragment_to_otpVerificationFragment, bundle)
                } else {
                    Toast.makeText(requireContext(), "No account found with this email", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun verifyAndSendSmsOtp(phone: String) {
        // Ensure phone starts with +91 if needed, or matches DB format
        val formattedPhone = if (phone.startsWith("+")) phone else "+91$phone"
        
        database.reference.child("Users").orderByChild("mobile").equalTo(phone)
            .get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val bundle = Bundle().apply {
                        putString("recoveryType", "sms")
                        putString("phoneNumber", formattedPhone)
                    }
                    findNavController().navigate(R.id.action_forgotPasswordSelectionFragment_to_otpVerificationFragment, bundle)
                } else {
                    Toast.makeText(requireContext(), "No account found with this number", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}