package com.example.campusconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.databinding.FragmentPostStartupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PostStartupFragment : Fragment() {

    private var _binding: FragmentPostStartupBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostStartupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupStageSpinner()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSubmitIdea.setOnClickListener {
            submitStartupIdea()
        }
    }

    private fun setupStageSpinner() {
        val stages = arrayOf("Ideation", "MVP", "Early Traction", "Scaling")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, stages)
        binding.spinnerStage.setAdapter(adapter)
    }

    private fun submitStartupIdea() {
        val title = binding.etStartupTitle.text.toString().trim()
        val skills = binding.etRequiredSkills.text.toString().trim()
        val teamSize = binding.etTeamSize.text.toString().trim()
        val stage = binding.spinnerStage.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (title.isEmpty() || skills.isEmpty() || teamSize.isEmpty() || stage.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        
        // Fetch user name for the startup entry
        database.reference.child("Users").child(userId).child("fullName").get().addOnSuccessListener { snapshot ->
            val userName = snapshot.value?.toString() ?: "Unknown"
            val ideaId = database.reference.child("Ideas").push().key ?: return@addOnSuccessListener
            
            val startup = Startup(
                id = ideaId,
                title = title,
                requiredSkills = skills,
                teamSize = teamSize,
                stage = stage,
                description = description,
                studentName = userName,
                studentId = userId,
                status = "pending"
            )

            database.reference.child("Ideas").child(ideaId).setValue(startup)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "StartUp Idea Submitted Successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to submit idea: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}