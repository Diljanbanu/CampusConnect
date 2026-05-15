package com.example.campusconnect

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusconnect.databinding.FragmentHomeBinding
import com.example.campusconnect.repository.DataRepository

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var skillAdapter: SkillAdapter
    private val displaySkillList = mutableListOf<Skill>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupDashboardLists()
        setupSearch()
    }

    private fun setupUI() {
        val user = DataRepository.currentUser
        binding.tvUserName.text = user?.fullName?.split(" ")?.get(0) ?: "Student"

        binding.btnSeeAllEvents.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_eventsFragment)
        }
        
        val featuredEvent = DataRepository.allApprovedEvents.firstOrNull { it.status == "approved" }
        if (featuredEvent != null) {
            binding.tvFeaturedEventTitle.text = featuredEvent.title
            binding.tvFeaturedEventDate.text = featuredEvent.date
            binding.tvFeaturedEventLocation.text = featuredEvent.location
        } else {
            binding.tvFeaturedEventTitle.text = "Annual Convocation"
            binding.tvFeaturedEventDate.text = "24 Oct"
            binding.tvFeaturedEventLocation.text = "RKU Dome"
        }

        binding.btnSearch.setOnClickListener {
            if (binding.cvHomeSearch.visibility == View.VISIBLE) {
                binding.cvHomeSearch.visibility = View.GONE
                filterHomeSkills("")
            } else {
                binding.cvHomeSearch.visibility = View.VISIBLE
                binding.etHomeSearch.requestFocus()
            }
        }
        
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_notificationsFragment)
        }
        
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        val uid = DataRepository.currentUser?.uid ?: return
        com.google.firebase.database.FirebaseDatabase.getInstance().reference
            .child("Notifications").child(uid).orderByChild("isRead").equalTo(false)
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (_binding == null) return
                    val unreadCount = snapshot.childrenCount
                    binding.viewNotificationBadge.visibility =
                        if (unreadCount > 0) View.VISIBLE else View.GONE
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
    }

    private fun setupDashboardLists() {
        displaySkillList.clear()
        displaySkillList.addAll(DataRepository.allApprovedSkills.sortedByDescending { it.id }.take(5))

        skillAdapter = SkillAdapter(
            displaySkillList,
            isProfilePage = false,
            onViewProfileClick = { uid ->
                val bundle = Bundle().apply { putString("userId", uid) }
                findNavController().navigate(R.id.action_homeFragment_to_profileFragment, bundle)
            }
        )
        binding.rvLatestSkills.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLatestSkills.adapter = skillAdapter
        binding.rvLatestSkills.isNestedScrollingEnabled = false
    }

    private fun setupSearch() {
        binding.etHomeSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterHomeSkills(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterHomeSkills(query: String) {
        val filtered = if (query.isEmpty()) {
            DataRepository.allApprovedSkills.sortedByDescending { it.id }.take(5)
        } else {
            DataRepository.allApprovedSkills.filter {
                it.skillName?.contains(query, ignoreCase = true) == true ||
                it.studentName?.contains(query, ignoreCase = true) == true
            }
        }
        displaySkillList.clear()
        displaySkillList.addAll(filtered)
        skillAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
