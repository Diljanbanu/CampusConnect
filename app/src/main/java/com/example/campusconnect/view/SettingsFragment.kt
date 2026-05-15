package com.example.campusconnect.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.R
import com.example.campusconnect.databinding.FragmentSettingsBinding
import com.example.campusconnect.repository.DataRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupUI()
        loadPreferences()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnPrivacy.setOnClickListener {
            Toast.makeText(requireContext(), "Privacy settings coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_changePasswordFragment)
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("notificationsEnabled", isChecked)
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("darkModeEnabled", isChecked)
            applyDarkMode(isChecked)
        }

        binding.btnAboutUs.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_aboutUsFragment)
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            Toast.makeText(requireContext(), "Privacy Policy coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.tvAppVersion.text = "1.0.0-build1"
    }

    private fun applyDarkMode(isEnabled: Boolean) {
        val sharedPref = requireActivity().getSharedPreferences("CampusConnectPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("darkModeEnabled", isEnabled).apply()
        
        if (isEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun loadPreferences() {
        val sharedPref = requireActivity().getSharedPreferences("CampusConnectPrefs", Context.MODE_PRIVATE)
        binding.switchDarkMode.isChecked = sharedPref.getBoolean("darkModeEnabled", false)
        
        val user = DataRepository.currentUser
        user?.let {
            binding.switchNotifications.isChecked = it.notificationsEnabled
        }
    }

    private fun updatePreference(key: String, value: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        database.reference.child("Users").child(uid).child(key).setValue(value)
            .addOnSuccessListener {
                when (key) {
                    "notificationsEnabled" -> DataRepository.currentUser = DataRepository.currentUser?.copy(notificationsEnabled = value)
                    "darkModeEnabled" -> DataRepository.currentUser = DataRepository.currentUser?.copy(darkModeEnabled = value)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
