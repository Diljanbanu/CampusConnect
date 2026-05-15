package com.example.campusconnect.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusconnect.R
import com.example.campusconnect.databinding.FragmentEventsBinding
import com.example.campusconnect.model.Event
import com.example.campusconnect.repository.DataRepository
import com.example.campusconnect.util.NotificationHelper
import com.example.campusconnect.view.adapter.EventAdapter
import com.example.campusconnect.view.adapter.FeaturedEventAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    
    private lateinit var upcomingAdapter: EventAdapter
    private val upcomingList = mutableListOf<Event>()
    
    private lateinit var registeredAdapter: EventAdapter
    private val myRegisteredEvents = mutableListOf<Event>()
    private val currentRegisteredDisplayList = mutableListOf<Event>()
    
    private lateinit var featuredAdapter: FeaturedEventAdapter
    private val featuredList = mutableListOf<Event>()
    
    private val allApprovedEvents = mutableListOf<Event>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupUI()
        setupRecyclerViews()
        fetchEvents()
        updateNotificationBadge()
        setupSearch()
        
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_notificationsFragment)
        }
    }

    private fun setupUI() {
        val user = DataRepository.currentUser
        binding.tvUserName.text = user?.fullName?.split(" ")?.get(0) ?: "Student"
        
        binding.tabUpcoming.setOnClickListener { selectTab(binding.tabUpcoming) }
        binding.tabLive.setOnClickListener { selectTab(binding.tabLive) }
        binding.tabPast.setOnClickListener { selectTab(binding.tabPast) }
        
        for (i in 0 until binding.llCategories.childCount) {
            val child = binding.llCategories.getChildAt(i)
            child.setOnClickListener {
                resetCategoryTints()
                if (child is TextView) {
                    child.setBackgroundResource(R.drawable.bg_category_selected)
                    child.setTextColor(requireContext().getColor(R.color.white))
                    filterByMainCategory(child.text.toString())
                } else if (child is ViewGroup) {
                    child.setBackgroundResource(R.drawable.bg_category_selected)
                    filterByMainCategory("Live Now")
                }
            }
        }
    }

    private fun resetCategoryTints() {
        for (i in 0 until binding.llCategories.childCount) {
            val child = binding.llCategories.getChildAt(i)
            child.setBackgroundResource(R.drawable.bg_category_item)
            if (child is TextView) {
                child.setTextColor(requireContext().getColor(R.color.gray_text))
            }
        }
    }

    private fun filterByMainCategory(category: String) {
        upcomingList.clear()
        val filtered = when (category) {
            "All" -> allApprovedEvents.filter { it.eventStatus == "UPCOMING" }
            "Upcoming" -> allApprovedEvents.filter { it.eventStatus == "UPCOMING" }
            "Live Now" -> allApprovedEvents.filter { it.eventStatus == "LIVE" }
            else -> allApprovedEvents.filter { it.category == category && it.eventStatus == "UPCOMING" }
        }
        upcomingList.addAll(filtered)
        upcomingAdapter.notifyDataSetChanged()
    }

    private fun selectTab(selected: TextView) {
        val tabs = listOf(binding.tabUpcoming, binding.tabLive, binding.tabPast)
        tabs.forEach { 
            it.setBackgroundResource(R.drawable.bg_category_item)
            it.setTextColor(requireContext().getColor(R.color.gray_text))
        }
        
        selected.setBackgroundResource(R.drawable.bg_category_selected)
        selected.setTextColor(requireContext().getColor(R.color.white))
        
        filterRegisteredList(selected.id)
    }

    private fun setupRecyclerViews() {
        featuredAdapter = FeaturedEventAdapter(featuredList)
        binding.vpFeaturedEvents.adapter = featuredAdapter
        TabLayoutMediator(binding.tlIndicator, binding.vpFeaturedEvents) { _, _ -> }.attach()

        upcomingAdapter = EventAdapter(
            upcomingList,
            onRegisterClick = { registerForEvent(it) }
        )
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = upcomingAdapter

        registeredAdapter = EventAdapter(currentRegisteredDisplayList)
        binding.rvMyRegisteredEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyRegisteredEvents.adapter = registeredAdapter
    }

    private fun fetchEvents() {
        val uid = auth.currentUser?.uid ?: return
        
        database.reference.child("Events").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                allApprovedEvents.clear()
                upcomingList.clear()
                featuredList.clear()
                
                for (ds in snapshot.children) {
                    val event = ds.getValue(Event::class.java)
                    if (event != null && event.status == "approved") {
                        allApprovedEvents.add(event)
                        if (event.eventStatus == "UPCOMING") {
                            upcomingList.add(event)
                        }
                        if (event.isFeatured) {
                            featuredList.add(event)
                        }
                    }
                }
                
                if (featuredList.isEmpty() && allApprovedEvents.isNotEmpty()) {
                    featuredList.add(allApprovedEvents.first())
                }
                
                featuredAdapter.notifyDataSetChanged()
                upcomingAdapter.notifyDataSetChanged()
                fetchUserRegistrations(uid)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchUserRegistrations(uid: String) {
        database.reference.child("Registrations").orderByChild("studentId").equalTo(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    val registeredIds = snapshot.children.mapNotNull { it.child("eventId").value.toString() }
                    
                    myRegisteredEvents.clear()
                    myRegisteredEvents.addAll(allApprovedEvents.filter { registeredIds.contains(it.id) })
                    
                    updateCountsAndTabs()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateCountsAndTabs() {
        val upcomingCount = myRegisteredEvents.count { it.eventStatus == "UPCOMING" }
        val liveCount = myRegisteredEvents.count { it.eventStatus == "LIVE" }
        val pastCount = myRegisteredEvents.count { it.eventStatus == "COMPLETED" }

        binding.tabUpcoming.text = "Upcoming ($upcomingCount)"
        binding.tabLive.text = "Live ($liveCount)"
        binding.tabPast.text = "Past ($pastCount)"
        
        binding.tvRegistrationCount.text = "My Registrations\n${String.format("%02d", myRegisteredEvents.size)} Events"
        binding.tvReminderCount.text = "Reminders\n${String.format("%02d", upcomingCount)} Upcoming"
        
        filterRegisteredList(binding.tabUpcoming.id)
    }

    private fun filterRegisteredList(tabId: Int) {
        val status = when (tabId) {
            binding.tabUpcoming.id -> "UPCOMING"
            binding.tabLive.id -> "LIVE"
            else -> "COMPLETED"
        }
        
        currentRegisteredDisplayList.clear()
        currentRegisteredDisplayList.addAll(myRegisteredEvents.filter { it.eventStatus == status })
        registeredAdapter.notifyDataSetChanged()
    }

    private fun registerForEvent(event: Event) {
        val uid = auth.currentUser?.uid ?: return
        
        database.reference.child("Registrations").orderByChild("studentId").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val alreadyRegistered = snapshot.children.any { it.child("eventId").value == event.id }
                    if (alreadyRegistered) {
                        Toast.makeText(requireContext(), "Already registered!", Toast.LENGTH_SHORT).show()
                    } else {
                        performRegistration(uid, event)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun performRegistration(uid: String, event: Event) {
        val regId = database.reference.child("Registrations").push().key ?: return
        val registration = mapOf(
            "id" to regId,
            "eventId" to event.id,
            "studentId" to uid,
            "timestamp" to ServerValue.TIMESTAMP
        )
        
        database.reference.child("Registrations").child(regId).setValue(registration)
            .addOnSuccessListener {
                event.id?.let { id ->
                    database.reference.child("Events").child(id).child("registeredCount")
                        .setValue((event.registeredCount ?: 0) + 1)
                }
                Toast.makeText(requireContext(), "Successfully registered for ${event.title}", Toast.LENGTH_SHORT).show()
                NotificationHelper.sendNotification(uid, "Event Registered", "See you at ${event.title}!", "event")
            }
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            binding.etSearchEvents.visibility = if (binding.etSearchEvents.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        binding.etSearchEvents.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterEvents(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterEvents(query: String) {
        val filtered = allApprovedEvents.filter {
            it.title?.contains(query, ignoreCase = true) == true ||
            it.location?.contains(query, ignoreCase = true) == true ||
            it.description?.contains(query, ignoreCase = true) == true
        }
        upcomingList.clear()
        upcomingList.addAll(filtered.filter { it.eventStatus == "UPCOMING" })
        upcomingAdapter.notifyDataSetChanged()
    }

    private fun updateNotificationBadge() {
        val uid = auth.currentUser?.uid ?: return
        database.reference.child("Notifications").child(uid).orderByChild("isRead").equalTo(false)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    binding.viewNotificationBadge.visibility = 
                        if (snapshot.childrenCount > 0) View.VISIBLE else View.GONE
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
