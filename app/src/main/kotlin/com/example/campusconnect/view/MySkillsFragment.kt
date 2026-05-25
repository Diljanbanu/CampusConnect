package com.example.campusconnect.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            // Future implementation for adding a skill from here
        }
    }

    private fun setupRecyclerViews() {
        skillsAdapter = MySkillCardAdapter(mySkills)
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
                    var highestRating = 0f
                    var highestRatedSkillName = "None"
                    
                    for (ds in snapshot.children) {
                        val skill = ds.getValue(Skill::class.java)
                        if (skill != null) {
                            mySkills.add(skill)
                            if (skill.rating > highestRating) {
                                highestRating = skill.rating
                                highestRatedSkillName = skill.skillName ?: "None"
                            }
                        }
                    }
                    
                    if (highestRating > 0) {
                        binding.tvHighestRatedSkill.text = "$highestRatedSkillName (${String.format("%.1f", highestRating)}★)"
                    }
                    
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
                    
                    binding.tvTotalMentored.text = "${mentoringList.size}+"
                    mentoringAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
