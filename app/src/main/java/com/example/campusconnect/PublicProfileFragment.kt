package com.example.campusconnect

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.databinding.FragmentPublicProfileBinding
import com.google.android.material.chip.Chip
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PublicProfileFragment : Fragment() {

    private var _binding: FragmentPublicProfileBinding? = null
    private val binding get() = _binding!!
    private val database = FirebaseDatabase.getInstance()
    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPublicProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = arguments?.getString("userId")
        
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        
        loadUserData()
        loadStats()
        
        binding.btnMessage.setOnClickListener { startChat() }
        binding.btnHeaderChat.setOnClickListener { startChat() }
    }

    private fun loadUserData() {
        val uid = userId ?: return
        database.reference.child("Users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    binding.tvPublicName.text = it.fullName
                    binding.tvPublicRole.text = it.branch ?: "Student"
                    binding.tvPublicEmail.text = it.email
                    binding.tvPublicDept.text = it.branch ?: "General"
                    binding.tvPublicBio.text = it.bio ?: "No bio available."
                    
                    if (!it.profileImageUrl.isNullOrEmpty()) {
                        try {
                            val decodedByte = Base64.decode(it.profileImageUrl, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                            binding.ivPublicProfileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {}
                    }

                    // Load Skills
                    binding.publicSkillChipGroup.removeAllViews()
                    it.skills?.split(",")?.forEach { skill ->
                        if (skill.isNotBlank()) {
                            val chip = Chip(requireContext()).apply {
                                text = skill.trim()
                                setChipBackgroundColorResource(R.color.primaryRedLight)
                                setTextColor(requireContext().getColor(R.color.primaryRed))
                            }
                            binding.publicSkillChipGroup.addView(chip)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadStats() {
        val uid = userId ?: return
        // Skills count
        database.reference.child("Skills").orderByChild("studentId").equalTo(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding != null) binding.tvStatSkills.text = String.format("%02d", snapshot.childrenCount)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Ideas/Projects count
        database.reference.child("Ideas").orderByChild("studentId").equalTo(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                val count = snapshot.childrenCount
                binding.tvStatProjects.text = String.format("%02d", count)
                
                // Show first project if available
                if (count > 0) {
                    val firstProj = snapshot.children.first().child("title").value?.toString()
                    val firstDesc = snapshot.children.first().child("description").value?.toString()
                    binding.tvProjName.text = firstProj
                    binding.tvProjDesc.text = firstDesc
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        
        // Connections (placeholder logic)
        binding.tvStatConnections.text = "18"
        binding.tvStatCertificates.text = "03"
    }

    private fun startChat() {
        val uid = userId ?: return
        val name = binding.tvPublicName.text.toString()
        val bundle = Bundle().apply {
            putString("receiverId", uid)
            putString("receiverName", name)
        }
        findNavController().navigate(R.id.action_publicProfileFragment_to_chatFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
