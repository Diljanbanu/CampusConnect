package com.example.campusconnect.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusconnect.R
import com.example.campusconnect.databinding.FragmentChatTabBinding
import com.example.campusconnect.model.ChatListItem
import com.example.campusconnect.model.Message
import com.example.campusconnect.model.User
import com.example.campusconnect.view.adapter.UserAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatTabFragment : Fragment() {

    private var _binding: FragmentChatTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userAdapter: UserAdapter
    private val activeUserList = mutableListOf<User>()
    private val filteredUserList = mutableListOf<User>()
    private val lastMessageTimes = mutableMapOf<String, Long>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupRecyclerView()
        fetchActiveChats()
    }

    fun filter(query: String) {
        filteredUserList.clear()
        if (query.isEmpty()) {
            filteredUserList.addAll(activeUserList)
        } else {
            val lowerQuery = query.lowercase()
            for (user in activeUserList) {
                if (user.fullName?.lowercase()?.contains(lowerQuery) == true) {
                    filteredUserList.add(user)
                }
            }
        }
        userAdapter.notifyDataSetChanged()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(filteredUserList) { user ->
            val bundle = Bundle().apply {
                putString("receiverId", user.uid)
                putString("receiverName", user.fullName)
            }
            findNavController().navigate(R.id.action_chatListFragment_to_chatFragment, bundle)
        }
        binding.rvChatList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChatList.adapter = userAdapter
    }

    private fun fetchActiveChats() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        database.reference.child("Chats").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                
                val activeIds = mutableSetOf<String>()
                lastMessageTimes.clear()

                for (roomSnapshot in snapshot.children) {
                    val roomId = roomSnapshot.key ?: continue
                    if (roomId.contains(currentUserId)) {
                        val otherId = roomId.replace(currentUserId, "")
                        activeIds.add(otherId)
                        
                        var latestTime = 0L
                        for (msgSnapshot in roomSnapshot.children) {
                            val time = msgSnapshot.child("timestamp").value as? Long ?: 0L
                            if (time > latestTime) latestTime = time
                        }
                        lastMessageTimes[otherId] = latestTime
                    }
                }
                fetchUsersDetails(activeIds)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchUsersDetails(activeIds: Set<String>) {
        database.reference.child("Users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                activeUserList.clear()
                
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    if (user != null && activeIds.contains(user.uid)) {
                        activeUserList.add(user)
                    }
                }
                
                if (activeUserList.isEmpty()) {
                    activeUserList.add(User(uid = "group1", fullName = "UI/UX Community"))
                }
                
                activeUserList.sortByDescending { lastMessageTimes[it.uid] ?: 0L }
                filter("")
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
