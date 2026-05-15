package com.example.campusconnect.view

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusconnect.R
import com.example.campusconnect.databinding.FragmentSkillsBinding
import com.example.campusconnect.model.Skill
import com.example.campusconnect.repository.DataRepository
import com.example.campusconnect.view.adapter.SkillAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SkillsFragment : Fragment() {

    private var _binding: FragmentSkillsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var skillAdapter: SkillAdapter
    private val skillList = mutableListOf<Skill>()
    private val fullSkillList = mutableListOf<Skill>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSkillsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        
        if (auth.currentUser == null) {
            findNavController().navigate(R.id.action_skillsFragment_to_loginFragment)
            return
        }

        val user = DataRepository.currentUser
        binding.tvUserName.text = user?.fullName?.split(" ")?.get(0) ?: "Student"
        
        setupRecyclerView()
        fetchSkills()
        setupSearch()

        binding.btnAddSkill.setOnClickListener {
            showAddSkillDialog()
        }

        binding.btnSearch.setOnClickListener {
            if (binding.cvSearch.visibility == View.VISIBLE) {
                binding.cvSearch.visibility = View.GONE
                filterSkills("")
            } else {
                binding.cvSearch.visibility = View.VISIBLE
                binding.etSearchSkills.requestFocus()
            }
        }

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_skillsFragment_to_notificationsFragment)
        }
        
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        val uid = auth.currentUser?.uid ?: return
        database.reference.child("Notifications").child(uid).orderByChild("isRead").equalTo(false)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    val unreadCount = snapshot.childrenCount
                    binding.viewNotificationBadge.visibility = 
                        if (unreadCount > 0) View.VISIBLE else View.GONE
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupRecyclerView() {
        skillAdapter = SkillAdapter(
            skillList, 
            isProfilePage = false,
            onChatClick = { skill ->
                val bundle = Bundle().apply {
                    putString("receiverId", skill.studentId)
                    putString("receiverName", skill.studentName)
                }
                findNavController().navigate(R.id.action_skillsFragment_to_chatFragment, bundle)
            },
            onViewProfileClick = { userId ->
                val bundle = Bundle().apply {
                    putString("userId", userId)
                }
                findNavController().navigate(R.id.action_skillsFragment_to_publicProfileFragment, bundle)
            }
        )
        binding.rvSkills.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSkills.adapter = skillAdapter
    }

    private fun fetchSkills() {
        val skillsRef = database.reference.child("Skills")
        skillsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                fullSkillList.clear()
                for (skillSnapshot in snapshot.children) {
                    val skill = skillSnapshot.getValue(Skill::class.java)
                    if (skill != null && skill.status == "approved") {
                        fullSkillList.add(skill)
                    }
                }
                filterSkills(binding.etSearchSkills.text.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Failed to load skills", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupSearch() {
        binding.etSearchSkills.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSkills(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterSkills(query: String) {
        val filteredList = fullSkillList.filter { 
            it.skillName?.contains(query, ignoreCase = true) == true ||
            it.studentName?.contains(query, ignoreCase = true) == true ||
            it.category?.contains(query, ignoreCase = true) == true
        }
        skillList.clear()
        skillList.addAll(filteredList)
        skillAdapter.notifyDataSetChanged()
    }

    private fun showAddSkillDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add New Skill")
        val layout = LinearLayout(requireContext()).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val inputSkill = EditText(requireContext()).apply { hint = "Skill Name (e.g. Flutter Dev)" }
        val inputCategory = EditText(requireContext()).apply { hint = "Category (e.g. TECH)" }
        val inputDesc = EditText(requireContext()).apply { hint = "Description" }
        
        layout.addView(inputSkill)
        layout.addView(inputCategory)
        layout.addView(inputDesc)
        
        builder.setView(layout)
        builder.setPositiveButton("Add") { _, _ ->
            val name = inputSkill.text.toString().trim()
            val category = inputCategory.text.toString().trim().uppercase()
            val desc = inputDesc.text.toString().trim()
            
            if (name.isNotEmpty()) {
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                val user = DataRepository.currentUser
                val skillId = database.reference.child("Skills").push().key ?: return@setPositiveButton
                val skill = Skill(
                    id = skillId,
                    skillName = name,
                    studentName = user?.fullName,
                    studentMobile = user?.mobile,
                    studentEmail = user?.email,
                    studentId = userId,
                    description = desc,
                    category = category,
                    status = "approved"
                )
                database.reference.child("Skills").child(skillId).setValue(skill)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Skill added successfully", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
