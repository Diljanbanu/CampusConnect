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
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.databinding.FragmentResumeBuilderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class ResumeBuilderFragment : Fragment() {

    private var _binding: FragmentResumeBuilderBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var selectedImageBitmap: Bitmap? = null
    private var existingProfileImageUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResumeBuilderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupSpinners()
        loadExistingResumeData()
        
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnUploadPhoto.setOnClickListener {
            openGallery()
        }

        binding.btnGeneratePDF.setOnClickListener {
            saveResumeData()
        }
    }

    private fun setupSpinners() {
        val courses = arrayOf("BCA", "MCA", "B.Tech CE", "B.Tech IT", "BBA", "MBA")
        val courseAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, courses)
        binding.spinnerCourse.setAdapter(courseAdapter)

        val types = arrayOf("Internship", "Full-time", "Part-time", "Freelance")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        binding.spinnerType.setAdapter(typeAdapter)
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
                binding.ivProfessionalPhoto.setImageBitmap(selectedImageBitmap)
                binding.ivProfessionalPhoto.setPadding(0, 0, 0, 0)
            }
        }
    }

    private fun loadExistingResumeData() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("Resumes").child(userId).get().addOnSuccessListener { snapshot ->
            if (_binding == null) return@addOnSuccessListener
            if (snapshot.exists()) {
                val resume = snapshot.getValue(Resume::class.java)
                resume?.let {
                    binding.etFullName.setText(it.fullName)
                    binding.etEmail.setText(it.email)
                    binding.etPhone.setText(it.phone)
                    binding.spinnerCourse.setText(it.course, false)
                    binding.etSemester.setText(it.semester)
                    binding.etDepartment.setText(it.department)
                    binding.etObjective.setText(it.objective)
                    binding.etSkills.setText(it.skills)
                    binding.etLanguages.setText(it.languages)
                    binding.etCompany.setText(it.company)
                    binding.etRole.setText(it.role)
                    binding.etDuration.setText(it.duration)
                    binding.spinnerType.setText(it.jobType, false)
                    binding.etWorkDesc.setText(it.workDescription)
                    binding.etProjects.setText(it.projects)
                    binding.etCertificates.setText(it.certificates)
                    binding.etLinkedIn.setText(it.linkedin)
                    binding.etGitHub.setText(it.github)

                    existingProfileImageUrl = it.profileImageUrl
                    if (!it.profileImageUrl.isNullOrEmpty()) {
                        try {
                            val decodedByte = Base64.decode(it.profileImageUrl, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                            binding.ivProfessionalPhoto.setImageBitmap(bitmap)
                            binding.ivProfessionalPhoto.setPadding(0, 0, 0, 0)
                        } catch (e: Exception) {}
                    }
                }
            } else {
                // If no resume, load basic info from User profile
                loadBasicInfoFromUser()
            }
        }
    }

    private fun loadBasicInfoFromUser() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("Users").child(userId).get().addOnSuccessListener { snapshot ->
            if (_binding == null) return@addOnSuccessListener
            val user = snapshot.getValue(User::class.java)
            user?.let {
                binding.etFullName.setText(it.fullName)
                binding.etEmail.setText(it.email)
                binding.etPhone.setText(it.mobile)
                binding.spinnerCourse.setText(it.branch, false)
                binding.etLinkedIn.setText(it.linkedin)
                binding.etGitHub.setText(it.github)
                
                if (!it.profileImageUrl.isNullOrEmpty()) {
                    existingProfileImageUrl = it.profileImageUrl
                    try {
                        val decodedByte = Base64.decode(it.profileImageUrl, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                        binding.ivProfessionalPhoto.setImageBitmap(bitmap)
                        binding.ivProfessionalPhoto.setPadding(0, 0, 0, 0)
                    } catch (e: Exception) {}
                }
            }
        }
    }

    private fun saveResumeData() {
        val userId = auth.currentUser?.uid ?: return
        
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        
        if (fullName.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "Name and Email are required", Toast.LENGTH_SHORT).show()
            return
        }

        val profileImage = if (selectedImageBitmap != null) {
            encodeImage(selectedImageBitmap!!)
        } else {
            existingProfileImageUrl
        }

        val resume = Resume(
            userId = userId,
            profileImageUrl = profileImage,
            fullName = fullName,
            email = email,
            phone = binding.etPhone.text.toString().trim(),
            course = binding.spinnerCourse.text.toString().trim(),
            semester = binding.etSemester.text.toString().trim(),
            department = binding.etDepartment.text.toString().trim(),
            objective = binding.etObjective.text.toString().trim(),
            skills = binding.etSkills.text.toString().trim(),
            languages = binding.etLanguages.text.toString().trim(),
            company = binding.etCompany.text.toString().trim(),
            role = binding.etRole.text.toString().trim(),
            duration = binding.etDuration.text.toString().trim(),
            jobType = binding.spinnerType.text.toString().trim(),
            workDescription = binding.etWorkDesc.text.toString().trim(),
            projects = binding.etProjects.text.toString().trim(),
            certificates = binding.etCertificates.text.toString().trim(),
            linkedin = binding.etLinkedIn.text.toString().trim(),
            github = binding.etGitHub.text.toString().trim()
        )

        binding.btnGeneratePDF.isEnabled = false
        database.reference.child("Resumes").child(userId).setValue(resume)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Resume data saved successfully!", Toast.LENGTH_SHORT).show()
                binding.btnGeneratePDF.isEnabled = true
                // In a real app, here you would trigger the PDF generation
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save: ${it.message}", Toast.LENGTH_SHORT).show()
                binding.btnGeneratePDF.isEnabled = true
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
