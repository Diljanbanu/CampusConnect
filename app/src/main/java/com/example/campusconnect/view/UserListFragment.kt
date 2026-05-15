package com.example.campusconnect.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusconnect.R
import com.example.campusconnect.databinding.FragmentUserListBinding
import com.example.campusconnect.model.User
import com.example.campusconnect.view.adapter.ContactAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class UserListFragment : Fragment() {

    private var _binding: FragmentUserListBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var contactAdapter: ContactAdapter
    private val allUsers = mutableListOf<User>()
    private val filteredList = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupRecyclerView()
        fetchAllUsers()
        setupSearch()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter(filteredList) { user ->
            val bundle = Bundle().apply {
                putString("receiverId", user.uid)
                putString("receiverName", user.fullName)
            }
            findNavController().navigate(R.id.action_userListFragment_to_chatFragment, bundle)
        }
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = contactAdapter
    }

    private fun fetchAllUsers() {
        val currentUserId = auth.currentUser?.uid ?: return
        database.reference.child("Users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                allUsers.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    if (user != null && user.uid != currentUserId) {
                        allUsers.add(user)
                    }
                }
                allUsers.sortBy { it.fullName?.lowercase() ?: "" }
                filter("")
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupSearch() {
        binding.etSearchUsers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filter(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(allUsers)
        } else {
            val lowerQuery = query.lowercase(Locale.getDefault())
            for (user in allUsers) {
                if (user.fullName?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true) {
                    filteredList.add(user)
                }
            }
        }
        binding.tvContactCount.text = "${filteredList.size} contacts"
        contactAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
