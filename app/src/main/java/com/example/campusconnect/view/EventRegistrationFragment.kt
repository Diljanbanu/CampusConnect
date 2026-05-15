package com.example.campusconnect.view

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.databinding.FragmentEventRegistrationBinding
import com.example.campusconnect.model.Event
import com.example.campusconnect.model.EventRegistration
import com.example.campusconnect.repository.DataRepository
import com.example.campusconnect.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class EventRegistrationFragment : Fragment() {

    private var _binding: FragmentEventRegistrationBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private var event: Event? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        event = arguments?.getSerializable("event") as? Event

        if (event == null) {
            findNavController().navigateUp()
            return
        }

        setupUI()
        setupSpinners()
        loadUserData()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnConfirmRegistration.setOnClickListener { validateAndRegister() }
    }

    private fun setupUI() {
        event?.let {
            binding.tvEventTitle.text = it.title
            binding.tvEventType.text = it.type
            binding.tvEventDate.text = it.date
            binding.tvEventLocation.text = it.location

            if (!it.imageUrl.isNullOrEmpty()) {
                try {
                    val decodedByte = Base64.decode(it.imageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                    binding.ivEventImage.setImageBitmap(bitmap)
                } catch (e: Exception) {}
            }
        }
    }

    private fun setupSpinners() {
        val courses = arrayOf("BCA", "MCA", "B.Tech CE", "B.Tech IT", "BBA", "MBA")
        val courseAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, courses)
        binding.spinnerCourse.setAdapter(courseAdapter)

        val semesters = arrayOf("Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5", "Sem 6", "Sem 7", "Sem 8")
        val semAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, semesters)
        binding.spinnerSemester.setAdapter(semAdapter)
    }

    private fun loadUserData() {
        val user = DataRepository.currentUser
        user?.let {
            binding.etFullName.setText(it.fullName)
            binding.etEmail.setText(it.email)
            binding.etPhone.setText(it.mobile)
            binding.spinnerCourse.setText(it.branch, false)
        }
    }

    private fun validateAndRegister() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val course = binding.spinnerCourse.text.toString().trim()
        val sem = binding.spinnerSemester.text.toString().trim()
        val enrollment = binding.etEnrollment.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || course.isEmpty() || sem.isEmpty() || enrollment.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!binding.cbTerms.isChecked) {
            Toast.makeText(requireContext(), "Please agree to terms and conditions", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnConfirmRegistration.isEnabled = false
        
        val uid = auth.currentUser?.uid ?: return
        val registrationId = firestore.collection("Registrations").document().id
        
        val registration = EventRegistration(
            id = registrationId,
            eventId = event?.id,
            eventTitle = event?.title,
            studentId = uid,
            fullName = name,
            email = email,
            phone = phone,
            course = course,
            semester = sem,
            enrollmentNumber = enrollment,
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("Registrations").document(registrationId).set(registration)
            .addOnSuccessListener {
                event?.id?.let { eId ->
                    database.reference.child("Events").child(eId).child("registeredCount")
                        .setValue((event?.registeredCount ?: 0) + 1)
                    
                    val rtdbReg = mapOf("eventId" to eId, "studentId" to uid)
                    database.reference.child("Registrations").push().setValue(rtdbReg)
                }

                Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_LONG).show()
                NotificationHelper.sendNotification(uid, "Registration Success", "You are registered for ${event?.title}", "event")
                findNavController().navigateUp()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Registration failed: ${it.message}", Toast.LENGTH_SHORT).show()
                binding.btnConfirmRegistration.isEnabled = true
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
