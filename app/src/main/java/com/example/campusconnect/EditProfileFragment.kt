package com.example.campusconnect

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.databinding.FragmentEditProfileBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var selectedImageBitmap: Bitmap? = null
    private val skillList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        loadCurrentUserData()

        binding.btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnUpdateProfile.setOnClickListener {
            updateProfile()
        }

        binding.btnChangePhoto.setOnClickListener {
            openGallery()
        }

        binding.btnAddSkill.setOnClickListener {
            val skill = binding.etAddSkill.text.toString().trim()
            if (skill.isNotEmpty() && !skillList.contains(skill)) {
                addSkillChip(skill)
                binding.etAddSkill.text.clear()
            }
        }
    }

    private fun addSkillChip(skill: String) {
        skillList.add(skill)
        val chip = Chip(requireContext()).apply {
            text = skill
            isCloseIconVisible = true
            setChipBackgroundColorResource(R.color.primaryRedLight)
            setTextColor(requireContext().getColor(R.color.primaryRed))
            setCloseIconTintResource(R.color.primaryRed)
            setOnCloseIconClickListener {
                skillList.remove(skill)
                binding.skillChipGroup.removeView(this)
            }
        }
        binding.skillChipGroup.addView(chip)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                selectedImageBitmap = BitmapFactory.decodeStream(inputStream)
                binding.ivEditProfileImage.setImageBitmap(selectedImageBitmap)
                binding.ivEditProfileImage.setPadding(0, 0, 0, 0)
            }
        }
    }

    private fun loadCurrentUserData() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("Users").child(userId).get().addOnSuccessListener { snapshot ->
            if (_binding == null) return@addOnSuccessListener
            if (snapshot.exists()) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    binding.etEditName.setText(it.fullName)
                    binding.etEditMobile.setText(it.mobile)
                    binding.etEditBio.setText(it.bio)
                    binding.etLinkedIn.setText(it.linkedin)
                    binding.etGitHub.setText(it.github)
                    binding.etWebsite.setText(it.website)

                    // Load skills
                    it.skills?.split(",")?.forEach { skill ->
                        val trimmed = skill.trim()
                        if (trimmed.isNotEmpty()) addSkillChip(trimmed)
                    }

                    // Load Image
                    if (!it.profileImageUrl.isNullOrEmpty()) {
                        try {
                            val decodedByte = Base64.decode(it.profileImageUrl, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                            binding.ivEditProfileImage.setImageBitmap(bitmap)
                            binding.ivEditProfileImage.setPadding(0, 0, 0, 0)
                        } catch (e: Exception) {}
                    }
                }
            }
        }
    }

    private fun updateProfile() {
        val name = binding.etEditName.text.toString().trim()
        val mobile = binding.etEditMobile.text.toString().trim()
        val bio = binding.etEditBio.text.toString().trim()
        val linkedin = binding.etLinkedIn.text.toString().trim()
        val github = binding.etGitHub.text.toString().trim()
        val website = binding.etWebsite.text.toString().trim()
        val skills = skillList.joinToString(",")

        if (name.isEmpty()) {
            binding.etEditName.error = "Name is required"
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val updates = mutableMapOf<String, Any>(
            "fullName" to name,
            "mobile" to mobile,
            "bio" to bio,
            "skills" to skills,
            "linkedin" to linkedin,
            "github" to github,
            "website" to website
        )

        selectedImageBitmap?.let {
            updates["profileImageUrl"] = encodeImage(it)
        }

        binding.btnUpdateProfile.isEnabled = false
        database.reference.child("Users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
                binding.btnUpdateProfile.isEnabled = true
            }
    }

    private fun encodeImage(bm: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
