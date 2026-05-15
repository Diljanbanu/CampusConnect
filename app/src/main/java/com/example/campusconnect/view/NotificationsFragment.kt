package com.example.campusconnect.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusconnect.databinding.FragmentNotificationsBinding
import com.example.campusconnect.model.Notification
import com.example.campusconnect.view.adapter.NotificationAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<Notification>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupRecyclerView()
        fetchNotifications()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnMarkAllRead.setOnClickListener {
            markAllAsRead()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notificationList) { notification ->
            markAsRead(notification)
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter
    }

    private fun fetchNotifications() {
        val uid = auth.currentUser?.uid ?: return
        database.reference.child("Notifications").child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    notificationList.clear()
                    for (ds in snapshot.children) {
                        val notification = ds.getValue(Notification::class.java)
                        if (notification != null) {
                            notificationList.add(notification)
                        }
                    }
                    notificationList.sortByDescending { it.timestamp }
                    adapter.notifyDataSetChanged()
                    
                    binding.tvNoNotifications.visibility = if (notificationList.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun markAsRead(notification: Notification) {
        val uid = auth.currentUser?.uid ?: return
        val id = notification.id ?: return
        database.reference.child("Notifications").child(uid).child(id).child("isRead").setValue(true)
    }

    private fun markAllAsRead() {
        val uid = auth.currentUser?.uid ?: return
        val ref = database.reference.child("Notifications").child(uid)
        ref.get().addOnSuccessListener { snapshot ->
            val updates = mutableMapOf<String, Any>()
            for (ds in snapshot.children) {
                updates["/${ds.key}/isRead"] = true
            }
            if (updates.isNotEmpty()) {
                ref.updateChildren(updates)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
