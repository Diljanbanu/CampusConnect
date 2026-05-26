package com.example.campusconnect.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusconnect.R
import com.example.campusconnect.databinding.FragmentMySkillsBinding
import com.example.campusconnect.model.MentorshipRecord
import com.example.campusconnect.model.Skill
import com.example.campusconnect.view.adapter.MentorshipListAdapter
import com.example.campusconnect.view.adapter.MySkillCardAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MySkillsFragment : Fragment() {

    private var _binding: FragmentMySkillsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    
    private val mySkills = mutableListOf<Skill>()
    private val mentoringList = mutableListOf<MentorshipRecord>()
    
    private lateinit var skillsAdapter: MySkillCardAdapter
    private lateinit var mentoringAdapter: MentorshipListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMySkillsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        
        setupRecyclerViews()
        loadData()
        
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnAddSkill.setOnClickListener {
            // Implementation for adding a skill
            Toast.makeText(context, "Feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerViews() {
        skillsAdapter = MySkillCardAdapter(
            mySkills,
            onEditClick = { skill -> /* Edit logic */ },
            onDeleteClick = { skill -> deleteSkill(skill) }
        )
        binding.rvMySkills.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMySkills.adapter = skillsAdapter

        mentoringAdapter = MentorshipListAdapter(mentoringList)
        binding.rvMentoringList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMentoringList.adapter = mentoringAdapter
    }

    private fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        
        // 1. Load My Skills
        database.reference.child("Skills").orderByChild("studentId").equalTo(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    mySkills.clear()
                    var topSkillName = "None"
                    var maxRating = 0f
                    var recentlyAdded = "None"
                    var mostViewed = "None"
                    var maxViews = -1
                    
                    val list = mutableListOf<Skill>()
                    for (ds in snapshot.children) {
                        val skill = ds.getValue(Skill::class.java)
                        if (skill != null) {
                            list.add(skill)
                        }
                    }
                    
                    // Sort by timestamp for recently added
                    list.sortByDescending { it.timestamp ?: 0L }
                    if (list.isNotEmpty()) {
                        recentlyAdded = list.first().skillName ?: "None"
                    }
                    
                    for (skill in list) {
                        mySkills.add(skill)
                        if (skill.rating > maxRating) {
                            maxRating = skill.rating
                            topSkillName = skill.skillName ?: "None"
                        }
                        if (skill.views > maxViews) {
                            maxViews = skill.views
                            mostViewed = skill.skillName ?: "None"
                        }
                    }
                    
                    binding.tvTopSkill.text = topSkillName
                    binding.tvRecentlyAdded.text = recentlyAdded
                    binding.tvMostViewed.text = mostViewed
                    
                    skillsAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
            
        // 2. Load Mentorship Records
        database.reference.child("Mentorships").orderByChild("mentorId").equalTo(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    mentoringList.clear()
                    for (ds in snapshot.children) {
                        ds.getValue(MentorshipRecord::class.java)?.let { mentoringList.add(it) }
                    }
                    
                    binding.tvTotalEngaged.text = mentoringList.size.toString()
                    mentoringAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
            
        // 3. Load Interaction Chats Count
        database.reference.child("Chats").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                var chatCount = 0
                for (roomSnapshot in snapshot.children) {
                    if (roomSnapshot.key?.contains(uid) == true) {
                        chatCount += roomSnapshot.childrenCount.toInt()
                    }
                }
                binding.tvInteractions.text = "$chatCount Chats"
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun deleteSkill(skill: Skill) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Skill")
            .setMessage("Are you sure you want to remove '${skill.skillName}'?")
            .setPositiveButton("Delete") { _, _ ->
                skill.id?.let { id ->
                    database.reference.child("Skills").child(id).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Skill deleted", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
