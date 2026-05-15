package com.example.campusconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.campusconnect.databinding.FragmentStartupDetailsBinding
import com.google.firebase.database.*

class StartUpDetailsFragment : Fragment() {

    private var _binding: FragmentStartupDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartupDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startup = arguments?.getSerializable("startup") as? Startup
        
        startup?.let { currentStartup ->
            binding.tvStartupTitle.text = currentStartup.title
            binding.tvStartupDesc.text = currentStartup.description
            binding.tvRequiredSkills.text = currentStartup.requiredSkills ?: "No specific skills mentioned"
            
            val startupId = currentStartup.id ?: return@let
            val teamRef = FirebaseDatabase.getInstance().reference.child("Ideas").child(startupId).child("teamMembers")
            
            teamRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    
                    // Update Count
                    val joinedCount = snapshot.childrenCount
                    binding.tvTeamCount.text = "$joinedCount of 4 joined"
                    
                    // Clear and Populate Team List
                    binding.llTeamMembers.removeAllViews()
                    for (memberSnapshot in snapshot.children) {
                        val member = memberSnapshot.getValue(TeamMember::class.java)
                        if (member != null) {
                            addMemberToLayout(member)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun addMemberToLayout(member: TeamMember) {
        val memberView = LayoutInflater.from(requireContext()).inflate(R.layout.item_team_member, binding.llTeamMembers, false)
        
        val tvName = memberView.findViewById<TextView>(R.id.tvMemberName)
        val tvRole = memberView.findViewById<TextView>(R.id.tvMemberRole)
        val ivAvatar = memberView.findViewById<ImageView>(R.id.ivMemberAvatar)
        
        tvName.text = member.name
        tvRole.text = member.role
        
        if (!member.imageUrl.isNullOrEmpty()) {
            Glide.with(requireContext()).load(member.imageUrl).circleCrop().into(ivAvatar)
        } else {
            ivAvatar.setImageResource(android.R.drawable.ic_menu_report_image)
        }
        
        binding.llTeamMembers.addView(memberView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}