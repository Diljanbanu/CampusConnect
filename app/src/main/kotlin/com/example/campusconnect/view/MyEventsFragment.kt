package com.example.campusconnect.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusconnect.R
import com.example.campusconnect.databinding.FragmentMyEventsBinding
import com.example.campusconnect.model.Event
import com.example.campusconnect.model.Poll
import com.example.campusconnect.repository.DataRepository
import com.example.campusconnect.view.adapter.MyRegistrationAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyEventsFragment : Fragment() {

    private var _binding: FragmentMyEventsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var adapter: MyRegistrationAdapter
    
    private val myRegisteredEvents = mutableListOf<Event>()
    private val displayList = mutableListOf<Event>()
    private val savedEvents = mutableListOf<Event>()
    private var currentTab = "Upcoming"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupRecyclerView()
        setupTabs()
        loadUserData()
        fetchRegisteredEvents()
        fetchFeedbackPoll()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupRecyclerView() {
        adapter = MyRegistrationAdapter(displayList) { event ->
            Toast.makeText(context, "Processing ${event.title}...", Toast.LENGTH_SHORT).show()
        }
        binding.rvRegistrations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRegistrations.adapter = adapter
    }

    private fun setupTabs() {
        val tabs = mapOf(
            binding.tabUpcoming to "Upcoming",
            binding.tabLive to "Live",
            binding.tabPast to "Past",
            binding.tabSaved to "Saved"
        )

        tabs.forEach { (view, name) ->
            view.setOnClickListener {
                currentTab = name
                updateTabUI(view)
                filterList()
            }
        }
    }

    private fun updateTabUI(selected: TextView) {
        val tabViews = listOf(binding.tabUpcoming, binding.tabLive, binding.tabPast, binding.tabSaved)
        tabViews.forEach { 
            it.setBackgroundResource(android.R.color.transparent)
            it.setTextColor(requireContext().getColor(R.color.gray_text))
            it.typeface = android.graphics.Typeface.DEFAULT
        }
        selected.setBackgroundResource(R.drawable.bg_white_rounded_card)
        selected.setTextColor(requireContext().getColor(R.color.black))
        selected.typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private fun loadUserData() {
        val user = DataRepository.currentUser
        binding.tvAttendanceRate.text = user?.attendanceRate ?: "0%"
        binding.tvCertificates.text = "${user?.certificatesEarned ?: 0} Verified"
    }

    private fun fetchRegisteredEvents() {
        val uid = auth.currentUser?.uid ?: return
        
        database.reference.child("Registrations").orderByChild("studentId").equalTo(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    val registeredIds = snapshot.children.mapNotNull { it.child("eventId").value.toString() }
                    
                    database.reference.child("Events").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(eventSnapshot: DataSnapshot) {
                            if (_binding == null) return
                            myRegisteredEvents.clear()
                            for (ds in eventSnapshot.children) {
                                val event = ds.getValue(Event::class.java)
                                if (event != null && registeredIds.contains(ds.key)) {
                                    myRegisteredEvents.add(event)
                                }
                            }
                            
                            val liveCount = myRegisteredEvents.count { it.eventStatus == "LIVE" }
                            binding.tabLive.text = "Live ($liveCount)"
                            
                            filterList()
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun filterList() {
        displayList.clear()
        when (currentTab) {
            "Upcoming" -> displayList.addAll(myRegisteredEvents.filter { it.eventStatus == "UPCOMING" })
            "Live" -> displayList.addAll(myRegisteredEvents.filter { it.eventStatus == "LIVE" })
            "Past" -> displayList.addAll(myRegisteredEvents.filter { it.eventStatus == "COMPLETED" })
            "Saved" -> displayList.addAll(savedEvents)
        }
        
        binding.tvEmpty.visibility = if (displayList.isEmpty()) View.VISIBLE else View.GONE
        adapter.notifyDataSetChanged()
    }

    private fun fetchFeedbackPoll() {
        database.reference.child("FeedbackPolls").orderByChild("active").equalTo(true).limitToLast(1)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null || !snapshot.exists()) return
                    val poll = snapshot.children.first().getValue(Poll::class.java)
                    poll?.let { bindPoll(it) }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun bindPoll(poll: Poll) {
        val b = binding.layoutPoll
        b.tvPollQuestion.text = poll.question
        
        if (poll.options.size >= 2) {
            b.tvOption1Title.text = poll.options[0].title
            b.tvOption1Percent.text = "${poll.options[0].votes}%"
            b.tvOption2Title.text = poll.options[1].title
            b.tvOption2Percent.text = "${poll.options[1].votes}%"
            
            b.btnOption1.setOnClickListener { vote(poll, 0) }
            b.btnOption2.setOnClickListener { vote(poll, 1) }
        }
    }

    private fun vote(poll: Poll, index: Int) {
        val uid = auth.currentUser?.uid ?: return
        val pollId = poll.id ?: return
        
        val updates = mutableMapOf<String, Any>()
        updates["FeedbackPolls/$pollId/options/$index/votes"] = poll.options[index].votes + 1
        updates["Users/$uid/votedFeedbackPolls/$pollId"] = index
        
        database.reference.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(requireContext(), "Feedback recorded", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
